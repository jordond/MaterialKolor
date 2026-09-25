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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.PanelEdge
import com.materialkolor.builder.kit.skin.headless.overlayFeedback
import com.materialkolor.builder.kit.skin.headless.panelEnter
import com.materialkolor.builder.kit.skin.headless.panelExit
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType

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
 * @param[subtitle] A quieter line under the title, or null for none.
 * @param[footer] What sits along the bottom under a hairline, such as the panel's actions, or null
 * for no footer.
 * @param[content] The panel's body, between the header and the footer. It takes the height they
 * leave and stands [OverlayMetrics.panelPadding] in from the sides.
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
    subtitle: String? = null, // b-511
    footer: (@Composable () -> Unit)? = null, // b-511
    content: @Composable ColumnScope.() -> Unit,
) {
    require(edge != PanelEdge.Bottom) { "A drawer pins to the start or the end, sheets own the bottom" }
    require(widthFraction > 0f && widthFraction <= 1f) { "A drawer takes a share of the page, got $widthFraction" }
    val alignment = if (edge == PanelEdge.Start) Alignment.CenterStart else Alignment.CenterEnd
    HeadlessModal(visible, onDismissRequest, style.scrim, alignment, returnFocusTo) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = alignment) {
            val fullWidth = widthFraction >= 1f
            val shape = drawerShape(edge, if (fullWidth) 0.dp else style.drawerRadius) // b-511
            Column(
                modifier = modifier
                    .animateEnterExit(enter = panelEnter(edge), exit = panelExit(edge))
                    .width(min(maxWidth * widthFraction, widthCap))
                    .fillMaxHeight()
                    .shadow(style.shadow, shape)
                    .clip(shape)
                    .background(style.surface)
                    .then(if (style.border != null && !fullWidth) Modifier.border(style.border, shape) else Modifier)
                    .modalPane(title)
                    .keepTaps(),
            ) {
                // b-511
                val padding = OverlayMetrics.panelPadding
                HeadlessPanelHeader(title, closeLabel, onDismissRequest, style, subtitle)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = padding)
                        .padding(bottom = if (footer == null) padding else 0.dp),
                    content = content,
                )
                if (footer != null) PanelFooter(style, footer)
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

/**
 * A panel's title with its close button, the first stop for the keyboard inside the panel, and
 * [subtitle] under them when there is one. The close button sits on the title's line, and the whole
 * header keeps [OverlayMetrics.panelPadding] from the panel's edges.
 */
@Composable
internal fun HeadlessPanelHeader(
    title: String,
    closeLabel: String,
    onClose: () -> Unit,
    style: OverlayStyle,
    subtitle: String? = null, // b-511
) {
    val tokens = LocalBuilderTokens.current
    val padding = OverlayMetrics.panelPadding
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = padding, end = padding, top = padding, bottom = tokens.spacing.large),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            val type = LocalBuilderType.current
            BasicText(
                text = title,
                modifier = Modifier.weight(1f).modalTitle(),
                style = (style.panelTitle ?: type.title).merge(color = style.content),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HeadlessCloseButton(closeLabel, onClose, style)
        }
        if (subtitle != null) {
            BuilderText(text = subtitle, style = BuilderTextStyle.Body, color = style.muted)
        }
    }
}

/** A panel's footer, under a hairline and in from the edges as far as the header. */
@Composable
private fun PanelFooter(
    style: OverlayStyle,
    content: @Composable () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val padding = OverlayMetrics.panelPadding
    Column(
        Modifier.fillMaxWidth().padding(start = padding, end = padding, top = tokens.spacing.large, bottom = padding),
    ) {
        Box(Modifier.fillMaxWidth().height(tokens.outlineWidth).background(style.divider))
        Box(Modifier.fillMaxWidth().padding(top = tokens.spacing.large)) { content() }
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
