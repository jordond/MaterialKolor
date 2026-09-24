package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.overlayStyle
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.text_toolbar_copy
import com.materialkolor.builder.kit.generated.resources.text_toolbar_cut
import com.materialkolor.builder.kit.generated.resources.text_toolbar_paste
import com.materialkolor.builder.kit.generated.resources.text_toolbar_select_all
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.overlayFeedback
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.roundToInt

/** A button the page's text toolbar can show, in the order the row lays them out. */
internal enum class TextToolbarAction {
    Cut,
    Copy,
    Paste,
    SelectAll,
}

/**
 * One menu the page's text toolbar shows.
 *
 * @property[rect] The selection's bounds in the root, as foundation reports them.
 * @property[actions] What each button does, in the row's order. A button foundation hands no
 * callback for is left out.
 */
internal class TextToolbarMenu(
    val rect: Rect,
    val actions: Map<TextToolbarAction, () -> Unit>,
)

/**
 * The text selection toolbar where overlays render in the page (D40).
 *
 * Foundation's web toolbar opens in a popup, which would take the web mirror over, so the root
 * [OverlayHost] provides this one in its place and draws its menu last, over every layer and the
 * top slot. A text field shows a menu on a touch selection with the selection's bounds and a
 * callback for each button it offers. It hides it again once the selection collapses or the field
 * blurs, and after Cut, Copy or Paste. Select all shows it again over the whole text.
 *
 * A field shows its menu again each time it moves, so a scroll would drag the row along with it.
 * A scroll in the host hides the row instead, and the menus a field shows while the scroll runs are
 * dropped. Once the scroll settles, or the next press lands, the next menu shows again.
 */
internal class PageTextToolbar : TextToolbar {
    /** The menu on show, or null while the toolbar is hidden. */
    var menu: TextToolbarMenu? by mutableStateOf(null)
        private set

    /** Whether a scroll in the host is moving the page under the row right now. */
    private var scrolling = false

    /**
     * Hides the row as soon as a scroll moves something in the host and keeps it hidden until the
     * fling that ends the scroll has run out.
     */
    val scrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (consumed != Offset.Zero) {
                scrolling = true
                hide()
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity,
        ): Velocity {
            scrolling = false
            return Velocity.Zero
        }
    }

    /**
     * Notes a new press. A fling that a press stopped never reports its end, so the press ends the
     * scroll instead, and a drag that follows starts a new one.
     */
    fun pressed() {
        scrolling = false
    }

    /**
     * The locals of the skin the builder is drawn in, which the host above the skin cannot see.
     * The row wears that skin's menu dress, and until they arrive nothing is drawn.
     */
    var locals: CompositionLocalContext? by mutableStateOf(null)

    /**
     * Follows [menu] without subscribing the reader. A text field reads it while it composes, and
     * a field that recomposed on every menu shown would hide the next one straight away.
     */
    override val status: TextToolbarStatus
        get() {
            val shown = Snapshot.withoutReadObservation { menu != null }
            return if (shown) TextToolbarStatus.Shown else TextToolbarStatus.Hidden
        }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        if (scrolling) return
        val actions = buildMap {
            if (onCutRequested != null) put(TextToolbarAction.Cut, onCutRequested)
            if (onCopyRequested != null) put(TextToolbarAction.Copy, onCopyRequested)
            if (onPasteRequested != null) put(TextToolbarAction.Paste, onPasteRequested)
            if (onSelectAllRequested != null) put(TextToolbarAction.SelectAll, onSelectAllRequested)
        }
        menu = if (actions.isEmpty()) null else TextToolbarMenu(rect, actions)
    }

    override fun hide() {
        menu = null
    }
}

/**
 * Hands the page's text toolbar the locals it is called in.
 *
 * `BuilderTheme` calls it once inside the skin, so the row the root host draws wears the builder's
 * own skin, and it does nothing where the toolbar in reach is not the page's.
 */
@Composable
internal fun PageTextToolbarLocals() {
    val toolbar = LocalTextToolbar.current as? PageTextToolbar ?: return
    val locals = currentCompositionLocalContext
    SideEffect { toolbar.locals = locals }
}

/**
 * Draws the menu [toolbar] shows as one row of buttons over the whole of [host].
 *
 * The row sits above the selection with a gap, or below it when there is no room above, and stays
 * inside the host. The selection's bounds come in the root's coordinates, a scaled preview pane's
 * scale already in them, and are mapped into the host's, which only moves them when the host does
 * not sit at the root's origin. While none of the selection lies inside the host, a field scrolled
 * out of the page say, no row is drawn at all rather than one pinned to the host's edge. The layer
 * spans the host but only the row takes presses, so a button in the page beside it still answers.
 *
 * Each button runs its callback straight from its own `onClick`, with no effect, dispatch,
 * snapshot hop or `launch` in between. Paste reads the browser's clipboard, which the browser only
 * allows during the user activation of the tap itself. The buttons never take focus, so the field
 * keeps its focus and its selection, and a field that commits on blur commits nothing.
 */
@Composable
internal fun PageTextToolbarLayer(
    toolbar: PageTextToolbar,
    host: OverlayHostState,
    modifier: Modifier,
) {
    val menu = toolbar.menu ?: return
    val locals = toolbar.locals ?: return
    // Read along with the menu, which a field shows again each time it moves.
    val selection = host.visibleFromRoot(menu.rect) ?: return
    CompositionLocalProvider(locals) {
        val spacing = LocalBuilderTokens.current.spacing
        val gap = with(LocalDensity.current) { spacing.small.roundToPx() }
        Layout(
            content = { TextToolbarRow(menu, overlayStyle(LocalSkin.current.library)) },
            modifier = modifier,
        ) { measurables, constraints ->
            val row = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            layout(width, height) {
                val above = selection.top.roundToInt() - gap - row.height
                val y = if (above >= 0) above else selection.bottom.roundToInt() + gap
                val x = selection.center.x.roundToInt() - row.width / 2
                row.place(
                    x = x.coerceIn(0, max(0, width - row.width)),
                    y = y.coerceIn(0, max(0, height - row.height)),
                )
            }
        }
    }
}

/**
 * Hides [toolbar]'s row while a scroll in the host moves the page, and notes each press that lands
 * in the host so a fling cut short by one does not keep the row hidden.
 */
internal fun Modifier.pageTextToolbarScroll(toolbar: PageTextToolbar): Modifier =
    nestedScroll(toolbar.scrollConnection).pointerInput(toolbar) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { change -> change.changedToDownIgnoreConsumed() }) toolbar.pressed()
            }
        }
    }

/**
 * The smallest a button may be. The row only ever answers a touch, and a coarse pointer gets the same
 * target at every size.
 */
private val TouchTarget: Dp = LayoutInfo(widthDp = 0.dp, heightDp = 0.dp, coarsePointer = true).minTouchTarget

@Composable
private fun TextToolbarRow(
    menu: TextToolbarMenu,
    style: OverlayStyle,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = Modifier
            .shadow(style.shadow, style.popoverShape)
            .clip(style.popoverShape)
            .background(style.surface)
            .then(if (style.border != null) Modifier.border(style.border, style.popoverShape) else Modifier)
            .padding(tokens.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
    ) {
        for ((action, onClick) in menu.actions) {
            key(action) { TextToolbarButton(label(action), onClick, style) }
        }
    }
}

@Composable
private fun TextToolbarButton(
    label: String,
    onClick: () -> Unit,
    style: OverlayStyle,
) {
    val tokens = LocalBuilderTokens.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .sizeIn(minWidth = TouchTarget, minHeight = TouchTarget)
            .overlayFeedback(interaction, style)
            .focusProperties { canFocus = false }
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = tokens.spacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        BuilderText(label, style = BuilderTextStyle.Label, color = style.content, maxLines = 1)
    }
}

@Composable
private fun label(action: TextToolbarAction): String =
    when (action) {
        TextToolbarAction.Cut -> stringResource(Res.string.text_toolbar_cut)
        TextToolbarAction.Copy -> stringResource(Res.string.text_toolbar_copy)
        TextToolbarAction.Paste -> stringResource(Res.string.text_toolbar_paste)
        TextToolbarAction.SelectAll -> stringResource(Res.string.text_toolbar_select_all)
    }

/** [rect] taken from the root's coordinates into the host's, or null while none of it lies inside the host. */
private fun OverlayHostState.visibleFromRoot(rect: Rect): Rect? {
    val layout = coordinates?.takeIf { it.isAttached } ?: return null
    val root = layout.findRootCoordinates()
    val mapped = Rect(layout.localPositionOf(root, rect.topLeft), layout.localPositionOf(root, rect.bottomRight))
    val size = layout.size
    val inside = mapped.right >= 0f && mapped.bottom >= 0f && mapped.left <= size.width && mapped.top <= size.height
    return if (inside) mapped else null
}
