package com.materialkolor.builder.kit.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
 * The reveal that plays on every discrete change, such as a library switch, the light and dark
 * toggle, a style chip, a preset, a shuffle or an image candidate (F-03, MO-04).
 *
 * Call [reveal] with the change. The host draws one more frame of the old UI into a layer, the
 * change applies, and the old frame shrinks away behind a circle growing out of the origin. Drags
 * never come through here, and neither do undo, redo or keyboard nudges.
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

    /** The origin of the running reveal, in the host's coordinates. */
    internal var origin: Offset by mutableStateOf(Offset.Unspecified)

    /** True when the running reveal fades the old frame out instead of cutting a circle into it. */
    internal var crossfade: Boolean by mutableStateOf(false)

    /** The rasterized old frame under [SnapshotMode.Bitmap], null otherwise. */
    internal var bitmap: ImageBitmap? by mutableStateOf(null)

    /** Zero while the old frame covers everything, one once it is gone. Only draw reads it. */
    internal val progress: Animatable<Float, AnimationVector1D> = Animatable(1f)

    private val switching = Mutex()

    /**
     * Applies [change] behind a reveal out of [origin].
     *
     * The change applies straight away with no capture when motion is frozen or the tab is hidden,
     * and also when the host does not draw within 100 ms. A reveal that arrives while another is
     * still running snaps that one to its end first. Under reduced motion the old frame crossfades
     * out in place instead.
     *
     * Returns once the reveal has finished or been cut short by the next one. Cancelling the caller
     * after the change has applied does not stop the reveal, since the animation belongs to the host.
     *
     * @param[origin] Where the circle grows from, in the host's coordinates. [Offset.Unspecified]
     * grows it from the middle.
     * @param[awaitBeforeReveal] Runs before the capture, for up to 300 ms. The app uses it to wait on
     * the font of a skin it has not shown yet, so the new frame does not arrive in a fallback face.
     * @param[change] The edit. It runs once, synchronously, whatever path the reveal takes.
     */
    public suspend fun reveal(
        origin: Offset,
        awaitBeforeReveal: suspend () -> Unit = {},
        change: () -> Unit,
    ) {
        val animation = switching.withLock {
            progress.snapTo(1f)
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
            this.origin = origin
            crossfade = environment.reduced
            change()
            progress.snapTo(0f)

            val spec = if (environment.reduced) {
                environment.motion.crossfade<Float>()
            } else {
                environment.motion.reveal<Float>()
            }
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    progress.animateTo(targetValue = 1f, animationSpec = spec)
                } finally {
                    if (bitmap === image) bitmap = null
                }
            }
        }
        animation.join()
    }

    /** Asks the host to record its next frame and waits for it. False when nothing drew in time. */
    private suspend fun capture(): Boolean {
        val captured = CompletableDeferred<Unit>()
        pendingCapture = captured
        return try {
            finishesWithin(CaptureTimeout) { captured.await() }
        } finally {
            pendingCapture = null
        }
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
