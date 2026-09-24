package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.composeunstyled.Text
import com.composeunstyled.UnstyledVerticalSeparator
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.canvas.DemoAppState

private val SwatchSize = 24.dp
private val SwatchShape = RoundedCornerShape(6.dp)

/**
 * The token side panel, the drawer of the dashboard. It lists every MaterialKolor token the
 * screen reads, by name and with a swatch of its color in the pane's mode.
 *
 * The panel is part of the app's layout, docked beside the page on a tablet or desktop and laid
 * over it on a phone. Its close button and the top bar's toggle both put it away, and focus goes
 * back to the toggle if the panel held it. The app leaves Back to the builder.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[focus] Where focus goes in this copy as the panel opens and closes.
 * @param[modifier] Applied to the panel. Size it here.
 */
@Composable
internal fun TokenPanel(
    state: DemoAppState,
    focus: DashboardFocus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .tracksFocus(focus, DashboardArea.TokenPanel)
            .previewRoles(UnstyledComponent.Drawer)
            .background(DashboardToken.SurfaceContainerLow.color),
    ) {
        UnstyledVerticalSeparator(DashboardToken.OutlineVariant.color)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // The close button's tooltip floats over the list, so the header draws above it.
            Row(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxWidth()
                    .height(TopBarHeight)
                    .padding(start = SectionGap, end = Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DashboardCopy.Tokens,
                    modifier = Modifier.weight(1f),
                    style = HeadingStyle,
                    color = DashboardToken.OnSurface.color,
                )
                DashboardIconButton(
                    icon = Lucide.X,
                    label = DashboardCopy.CloseTokens,
                    onClick = { focus.closeTokenPanel(state) },
                    tooltip = Overhang.BelowEnd,
                    focusRequester = focus.drawerClose,
                    on = DashboardToken.SurfaceContainerLow,
                )
            }
            Text(
                text = DashboardCopy.TokensNote,
                modifier = Modifier.padding(horizontal = SectionGap),
                style = SmallStyle,
                color = DashboardToken.OnSurfaceVariant.color,
            )
            LazyColumn(
                state = state.rememberListState(DashboardTokenList),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(SectionGap),
                verticalArrangement = Arrangement.spacedBy(Gap),
            ) {
                items(DashboardToken.entries, key = { entry -> entry.name }) { entry -> TokenRow(entry) }
            }
        }
    }
}

/** Puts the token panel away, and hands focus back to the top bar's toggle if the panel held it. */
internal fun DashboardFocus.closeTokenPanel(state: DemoAppState) {
    state.setOn(DashboardDrawerSwitch, false)
    handBack(DashboardArea.TokenPanel, drawerToggle)
}

/**
 * A token's swatch and name. The swatch declares the token's role and its border's, so Inspect
 * names them too.
 */
@Composable
private fun TokenRow(entry: DashboardToken) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(SwatchSize)
                .previewRoles(*listOf(entry.role, Role.OutlineVariant).distinct().toTypedArray())
                .clip(SwatchShape)
                .background(entry.color)
                .border(1.dp, DashboardToken.OutlineVariant.color, SwatchShape),
        )
        Text(
            text = entry.token.name,
            style = BodyStyle,
            fontFamily = FontFamily.Monospace,
            color = DashboardToken.OnSurface.color,
            maxLines = 1,
        )
    }
}
