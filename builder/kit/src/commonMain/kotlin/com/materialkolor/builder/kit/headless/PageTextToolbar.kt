package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
 * callback for each button it offers, and hides it again once the selection collapses, the field
 * blurs or a button has run.
 */
internal class PageTextToolbar : TextToolbar {
    /** The menu on show, or null while the toolbar is hidden. */
    var menu: TextToolbarMenu? by mutableStateOf(null)
        private set

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
 * inside the host. The selection's bounds come in the root's coordinates and are mapped into the
 * host's, so a field in a scaled preview pane still gets the row at its selection. The layer
 * spans the host but only the row takes presses, so the page around it stays live.
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
    CompositionLocalProvider(locals) {
        val spacing = LocalBuilderTokens.current.spacing
        val gap = with(LocalDensity.current) { spacing.small.roundToPx() }
        Layout(
            content = { TextToolbarRow(menu, overlayStyle(LocalSkin.current.library), touchTarget(host)) },
            modifier = modifier,
        ) { measurables, constraints ->
            val row = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            layout(width, height) {
                val selection = host.fromRoot(menu.rect)
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
 * The smallest a button may be. The row only ever answers a touch, so it takes the target a coarse
 * pointer gets at the host's size.
 */
@Composable
private fun touchTarget(host: OverlayHostState): Dp {
    val size = host.coordinates?.size ?: IntSize.Zero
    val layout = with(LocalDensity.current) {
        LayoutInfo.of(size.width.toDp(), size.height.toDp(), coarsePointer = true)
    }
    return layout.minTouchTarget
}

@Composable
private fun TextToolbarRow(
    menu: TextToolbarMenu,
    style: OverlayStyle,
    target: Dp,
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
            key(action) { TextToolbarButton(label(action), onClick, style, target) }
        }
    }
}

@Composable
private fun TextToolbarButton(
    label: String,
    onClick: () -> Unit,
    style: OverlayStyle,
    target: Dp,
) {
    val tokens = LocalBuilderTokens.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = target)
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

/** [rect] taken from the root's coordinates into the host's, or as it is before the host is placed. */
private fun OverlayHostState.fromRoot(rect: Rect): Rect {
    val layout = coordinates?.takeIf { it.isAttached } ?: return rect
    val root = layout.findRootCoordinates()
    return Rect(layout.localPositionOf(root, rect.topLeft), layout.localPositionOf(root, rect.bottomRight))
}
