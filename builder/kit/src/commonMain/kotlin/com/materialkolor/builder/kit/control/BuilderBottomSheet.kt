package com.materialkolor.builder.kit.control

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.sheet_detent_full
import com.materialkolor.builder.kit.generated.resources.sheet_detent_half
import com.materialkolor.builder.kit.generated.resources.sheet_detent_peek
import com.materialkolor.builder.kit.headless.HeadlessBottomSheet
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import org.jetbrains.compose.resources.stringResource

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

/**
 * The kit's names for the detents, Peek, Half and Full in English. The lookup is remembered on the
 * three names, so a sheet that keeps the default gets the same one on every recomposition.
 */
@Composable
private fun detentNames(): (BottomSheetDetent) -> String {
    val peek = stringResource(Res.string.sheet_detent_peek)
    val half = stringResource(Res.string.sheet_detent_half)
    val full = stringResource(Res.string.sheet_detent_full)
    return remember(peek, half, full) {
        { detent: BottomSheetDetent ->
            when (detent) {
                BottomSheetDetent.Peek -> peek
                BottomSheetDetent.Half -> half
                BottomSheetDetent.Full -> full
            }
        }
    }
}

/**
 * A sheet docked to the bottom of its host that rests at peek, half and full. The poster lives in
 * one at Compact.
 *
 * It fills the host it is given and slides inside it, so put it in a box over the content it
 * covers. It drags anywhere on its surface, and its handle takes focus so the arrows, Page Up,
 * Page Down, Home and End move it too. Tab onto a row below the fold raises the sheet until the
 * row shows, and a scrolling body hands a drag back to the sheet once it reaches its top. Read or
 * drive the detent through [state]. Material3's own bottom sheet only has two states, so every skin
 * draws the headless sheet in its own dress.
 *
 * @param[state] The sheet's detent, from `rememberBottomSheetState`.
 * @param[label] The sheet's name, read on the handle.
 * @param[modifier] Applied to the host the sheet slides inside.
 * @param[detentLabel] What the handle reads as its state at each detent, the kit's name for the
 * detent unless the caller words it.
 * @param[peekHeight] How much of the sheet shows at peek.
 * @param[content] The sheet's body, below the handle.
 */
@Composable
public fun BuilderBottomSheet(
    state: BottomSheetState,
    label: String,
    modifier: Modifier = Modifier,
    detentLabel: (BottomSheetDetent) -> String = detentNames(),
    peekHeight: Dp = OverlayMetrics.sheetPeekHeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    HeadlessBottomSheet(
        state = state,
        label = label,
        detentLabel = detentLabel,
        peekHeight = peekHeight,
        style = overlayStyle(LocalSkin.current.library),
        modifier = modifier,
        content = content,
    )
}
