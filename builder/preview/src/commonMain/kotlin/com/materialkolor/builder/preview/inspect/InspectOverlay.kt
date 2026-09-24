package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toRect
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.split.PaneSide
import com.materialkolor.builder.preview.split.SplitState
import kotlinx.coroutines.flow.drop
import kotlin.math.max
import kotlin.math.roundToInt

/** Tags the Inspect card, for tests to find what it shows. */
public const val INSPECT_CARD_TAG: String = "inspect-card"

/**
 * What the pinned Inspect card can do, and how Inspect is left.
 *
 * @property[pinEnabled] Whether the target takes role pins. Pin this role is disabled when it does not.
 * @property[onPin] Pins a role, in dark mode when `isDark` is true and light mode otherwise, to the
 * color it has there now.
 * @property[onShowOnRamp] Shows a color on its tonal ramp, in the mode it was inspected in.
 * @property[onJumpToKeyColor] Opens the key colors a color comes from.
 * @property[onLeave] Turns Inspect off, which Esc asks for once no card is pinned.
 */
@Immutable
public class InspectActions(
    public val pinEnabled: Boolean,
    public val onPin: (role: Role, isDark: Boolean, argb: Argb) -> Unit,
    public val onShowOnRamp: (ref: ColorRef, isDark: Boolean) -> Unit,
    public val onJumpToKeyColor: (ref: ColorRef) -> Unit,
    public val onLeave: () -> Unit,
)

/**
 * The preview [content] with Inspect over it while [on] (F-44).
 *
 * Off, it only provides a null [LocalInspectRegistry], so the content pays nothing. On, it provides
 * a registry and watches the pointer ahead of the content. The element under the pointer gets an
 * outline and a card beside it with each color it declared, in the mode of the copy it sits in, and
 * the contrast of its first pair the audit rates. A press over the preview never reaches the content,
 * apart from one on the split handle, and letting go pins the card, which then offers Pin this role,
 * Show on ramp and Jump to key color for the first color. Letting go over another element moves the
 * pin and over bare canvas drops it, and a touch tap inspects and pins at once. Keyboard focus on a
 * declared element shows its card too, read only. Esc drops a pinned card first and leaves Inspect
 * after.
 *
 * The card is drawn in this layout's own tree, never in a popup, and kept inside it.
 *
 * @param[on] Whether Inspect is on for what the canvas shows.
 * @param[result] The theme the preview wears, which the card reads colors and ratings from.
 * @param[shown] What the canvas shows, both copies of a split or one copy alone.
 * @param[split] Where the handle sits while [shown] is Split. The content has to fill this layout,
 * so the copies line up with it.
 * @param[actions] What the pinned card's actions and Esc do.
 * @param[modifier] Applied to the layout around the content.
 * @param[scene] What the content shows, such as a tab. A new one drops any card.
 * @param[content] The preview.
 */
@Composable
public fun InspectOverlay(
    on: Boolean,
    result: ThemeResult,
    shown: PreviewMode,
    split: SplitState,
    actions: InspectActions,
    modifier: Modifier = Modifier,
    scene: Any? = null,
    content: @Composable () -> Unit,
) {
    val state = remember(on, shown, scene) { if (on) InspectOverlayState() else null }
    val tokens = LocalBuilderTokens.current
    val layoutDirection = LocalLayoutDirection.current
    val currentShown = rememberUpdatedState(shown)
    val leave by rememberUpdatedState(actions.onLeave)
    val focus = remember { FocusRequester() }
    val watch = if (state == null) {
        Modifier
    } else {
        Modifier
            .onGloballyPositioned { coordinates -> state.origin = coordinates.positionInWindow() }
            .inspectPointer(state, currentShown, split, layoutDirection, tokens.spacing.section, focus)
            .onKeyEvent { event -> state.onEscape(event, focus) { leave() } }
            .focusRequester(focus)
            .onFocusChanged { focusState -> state.holdsFocus = focusState.isFocused }
            .focusProperties { canFocus = state.pinned != null || state.holdsFocus }
            .focusTarget()
    }
    Box(modifier.then(watch)) {
        CompositionLocalProvider(LocalInspectRegistry provides state?.registry, content = content)
        if (state != null) InspectFindings(state, shown, result, actions)
    }
}

/** One element under Inspect and the mode of the copy it sits in. */
@Immutable
internal data class InspectTarget(
    val entry: InspectEntry,
    val isDark: Boolean,
)

/** What one stretch of Inspect over one view of the canvas has found. */
@Stable
internal class InspectOverlayState {
    val registry: InspectRegistry = InspectRegistry()

    /** The element under a hovering pointer. */
    var hovered: InspectTarget? by mutableStateOf(null)

    /** The element a press pinned the card to. */
    var pinned: InspectTarget? by mutableStateOf(null)

    /** Where this layout's top start corner sits in the window, the space the registry keeps. */
    var origin: Offset by mutableStateOf(Offset.Zero)

    /** Whether this layout holds focus itself, as it does after a pointer pins a card, so Esc reaches it. */
    var holdsFocus: Boolean by mutableStateOf(false)

    /** Where the card sits in this layout, or null while none shows. Only the pointer reads it. */
    var card: Rect? = null

    /**
     * Drop the pinned card on Esc, or leave Inspect when none is pinned.
     *
     * Dropping the card takes focus to the layout through [focus] first. An action on the card may
     * hold focus, and when the card loses its actions focus would leave the whole tree with them, so
     * the next Esc would reach nothing.
     */
    fun onEscape(
        event: KeyEvent,
        focus: FocusRequester,
        leave: () -> Unit,
    ): Boolean {
        if (event.type != KeyEventType.KeyDown || event.key != Key.Escape) return false
        if (pinned != null) {
            focus.requestFocus()
            pinned = null
        } else {
            leave()
        }
        return true
    }
}

/** What a press over the overlay turned out to be, until every pointer is up again. */
private enum class Press {
    /** A press over a copy of the preview, kept from the content and pinning the card when it ends. */
    Inspect,

    /** A press on the split handle or the card, left alone. */
    PassThrough,
}

/**
 * Watch the pointer ahead of the content. A hovering pointer inspects what is under it, and a press
 * anywhere but the handle strip and the card is kept from the content and pins the card as it ends.
 */
private fun Modifier.inspectPointer(
    state: InspectOverlayState,
    shown: State<PreviewMode>,
    split: SplitState,
    layoutDirection: LayoutDirection,
    handleThickness: Dp,
    focus: FocusRequester,
): Modifier =
    pointerInput(state, split, layoutDirection, handleThickness) {
        val thickness = handleThickness.roundToPx()
        awaitPointerEventScope {
            fun targetAt(position: Offset): InspectTarget? =
                inspectAt(state, position, size.width, shown.value, split.fraction, layoutDirection)

            var press: Press? = null
            var last: Offset? = null
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val position = event.changes.firstOrNull()?.position ?: continue
                val overCard = state.card?.contains(position) == true
                if (press == null && event.changes.any { change -> change.changedToDownIgnoreConsumed() }) {
                    val onHandle = shown.value == PreviewMode.Split &&
                        onHandle(position.x, size.width, split.fraction, thickness, layoutDirection)
                    press = if (overCard || onHandle) Press.PassThrough else Press.Inspect
                }
                if (press == Press.Inspect) event.changes.forEach { change -> change.consume() }
                when {
                    press != null && event.changes.none { change -> change.pressed } -> {
                        if (press == Press.Inspect) {
                            val pinned = targetAt(position)
                            state.pinned = pinned
                            // A pointer press drops focus from most of the preview, so the overlay takes it for Esc.
                            if (pinned != null) focus.requestFocus()
                        }
                        press = null
                    }
                    press != null || overCard -> {
                        Unit
                    }
                    event.type == PointerEventType.Exit -> {
                        state.hovered = null
                        last = null
                    }
                    position != last || event.type == PointerEventType.Scroll -> {
                        state.hovered = targetAt(position)
                        last = position
                    }
                }
            }
        }
    }

/** The element at [position] in this layout and the mode of the copy it sits in, or null for none. */
private fun inspectAt(
    state: InspectOverlayState,
    position: Offset,
    width: Int,
    shown: PreviewMode,
    fraction: Float,
    layoutDirection: LayoutDirection,
): InspectTarget? {
    val side = paneSideAt(position.x, width, shown, fraction, layoutDirection)
    val entry = state.registry.hit(side, position + state.origin) ?: return null
    return InspectTarget(entry, isDark(side, shown))
}

/**
 * The copy of the preview at [x] across a layout [width] wide. The start copy runs from the start
 * edge to the handle at [fraction] and the end copy on past it. One copy alone is the start copy.
 */
internal fun paneSideAt(
    x: Float,
    width: Int,
    shown: PreviewMode,
    fraction: Float,
    layoutDirection: LayoutDirection,
): PaneSide {
    if (shown != PreviewMode.Split) return PaneSide.Start
    val end = if (layoutDirection == LayoutDirection.Ltr) x >= width * fraction else x < width * (1f - fraction)
    return if (end) PaneSide.End else PaneSide.Start
}

/** Whether a copy on [side] wears the dark scheme while the canvas shows [shown]. A split is light then dark. */
internal fun isDark(
    side: PaneSide,
    shown: PreviewMode,
): Boolean =
    when (shown) {
        PreviewMode.Light -> false
        PreviewMode.Split -> side == PaneSide.End
        PreviewMode.Dark -> true
    }

/**
 * Whether [x] falls in the split handle's strip, [thickness] wide and centred on [fraction] of
 * [width] from the start edge, the way the handle places itself.
 */
private fun onHandle(
    x: Float,
    width: Int,
    fraction: Float,
    thickness: Int,
    layoutDirection: LayoutDirection,
): Boolean {
    val along = if (layoutDirection == LayoutDirection.Ltr) x else width - x
    val start = (width * fraction - thickness / 2f).roundToInt().coerceIn(0, max(0, width - thickness))
    return along >= start && along < start + thickness
}

/**
 * The outline and card for the pinned element, else the hovered one, else the one keyboard focus is
 * on. A pointer hovering after focus moved wins over it, and focus moving wins over the hover.
 */
@Composable
private fun BoxScope.InspectFindings(
    state: InspectOverlayState,
    shown: PreviewMode,
    result: ThemeResult,
    actions: InspectActions,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.registry.focusedOwner() }.drop(1).collect { state.hovered = null }
    }
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val focused = if (keyboard) state.registry.focused else null
    val pinned = state.pinned
    val target = pinned ?: state.hovered ?: focused?.let { entry -> InspectTarget(entry, isDark(entry.side, shown)) }
    if (target == null) return
    val tokens = LocalBuilderTokens.current
    Spacer(
        Modifier.matchParentSize().drawBehind {
            val bounds = target.entry.bounds.translate(-state.origin)
            drawRect(
                color = tokens.focus,
                topLeft = bounds.topLeft,
                size = bounds.size,
                style = Stroke(tokens.outlineWidth.toPx()),
            )
        },
    )
    DisposableEffect(state) { onDispose { state.card = null } }
    Box(Modifier.placeCard(target.entry.bounds, state, tokens.spacing.small)) {
        // Only some skins draw a card that takes presses, so it takes them itself to keep them from the preview.
        InspectCard(target, pinned = target == pinned, result, actions, Modifier.pointerInput(Unit) {})
    }
}

/**
 * Take the whole layout and place the card beside [bounds], a window rect, noting where it went so
 * a press on it is left alone.
 */
private fun Modifier.placeCard(
    bounds: Rect,
    state: InspectOverlayState,
    gap: Dp,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
        layout(width, height) {
            val card = IntSize(placeable.width, placeable.height)
            val spot = cardSpot(bounds.translate(-state.origin), card, IntSize(width, height), gap.roundToPx())
            state.card = IntRect(spot, card).toRect()
            placeable.place(spot)
        }
    }

/**
 * Where a [card] goes beside an [element] in a layout of size [room], both in its own space.
 *
 * It sits [gap] past the side with more room, level with the element's top, or on the other side
 * when that one is too narrow. With no room on either side it goes below, or above when below is too
 * short. It is then held inside the room.
 */
internal fun cardSpot(
    element: Rect,
    card: IntSize,
    room: IntSize,
    gap: Int,
): IntOffset {
    val after = element.right + gap
    val before = element.left - gap - card.width
    val fitsAfter = after + card.width <= room.width
    val fitsBefore = before >= 0f
    val x: Float
    val y: Float
    if (fitsAfter || fitsBefore) {
        val moreAfter = room.width - element.right >= element.left
        x = if (fitsAfter && (moreAfter || !fitsBefore)) after else before
        y = element.top
    } else {
        val below = element.bottom + gap
        x = element.left
        y = if (below + card.height <= room.height) below else element.top - gap - card.height
    }
    return IntOffset(
        x.roundToInt().coerceIn(0, max(0, room.width - card.width)),
        y.roundToInt().coerceIn(0, max(0, room.height - card.height)),
    )
}
