package com.materialkolor.builder.feature.image

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.platform.DecodedImage
import com.materialkolor.builder.core.platform.ImageHandle
import com.materialkolor.builder.core.platform.ImageInput
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.engine.image.PixelSample
import com.materialkolor.builder.engine.image.SeedExtractor
import dev.stateholder.extensions.viewmodel.StateViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Takes a seed from an image, picked, dropped or pasted.
 *
 * Each image shows skeleton chips at once, its thumbnail once it decodes, and its candidates once
 * the extractor has scored them, with a `yield()` between the stages so a frame can land. The top
 * candidate then comes out of [results] for the workspace to seed from, as one undo entry. A newer
 * image cancels the job of the one before it, so the older seed never lands.
 *
 * Only the newest image stays in memory, and only for the project it came in on. Nothing here is
 * saved. The document keeps the file name and the candidates, which is all a reload has to go on.
 *
 * Drops and pastes are left alone while the color picker is open, since the picker owns the seed
 * then. A pasted text is not an image and is left for whatever reads pasted text.
 *
 * @property[images] Where picks, drops and decodes come from. The Image button calls its `pick`
 * straight from the click, with no hop through here, since browsers only open the picker there.
 */
@Stable
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class ImageSeedModel(
    val images: ImageInput,
    pastes: PasteInput,
) : StateViewModel<ImageSeedModel.State>(State()) {
    private val emitted = Channel<ImageSeedResult>(Channel.UNLIMITED)

    /**
     * What each image came to, for the workspace to act on once.
     */
    val results: Flow<ImageSeedResult> = emitted.receiveAsFlow()

    private var job: Job? = null
    private var arrivals = 0
    private var picking = false
    private var project = 0

    init {
        viewModelScope.launch {
            images.drops.collect { handle -> if (!picking) take(handle) }
        }
        viewModelScope.launch {
            pastes.pastes.collect { paste ->
                if (paste is Paste.Files && !picking) take(paste.files)
            }
        }
    }

    /**
     * Pull colors from [handle], cancelling whatever image came before it.
     */
    fun take(handle: ImageHandle) {
        take(listOf(handle))
    }

    /**
     * Pull colors from the last of [handles] that reads as an image, cancelling whatever image came
     * before them. Several files come from one paste, and a file that is not an image among them
     * never costs the seed the others could give.
     */
    private fun take(handles: List<ImageHandle>) {
        val last = handles.lastOrNull() ?: return
        job?.cancel()
        val arriving = ArrivingImage(id = ++arrivals, name = last.name ?: "")
        updateState { state -> state.copy(arriving = arriving) }
        job = viewModelScope.launch { read(handles, arriving) }
    }

    /**
     * Keep up with the workspace. While [picking] the color picker is open and owns the seed, so
     * drops and pastes are left alone and an image still on its way is stopped before it lands in
     * the picker's edit. A new [project] drops the newest image and stops the one on its way.
     */
    fun follow(
        picking: Boolean,
        project: Int,
    ) {
        val opened = picking && !this.picking
        this.picking = picking
        if (project != this.project) {
            this.project = project
            job?.cancel()
            updateState { State() }
        } else if (opened) {
            job?.cancel()
            updateState { state -> state.copy(arriving = null) }
        }
    }

    /**
     * The document now shows [arriving]'s seed, so its skeleton can go.
     */
    fun landed(arriving: ArrivingImage) {
        updateState { state -> if (state.arriving == arriving) state.copy(arriving = null) else state }
    }

    private suspend fun read(
        handles: List<ImageHandle>,
        arriving: ArrivingImage,
    ) {
        val (handle, decoded) = decodeLast(handles) ?: run {
            updateState { state -> state.copy(arriving = null) }
            emitted.send(ImageSeedResult.Unsupported)
            return
        }
        val shown = arriving.copy(name = handle.name ?: "", thumbnail = decoded.thumbnail)
        updateState { state -> state.copy(arriving = shown) }
        yield()
        val seeds = SeedExtractor.extract(PixelSample(decoded.pixels, decoded.width, decoded.height))
        yield()
        val source = SeedSource.Image(shown.name, seeds.candidates)
        val newest = NewestImage(
            source = source,
            thumbnail = decoded.thumbnail,
            detail = decoded.detail,
            mostlyGray = seeds.mostlyGray,
            project = project,
        )
        val landing = shown.copy(lands = source)
        updateState { state -> state.copy(arriving = landing, newest = newest) }
        emitted.send(ImageSeedResult.Seeded(seeds.candidates.first(), source))
        // The seed lands behind a reveal a frame or so later. Should the workspace never say so, the
        // skeleton still goes.
        delay(LANDING_TIMEOUT_MILLIS)
        landed(landing)
    }

    /**
     * The last of [handles] that decodes, with its image, or null when none of them does.
     */
    private suspend fun decodeLast(handles: List<ImageHandle>): Pair<ImageHandle, DecodedImage>? {
        for (handle in handles.asReversed()) {
            val decoded = images.decode(handle) ?: continue
            return handle to decoded
        }
        return null
    }

    /**
     * @property[arriving] The image on its way, or null when none is.
     * @property[newest] The newest image that gave a seed, or null when there is none.
     */
    @Immutable
    data class State(
        val arriving: ArrivingImage? = null,
        val newest: NewestImage? = null,
    ) {
        /**
         * This state as the open project sees it, with an image from another project left out.
         */
        fun forProject(project: Int): State =
            if (newest == null || newest.project == project) this else copy(newest = null)
    }

    private companion object {
        /**
         * Longer than any reveal waits before its change lands.
         */
        const val LANDING_TIMEOUT_MILLIS = 1_000L
    }
}

/**
 * An image on its way to a seed.
 *
 * @property[id] Tells one arrival from the next, even for the same file.
 * @property[name] The file name, empty when the platform gave none. Among several pasted files it is
 * the last one's until one of them has decoded.
 * @property[thumbnail] The image at up to 256 px, once it has decoded.
 * @property[lands] The seed source it sets, once its candidates are known.
 */
@Immutable
internal data class ArrivingImage(
    val id: Int,
    val name: String,
    val thumbnail: ImageBitmap? = null,
    val lands: SeedSource.Image? = null,
)

/**
 * The newest image that gave a seed, kept in memory only.
 *
 * @property[source] What the document says of it, the file name and the candidates.
 * @property[thumbnail] The image at up to 256 px.
 * @property[detail] The image at up to 1024 px, for picking an exact pixel.
 * @property[mostlyGray] Whether gray covers most of it, so its candidates look muted.
 * @property[project] The project it came in on.
 */
@Immutable
internal data class NewestImage(
    val source: SeedSource.Image,
    val thumbnail: ImageBitmap,
    val detail: ImageBitmap,
    val mostlyGray: Boolean,
    val project: Int,
)

/**
 * What one image came to.
 */
internal sealed interface ImageSeedResult {
    /**
     * The image gave candidates and [top] leads them.
     *
     * @property[source] The seed source that names the file and keeps every candidate.
     */
    data class Seeded(
        val top: Argb,
        val source: SeedSource.Image,
    ) : ImageSeedResult {
        /**
         * The edit that makes [top] the seed.
         */
        val change: DocumentChange.SetSeed
            get() = DocumentChange.SetSeed(top, source)
    }

    /**
     * The file is not an image the platform can read, or too big to read safely.
     */
    data object Unsupported : ImageSeedResult
}
