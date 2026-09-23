package com.materialkolor.builder.kit.headless

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.overlayFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The heights a bottom sheet rests at, smallest first. */
public enum class BottomSheetDetent {
    /** Only the top of the sheet shows, enough to see what it holds. */
    Peek,

    /** The sheet covers half of its host. */
    Half,

    /** The sheet covers all of its host. */
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

    /** The sheet's own settle motion, handed in by the skin it is drawn in. */
    internal var settleSpec: AnimationSpec<Float> = snap()

    /** The detent the sheet last came to rest at. */
    public val detent: BottomSheetDetent
        get() = draggable.settledValue

    /** The detent the sheet is on its way to, which is [detent] while it rests. */
    public val targetDetent: BottomSheetDetent
        get() = draggable.targetValue

    /** Moves the sheet to [detent] with the skin's motion. */
    public suspend fun animateTo(detent: BottomSheetDetent) {
        draggable.animateTo(detent, settleSpec)
    }

    /** Moves the sheet to [detent] at once. */
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

/** A bottom sheet state that starts at [initialDetent] and survives recreation. */
@Composable
public fun rememberBottomSheetState(initialDetent: BottomSheetDetent = BottomSheetDetent.Peek): BottomSheetState =
    rememberSaveable(saver = BottomSheetState.Saver) { BottomSheetState(initialDetent) }

/**
 * A sheet docked to the bottom of its host that rests at three detents.
 *
 * It can be dragged anywhere on its surface, and its handle takes focus so the keyboard can move it
 * too. Up and Page Up raise it one detent, Down and Page Down lower it, Home and End jump to the
 * ends, and Enter or Space step through the detents in turn. The handle reads the detent as its
 * state and offers expand and collapse to assistive technology.
 *
 * @param[state] The sheet's detent.
 * @param[label] The sheet's name, read on the handle and as the pane title.
 * @param[peekHeight] How much of the sheet shows at [BottomSheetDetent.Peek].
 * @param[style] The skin's overlay style.
 * @param[modifier] Applied to the host the sheet slides inside.
 * @param[content] The sheet's body, below the handle.
 */
@Composable
internal fun HeadlessBottomSheet(
    state: BottomSheetState,
    label: String,
    peekHeight: Dp,
    style: OverlayStyle,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val settle = LocalBuilderMotion.current.spatial<Float>()
    SideEffect { state.settleSpec = settle }
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val height = constraints.maxHeight.toFloat()
        val peek = with(LocalDensity.current) { peekHeight.toPx() }.coerceIn(0f, height)
        val anchors = remember(height, peek) {
            DraggableAnchors {
                BottomSheetDetent.Full at 0f
                BottomSheetDetent.Half at minOf(height / 2f, height - peek)
                BottomSheetDetent.Peek at height - peek
            }
        }
        SideEffect { state.draggable.updateAnchors(anchors) }
        val shape = RoundedCornerShape(topStart = style.panelRadius, topEnd = style.panelRadius)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
                .offset {
                    val offset = state.draggable.offset
                    val y = if (offset.isNaN()) anchors.positionOf(state.draggable.currentValue) else offset
                    IntOffset(0, y.roundToInt())
                }.anchoredDraggable(
                    state = state.draggable,
                    orientation = Orientation.Vertical,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(state.draggable, animationSpec = settle),
                ).shadow(style.shadow, shape)
                .clip(shape)
                .background(style.surface)
                .then(if (style.border != null) Modifier.border(style.border, shape) else Modifier)
                .semantics { paneTitle = label },
        ) {
            SheetHandle(state, label, style, rememberCoroutineScope())
            content()
        }
    }
}

@Composable
private fun SheetHandle(
    state: BottomSheetState,
    label: String,
    style: OverlayStyle,
    scope: CoroutineScope,
) {
    val interaction = remember { MutableInteractionSource() }
    val detent = state.detent
    val detents = BottomSheetDetent.entries
    val higher = detents.getOrNull(detent.ordinal + 1)
    val lower = detents.getOrNull(detent.ordinal - 1)

    fun moveTo(target: BottomSheetDetent?): Boolean {
        if (target == null || target == detent) return false
        scope.launch { state.animateTo(target) }
        return true
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(LocalLayout.current.minTouchTarget)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.PageUp -> moveTo(higher)
                    Key.DirectionDown, Key.PageDown -> moveTo(lower)
                    Key.MoveHome -> moveTo(BottomSheetDetent.Full)
                    Key.MoveEnd -> moveTo(BottomSheetDetent.Peek)
                    else -> false
                }
            }.semantics {
                contentDescription = label
                stateDescription = detent.name
                if (higher != null) expand { moveTo(higher) }
                if (lower != null) collapse { moveTo(lower) }
            }.overlayFeedback(interaction, style, shape = RectangleShape)
            .clickable(interaction, null, role = Role.Button) {
                moveTo(higher ?: BottomSheetDetent.Peek)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(HandleWidth, HandleHeight).background(style.thumb, RoundedCornerShape(HandleHeight)))
    }
}

private val HandleWidth = 32.dp
private val HandleHeight = 4.dp
