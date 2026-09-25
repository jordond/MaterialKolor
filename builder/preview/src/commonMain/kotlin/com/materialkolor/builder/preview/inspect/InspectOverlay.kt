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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
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

/**
 * Tags the Inspect card, for tests to find what it shows.
 */
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
 * The preview [content] with Inspect over it while [on].
 *
 * Off, it only provides a null [LocalInspectRegistry], so the content pays nothing. On, it provides
 * a registry and watches the pointer ahead of the content. The element under the pointer gets an
 * outline and a card beside it with each color it declared, in the mode of the copy it sits in, and
 * the contrast of its first pair the audit rates. A press over the preview never reaches the content,
 * apart from one on the split handle, and letting go pins the card, which then offers Pin this role,
 * Show on ramp and Jump to key color for the first color. Letting go over another element moves the
 * pin and over bare canvas drops it, and a touch tap inspects and pins at once. Keyboard focus on a
 * declared element shows its card too, read only, and [PinKey] pins it and moves focus to its first
 * enabled action. The element never hears that key, while Enter and Space still reach it. Esc drops
 * a pinned card first and leaves Inspect after. While this layout holds focus itself and the
 * keyboard is in use, the preview shows a focus ring.
 *
 * The outline and card follow their element as the preview scrolls and go when it leaves. An element
 * scrolled out of sight inside the preview, such as off the side of a horizontal scroll, keeps its
 * pin but shows nothing until it scrolls back. When the handle crosses one, they move to the other
 * copy's element at its centre, or go when there is none. The card is drawn in this layout's own
 * tree, never in a popup, and kept inside it.
 *
 * @param[on] Whether Inspect is on for what the canvas shows.
 * @param[result] The theme the preview uses, which the card reads colors and ratings from.
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
    // Whether this layout holds focus itself, as it does after a pointer pins a card, so Esc reaches
    // it. It outlives the state, which a new mode or tab replaces while the focus stays put.
    val holdsFocus = remember(on) { mutableStateOf(false) }
    val tokens = LocalBuilderTokens.current
    val layoutDirection = LocalLayoutDirection.current
    val inputModes = LocalInputModeManager.current
    val currentShown = rememberUpdatedState(shown)
    val leave by rememberUpdatedState(actions.onLeave)
    val focus = remember { FocusRequester() }
    val watch = if (state == null) {
        Modifier
    } else {
        Modifier
            .onGloballyPositioned { coordinates ->
                state.origin = coordinates.positionInWindow()
                state.width = coordinates.size.width
            }.focusRing(holdsFocus, inputModes, tokens.focus, tokens.highlightWidth)
            .inspectPointer(state, currentShown, split, layoutDirection, tokens.spacing.section, focus)
            .onPreviewKeyEvent { event -> state.onPinKey(event, currentShown.value, inputModes) }
            .onKeyEvent { event -> state.onEscape(event, focus, inputModes) { leave() } }
            .focusRequester(focus)
            .onFocusChanged { focusState -> holdsFocus.value = focusState.isFocused }
            .focusProperties { canFocus = state.pinned != null || holdsFocus.value }
            .focusTarget()
    }
    if (state != null) FollowTargets(state, shown, split, layoutDirection, focus)
    Box(modifier.then(watch)) {
        CompositionLocalProvider(LocalInspectRegistry provides state?.registry, content = content)
        if (state != null) InspectFindings(state, shown, result, actions, focus)
    }
}

/**
 * One element under Inspect and the mode of the copy it sits in.
 *
 * @property[owner] The element's key in the registry. The outline and card read where it sits from
 * there as they are drawn and placed, so they follow it while the preview scrolls.
 * @property[side] The copy of the split it sits in.
 * @property[roles] The colors it declared.
 * @property[isDark] Whether that copy is in the dark scheme.
 */
@Immutable
internal data class InspectTarget(
    val owner: Any,
    val side: PaneSide,
    val roles: List<ColorRef>,
    val isDark: Boolean,
)

/**
 * What one stretch of Inspect over one view of the canvas has found.
 */
@Stable
internal class InspectOverlayState {
    val registry: InspectRegistry = InspectRegistry()

    /**
     * The element under a hovering pointer.
     */
    var hovered: InspectTarget? by mutableStateOf(null)

    /**
     * The element a press pinned the card to.
     */
    var pinned: InspectTarget? by mutableStateOf(null)

    /**
     * Where this layout's top start corner sits in the window, the space the registry keeps.
     */
    var origin: Offset by mutableStateOf(Offset.Zero)

    /**
     * How wide this layout is, for finding the copy a point falls in away from the pointer.
     */
    var width: Int = 0

    /**
     * Where the card sits in this layout, or null while none shows. Only the pointer reads it.
     */
    var card: Rect? = null

    /**
     * Whether focus is on the card or inside it, so dropping the pin hands focus to the layout first.
     */
    var cardHasFocus: Boolean = false

    /**
     * Whether the card's first enabled action takes focus once it shows, as it does after [PinKey].
     */
    var focusActions: Boolean by mutableStateOf(false)

    /**
     * Whether the [PinKey] press that pinned the card is still down, so its repeats and release go too.
     */
    private var pinKeyDown = false

    /**
     * Pin the card of the element keyboard focus is on as [PinKey] goes down, and ask for focus on its
     * first enabled action. The rest of that press is taken too, repeats and release alike, so the
     * element never hears the key and never activates. Enter and Space alone pass through. Like Esc,
     * the key tells [inputModes] the keyboard is in use.
     */
    fun onPinKey(
        event: KeyEvent,
        shown: PreviewMode,
        inputModes: InputModeManager,
    ): Boolean {
        if (event.key != PinKey.key) return false
        if (event.type == KeyEventType.KeyUp) return pinKeyDown.also { pinKeyDown = false }
        if (event.type != KeyEventType.KeyDown) return false
        if (!PinKey.matches(event)) {
            pinKeyDown = false
            return false
        }
        // Held down, the key repeats on the card's action that focus moved to.
        val target = focusedTarget(shown) ?: return pinKeyDown
        inputModes.requestInputMode(InputMode.Keyboard)
        pinned = target
        focusActions = true
        pinKeyDown = true
        return true
    }

    /**
     * Drop the pinned card on Esc, or leave Inspect when none is pinned.
     *
     * Compose counts only focus keys such as Tab as keyboard use, so Esc first tells [inputModes]
     * the keyboard is in use, and focus then moves the way it would after Tab. Dropping the card
     * takes focus to the layout through [focus] first. An action on the card may hold focus, and
     * when the card loses its actions focus would leave the whole tree with them, so the next Esc
     * would reach nothing.
     */
    fun onEscape(
        event: KeyEvent,
        focus: FocusRequester,
        inputModes: InputModeManager,
        leave: () -> Unit,
    ): Boolean {
        if (event.type != KeyEventType.KeyDown || event.key != Key.Escape) return false
        inputModes.requestInputMode(InputMode.Keyboard)
        if (pinned != null) {
            focus.requestFocus()
            pinned = null
        } else {
            leave()
        }
        return true
    }

    /**
     * The element at [position] in this layout and the mode of the copy it sits in, or null for none.
     */
    fun targetAt(
        position: Offset,
        shown: PreviewMode,
        fraction: Float,
        layoutDirection: LayoutDirection,
    ): InspectTarget? {
        val side = paneSideAt(position.x, width, shown, fraction, layoutDirection)
        val owner = registry.ownerAt(side, position + origin) ?: return null
        val entry = registry.entryOf(owner) ?: return null
        return InspectTarget(owner, side, entry.roles, isDark(side, shown))
    }

    /**
     * The element keyboard focus is on, for the card it shows, or null when focus is on none.
     */
    fun focusedTarget(shown: PreviewMode): InspectTarget? {
        val owner = registry.focusedOwner() ?: return null
        val entry = registry.entryOf(owner) ?: return null
        return InspectTarget(owner, entry.side, entry.roles, isDark(entry.side, shown))
    }

    /**
     * Whether the pinned or the hovered element has left the screen.
     */
    fun hasLeft(): Boolean = pinned.hasLeft() || hovered.hasLeft()

    /**
     * Drop the pinned and the hovered element once they have left the screen. Dropping the pin hands
     * focus to the layout through [focus] first, as [unpin] says.
     */
    fun dropLeft(focus: FocusRequester) {
        if (pinned.hasLeft()) unpin(focus)
        if (hovered.hasLeft()) hovered = null
    }

    /**
     * Keep the pinned and hovered elements on the copy shown at their centres, after the handle
     * moved to [fraction]. Once the handle crosses one, the target moves to what the other copy has
     * at that centre, or goes when it has nothing there. Dropping the pin hands focus to the layout
     * through [focus] first, as [unpin] says.
     */
    fun followHandle(
        shown: PreviewMode,
        fraction: Float,
        layoutDirection: LayoutDirection,
        focus: FocusRequester,
    ) {
        val pin = pinned
        if (pin != null) {
            val moved = pin.across(shown, fraction, layoutDirection)
            if (moved == null) unpin(focus) else pinned = moved
        }
        hovered = hovered?.across(shown, fraction, layoutDirection)
    }

    /**
     * Drop the pinned card as its element goes rather than on Esc. An action on the card may hold
     * focus, and would take it out of the whole tree as the card loses its actions, so Esc would then
     * reach nothing. While the card holds focus, the layout takes it through [focus] first, as Esc does.
     */
    private fun unpin(focus: FocusRequester) {
        if (cardHasFocus) focus.requestFocus()
        pinned = null
    }

    private fun InspectTarget?.hasLeft(): Boolean = this != null && registry.entryOf(owner) == null

    private fun InspectTarget.across(
        shown: PreviewMode,
        fraction: Float,
        layoutDirection: LayoutDirection,
    ): InspectTarget? {
        val bounds = (registry.entryOf(owner) ?: return null).bounds
        // Scrolled out of sight, it has no centre to find the other copy's element at, so it stays put.
        if (bounds.isEmpty) return this
        val centre = bounds.center - origin
        val now = paneSideAt(centre.x, width, shown, fraction, layoutDirection)
        return if (now == side) this else targetAt(centre, shown, fraction, layoutDirection)
    }
}

/**
 * Drop what Inspect found once its element leaves, and keep it on the copy the handle shows as the
 * handle moves.
 */
@Composable
private fun FollowTargets(
    state: InspectOverlayState,
    shown: PreviewMode,
    split: SplitState,
    layoutDirection: LayoutDirection,
    focus: FocusRequester,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.hasLeft() }.collect { left -> if (left) state.dropLeft(focus) }
    }
    LaunchedEffect(state, shown, split, layoutDirection) {
        snapshotFlow { split.fraction }
            .drop(1)
            .collect { fraction -> state.followHandle(shown, fraction, layoutDirection, focus) }
    }
}

/**
 * Ring the preview in [color] while this layout holds focus itself and the keyboard is in use, so a
 * keyboard user sees where Esc goes. A pointer pin takes the focus too but draws no ring.
 */
private fun Modifier.focusRing(
    holdsFocus: State<Boolean>,
    inputModes: InputModeManager,
    color: Color,
    width: Dp,
): Modifier =
    drawWithContent {
        drawContent()
        if (holdsFocus.value && inputModes.inputMode == InputMode.Keyboard) {
            val stroke = width.toPx()
            drawRect(
                color = color,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke),
            )
        }
    }

/**
 * What a press over the overlay turned out to be, until every pointer is up again.
 */
private enum class Press {
    /**
     * A press over a copy of the preview, kept from the content and pinning the card when it ends.
     */
    Inspect,

    /**
     * A press on the split handle or the card, left alone.
     */
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
                state.targetAt(position, shown.value, split.fraction, layoutDirection)

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
                        // Hover holds still through a press and over the card.
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

/**
 * The outline and card for the pinned element, else the hovered one, else the one keyboard focus is
 * on. A pointer hovering after focus moved wins over it, and focus moving wins over the hover. Once
 * [PinKey] pins a card, its first enabled action takes focus, or [focus] does for a card with none.
 */
@Composable
private fun BoxScope.InspectFindings(
    state: InspectOverlayState,
    shown: PreviewMode,
    result: ThemeResult,
    actions: InspectActions,
    focus: FocusRequester,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.registry.focusedOwner() }.drop(1).collect { state.hovered = null }
    }
    // Derived, so the card recomposes when focus moves to another element but not as that one moves.
    val focusedTarget = remember(state, shown) { derivedStateOf { state.focusedTarget(shown) } }
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val pinned = state.pinned
    val hovered = state.hovered
    val target = pinned ?: hovered ?: if (keyboard) focusedTarget.value else null
    if (target == null) return
    val tokens = LocalBuilderTokens.current
    val firstAction = remember(state) { FocusRequester() }
    LaunchedEffect(state, pinned, state.focusActions) {
        if (pinned == null || !state.focusActions) return@LaunchedEffect
        state.focusActions = false
        if (pinned.roles.isEmpty()) focus.requestFocus() else firstAction.requestFocus()
    }
    Spacer(
        Modifier.matchParentSize().drawBehind {
            val entry = state.registry.entryOf(target.owner) ?: return@drawBehind
            // Scrolled out of sight, the element keeps empty bounds and gets no outline.
            if (entry.bounds.isEmpty) return@drawBehind
            val bounds = entry.bounds.translate(-state.origin)
            drawRect(
                color = tokens.focus,
                topLeft = bounds.topLeft,
                size = bounds.size,
                style = Stroke(tokens.outlineWidth.toPx()),
            )
        },
    )
    DisposableEffect(state) {
        onDispose {
            state.card = null
            state.cardHasFocus = false
        }
    }
    Box(
        Modifier
            .placeCard(target.owner, state, tokens.spacing.small)
            .onFocusChanged { focusState -> state.cardHasFocus = focusState.hasFocus },
    ) {
        // Only some skins draw a card that takes presses, so it takes them itself to keep them from the preview.
        InspectCard(
            target = target,
            pinned = target == pinned,
            result = result,
            actions = actions,
            modifier = Modifier.pointerInput(Unit) {},
            firstAction = firstAction,
            pinHint = pinned == null && hovered == null,
        )
    }
}

/**
 * Take the whole layout and place the card beside the element [owner], wherever it sits now, noting
 * where the card went so a press on it is left alone. While the element is out of sight, with no
 * bounds or empty ones, the card is not placed at all.
 */
private fun Modifier.placeCard(
    owner: Any,
    state: InspectOverlayState,
    gap: Dp,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
        layout(width, height) {
            val bounds = state.registry.entryOf(owner)?.bounds
            if (bounds == null || bounds.isEmpty) {
                state.card = null
                return@layout
            }
            val card = IntSize(placeable.width, placeable.height)
            val spot = cardSpot(bounds.translate(-state.origin), card, IntSize(width, height), gap.roundToPx())
            state.card = IntRect(spot, card).toRect()
            placeable.place(spot)
        }
    }
