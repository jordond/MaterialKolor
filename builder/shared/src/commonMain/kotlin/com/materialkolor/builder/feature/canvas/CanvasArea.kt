package com.materialkolor.builder.feature.canvas

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.SplitState
import dev.stateholder.dispatcher.Dispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull

/**
 * The color vision the canvas simulates. Nothing remembers it across a reload.
 */
internal enum class VisionSimulation {
    None,
    Protanopia,
    Deuteranopia,
    Tritanopia,
    Achromatopsia,
}

/**
 * What the canvas shows for this simulation, grayscale while B is [held].
 */
internal fun VisionSimulation.whileHeld(held: Boolean): VisionSimulation =
    if (held) VisionSimulation.Achromatopsia else this

/**
 * The preview and its tabs, the active tab kept clear of [contentPadding].
 *
 * The tabs run along the top and only the one showing composes. App and Components each get one
 * [DemoAppState], made here above the tab switch, so both copies of a split share it and it lives
 * through a tab switch. On a phone the app is always framed as a phone.
 */
@Composable
internal fun CanvasArea(
    state: WorkspaceModel.State,
    contentPadding: PaddingValues,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val appState = remember { DemoAppState() }
    val componentsState = remember { DemoAppState() }
    val preview = rememberPreviewSplit(state.view.mode, state.view.splitFraction) { fraction ->
        dispatcher.dispatch(WorkspaceAction.SetSplitFraction(fraction))
    }
    val specs = rememberPaneSpecs(state.vision.whileHeld(state.grayscaleHeld))
    val compact = LocalLayout.current.windowClass == WindowClass.Compact
    // The tabs start at the canvas's own inset, as the preview window below them does.
    val inset = canvasInset(compact)
    Column(modifier.fillMaxSize()) {
        CanvasTabs(
            selected = state.view.tab,
            onSelect = { tab -> dispatcher.dispatch(WorkspaceAction.SetPreviewTab(tab)) },
            modifier = Modifier.padding(start = inset, end = inset, top = inset),
        )
        InspectLayer(state, preview, dispatcher, Modifier.weight(1f).fillMaxWidth().padding(contentPadding)) {
            CanvasTabTransition(state.view.tab, Modifier.fillMaxSize()) { tab ->
                CanvasTabBody(
                    tab = tab,
                    mode = state.view.mode,
                    preview = preview,
                    specs = specs,
                    appState = appState,
                    componentsState = componentsState,
                    dispatcher = dispatcher,
                    rampHighlight = state.rampHighlight,
                    generation = state.projectGeneration,
                    deviceWidth = if (compact) DeviceWidth.Phone else state.view.deviceWidth,
                )
            }
        }
    }
}

/**
 * The dock under the preview with preview mode, device width, inspect and vision, drawn in a
 * `DockRegion`.
 */
@Composable
internal fun CanvasDock(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    DockContent(state, dispatcher, modifier)
}

/**
 * How many unconfirmed saves of the handle to keep before the oldest is dropped.
 */
private const val MAX_UNCONFIRMED = 32

/**
 * How long the handle has to rest before where it rests is saved, so a drag saves once.
 */
internal const val HANDLE_SETTLE_MILLIS = 250L

/**
 * Where the split handle sits and what the canvas composes for the preview mode.
 *
 * Light and Dark compose one copy. A switch first slides the handle to the matching edge, the end
 * edge for Light so the light copy fills the canvas, or back to where Split left it, and only then
 * drops the copy that slid out of view. Moves of the handle in Split are handed on to be saved. A
 * saved fraction that did not start here, as when another project opens, moves the handle, and
 * steers a slide that is under way.
 *
 * @param[mode] The mode the canvas opens in.
 * @param[saved] Where the handle was saved.
 */
@Stable
internal class PreviewSplit(
    mode: PreviewMode,
    saved: Float,
) {
    /**
     * The handle both copies of a split read.
     */
    val split: SplitState = SplitState(handleFor(mode, saved))

    /**
     * What the canvas composes, Split for as long as the handle slides.
     */
    var shown: PreviewMode by mutableStateOf(mode)
        private set

    private var sliding by mutableStateOf(false)

    /**
     * Where Split puts the handle, the last fraction saved or sent to be saved.
     */
    private var handle = saved

    /**
     * Fractions sent to be saved that have not come back as saved yet, oldest first.
     */
    private val unconfirmed = ArrayDeque<Float>()

    /**
     * Counts the saved fractions that did not start here. A save waiting on an older count is stale.
     */
    private var foreignSaves: Int = 0

    /**
     * Counts the pointer releases over the canvas, each a cue to save a moved handle at once.
     */
    private var releases by mutableIntStateOf(0)

    /**
     * Slide the handle to where [mode] keeps it over [spec], or jump there when it is null.
     *
     * The slide reads where [mode] keeps the handle on every frame, so a fraction saved while it
     * runs, as when another project opens with another mode, is where it ends.
     */
    suspend fun slideTo(
        mode: PreviewMode,
        spec: AnimationSpec<Float>?,
    ) {
        if (spec == null || (shown == mode && split.fraction == handleFor(mode, handle))) {
            split.fraction = handleFor(mode, handle)
            shown = mode
            sliding = false
            return
        }
        // A slide cut short leaves sliding on, so the one that replaces it never saves a halfway handle.
        sliding = true
        if (shown != PreviewMode.Split) split.fraction = handleFor(shown, handle)
        shown = PreviewMode.Split
        val from = split.fraction
        animate(0f, 1f, animationSpec = spec) { progress, _ ->
            split.fraction = from + (handleFor(mode, handle) - from) * progress
        }
        split.fraction = handleFor(mode, handle)
        shown = mode
        sliding = false
    }

    /**
     * Where the handle rests in Split, or null while it slides or one copy shows.
     */
    fun restingHandle(): Float? = if (sliding || shown != PreviewMode.Split) null else split.fraction

    /**
     * True when [fraction] is news to be saved, which it then counts as sent.
     */
    fun send(fraction: Float): Boolean {
        if (fraction == handle) return false
        handle = fraction
        if (unconfirmed.size == MAX_UNCONFIRMED) unconfirmed.removeFirst()
        unconfirmed.addLast(fraction)
        return true
    }

    /**
     * A mark to hold a waiting save against, which [savedElsewhereSince] checks when the wait is over.
     */
    fun mark(): Int = foreignSaves

    /**
     * True when a fraction saved elsewhere came in after [mark] was taken, which makes a save waiting on it stale.
     */
    fun savedElsewhereSince(mark: Int): Boolean = foreignSaves != mark

    /**
     * A pointer let go over the canvas. The handle, if it rests somewhere new, is saved a frame later.
     */
    fun release() {
        releases++
    }

    /**
     * The release count, read in a snapshot flow so each release is heard once the frame is applied.
     */
    fun releaseCount(): Int = releases

    /**
     * The saved fraction is now [saved]. Unless it is one sent from here, the handle goes there.
     */
    fun onSaved(saved: Float) {
        val sent = unconfirmed.indexOf(saved)
        if (sent >= 0) {
            repeat(sent + 1) { unconfirmed.removeFirst() }
            return
        }
        if (saved == handle) return
        unconfirmed.clear()
        foreignSaves++
        handle = saved
        if (shown == PreviewMode.Split && !sliding) split.fraction = saved
    }
}

/**
 * Where [mode] keeps the handle, [split] for Split.
 */
private fun handleFor(
    mode: PreviewMode,
    split: Float,
): Float =
    when (mode) {
        PreviewMode.Light -> 1f
        PreviewMode.Split -> split
        PreviewMode.Dark -> 0f
    }

/**
 * The split handle for the preview [mode], opening at [saved], sliding between modes on the skin's
 * slide motion and jumping under `LocalMotionFrozen`. [onSave] hears where the handle comes to rest
 * in Split once it has stayed there for [HANDLE_SETTLE_MILLIS], so a keyboard or assistive move
 * saves once. A pointer letting go over the canvas saves a moved handle straight away instead, so a
 * drag that ends just before the tab closes is not lost.
 */
@Composable
internal fun rememberPreviewSplit(
    mode: PreviewMode,
    saved: Float,
    onSave: (fraction: Float) -> Unit,
): PreviewSplit {
    val preview = remember { PreviewSplit(mode, saved) }
    val spec = if (LocalMotionFrozen.current) null else LocalBuilderMotion.current.slide<Float>()
    val currentMode by rememberUpdatedState(mode)
    val save by rememberUpdatedState(onSave)
    LaunchedEffect(preview, mode) { preview.slideTo(mode, spec) }
    LaunchedEffect(preview, saved) { preview.onSaved(saved) }
    LaunchedEffect(preview) {
        // Leaving Split keeps a waiting save. A fraction saved from elsewhere in the meantime drops it.
        snapshotFlow { if (currentMode == PreviewMode.Split) preview.restingHandle() else null }
            .filterNotNull()
            .collectLatest { fraction ->
                val mark = preview.mark()
                delay(HANDLE_SETTLE_MILLIS)
                if (!preview.savedElsewhereSince(mark) && preview.send(fraction)) save(fraction)
            }
    }
    LaunchedEffect(preview) {
        // Sending the fraction here leaves nothing new for the waiting save above, which then drops it.
        snapshotFlow { preview.releaseCount() }
            .drop(1)
            .collect {
                val fraction = if (currentMode == PreviewMode.Split) preview.restingHandle() else null
                if (fraction != null && preview.send(fraction)) save(fraction)
            }
    }
    return preview
}
