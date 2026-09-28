package com.materialkolor.builder.kit.control

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * The heights a bottom sheet rests at, smallest first.
 */
public enum class BottomSheetDetent {
    /**
     * Only the top of the sheet shows, enough to see what it holds.
     */
    Peek,

    /**
     * The sheet covers half of its host.
     */
    Half,

    /**
     * The sheet covers all of its host.
     */
    Full,
}

/**
 * Where a bottom sheet rests and where it is heading.
 *
 * Hoist it to read the detent from outside, for instance to hide something the sheet covers at
 * [BottomSheetDetent.Full], or to move the sheet from a button.
 */
@Stable
public class BottomSheetState internal constructor(
    initialDetent: BottomSheetDetent,
) {
    internal val draggable: AnchoredDraggableState<BottomSheetDetent> = AnchoredDraggableState(initialDetent)

    /**
     * How the sheet rises to a higher detent, handed in by the motion set it is drawn in.
     */
    internal var raiseSpec: AnimationSpec<Float> = snap()

    /**
     * How the sheet sinks to a lower detent.
     */
    internal var lowerSpec: AnimationSpec<Float> = snap()

    /**
     * The detent the sheet last came to rest at.
     */
    public val detent: BottomSheetDetent
        get() = draggable.settledValue

    /**
     * The detent the sheet is on its way to, which is [detent] while it rests.
     */
    public val targetDetent: BottomSheetDetent
        get() = draggable.targetValue

    /**
     * Moves the sheet to [detent] with the skin's motion.
     */
    public suspend fun animateTo(detent: BottomSheetDetent) {
        draggable.animateTo(detent, if (detent > targetDetent) raiseSpec else lowerSpec)
    }

    /**
     * Lets the sheet coast on [velocity] to the detent the fling carries it to.
     */
    internal suspend fun fling(
        velocity: Float,
        behavior: FlingBehavior,
    ) {
        draggable.anchoredDrag { anchors ->
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    val from = draggable.offset
                    dragTo((from + pixels).coerceIn(anchors.minPosition(), anchors.maxPosition()))
                    return draggable.offset - from
                }
            }
            with(behavior) { scope.performFling(velocity) }
        }
    }

    /**
     * Moves the sheet to [detent] at once.
     */
    public suspend fun snapTo(detent: BottomSheetDetent) {
        draggable.snapTo(detent)
    }

    internal companion object {
        val Saver: Saver<BottomSheetState, String> = Saver(
            save = { state -> state.detent.name },
            restore = { name -> BottomSheetState(BottomSheetDetent.valueOf(name)) },
        )
    }
}

/**
 * A bottom sheet state that starts at [initialDetent] and survives recreation.
 */
@Composable
public fun rememberBottomSheetState(initialDetent: BottomSheetDetent = BottomSheetDetent.Peek): BottomSheetState =
    rememberSaveable(saver = BottomSheetState.Saver) { BottomSheetState(initialDetent) }
