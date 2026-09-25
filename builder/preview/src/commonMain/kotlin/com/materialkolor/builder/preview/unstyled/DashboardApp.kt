package com.materialkolor.builder.preview.unstyled

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.PanelRight
import com.composeunstyled.FocusVisibilityProvider
import com.composeunstyled.Text
import com.composeunstyled.UnstyledHorizontalSeparator
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState

/**
 * The height of the top bar, which the phone's navigation opens under.
 */
internal val TopBarHeight = 64.dp

/**
 * How wide the token side panel is, docked or not.
 */
internal val DrawerWidth = 300.dp

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
    val focus = rememberDashboardFocus()
    // Tells keyboard focus from a press, so a click leaves no tooltip or focus ring behind.
    FocusVisibilityProvider(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .previewRoles(UnstyledComponent.App)
                .background(DashboardToken.Surface.color),
        ) {
            when (deviceWidth) {
                DeviceWidth.Phone -> DashboardPhone(state, focus)
                DeviceWidth.Tablet -> DashboardWide(state, focus, rail = true)
                DeviceWidth.Desktop -> DashboardWide(state, focus, rail = false)
            }
        }
    }
}

@Composable
private fun DashboardWide(
    state: DemoAppState,
    focus: DashboardFocus,
    rail: Boolean,
) {
    Row(Modifier.fillMaxSize()) {
        // The rail's tooltips float over the page, so the rail draws above it.
        if (rail) DashboardRail(state, Modifier.zIndex(1f)) else DashboardSidebar(state)
        DashboardMain(state, focus, phone = false, Modifier.weight(1f))
        AnimatedVisibility(
            visible = state.isOn(DashboardDrawerSwitch),
            enter = expandHorizontally(panelMotion()),
            exit = shrinkHorizontally(panelMotion()),
        ) {
            TokenPanel(state, focus, Modifier.width(DrawerWidth).fillMaxHeight())
        }
    }
}

/**
 * The phone's layout. The token panel lies over the top bar and the page as a modal would, so
 * while it is open they leave the Tab order and the semantics tree, and Esc puts the panel away.
 * Back stays with the builder.
 */
@Composable
private fun DashboardPhone(
    state: DemoAppState,
    focus: DashboardFocus,
) {
    val drawerOpen = state.isOn(DashboardDrawerSwitch)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .tracksFocus(focus, DashboardArea.Main)
                .focusProperties {
                    // Read when focus tries to enter, so it sees the panel close in the same frame.
                    onEnter = { if (state.isOn(DashboardDrawerSwitch)) cancelFocusChange() }
                }.focusGroup()
                .then(if (drawerOpen) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            DashboardMain(state, focus, phone = true, Modifier.fillMaxSize())
            AnimatedVisibility(
                visible = state.isOn(DashboardNavSwitch),
                modifier = Modifier.padding(top = TopBarHeight),
                enter = expandVertically(panelMotion()),
                exit = shrinkVertically(panelMotion()),
            ) {
                PhoneNav(state, focus)
            }
        }
        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(panelMotion()),
            exit = fadeOut(panelMotion()),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                        if (escape) focus.closeTokenPanel(state)
                        escape
                    },
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .previewRoles(UnstyledComponent.Scrim)
                        .background(DashboardToken.Scrim.color.copy(alpha = ScrimAlpha))
                        .pointerInput(state, focus) {
                            detectTapGestures { focus.closeTokenPanel(state) }
                        },
                )
                TokenPanel(
                    state = state,
                    focus = focus,
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
    focus: DashboardFocus,
    phone: Boolean,
    modifier: Modifier,
) {
    Column(modifier.fillMaxHeight()) {
        // The top bar's tooltips float over the page, so the bar draws above it.
        DashboardTopBar(state, focus, phone, Modifier.zIndex(1f))
        DashboardPage(state, focus, phone, Modifier.weight(1f))
    }
}

@Composable
private fun DashboardTopBar(
    state: DemoAppState,
    focus: DashboardFocus,
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
                    onClick = { keyboard ->
                        state.setOn(DashboardNavSwitch, !navOpen)
                        if (navOpen) {
                            focus.handBack(DashboardArea.PhoneNav, focus.navToggle)
                        } else if (keyboard) {
                            focus.moveTo(focus.firstDestination)
                        }
                    },
                    tooltip = Overhang.BelowStart,
                    toggled = navOpen,
                    expanded = navOpen,
                    focusRequester = focus.navToggle,
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
                onClick = { keyboard ->
                    state.setOn(DashboardDrawerSwitch, !drawerOpen)
                    if (drawerOpen) {
                        focus.handBack(DashboardArea.TokenPanel, focus.drawerToggle)
                    } else if (keyboard || (phone && focus.holds(DashboardArea.Main))) {
                        // On a phone the panel covers whatever held focus, so focus follows it in.
                        focus.moveTo(focus.drawerClose)
                    }
                },
                tooltip = Overhang.BelowEnd,
                toggled = drawerOpen,
                focusRequester = focus.drawerToggle,
            )
        }
        UnstyledHorizontalSeparator(DashboardToken.OutlineVariant.color)
    }
}
