package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A part of the dashboard that can hold focus while it shows. */
internal enum class DashboardArea {
    /** The order status menu. */
    StatusMenu,

    /** The navigation under a phone's top bar. */
    PhoneNav,

    /** The token side panel, docked or laid over a phone. */
    TokenPanel,

    /** The top bar and the page, which the token panel covers on a phone. */
    Main,
}

/**
 * Where keyboard focus goes as the dashboard's panels open and close, in one copy of a split.
 *
 * Both copies show a panel as soon as either opens it, since the app state they share holds the
 * switch, but only the copy that was pressed moves focus. An open from the keyboard takes focus
 * into the panel, and a pick or a close hands it back to the button that opens the panel, so it
 * never drops to the root with the node that held it.
 */
internal class DashboardFocus(
    private val scope: CoroutineScope,
) {
    /** The order status button. */
    val statusButton: FocusRequester = FocusRequester()

    /** The first pick of the status menu. */
    val firstStatus: FocusRequester = FocusRequester()

    /** The Navigation button of a phone's top bar. */
    val navToggle: FocusRequester = FocusRequester()

    /** The first destination of a phone's navigation. */
    val firstDestination: FocusRequester = FocusRequester()

    /** The top bar's button that shows and hides the token panel. */
    val drawerToggle: FocusRequester = FocusRequester()

    /** The token panel's close button. */
    val drawerClose: FocusRequester = FocusRequester()

    /** The open status menu, which the page scrolls to show whole. */
    val statusMenu: BringIntoViewRequester = BringIntoViewRequester()

    /** The areas of this copy that hold focus. Plain, since nothing draws from it. */
    private val holding = mutableSetOf<DashboardArea>()

    fun onFocusChanged(
        area: DashboardArea,
        hasFocus: Boolean,
    ) {
        if (hasFocus) holding += area else holding -= area
    }

    /** Whether [area] holds focus. */
    fun holds(area: DashboardArea): Boolean = area in holding

    /** Moves focus to [target] once the next frame has put it in place. */
    fun moveTo(target: FocusRequester) {
        scope.launch {
            withFrameNanos { }
            // A copy too short to show the target has no node to take focus, and that is fine.
            runCatching { target.requestFocus() }
        }
    }

    /** Hands focus to [toggle] when [area] holds it, before [area] goes and takes it along. */
    fun handBack(
        area: DashboardArea,
        toggle: FocusRequester,
    ) {
        if (area !in holding) return
        runCatching { toggle.requestFocus() }
    }

    /** Scrolls the page once the open status menu is laid out, so all of it shows. */
    fun revealStatusMenu() {
        scope.launch {
            // The menu composes in the next frame but has no place in the page's list until the one after.
            withFrameNanos { }
            withFrameNanos { }
            statusMenu.bringIntoView()
        }
    }
}

/** A [DashboardFocus] for this copy of the dashboard. */
@Composable
internal fun rememberDashboardFocus(): DashboardFocus {
    val scope = rememberCoroutineScope()
    return remember(scope) { DashboardFocus(scope) }
}

/** Lets [focus] know whether [area] holds focus. */
internal fun Modifier.tracksFocus(
    focus: DashboardFocus,
    area: DashboardArea,
): Modifier = onFocusChanged { state -> focus.onFocusChanged(area, state.hasFocus) }
