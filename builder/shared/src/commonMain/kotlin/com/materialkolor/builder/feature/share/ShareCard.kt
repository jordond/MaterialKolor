package com.materialkolor.builder.feature.share

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The card in [bytes] as an image, or null when they are not an image Skia reads.
 *
 * The local dev server answers a card path with its HTML page, so null is what a card on localhost
 * always comes back as.
 */
internal fun decodeCard(bytes: ByteArray): ImageBitmap? =
    try {
        bytes.decodeToImageBitmap()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

/**
 * Where the share dialog's card is, as [ShareCardLoader] tells it.
 */
@Stable
internal sealed interface CardState {
    /**
     * No card has shown yet and one is on its way.
     */
    data object Loading : CardState

    /**
     * The link changed, so [previous] stays up while the new card is on its way.
     */
    data class Updating(
        val previous: ImageBitmap,
    ) : CardState

    /**
     * The card for the latest link.
     */
    data class Shown(
        val card: ImageBitmap,
    ) : CardState

    /**
     * The latest link has no card to show.
     */
    data object Failed : CardState
}

/**
 * Loads the card for the link the share dialog shows, one link at a time.
 *
 * The first link loads at once. Every later one waits [pause] first, keeping the card that is up
 * as [CardState.Updating], so typing a name fetches once the typing stops. A newer link cancels the
 * wait or the fetch still going for the one before, and the same link again changes nothing. A
 * [load] that gives null is [CardState.Failed], whether or not a card was up before.
 *
 * @param[scope] Where the loads run, cancelled with the dialog.
 * @param[load] Fetches and decodes the card at a url, null when there is none.
 * @param[pause] How long a later link waits before it loads.
 */
internal class ShareCardLoader(
    private val scope: CoroutineScope,
    private val load: suspend (String) -> ImageBitmap?,
    private val pause: Duration = 500.milliseconds,
) {
    private val _state = MutableStateFlow<CardState>(CardState.Loading)

    /**
     * The card for the latest link, or where it is.
     */
    val state: StateFlow<CardState> = _state.asStateFlow()

    private var url: String? = null
    private var running: Job? = null

    /**
     * Load the card at [url], unless it is the one already asked for.
     */
    fun show(url: String) {
        if (url == this.url) return
        val first = this.url == null
        this.url = url
        running?.cancel()
        val previous = when (val state = _state.value) {
            is CardState.Shown -> state.card
            is CardState.Updating -> state.previous
            CardState.Loading, CardState.Failed -> null
        }
        _state.value = if (previous != null) CardState.Updating(previous) else CardState.Loading
        running = scope.launch {
            if (!first) delay(pause)
            val card = load(url)
            ensureActive()
            _state.value = if (card != null) CardState.Shown(card) else CardState.Failed
        }
    }
}
