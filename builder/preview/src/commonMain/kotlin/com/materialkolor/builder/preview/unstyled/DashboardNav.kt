package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Lucide
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledVerticalSeparator
import com.composeunstyled.focusRing
import com.materialkolor.builder.kit.control.foldedSelectedName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose

private val SidebarWidth = 240.dp
private val RailWidth = 80.dp
private val NavItemHeight = 40.dp
private val RailItemWidth = 48.dp
private val BrandSize = 32.dp

/** The destination the sidebar has picked. */
internal fun DemoAppState.destination(): DashboardDestination =
    DashboardDestination.entries[choice(DashboardNavChoice, DashboardDestination.entries.size)]

private fun DemoAppState.go(destination: DashboardDestination) {
    choose(DashboardNavChoice, DashboardDestination.entries.size, destination.ordinal)
}

/** The desktop's sidebar, the brand over every destination with its name. */
@Composable
internal fun DashboardSidebar(state: DemoAppState) {
    val current = state.destination()
    Row(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(SidebarWidth)
                .fillMaxHeight()
                .previewRoles(UnstyledComponent.Sidebar)
                .background(DashboardToken.SurfaceContainerLow.color)
                .padding(SectionGap),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.padding(bottom = SectionGap),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark()
                Text(DashboardCopy.Brand, style = HeadingStyle, color = DashboardToken.OnSurface.color)
            }
            for (destination in DashboardDestination.entries) {
                NavItem(destination, selected = destination == current, compact = false) { state.go(destination) }
            }
        }
        UnstyledVerticalSeparator(DashboardToken.OutlineVariant.color)
    }
}

/** The tablet's rail, the sidebar folded down to its icons. */
@Composable
internal fun DashboardRail(
    state: DemoAppState,
    modifier: Modifier,
) {
    val current = state.destination()
    Row(modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(RailWidth)
                .fillMaxHeight()
                .previewRoles(UnstyledComponent.Sidebar)
                .background(DashboardToken.SurfaceContainerLow.color)
                .padding(vertical = SectionGap),
            verticalArrangement = Arrangement.spacedBy(Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(Modifier.padding(bottom = Gap))
            for (destination in DashboardDestination.entries) {
                NavItem(destination, selected = destination == current, compact = true) { state.go(destination) }
            }
        }
        UnstyledVerticalSeparator(DashboardToken.OutlineVariant.color)
    }
}

/**
 * The navigation under a phone's top bar. A pick goes there and puts the navigation away, and
 * focus goes back to the Navigation button if the navigation held it.
 */
@Composable
internal fun PhoneNav(
    state: DemoAppState,
    focus: DashboardFocus,
) {
    val current = state.destination()
    Column(
        Modifier
            .fillMaxWidth()
            .tracksFocus(focus, DashboardArea.PhoneNav)
            .previewRoles(UnstyledComponent.Sidebar)
            .background(DashboardToken.SurfaceContainerLow.color),
    ) {
        Column(Modifier.padding(Gap), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (destination in DashboardDestination.entries) {
                val first = destination.ordinal == 0
                NavItem(
                    destination = destination,
                    selected = destination == current,
                    compact = false,
                    modifier = if (first) Modifier.focusRequester(focus.firstDestination) else Modifier,
                ) {
                    state.go(destination)
                    state.setOn(DashboardNavSwitch, false)
                    focus.handBack(DashboardArea.PhoneNav, focus.navToggle)
                }
            }
        }
        UnstyledHorizontalSeparator(DashboardToken.OutlineVariant.color)
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BrandSize)
            .previewRoles(UnstyledComponent.Brand)
            .clip(ControlShape)
            .background(DashboardToken.Primary.color),
        contentAlignment = Alignment.Center,
    ) {
        UnstyledIcon(
            imageVector = Lucide.Activity,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = DashboardToken.OnPrimary.color,
        )
    }
}

/**
 * A sidebar destination, its icon and label, or its icon alone in the rail with the label as a
 * tooltip. On the web its name carries whether it is the current one.
 *
 * @param[modifier] Applied to the button, where a focus requester takes effect.
 */
@Composable
private fun NavItem(
    destination: DashboardDestination,
    selected: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val visibility = tooltipVisibility(interactions)
    val content = if (selected) DashboardToken.OnSecondaryContainer.color else DashboardToken.OnSurfaceVariant.color
    val bounds = if (compact) {
        Modifier.size(RailItemWidth, NavItemHeight)
    } else {
        Modifier.fillMaxWidth().height(NavItemHeight)
    }
    Box(visibility.onEscape) {
        UnstyledButton(
            onClick = onClick,
            modifier = modifier
                .then(bounds)
                .previewRoles(if (selected) UnstyledComponent.SelectedNavItem else UnstyledComponent.NavItem)
                .semantics { this.selected = selected }
                .foldedSelectedName(destination.label, selected)
                .focusRing(interactions, 2.dp, DashboardToken.Primary.color, ControlShape)
                .clip(ControlShape)
                .background(if (selected) DashboardToken.SecondaryContainer.color else Color.Transparent),
            contentPadding = if (compact) PaddingValues() else PaddingValues(horizontal = 12.dp),
            interactionSource = interactions,
            contentAlignment = if (compact) Alignment.Center else Alignment.CenterStart,
        ) {
            if (compact) {
                UnstyledIcon(destination.icon, null, modifier = Modifier.size(IconSize), tint = content)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UnstyledIcon(destination.icon, null, modifier = Modifier.size(IconSize), tint = content)
                    Text(destination.label, style = LabelStyle, color = content)
                }
            }
        }
        if (compact && visibility.shown) {
            DashboardTooltip(
                text = destination.label,
                modifier = Modifier.align(Overhang.After.alignment).overhang(Overhang.After, SectionGap),
            )
        }
    }
}
