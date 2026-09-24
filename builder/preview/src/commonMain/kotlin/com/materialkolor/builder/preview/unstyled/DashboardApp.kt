package com.materialkolor.builder.preview.unstyled

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.PanelRight
import com.composeunstyled.FocusVisibilityProvider
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledVerticalSeparator
import com.composeunstyled.collectIsFocusVisibleAsState
import com.composeunstyled.focusRing
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose

/** The height of the top bar, which the phone's navigation opens under. */
internal val TopBarHeight = 64.dp

/** How wide the token side panel is, docked or not. */
internal val DrawerWidth = 300.dp

private val SidebarWidth = 240.dp
private val RailWidth = 80.dp
private val NavItemHeight = 40.dp
private val RailItemWidth = 48.dp
private val BrandSize = 32.dp
private const val ScrimAlpha = 0.32f

/**
 * The web dashboard, the Unstyled sample app of the App tab (F-20).
 *
 * Every part is a Compose Unstyled primitive colored from the MaterialKolor tokens of the pane's
 * theme, the way an app on `material-kolor-unstyled` would draw. Everything the app remembers
 * lives in [state], so the two copies of a split agree. A desktop gets the sidebar, the page and
 * the token side panel docked beside it. A tablet folds the sidebar into a rail whose icons name
 * themselves in tooltips. A phone gets a top bar whose menu button shows the navigation in place,
 * and the token panel over the page.
 *
 * The menu and the tooltips open in the app's own layout, never in a popup or a window.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The device the app lays itself out for.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun DashboardApp(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    // Tells keyboard focus from a press, so a click leaves no tooltip or focus ring behind.
    FocusVisibilityProvider(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .previewRoles(UnstyledComponent.App)
                .background(DashboardToken.Surface.color),
        ) {
            when (deviceWidth) {
                DeviceWidth.Phone -> DashboardPhone(state)
                DeviceWidth.Tablet -> DashboardWide(state, rail = true)
                DeviceWidth.Desktop -> DashboardWide(state, rail = false)
            }
        }
    }
}

/** The destination the sidebar has picked. */
internal fun DemoAppState.destination(): DashboardDestination =
    DashboardDestination.entries[choice(DashboardNavChoice, DashboardDestination.entries.size)]

private fun DemoAppState.go(destination: DashboardDestination) {
    choose(DashboardNavChoice, DashboardDestination.entries.size, destination.ordinal)
}

@Composable
private fun DashboardWide(
    state: DemoAppState,
    rail: Boolean,
) {
    Row(Modifier.fillMaxSize()) {
        // The rail's tooltips float over the page, so the rail draws above it.
        if (rail) DashboardRail(state, Modifier.zIndex(1f)) else DashboardSidebar(state)
        DashboardMain(state, phone = false, Modifier.weight(1f))
        AnimatedVisibility(
            visible = state.isOn(DashboardDrawerSwitch),
            enter = expandHorizontally(panelMotion()),
            exit = shrinkHorizontally(panelMotion()),
        ) {
            TokenPanel(state, Modifier.width(DrawerWidth).fillMaxHeight())
        }
    }
}

@Composable
private fun DashboardPhone(state: DemoAppState) {
    Box(Modifier.fillMaxSize()) {
        DashboardMain(state, phone = true, Modifier.fillMaxSize())
        AnimatedVisibility(
            visible = state.isOn(DashboardNavSwitch),
            modifier = Modifier.padding(top = TopBarHeight),
            enter = expandVertically(panelMotion()),
            exit = shrinkVertically(panelMotion()),
        ) {
            PhoneNav(state)
        }
        AnimatedVisibility(
            visible = state.isOn(DashboardDrawerSwitch),
            enter = fadeIn(panelMotion()),
            exit = fadeOut(panelMotion()),
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .matchParentSize()
                        .previewRoles(UnstyledComponent.Scrim)
                        .background(DashboardToken.Scrim.color.copy(alpha = ScrimAlpha))
                        .pointerInput(state) {
                            detectTapGestures { state.setOn(DashboardDrawerSwitch, false) }
                        },
                )
                TokenPanel(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(DrawerWidth)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun DashboardMain(
    state: DemoAppState,
    phone: Boolean,
    modifier: Modifier,
) {
    Column(modifier.fillMaxHeight()) {
        // The top bar's tooltips float over the page, so the bar draws above it.
        DashboardTopBar(state, phone, Modifier.zIndex(1f))
        DashboardPage(state, phone, Modifier.weight(1f))
    }
}

@Composable
private fun DashboardTopBar(
    state: DemoAppState,
    phone: Boolean,
    modifier: Modifier,
) {
    val navOpen = state.isOn(DashboardNavSwitch)
    val drawerOpen = state.isOn(DashboardDrawerSwitch)
    Column(modifier.fillMaxWidth().previewRoles(UnstyledComponent.TopBar)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopBarHeight)
                .padding(horizontal = if (phone) Gap else PageGap),
            horizontalArrangement = Arrangement.spacedBy(Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (phone) {
                DashboardIconButton(
                    icon = Lucide.Menu,
                    label = DashboardCopy.Navigation,
                    onClick = { state.setOn(DashboardNavSwitch, !navOpen) },
                    tooltip = Overhang.BelowStart,
                    toggled = navOpen,
                )
            }
            Text(
                text = state.destination().label,
                modifier = Modifier.weight(1f),
                style = TitleStyle,
                color = DashboardToken.OnSurface.color,
                maxLines = 1,
            )
            if (!phone) PageActions()
            DashboardIconButton(
                icon = Lucide.PanelRight,
                label = if (drawerOpen) DashboardCopy.HideTokens else DashboardCopy.ShowTokens,
                onClick = { state.setOn(DashboardDrawerSwitch, !drawerOpen) },
                tooltip = Overhang.BelowEnd,
                toggled = drawerOpen,
            )
        }
        UnstyledHorizontalSeparator(DashboardToken.OutlineVariant.color)
    }
}

@Composable
private fun DashboardSidebar(state: DemoAppState) {
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

@Composable
private fun DashboardRail(
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

/** The navigation under a phone's top bar. A pick goes there and puts the navigation away. */
@Composable
private fun PhoneNav(state: DemoAppState) {
    val current = state.destination()
    Column(
        Modifier
            .fillMaxWidth()
            .previewRoles(UnstyledComponent.Sidebar)
            .background(DashboardToken.SurfaceContainerLow.color),
    ) {
        Column(Modifier.padding(Gap), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (destination in DashboardDestination.entries) {
                NavItem(destination, selected = destination == current, compact = false) {
                    state.go(destination)
                    state.setOn(DashboardNavSwitch, false)
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
 * tooltip.
 */
@Composable
private fun NavItem(
    destination: DashboardDestination,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusVisibleAsState()
    val content = if (selected) DashboardToken.OnSecondaryContainer.color else DashboardToken.OnSurfaceVariant.color
    val bounds = if (compact) {
        Modifier.size(RailItemWidth, NavItemHeight)
    } else {
        Modifier.fillMaxWidth().height(NavItemHeight)
    }
    Box {
        UnstyledButton(
            onClick = onClick,
            modifier = bounds
                .previewRoles(if (selected) UnstyledComponent.SelectedNavItem else UnstyledComponent.NavItem)
                .semantics { this.selected = selected }
                .focusRing(interactions, 2.dp, DashboardToken.Primary.color, ControlShape)
                .clip(ControlShape)
                .background(if (selected) DashboardToken.SecondaryContainer.color else Color.Transparent),
            contentPadding = if (compact) PaddingValues() else PaddingValues(horizontal = 12.dp),
            interactionSource = interactions,
            contentAlignment = if (compact) Alignment.Center else Alignment.CenterStart,
        ) {
            if (compact) {
                UnstyledIcon(destination.icon, destination.label, modifier = Modifier.size(IconSize), tint = content)
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
        if (compact && (hovered || focused)) {
            DashboardTooltip(
                text = destination.label,
                modifier = Modifier.align(Overhang.After.alignment).overhang(Overhang.After, SectionGap),
            )
        }
    }
}
