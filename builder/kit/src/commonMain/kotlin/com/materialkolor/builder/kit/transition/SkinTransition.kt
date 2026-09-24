package com.materialkolor.builder.kit.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.materialkolor.builder.kit.motion.BuilderMotion
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.LocalTabVisible
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How long a reveal waits for the host to draw the old frame before it gives up on the capture. */
private val CaptureTimeout = 100.milliseconds

/** The longest a reveal holds the old frame while the app waits on a skin font (architecture 6.11). */
private val FontWaitTimeout = 300.milliseconds

/**
 * What the reveal draws the old frame from (spike S4).
 *
 * A recorded layer is the cheap path, but it may point at nested layers live rather than copying
 * them, and a skin that adds render effects can make it drift. The bitmap path rasterizes the
 * capture once per switch and draws that instead.
 */
public enum class SnapshotMode {
    /** Draw the recorded layer as it is. */
    Layer,

    /** Rasterize the recorded layer once, then draw the bitmap. */
    Bitmap,
}

/**
 * How the old frame gives way to the new one.
 *
 * The library switch plays [Circle] out of the switcher (MO-04). Every other discrete change, a
 * preset, a chip, a shuffle, a style or an image candidate, plays [Crossfade] (MO-02). Reduced
 * motion turns either one into the short crossfade.
 */
@Immutable
public sealed interface RevealStyle {
    /**
     * The old frame shrinks away behind a circle growing out of [origin], on the skin's reveal
     * spec.
     *
     * @property[origin] Where the circle grows from, in the host's coordinates. [Offset.Unspecified]
     * grows it from the middle.
     */
    public data class Circle(
        public val origin: Offset,
    ) : RevealStyle

    /** The old frame fades out in place, on the skin's crossfade spec. */
    public data object Crossfade : RevealStyle
}

/**
 * The reveal that plays on every discrete change, such as a library switch, the light and dark
 * toggle, a style chip, a preset, a shuffle or an image candidate (F-03, MO-02, MO-04).
 *
 * Call [reveal] with a [RevealStyle] and the change. The host draws one more frame of the old UI
 * into a layer, the change applies, and the old frame gives way to the new one, either behind a
 * growing circle or by fading out in place. Drags never come through here, and neither do undo,
 * redo or keyboard nudges.
 *
 * Get one from [rememberSkinTransition] and draw through [SkinTransitionHost].
 */
@Stable
public class SkinTransition internal constructor(
    internal val snapshot: GraphicsLayer,
    private val scope: CoroutineScope,
    private val environment: () -> RevealEnvironment,
) {
    /** Set while a reveal waits for the host to record the old frame. The host completes it from draw. */
    internal var pendingCapture: CompletableDeferred<Unit>? by mutableStateOf(null)

    /** How the running reveal draws, already turned into a crossfade under reduced motion. */
    internal var style: RevealStyle by mutableStateOf(RevealStyle.Crossfade)

    /** The rasterized old frame under [SnapshotMode.Bitmap], null otherwise. */
    internal var bitmap: ImageBitmap? by mutableStateOf(null)

    /** Zero while the old frame covers everything, one once it is gone. Only draw reads it. */
    internal val progress: Animatable<Float, AnimationVector1D> = Animatable(1f)

    private val switching = Mutex()

    // b-503a

    /** Counts reveals, so one whose animation has not started yet can tell a newer one took over. */
    private var generation: Int = 0

    /**
     * Applies [change] behind a reveal in the given [style].
     *
     * The host records the old frame while it draws, and the change applies at the start of the
     * frame after that, never inside a draw pass. The change applies straight away with no capture
     * when motion is frozen or the tab is hidden, and also when the host has not drawn within 100 ms
     * or its next frame has not come within 100 ms of that draw. A reveal that arrives while another
     * is still running snaps that one to its end first. Under reduced motion either style becomes the
     * short crossfade.
     *
     * The old frame covers everything for one more frame after the change, while the new UI composes
     * under it, and the animation starts on the frame after that. A skin switch can take the browser
     * hundreds of milliseconds to compose, and an animation timed from that frame would be over by
     * the time the next one drew.
     *
     * Returns once the reveal has finished or been cut short by the next one. Cancelling the caller
     * after the change has applied does not stop the reveal, since the animation belongs to the host.
     *
     * @param[style] A circle out of the library switcher, or a crossfade for every other discrete
     * change.
     * @param[awaitBeforeReveal] Runs before the capture, for up to 300 ms. The app uses it to wait on
     * the font of a skin it has not shown yet, so the new frame does not arrive in a fallback face.
     * @param[change] The edit. It runs once, synchronously, whatever path the reveal takes.
     */
    public suspend fun reveal(
        style: RevealStyle,
        awaitBeforeReveal: suspend () -> Unit = {},
        change: () -> Unit,
    ) {
        val animation = switching.withLock {
            progress.snapTo(1f)
            val turn = ++generation // b-503a
            val environment = environment()
            if (environment.frozen || !environment.tabVisible) {
                change()
                return
            }

            finishesWithin(FontWaitTimeout, awaitBeforeReveal)
            if (!capture()) {
                change()
                return
            }

            val image = when (environment.mode) {
                SnapshotMode.Layer -> null
                SnapshotMode.Bitmap -> snapshot.toImageBitmap()
            }
            bitmap = image
            val shown = if (environment.reduced) RevealStyle.Crossfade else style
            this.style = shown
            change()
            progress.snapTo(0f)

            val spec = when (shown) {
                is RevealStyle.Circle -> environment.motion.reveal<Float>()
                RevealStyle.Crossfade -> environment.motion.crossfade<Float>()
            }
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    // b-503a
                    // A newer reveal snaps this one to its end, which cancels nothing while it waits here.
                    nextFrame()
                    if (turn == generation) progress.animateTo(targetValue = 1f, animationSpec = spec)
                } finally {
                    if (bitmap === image) bitmap = null
                }
            }
        }
        animation.join()
    }

    /**
     * Asks the host to record its next frame, then waits for the frame after it. False when the host
     * did not draw within the timeout or the next frame did not come within it after that.
     *
     * The host completes the capture from inside its draw pass, and a dispatcher that resumes
     * inline would carry straight on from there. Waiting for the next frame on the host's clock is
     * the boundary. Everything after the capture, the state writes and the change included, runs
     * at the start of a frame and never inside a draw, whatever dispatcher the caller is on.
     *
     * A browser runs the timer and the frame on one thread, so a capture frame that takes longer than
     * the timeout holds the timer back until it ends. Whether the host drew decides, not which of the
     * two resumed first, and each wait gets a timeout of its own.
     */
    private suspend fun capture(): Boolean {
        val captured = CompletableDeferred<Unit>()
        pendingCapture = captured
        try {
            finishesWithin(CaptureTimeout) { captured.await() }
            if (!captured.isCompleted) return false
        } finally {
            pendingCapture = null
        }
        var framed = false
        finishesWithin(CaptureTimeout) { nextFrame { framed = true } }
        return framed
    }

    /**
     * Suspends until the next frame starts, on the clock the host animates with, and runs [onFrame]
     * in it. The caller's own context only has to carry a clock when the transition's scope somehow
     * lacks one.
     */
    private suspend fun nextFrame(onFrame: () -> Unit = {}) {
        val clock = scope.coroutineContext[MonotonicFrameClock]
        if (clock != null) clock.withFrameNanos { onFrame() } else withFrameNanos { onFrame() }
    }
}

/**
 * Runs [block] for at most [timeout] and says whether it finished in time.
 *
 * Compose's frame dispatchers pass `delay` through to the host but not timeouts, so
 * `withTimeoutOrNull` would run on a wall clock of its own that no test clock can drive. Racing a
 * plain delay keeps the timeout on the same clock as the frames.
 */
private suspend fun finishesWithin(
    timeout: Duration,
    block: suspend () -> Unit,
): Boolean =
    coroutineScope {
        val work = launch { block() }
        val timer = launch {
            delay(timeout)
            work.cancel()
        }
        work.join()
        timer.cancel()
        !work.isCancelled
    }

/** The locals a reveal reads at the moment it starts. */
internal data class RevealEnvironment(
    val motion: BuilderMotion,
    val mode: SnapshotMode,
    val frozen: Boolean,
    val reduced: Boolean,
    val tabVisible: Boolean,
)

/**
 * Remembers the [SkinTransition] for one [SkinTransitionHost].
 *
 * It picks up the skin's motion, [LocalMotionFrozen], [LocalReducedMotion] and [LocalTabVisible]
 * from where it is called, so call it inside the skin.
 *
 * @param[mode] What the reveal draws the old frame from. B-005 measures both and may change the
 * default.
 */
@Composable
public fun rememberSkinTransition(mode: SnapshotMode = SnapshotMode.Layer): SkinTransition {
    val snapshot = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val environment = rememberUpdatedState(
        RevealEnvironment(
            motion = LocalBuilderMotion.current,
            mode = mode,
            frozen = LocalMotionFrozen.current,
            reduced = LocalReducedMotion.current,
            tabVisible = LocalTabVisible.current,
        ),
    )
    return remember(snapshot, scope) {
        SkinTransition(snapshot = snapshot, scope = scope, environment = { environment.value })
    }
}
