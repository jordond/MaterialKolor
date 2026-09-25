package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.inspect.PreviewRoles
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import androidx.compose.ui.semantics.Role as SemanticsRole

/**
 * The tab of the range picker called [range].
 */
private fun tab(range: DashboardRange): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, SemanticsRole.Tab) and hasText(range.label)

/**
 * The kit's state words for a panel that is shut and one that is open, as a state description.
 */
private const val COLLAPSED = "Collapsed"
private const val EXPANDED = "Expanded"

/**
 * A clickable whose name starts with [name], with or without a state folded in after it.
 */
private fun named(name: String): SemanticsMatcher = hasClickAction() and hasContentDescription(name, substring = true)

@OptIn(ExperimentalTestApi::class)
class DashboardStatesTest {
    @Test
    fun panelButtons_offTheWeb_reportExpandedAndOpenFromTheAction() =
        runComposeUiTest {
            val state = shutDashboard(DeviceWidth.Phone, folds = false)
            val navigation = onNode(iconButton(DashboardCopy.Navigation))
            navigation.assert(hasStateDescription(COLLAPSED))
            navigation.performSemanticsAction(SemanticsActions.Expand)
            waitForIdle()
            state.isOn(DashboardNavSwitch) shouldBe true
            navigation.assert(hasStateDescription(EXPANDED))
            navigation.assert(hasContentDescriptionExactly(DashboardCopy.Navigation))

            val status = onNode(StatusButton)
            status.assert(hasStateDescription(COLLAPSED))
            status.performSemanticsAction(SemanticsActions.Expand)
            waitForIdle()
            state.isOn(DashboardMenuSwitch) shouldBe true
            status.assert(hasStateDescription(EXPANDED))
            status.performSemanticsAction(SemanticsActions.Collapse)
            waitForIdle()
            state.isOn(DashboardMenuSwitch) shouldBe false
            status.assertName(OrderFilter.All.label)
        }

    @Test
    fun picks_offTheWeb_reportSelectedAndGoByTheirTextAlone() =
        runComposeUiTest {
            val state = shutDashboard(DeviceWidth.Desktop, folds = false)
            state.setOn(DashboardMenuSwitch, true)
            waitForIdle()
            onNode(tab(DashboardRange.Week)).assert(isSelected()).assertName(DashboardRange.Week.label)
            val overview = onNode(hasClickAction() and hasText(DashboardDestination.Overview.label))
            overview.assert(isSelected()).assertName(DashboardDestination.Overview.label)
            onNode(menuItem(OrderFilter.All), useUnmergedTree = true)
                .assert(isSelected())
                .assertName(OrderFilter.All.label)
        }

    @Test
    fun panelButtons_onTheWeb_foldTheExpandedStateIntoTheName() =
        runComposeUiTest {
            val state = shutDashboard(DeviceWidth.Phone, folds = true)
            val navigation = onNode(named(DashboardCopy.Navigation))
            navigation.assertName("Navigation, collapsed")
            navigation.performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(DashboardNavSwitch) shouldBe true
            navigation.assertName("Navigation, expanded")

            val status = onNode(StatusButton)
            status.assertName("All statuses, collapsed")
            status.performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            status.assertName("All statuses, expanded")
        }

    @Test
    fun picks_onTheWeb_foldTheSelectedStateIntoTheName() {
        runComposeUiTest {
            val state = shutDashboard(DeviceWidth.Desktop, folds = true)
            state.setOn(DashboardMenuSwitch, true)
            waitForIdle()
            onNode(tab(DashboardRange.Week)).assertName("Week, tab, selected")
            onNode(tab(DashboardRange.Month)).assertName("Month, tab, not selected")
            onNode(named(DashboardDestination.Overview.label)).assertName("Overview, selected")
            onNode(named(DashboardDestination.Customers.label)).assertName("Customers, not selected")
            onNode(menuItem(OrderFilter.All), useUnmergedTree = true).assertName("All statuses, option, selected")
            onNode(menuItem(OrderFilter.Paid), useUnmergedTree = true).assertName("Paid, option, not selected")
        }
        runComposeUiTest {
            shutDashboard(DeviceWidth.Tablet, folds = true)
            onNode(named(DashboardDestination.Customers.label)).assertName("Customers, not selected")
        }
    }

    @Test
    fun legendDotsAndSwatches_declareTheRolesTheyPaint() =
        runComposeUiTest {
            val state = shutDashboard(DeviceWidth.Desktop, folds = false)
            state.setOn(DashboardDrawerSwitch, true)
            waitForIdle()
            val declared = onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .map { node -> node.config[PreviewRoles] }
            declared shouldContain listOf(ColorRef.OfRole(Role.Primary))
            declared shouldContain listOf(ColorRef.OfRole(Role.Secondary))
            declared shouldContain listOf(ColorRef.OfRole(Role.Scrim), ColorRef.OfRole(Role.OutlineVariant))
            declared shouldContain listOf(ColorRef.OfRole(Role.OutlineVariant))
        }
}

/**
 * Asserts the node goes by [name] alone.
 */
private fun SemanticsNodeInteraction.assertName(name: String): SemanticsNodeInteraction =
    assert(hasContentDescriptionExactly(name))

/**
 * The dashboard for [width] with every panel shut, in a frame tall enough that every lazy item composes.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.shutDashboard(
    width: DeviceWidth,
    folds: Boolean,
): DemoAppState {
    val state = DemoAppState()
    setContent {
        DashboardHarness(
            spec = LightSpec,
            state = state,
            width = width,
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(DashboardFrames.getValue(width).width.dp, 2400.dp),
            folds = folds,
        )
    }
    return state
}
