package com.materialkolor.builder.kit.control

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.kit.icon.IconId
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Modes: List<String> = listOf("Light", "Split", "Dark")

/**
 * The Fluent segmented control, switch, checkbox, tabs, disclosure and icon button, the controls
 * B-403b moved onto Fluent's components. Each rings on every side at 3 to 1 on keyboard focus in
 * both modes, and on the web folds its role and state into the name of the node that takes the
 * press.
 */
@OptIn(ExperimentalTestApi::class)
class FluentControlsTest {
    @Test
    fun segmented_firstMiddleAndLastChosen_lightAndDark_ringsEverySideAtThreeToOne() {
        for (chosen in Modes) {
            ringsInBothModes("$chosen chosen") {
                BuilderSegmented(Modes, chosen, onSelect = {}, label = "Preview mode") { mode -> mode }
            }
        }
    }

    @Test
    fun switch_onAndOff_lightAndDark_ringsEverySideAtThreeToOne() {
        for (checked in listOf(false, true)) {
            ringsInBothModes("checked = $checked") {
                BuilderSwitch(checked, {}, "Dark theme")
            }
        }
    }

    @Test
    fun checkbox_onAndOff_lightAndDark_ringsEverySideAtThreeToOne() {
        for (checked in listOf(false, true)) {
            ringsInBothModes("checked = $checked") {
                BuilderCheckbox(checked, {}, "Show tones")
            }
        }
    }

    @Test
    fun tabs_firstMiddleAndLastSelected_lightAndDark_ringsEverySideAtThreeToOne() {
        for (chosen in Modes) {
            ringsInBothModes("$chosen selected") {
                BuilderTabs(Modes, chosen, onSelect = {}, label = { tab -> tab })
            }
        }
    }

    @Test
    fun disclosure_openAndClosed_lightAndDark_ringsEverySideAtThreeToOne() {
        for (expanded in listOf(false, true)) {
            ringsInBothModes("expanded = $expanded") {
                BuilderDisclosure(expanded, {}, "Contrast") { BuilderText("Standard") }
            }
        }
    }

    @Test
    fun iconButton_everyEmphasis_lightAndDark_ringsEverySideAtThreeToOne() {
        for (emphasis in Emphasis.entries) {
            ringsInBothModes("$emphasis") {
                BuilderIconButton({}, IconId.Copy, "Copy", emphasis = emphasis)
            }
        }
    }

    @Test
    fun segmented_foldOn_namesEachOptionThatTakesThePressWithItsRoleAndState() =
        runComposeUiTest {
            var mode by mutableStateOf("Split")
            var enabled by mutableStateOf(true)
            showFluentFolded {
                BuilderSegmented(
                    options = Modes,
                    selected = mode,
                    onSelect = { next -> mode = next },
                    label = "Preview mode",
                    enabled = enabled,
                    optionLabel = { option -> option },
                )
            }

            onNodeWithContentDescription("Split, radio, selected")
                .assert(hasClickAction())
                .assert(hasRole(Role.RadioButton))
                .assertIsSelected()
            onNodeWithContentDescription("Dark, radio, not selected")
                .assert(hasClickAction())
                .assertIsNotSelected()
                .performClick()
            waitForIdle()
            mode shouldBe "Dark"
            onNodeWithContentDescription("Dark, radio, selected").assertIsSelected()
            onAllNodesWithText("Dark").assertCountEquals(1)

            enabled = false
            waitForIdle()
            onNodeWithContentDescription("Light, radio, not selected, disabled").assertIsNotEnabled()
        }

    @Test
    fun switch_foldOn_namesTheRowThatTakesThePressWithItsRoleAndState() =
        runComposeUiTest {
            var on by mutableStateOf(false)
            showFluentFolded {
                BuilderSwitch(on, { next -> on = next }, "Dark theme", Modifier.testTag("dark"))
                BuilderSwitch(true, {}, "Wi-Fi", Modifier.testTag("wifi"), enabled = false)
            }

            onNodeWithTag("dark")
                .assert(hasClickAction())
                .assert(isToggleable())
                .assert(hasRole(Role.Switch))
                .assert(hasContentDescriptionExactly("Dark theme, switch, off"))
                .performClick()
            waitForIdle()
            on shouldBe true
            onNodeWithTag("dark").assert(hasContentDescriptionExactly("Dark theme, switch, on"))
            onNodeWithTag("wifi")
                .assertIsNotEnabled()
                .assert(hasContentDescriptionExactly("Wi-Fi, switch, on, disabled"))
            onAllNodesWithText("Dark theme").assertCountEquals(1)
        }

    @Test
    fun checkbox_foldOn_namesTheRowThatTakesThePressWithItsRoleAndState() =
        runComposeUiTest {
            var ticked by mutableStateOf(true)
            var enabled by mutableStateOf(true)
            showFluentFolded {
                BuilderCheckbox(ticked, { next -> ticked = next }, "Show tones", Modifier.testTag("tones"), enabled)
            }

            onNodeWithTag("tones")
                .assert(hasClickAction())
                .assert(isToggleable())
                .assert(hasRole(Role.Checkbox))
                .assert(hasContentDescriptionExactly("Show tones, checkbox, checked"))
                .performClick()
            waitForIdle()
            ticked shouldBe false
            onNodeWithTag("tones").assert(hasContentDescriptionExactly("Show tones, checkbox, not checked"))

            enabled = false
            waitForIdle()
            onNodeWithTag("tones")
                .assertIsNotEnabled()
                .assert(hasContentDescriptionExactly("Show tones, checkbox, not checked, disabled"))
            onNodeWithTag("tones").performClick()
            waitForIdle()
            ticked shouldBe false
        }

    @Test
    fun tabs_foldOn_namesEachTabThatTakesThePressWithItsRoleAndState() =
        runComposeUiTest {
            var tab by mutableStateOf("Split")
            showFluentFolded {
                BuilderTabs(Modes, tab, onSelect = { next -> tab = next }, label = { name -> name })
            }

            onNodeWithContentDescription("Split, tab, selected")
                .assert(hasClickAction())
                .assert(hasRole(Role.Tab))
                .assertIsSelected()
            onNodeWithContentDescription("Light, tab, not selected")
                .assert(hasClickAction())
                .performClick()
            waitForIdle()
            tab shouldBe "Light"
            onNodeWithContentDescription("Light, tab, selected").assertIsSelected()
            onAllNodesWithText("Light").assertCountEquals(1)
        }

    @Test
    fun disclosure_foldOn_namesTheHeaderThatTakesThePressWithItsState() =
        runComposeUiTest {
            var open by mutableStateOf(false)
            showFluentFolded {
                BuilderDisclosure(open, { next -> open = next }, "Contrast", Modifier.testTag("contrast")) {
                    BuilderText("Standard", Modifier.testTag("body"))
                }
            }

            onNodeWithContentDescription("Contrast, collapsed")
                .assert(hasClickAction())
                .assert(hasRole(Role.Button))
                .performClick()
            waitForIdle()
            open shouldBe true
            onNodeWithContentDescription("Contrast, expanded").assert(hasClickAction())
            onNodeWithTag("body", useUnmergedTree = true).assertExists()
            onAllNodesWithText("Contrast").assertCountEquals(1)
        }

    @Test
    fun iconButton_foldOn_namesTheLayerThatTakesThePressWithItsPanelState() =
        runComposeUiTest {
            var shown by mutableStateOf(false)
            showFluentFolded {
                BuilderIconButton({ shown = !shown }, IconId.Inspect, "Inspector", expanded = shown)
                BuilderIconButton({}, IconId.Copy, "Copy", enabled = false)
            }

            onNodeWithContentDescription("Inspector, collapsed")
                .assert(hasClickAction())
                .assert(hasRole(Role.Button))
                .performClick()
            waitForIdle()
            onNodeWithContentDescription("Inspector, expanded").assert(hasClickAction())
            onNodeWithContentDescription("Copy, disabled").assertIsNotEnabled()
        }
}
