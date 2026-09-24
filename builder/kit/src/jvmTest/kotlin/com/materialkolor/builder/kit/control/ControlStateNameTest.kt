package com.materialkolor.builder.kit.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ControlStateNameTest {
    private val words = TestStateWords

    @Test
    fun plainClickable_everySkin_composesAndClicks() =
        forEachSkin { _, skin ->
            var clicks = 0
            setContent {
                ControlsHarness(skin) {
                    Box(Modifier.testTag(Plain).size(40.dp).clickable { clicks++ })
                }
            }
            onNodeWithTag(Plain).performClick()
            waitForIdle()
            clicks shouldBe 1
        }

    @Test
    fun foldStateIntoName_joinsNameStateAndDisabledNote() {
        foldStateIntoName("Tonal spot", ControlState.Selected(true), enabled = true, words) shouldBe
            "Tonal spot, selected"
        foldStateIntoName("Dark mode", ControlState.Switched(false), enabled = false, words) shouldBe
            "Dark mode, off, disabled"
        foldStateIntoName("Chroma", ControlState.Value("0.25, Quarter"), enabled = true, words) shouldBe
            "Chroma, 0.25, Quarter"
        foldStateIntoName("Delete", state = null, enabled = false, words) shouldBe "Delete, disabled"
        foldStateIntoName("Poster", ControlState.Value(""), enabled = true, words) shouldBe "Poster"
    }

    @Test
    fun foldStateIntoName_withRole_putsTheRoleWordBeforeTheState() {
        foldStateIntoName("Include dark scheme", ControlState.Checked(true), true, words, FoldedRole.Checkbox) shouldBe
            "Include dark scheme, checkbox, checked"
        foldStateIntoName("Tonal spot", ControlState.Selected(true), true, words, FoldedRole.Radio) shouldBe
            "Tonal spot, radio, selected"
        foldStateIntoName("Dark mode", ControlState.Switched(false), false, words, FoldedRole.Switch) shouldBe
            "Dark mode, switch, off, disabled"
        foldStateIntoName("Kotlin", ControlState.Selected(false), true, words, FoldedRole.Tab) shouldBe
            "Kotlin, tab, not selected"
        foldStateIntoName("Contrast", ControlState.Value("0.50"), true, words, FoldedRole.Slider) shouldBe
            "Contrast, slider, 0.50"
        foldStateIntoName("Exporting", ControlState.Value("40%"), true, words, FoldedRole.ProgressBar) shouldBe
            "Exporting, progress bar, 40%"
        foldStateIntoName("Style", ControlState.Value("Tonal spot"), false, words, FoldedRole.PopUpButton) shouldBe
            "Style, pop-up button, Tonal spot, disabled"
        foldStateIntoName("Dark", ControlState.Checked(true), true, words, FoldedRole.MenuItem) shouldBe
            "Dark, menu item, checked"
        foldStateIntoName("Vibrant", ControlState.Selected(false), true, words, FoldedRole.Option) shouldBe
            "Vibrant, option, not selected"
        foldStateIntoName("Delete theme?", null, true, words, FoldedRole.Dialog) shouldBe "Delete theme?, dialog"
    }

    @Test
    fun stateWords_readAloneKeepTheirCapital() {
        words.of(ControlState.Checked(false)) shouldBe "Not checked"
        words.of(ControlState.Expanded(true)) shouldBe "Expanded"
        words.afterName(ControlState.Checked(false)) shouldBe "not checked"
        words.afterName(ControlState.Value("Medium")) shouldBe "Medium"
        words.roleWord(FoldedRole.Radio) shouldBe "radio"
    }

    @Test
    fun flagOff_everySkin_namesStayTheLabel() =
        forEachSkin { _, skin ->
            setContent { ControlsHarness(skin) { StatefulControls() } }

            onNodeWithTag(Chip).assert(hasNoContentDescription())
            onNodeWithTag(Switch).assert(hasNoContentDescription()).assert(hasStateDescription("Off"))
            onNodeWithTag(Checkbox).assert(hasNoContentDescription()).assert(hasStateDescription("Checked"))
            onNodeWithTag(Toggle).assert(hasNoContentDescription())
            onNode(hasText("Colours") and hasStateDescription("Collapsed")).assert(hasNoContentDescription())
            onNodeWithTag(Row).assert(hasNoContentDescription())
            onNodeWithTag(Progress).assert(hasContentDescriptionExactly("Exporting"))
            onNodeWithTag(Slider).assert(hasContentDescriptionExactly("Chroma"))
            onNode(hasText("Dark") and hasRole(Role.Tab)).assert(hasNoContentDescription())
            onNode(hasText("Tonal spot") and isSelectable()).assert(hasNoContentDescription())
            onNode(hasContentDescription("Poster") and hasRole(Role.Button)).assert(hasStateDescription("Peek"))
            onNode(isSelectableGroup() and hasContentDescriptionExactly("Scheme style")).assertExists()
            onNode(isSelectableGroup() and hasContentDescriptionExactly("Layout")).assertExists()
        }

    @Test
    fun flagOn_everySkin_foldsTheStateIntoTheName() =
        forEachSkin { _, skin ->
            setContent {
                ControlsHarness(skin) {
                    CompositionLocalProvider(LocalFoldsStateIntoName provides true) { StatefulControls() }
                }
            }

            onNodeWithTag(Chip).assert(hasContentDescriptionExactly("Pins, selected"))
            onNodeWithTag(Switch)
                .assert(hasContentDescriptionExactly("AMOLED black, switch, off, disabled"))
                .assert(hasStateDescription("Off"))
            onNodeWithTag(Checkbox).assert(hasContentDescriptionExactly("Show pins, checkbox, checked"))
            onNodeWithTag(Toggle).assert(hasContentDescriptionExactly("Bold, not checked"))
            onNode(hasContentDescriptionExactly("Colours, Seed and roles, collapsed"))
                .assert(hasStateDescription("Collapsed"))
            onNodeWithTag(Row).assert(hasContentDescriptionExactly("Poster theme, selected"))
            onNode(hasContentDescriptionExactly("Dark, tab, selected")).assert(hasRole(Role.Tab)).assert(isSelected())
            onNode(hasContentDescriptionExactly("Light, tab, not selected"))
                .assert(hasRole(Role.Tab))
                .assert(isNotSelected())
            onNode(hasContentDescriptionExactly("Tonal spot, radio, selected")).assert(isSelected())
            onNode(hasContentDescriptionExactly("Vibrant, radio, not selected")).assert(isNotSelected())
            onNode(hasContentDescriptionExactly("Grid, radio, selected")).assert(isSelected())
            onNode(hasContentDescriptionExactly("Scheme style, pop-up button, Tonal spot"))
                .assert(hasRole(Role.DropdownList))
            onNode(hasContentDescriptionExactly("Poster, Peek")).assert(hasRole(Role.Button))
        }

    @Test
    fun flagOn_everySkin_foldsRoleLessNamesIntoText() =
        forEachSkin { _, skin ->
            setContent {
                ControlsHarness(skin) {
                    CompositionLocalProvider(LocalFoldsStateIntoName provides true) { StatefulControls() }
                }
            }

            onNodeWithTag(Progress)
                .assert(hasTextExactly("Exporting, progress bar, 40%"))
                .assert(hasNoContentDescription())
            onNodeWithTag(Slider)
                .assert(hasTextExactly("Chroma, slider, 0.25, disabled"))
                .assert(hasNoContentDescription())
            onNode(isSelectableGroup() and hasTextExactly("Scheme style")).assert(hasNoContentDescription())
            onNode(isSelectableGroup() and hasTextExactly("Layout")).assert(hasNoContentDescription())
        }

    /** One of every stateful control, each in a state worth reading. */
    @Composable
    private fun StatefulControls() {
        Box(Modifier.fillMaxSize()) {
            Column {
                BuilderFilterChip(true, {}, "Pins", Modifier.testTag(Chip))
                BuilderSwitch(false, {}, "AMOLED black", Modifier.testTag(Switch), enabled = false)
                BuilderCheckbox(true, {}, "Show pins", Modifier.testTag(Checkbox))
                BuilderToggleButton(false, {}, "Bold", Modifier.testTag(Toggle))
                BuilderDisclosure(false, {}, "Colours", summary = "Seed and roles") {}
                BuilderListRow("Poster theme", Modifier.testTag(Row), onClick = {}, selected = true)
                BuilderProgress("Exporting", Modifier.testTag(Progress), progress = 0.4f)
                BuilderSlider(
                    value = 0.25f,
                    onValueChange = {},
                    label = "Chroma",
                    modifier = Modifier.testTag(Slider).width(320.dp),
                    enabled = false,
                )
                BuilderTabs(listOf("Light", "Dark"), "Dark", {}, { tab -> tab })
                BuilderChoiceChips(listOf("Tonal spot", "Vibrant"), "Tonal spot", {}, "Scheme style") { it }
                BuilderSegmented(listOf("Grid", "List"), "Grid", {}, "Layout") { it }
                BuilderSelect("Scheme style", listOf("Tonal spot", "Vibrant"), "Tonal spot", {})
            }
            BuilderBottomSheet(rememberBottomSheetState(), label = "Poster") { BuilderText("#6750A4") }
        }
    }

    private fun isSelectableGroup(): SemanticsMatcher =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)

    private companion object {
        const val Plain = "plain"
        const val Chip = "chip"
        const val Switch = "switch"
        const val Checkbox = "checkbox"
        const val Toggle = "toggle"
        const val Row = "row"
        const val Progress = "progress"
        const val Slider = "slider"
    }
}
