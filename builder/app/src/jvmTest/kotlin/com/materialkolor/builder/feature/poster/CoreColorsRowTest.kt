package com.materialkolor.builder.feature.poster

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.FineTuneRow
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Seed = Argb(0x6750A4)

private val Teal = Argb(0x00897B)

private val Amber = Argb(0xFFB300)

private val Plain = ThemeDocument(seed = Seed)

private val TwoPins = mapOf(
    Role.OnPrimary to RolePin(light = Argb(0xFFFFFF), dark = Argb(0x222222)),
    Role.Surface to RolePin(light = Argb(0xFAFAFA)),
)

private const val NOTICE = "Pinned roles don’t follow the seed"

private const val ONE_RAMP = "Fluent builds one accent ramp from the primary palette"

private const val NO_ROLES = "Pins set Material roles, which Fluent does not use."

@OptIn(ExperimentalTestApi::class)
class CoreColorsRowTest {
    @Test
    fun row_title_opensAndClosesThroughTheWorkspace() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(keyColors = KeyColors(primary = Teal), pins = TwoPins))
            showSection(harness) { context, dispatcher -> CoreColorsRow(context, dispatcher) }

            onNodeWithText("1 key color set, 2 pinned roles").assertExists()
            onNodeWithText("Key colors").assertDoesNotExist()

            onNodeWithText("Core colors and pins").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.SetFineTuneRowOpen(FineTuneRow.CoreColors, open = true))
            onNodeWithText("Key colors").assertExists()
            onNodeWithText("Pinned roles").assertExists()

            onNodeWithText("Core colors and pins").performClick()
            waitForIdle()

            harness.actions.last() shouldBe WorkspaceAction.SetFineTuneRowOpen(FineTuneRow.CoreColors, open = false)
            harness.undoEntries() shouldBe 0
        }

    @Test
    fun row_nothingSet_saysTheSeedHasItAll() =
        runComposeUiTest {
            showSection(PosterHarness(Plain)) { context, dispatcher -> CoreColorsRow(context, dispatcher) }

            onNodeWithText("Everything follows the seed").assertExists()
        }

    @Test
    fun keyColor_unset_showsTheDerivedColorFromSeed() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            onAllNodesWithText("From seed").fetchSemanticsNodes().size shouldBe KeyColor.entries.size
            onNodeWithText("Reset all").assertDoesNotExist()
            onNodeWithContentDescription("Pick Tertiary").performClick()
            waitForIdle()

            val pick = WorkspaceAction.OpenPicker(PickerTarget.KeyColorOverride(KeyColor.Tertiary))
            harness.actions shouldBe listOf(pick)
        }

    @Test
    fun keyColor_clear_goesBackToFromSeedAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(keyColors = KeyColors(secondary = Teal)))
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            onAllNodesWithText("From seed").fetchSemanticsNodes().size shouldBe 5
            onNodeWithContentDescription("Clear Secondary").performClick()
            waitForIdle()

            harness.document.keyColors.secondary shouldBe null
            onAllNodesWithText("From seed").fetchSemanticsNodes().size shouldBe KeyColor.entries.size
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun keyColor_typedWithAlpha_setsItAndSaysAKeyColorIsOpaque() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            val field = onAllNodes(hasSetTextAction())[KeyColor.Secondary.ordinal]
            field.requestFocus()
            field.performTextReplacement("#80FF0000")
            field.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            val change = DocumentChange.SetKeyColor(KeyColor.Secondary, Argb(0xFF0000))
            harness.actions shouldBe listOf(WorkspaceAction.Edit(change, EditPhase.Discrete))
            onNodeWithText("A key color is always opaque, so the alpha was dropped", useUnmergedTree = true)
                .assertExists()
        }

    @Test
    fun keyColor_resetAll_handsEveryPaletteBackAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(keyColors = KeyColors(primary = Teal, tertiary = Amber)))
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            onNodeWithText("Reset all").performClick()
            waitForIdle()

            harness.document.keyColors shouldBe KeyColors()
            harness.undoEntries() shouldBe 1
            onNodeWithText("Reset all").assertDoesNotExist()
        }

    @Test
    fun primary_useAsSeed_makesItTheSeedAndClearsItAsOneEntry() =
        runComposeUiTest {
            val start = Plain.copy(keyColors = KeyColors(primary = Teal, tertiary = Amber))
            val harness = PosterHarness(start)
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            onNodeWithText("This moves only the primary palette", substring = true).assertExists()
            onNodeWithText("Use as seed").performClick()
            waitForIdle()

            harness.document shouldBe start.copy(
                seed = Teal,
                seedSource = SeedSource.Picked,
                keyColors = KeyColors(tertiary = Amber),
            )
            harness.undoEntries() shouldBe 1
            onNodeWithText("Use as seed").assertDoesNotExist()
        }

    @Test
    fun pins_clearAll_letsEveryRoleGoAsOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(pins = TwoPins))
            showSection(harness) { context, dispatcher -> PinnedRoles(context, dispatcher) }

            onNodeWithText("onPrimary, Light").assertExists()
            onNodeWithText("onPrimary, Dark").assertExists()
            onNodeWithText("surface, Light").assertExists()
            onNodeWithText("Clear all").performClick()
            waitForIdle()

            harness.document.pins shouldBe emptyMap()
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun pins_clearOneMode_keepsTheOther() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(pins = TwoPins))
            showSection(harness) { context, dispatcher -> PinnedRoles(context, dispatcher) }

            onNodeWithContentDescription("Clear onPrimary, Light").performClick()
            waitForIdle()

            harness.document.pins[Role.OnPrimary] shouldBe RolePin(dark = Argb(0x222222))
            onNodeWithText("onPrimary, Light").assertDoesNotExist()
            onNodeWithText("onPrimary, Dark").assertExists()
        }

    @Test
    fun pins_notice_showsOnlyWhilePinsExist() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            showSection(harness) { context, dispatcher -> PinnedRoles(context, dispatcher) }

            onNodeWithText(NOTICE).assertDoesNotExist()
            onNodeWithText("No pins yet", substring = true).assertExists()

            harness.document = harness.document.copy(pins = TwoPins)
            waitForIdle()

            onNodeWithText(NOTICE).assertExists()
            onNodeWithText("No pins yet", substring = true).assertDoesNotExist()
        }

    @Test
    fun fluent_showsWhyAndBlocksEverythingButThePrimary() =
        runComposeUiTest {
            val document = Plain.copy(
                library = Library.Fluent,
                keyColors = KeyColors(primary = Teal, secondary = Amber),
                pins = TwoPins,
            )
            val harness = PosterHarness(document)
            harness.openFineTuneRows = setOf(FineTuneRow.CoreColors)
            showSection(harness) { context, dispatcher -> CoreColorsRow(context, dispatcher) }

            onNodeWithText(ONE_RAMP, substring = true).assertExists()
            onNodeWithText(NO_ROLES).assertExists()
            onNodeWithText(NOTICE).assertDoesNotExist()
            onNodeWithContentDescription("Clear Secondary").assertIsNotEnabled()
            onNodeWithContentDescription("Pick Secondary").assertIsNotEnabled()
            onNodeWithText("Reset all").assertIsNotEnabled()
            onNodeWithContentDescription("Clear onPrimary, Light").assertIsNotEnabled()
            onNodeWithText("Clear all").assertIsNotEnabled()
            onNodeWithContentDescription("Pick Primary").assertIsEnabled()
            onNodeWithText("Use as seed").assertIsEnabled()

            onNodeWithContentDescription("Clear Primary").performClick()
            waitForIdle()

            harness.document.keyColors shouldBe KeyColors(secondary = Amber)
            harness.document.pins shouldBe TwoPins
        }
}
