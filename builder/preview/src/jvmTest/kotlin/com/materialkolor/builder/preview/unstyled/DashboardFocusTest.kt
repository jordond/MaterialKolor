package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** The clickable destination called [destination], in the sidebar or a phone's navigation. */
private fun navItem(destination: DashboardDestination): SemanticsMatcher =
    hasClickAction() and hasText(destination.label)

@OptIn(ExperimentalTestApi::class)
class DashboardFocusTest {
    @Test
    fun statusMenu_keyboardOpenThenPick_takesFocusInAndHandsItBack() =
        runDesktopComposeUiTest(1280, 800) {
            val state = dashboard(DeviceWidth.Desktop)

            onNode(StatusButton).requestFocus().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            state.isOn(DashboardMenuSwitch) shouldBe true
            onNode(menuItem(OrderFilter.All), useUnmergedTree = true).assertIsFocused()

            onNode(menuItem(OrderFilter.Paid), useUnmergedTree = true).requestFocus().performKeyInput {
                pressKey(Key.Enter)
            }
            waitForIdle()
            state.isOn(DashboardMenuSwitch) shouldBe false
            state.choice(DashboardFilterChoice, OrderFilter.entries.size) shouldBe OrderFilter.Paid.ordinal
            onNode(StatusButton).assertIsFocused()
        }

    @Test
    fun statusMenu_openedAt1280x800_scrollsWholeIntoTheFrame() =
        runDesktopComposeUiTest(1280, 800) {
            dashboard(DeviceWidth.Desktop)
            val frame = onRoot().fetchSemanticsNode().boundsInRoot

            // Left where it opens at this size, the menu shows two of its five picks whole.
            onNode(StatusButton).performClick()
            waitForIdle()
            val first = onNode(menuItem(OrderFilter.All), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val last = onNode(menuItem(OrderFilter.Refunded), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            // Bounds in the root are clipped to what shows, so a pick the frame cuts off comes out short.
            last.height shouldBe first.height
            first.top shouldBeGreaterThanOrEqual frame.top
            last.bottom shouldBeLessThanOrEqual frame.bottom
        }

    @Test
    fun phoneNav_keyboardOpenThenPick_takesFocusInAndHandsItBack() =
        runDesktopComposeUiTest(412, 900) {
            val state = dashboard(DeviceWidth.Phone)
            val toggle = onNode(iconButton(DashboardCopy.Navigation))

            toggle.requestFocus().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            state.isOn(DashboardNavSwitch) shouldBe true
            onNode(navItem(DashboardDestination.Overview)).assertIsFocused()

            onNode(navItem(DashboardDestination.Customers)).requestFocus().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            state.isOn(DashboardNavSwitch) shouldBe false
            state.destination() shouldBe DashboardDestination.Customers
            toggle.assertIsFocused()
        }

    @Test
    fun dockedDrawer_keyboardOpenThenClose_takesFocusInAndHandsItBack() =
        runDesktopComposeUiTest(1280, 800) {
            val state = dashboard(DeviceWidth.Desktop)

            onNode(iconButton(DashboardCopy.ShowTokens)).requestFocus().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            state.isOn(DashboardDrawerSwitch) shouldBe true
            val close = onNode(iconButton(DashboardCopy.CloseTokens))
            close.assertIsFocused()

            close.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            state.isOn(DashboardDrawerSwitch) shouldBe false
            onNode(iconButton(DashboardCopy.ShowTokens)).assertIsFocused()
        }

    @Test
    fun phoneDrawer_open_keepsThePageOutOfFocusAndSemanticsAndClosesOnEsc() =
        runDesktopComposeUiTest(412, 900) {
            val state = dashboard(DeviceWidth.Phone)
            val navigation = onAllNodes(iconButton(DashboardCopy.Navigation))
            navigation.assertCountEquals(1)

            onNode(iconButton(DashboardCopy.ShowTokens)).requestFocus().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            state.isOn(DashboardDrawerSwitch) shouldBe true
            val close = onNode(iconButton(DashboardCopy.CloseTokens))
            close.assertIsFocused()
            navigation.assertCountEquals(0)

            // Tab finds nothing to take focus in the page under the scrim.
            close.performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            close.assertIsFocused()

            // The first Esc hides the close button's tooltip, the second puts the panel away.
            onNodeWithText(DashboardCopy.CloseTokens).assertExists()
            close.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onAllNodesWithText(DashboardCopy.CloseTokens).assertCountEquals(0)
            state.isOn(DashboardDrawerSwitch) shouldBe true

            close.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            state.isOn(DashboardDrawerSwitch) shouldBe false
            navigation.assertCountEquals(1)
            onNode(iconButton(DashboardCopy.ShowTokens)).assertIsFocused()
        }

    @Test
    fun tooltip_esc_hidesItUntilFocusComesBack() =
        runDesktopComposeUiTest(840, 900) {
            dashboard(DeviceWidth.Tablet)
            val toggle = onNode(iconButton(DashboardCopy.ShowTokens))
            val tooltip = onAllNodesWithText(DashboardCopy.ShowTokens)

            toggle.requestFocus()
            waitForIdle()
            tooltip.assertCountEquals(1)
            toggle.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            tooltip.assertCountEquals(0)
            toggle.assertIsFocused()

            onNode(iconButton(DashboardDestination.Customers.label)).requestFocus()
            waitForIdle()
            toggle.requestFocus()
            waitForIdle()
            tooltip.assertCountEquals(1)
        }

    @Test
    fun phoneNavTooltip_whileTheNavigationIsOpen_staysHidden() =
        runDesktopComposeUiTest(412, 900) {
            val state = dashboard(DeviceWidth.Phone)
            val toggle = onNode(iconButton(DashboardCopy.Navigation))
            val tooltip = onAllNodesWithText(DashboardCopy.Navigation)

            toggle.performMouseInput { enter(center) }
            waitForIdle()
            tooltip.assertCountEquals(1)

            toggle.performMouseInput { click(center) }
            waitForIdle()
            state.isOn(DashboardNavSwitch) shouldBe true
            tooltip.assertCountEquals(0)

            toggle.performMouseInput { click(center) }
            waitForIdle()
            state.isOn(DashboardNavSwitch) shouldBe false
            tooltip.assertCountEquals(1)
        }
}

/** The dashboard for [width] filling the window, with its state to look into. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.dashboard(width: DeviceWidth): DemoAppState {
    val state = DemoAppState()
    setContent { DashboardHarness(LightSpec, state, width, Modifier.fillMaxSize()) }
    return state
}
