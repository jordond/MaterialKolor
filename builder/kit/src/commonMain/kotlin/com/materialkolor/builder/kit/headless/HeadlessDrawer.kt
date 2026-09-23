package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.PanelEdge
import com.materialkolor.builder.kit.skin.headless.overlayFeedback
import com.materialkolor.builder.kit.skin.headless.panelEnter
import com.materialkolor.builder.kit.skin.headless.panelExit
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A panel pinned to one edge of the page, over a veil. The projects drawer and the export sheet
 * both stand on it.
 *
 * @param[visible] Whether the panel is open.
 * @param[onDismissRequest] Called on Esc, on the veil and on the close button.
 * @param[title] The panel's name, shown in its header and read as its pane title.
 * @param[closeLabel] What the close button says to assistive technology.
 * @param[edge] The edge the panel is pinned to, [PanelEdge.Start] or [PanelEdge.End].
 * @param[widthFraction] How much of the page's width the panel takes.
 * @param[widthCap] The widest the panel may get.
 * @param[style] The skin's overlay style.
 * @param[returnFocusTo] The trigger that opened the panel.
 * @param[modifier] Applied to the panel.
 * @param[content] The panel's body, below the header.
 */
@Composable
internal fun HeadlessDrawer(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    closeLabel: String,
    edge: PanelEdge,
    widthFraction: Float,
    widthCap: Dp,
    style: OverlayStyle,
    returnFocusTo: FocusRequester?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    require(edge != PanelEdge.Bottom) { "A drawer pins to the start or the end, sheets own the bottom" }
    require(widthFraction > 0f && widthFraction <= 1f) { "A drawer takes a share of the page, got $widthFraction" }
    val alignment = if (edge == PanelEdge.Start) Alignment.CenterStart else Alignment.CenterEnd
    HeadlessModal(visible, onDismissRequest, style.scrim, alignment, returnFocusTo) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = alignment) {
            val fullWidth = widthFraction >= 1f
            val shape = drawerShape(edge, if (fullWidth) 0.dp else style.panelRadius)
            Column(
                modifier = modifier
                    .animateEnterExit(enter = panelEnter(edge), exit = panelExit(edge))
                    .width(min(maxWidth * widthFraction, widthCap))
                    .fillMaxHeight()
                    .shadow(style.shadow, shape)
                    .clip(shape)
                    .background(style.surface)
                    .then(if (style.border != null && !fullWidth) Modifier.border(style.border, shape) else Modifier)
                    .semantics { paneTitle = title }
                    .keepTaps(),
            ) {
                HeadlessPanelHeader(title, closeLabel, onDismissRequest, style)
                content()
            }
        }
    }
}

/** Stops a tap on a panel from falling through to the veil under it and closing the panel. */
internal fun Modifier.keepTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

private fun drawerShape(
    edge: PanelEdge,
    radius: Dp,
): Shape =
    when {
        radius == 0.dp -> RectangleShape
        edge == PanelEdge.Start -> RoundedCornerShape(topEnd = radius, bottomEnd = radius)
        else -> RoundedCornerShape(topStart = radius, bottomStart = radius)
    }

/** A panel's title with its close button, the first stop for the keyboard inside the panel. */
@Composable
internal fun HeadlessPanelHeader(
    title: String,
    closeLabel: String,
    onClose: () -> Unit,
    style: OverlayStyle,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = tokens.spacing.large, end = tokens.spacing.small, top = tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        BuilderText(
            text = title,
            modifier = Modifier.weight(1f),
            style = BuilderTextStyle.Title,
            color = style.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HeadlessCloseButton(closeLabel, onClose, style)
    }
}

/** A square close button as big as the touch target the layout asks for (AR-04). */
@Composable
internal fun HeadlessCloseButton(
    label: String,
    onClick: () -> Unit,
    style: OverlayStyle,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(LocalLayout.current.minTouchTarget)
            .overlayFeedback(interaction, style)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BuilderIcon(IconId.Close, contentDescription = label, tint = style.content)
    }
}
