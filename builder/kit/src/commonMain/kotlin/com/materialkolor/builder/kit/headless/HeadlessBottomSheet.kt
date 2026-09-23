package com.materialkolor.builder.kit.headless

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
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

    /** How the sheet rises to a higher detent, handed in by the motion set it is drawn in. */
    internal var raiseSpec: AnimationSpec<Float> = snap()

    /** How the sheet sinks to a lower detent. */
    internal var lowerSpec: AnimationSpec<Float> = snap()

    /** The detent the sheet last came to rest at. */
    public val detent: BottomSheetDetent
        get() = draggable.settledValue

    /** The detent the sheet is on its way to, which is [detent] while it rests. */
    public val targetDetent: BottomSheetDetent
        get() = draggable.targetValue

    /** Moves the sheet to [detent] with the skin's motion. */
    public suspend fun animateTo(detent: BottomSheetDetent) {
        draggable.animateTo(detent, if (detent > targetDetent) raiseSpec else lowerSpec)
    }

    /** Lets the sheet coast on [velocity] to the detent the fling carries it to. */
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
 * Focus never lands below the fold. When Tab reaches a row the sheet hides, the sheet rises to the
 * lowest detent that shows it (WCAG 2.4.11). A scrolling body raises the sheet before it scrolls up,
 * and hands a drag back down to the sheet once it has scrolled to its top, the way Material's sheet
 * does. The sheet rises with the panel arrival motion and sinks with the panel exit motion, and
 * snaps under reduced motion (MO-05).
 *
 * @param[state] The sheet's detent.
 * @param[label] The sheet's name, read on the handle and as the pane title.
 * @param[detentLabel] What the handle reads as its state at each detent.
 * @param[peekHeight] How much of the sheet shows at [BottomSheetDetent.Peek].
 * @param[style] The skin's overlay style.
 * @param[modifier] Applied to the host the sheet slides inside.
 * @param[content] The sheet's body, below the handle.
 */
@Composable
internal fun HeadlessBottomSheet(
    state: BottomSheetState,
    label: String,
    detentLabel: (BottomSheetDetent) -> String,
    peekHeight: Dp,
    style: OverlayStyle,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = LocalBuilderMotion.current
    val reduced = LocalReducedMotion.current
    val raise: AnimationSpec<Float> = if (reduced) snap() else motion.panelEnter()
    val lower: AnimationSpec<Float> = if (reduced) snap() else motion.panelExit()
    SideEffect {
        state.raiseSpec = raise
        state.lowerSpec = lower
    }
    val fling = AnchoredDraggableDefaults.flingBehavior(state.draggable, animationSpec = raise)
    val nestedScroll = remember(state, fling) { SheetScrollConnection(state, fling) }
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
                }.then(RaiseToShowFocusElement(state))
                .nestedScroll(nestedScroll)
                .anchoredDraggable(
                    state = state.draggable,
                    orientation = Orientation.Vertical,
                    flingBehavior = fling,
                ).shadow(style.shadow, shape)
                .clip(shape)
                .background(style.surface)
                .then(if (style.border != null) Modifier.border(style.border, shape) else Modifier)
                .semantics { paneTitle = label },
        ) {
            SheetHandle(state, label, detentLabel, style, rememberCoroutineScope())
            content()
        }
    }
}

@Composable
private fun SheetHandle(
    state: BottomSheetState,
    label: String,
    detentLabel: (BottomSheetDetent) -> String,
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
                stateDescription = detentLabel(detent)
                if (higher != null) expand { moveTo(higher) }
                if (lower != null) collapse { moveTo(lower) }
            }.overlayFeedback(interaction, style, shape = RectangleShape)
            .clickable(interaction, null, role = Role.Button) {
                moveTo(higher ?: BottomSheetDetent.Peek)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(
                    OverlayMetrics.sheetHandleWidth,
                    OverlayMetrics.sheetHandleHeight,
                ).background(style.thumb, RoundedCornerShape(OverlayMetrics.sheetHandleHeight)),
        )
    }
}

/**
 * Raises the sheet when a child inside asks to be seen, which every focusable does as it takes
 * focus. The sheet goes to the lowest detent that shows the whole child, and never sinks.
 */
private class RaiseToShowFocusElement(
    private val state: BottomSheetState,
) : ModifierNodeElement<RaiseToShowFocusNode>() {
    override fun create(): RaiseToShowFocusNode = RaiseToShowFocusNode(state)

    override fun update(node: RaiseToShowFocusNode) {
        node.state = state
    }

    override fun equals(other: Any?): Boolean = other is RaiseToShowFocusElement && other.state === state

    override fun hashCode(): Int = state.hashCode()
}

private class RaiseToShowFocusNode(
    var state: BottomSheetState,
) : Modifier.Node(),
    BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) {
        val bounds = boundsProvider() ?: return
        if (!childCoordinates.isAttached) return
        val sheet = requireLayoutCoordinates()
        val bottom = sheet.localPositionOf(childCoordinates, bounds.bottomLeft).y
        val height = sheet.size.height
        val anchors = state.draggable.anchors
        val shows = BottomSheetDetent.entries.firstOrNull { detent -> height - anchors.positionOf(detent) >= bottom }
        val target = shows ?: BottomSheetDetent.Full
        if (target > state.targetDetent) state.animateTo(target)
    }
}

/**
 * Shares a drag between the sheet and a scrolling body, the way Material's sheet does.
 *
 * A drag up raises the sheet before the body scrolls, and whatever the body leaves over moves the
 * sheet, so a body scrolled to its top hands a drag down back to the sheet.
 */
private class SheetScrollConnection(
    private val state: BottomSheetState,
    private val fling: FlingBehavior,
) : NestedScrollConnection {
    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val delta = available.y
        return if (delta < 0 && source == NestedScrollSource.UserInput) {
            Offset(0f, state.draggable.dispatchRawDelta(delta))
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (source == NestedScrollSource.UserInput) {
            Offset(0f, state.draggable.dispatchRawDelta(available.y))
        } else {
            Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val offset = state.draggable.offset
        val rising = available.y < 0 && !offset.isNaN() && offset > state.draggable.anchors.minPosition()
        if (!rising) return Velocity.Zero
        state.fling(available.y, fling)
        return available
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        state.fling(available.y, fling)
        return available
    }
}
