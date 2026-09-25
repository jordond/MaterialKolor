package com.materialkolor.builder.feature.image

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.poster.PosterHarness
import com.materialkolor.builder.feature.poster.showSection
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

private const val PRESETS = "Presets and starters"
private const val EYEDROPPER = "Pick a color from the image"

@OptIn(ExperimentalTestApi::class)
class PresetPickerTest {
    /**
     * A theme with something in every part a preset or a starter must leave alone.
     */
    private val busy = ThemeDocument(
        seed = Argb(0xFF00897B.toInt()),
        keyColors = KeyColors().with(KeyColor.Tertiary, Argb(0xFFFFB300.toInt())),
        style = Style.Rainbow,
        contrast = ContrastLevel.High,
        accents = listOf(Accent(name = "brand", seed = Argb(0xFF3949AB.toInt()))),
        pins = mapOf(Role.Primary to RolePin(light = Argb(0xFFC62828.toInt()))),
        library = Library.Unstyled,
    )

    @Test
    fun presetImage_setsOnlyTheSeed_asOneEntryThatKeepsThePreset() =
        runComposeUiTest {
            val harness = PosterHarness(busy)
            showButton(harness)
            val preset = Presets.images[2]

            choose("Image 3")

            val change = harness.actions
                .filterIsInstance<WorkspaceAction.EditWithReveal>()
                .single()
                .change
            change shouldBe DocumentChange.SetSeed(preset.candidates.first(), SeedSource.Preset("res-2"))
            change.merges shouldBe false
            harness.document shouldBe
                busy.copy(seed = preset.candidates.first(), seedSource = SeedSource.Preset("res-2"))
            harness.undoEntries() shouldBe 1
            onNodeWithText(PRESETS).assertDoesNotExist()
        }

    @Test
    fun starter_setsOnlyTheSeedStyleAndContrast_asOneEntry() =
        runComposeUiTest {
            val harness = PosterHarness(busy)
            showButton(harness)

            choose("Ink, TonalSpot, Medium contrast")

            harness.actions
                .filterIsInstance<WorkspaceAction.EditWithReveal>()
                .single()
                .change
                .shouldBeInstanceOf<DocumentChange.Replace>()
            harness.document shouldBe busy.copy(
                seed = Argb(0xFF1A237E.toInt()),
                seedSource = SeedSource.Preset("starter-ink"),
                style = Style.TonalSpot,
                contrast = ContrastLevel.Medium,
            )
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun uploadImage_asksTheWorkspaceForThePicker() =
        runComposeUiTest {
            val harness = PosterHarness(busy)
            showButton(harness)

            onNodeWithText("Image").performClick()
            onNodeWithText("Upload image").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.OpenImagePicker)
        }

    @Test
    fun presetSeed_showsThePictureAndItsChips_andAChipKeepsThePreset() =
        runComposeUiTest {
            val preset = Presets.images[2]
            val source = SeedSource.Preset(preset.id)
            val harness = PosterHarness(ThemeDocument(seed = preset.seed, seedSource = source))
            showSection(harness) { context, dispatcher -> ImageCandidateRow(context, dispatcher) }

            onNodeWithContentDescription(EYEDROPPER).assertExists()
            preset.candidates.forEach { candidate ->
                onNodeWithContentDescription(chipLabel(candidate), substring = true).assertExists()
            }
            onNodeWithContentDescription(chipLabel(preset.candidates[1]), substring = true).performClick()
            waitForIdle()

            harness.document.seed shouldBe preset.candidates[1]
            harness.document.seedSource shouldBe source
        }

    @Test
    fun presetPicture_opensTheEyedropper() =
        runComposeUiTest {
            val preset = Presets.images[0]
            val harness = PosterHarness(ThemeDocument(seed = preset.seed, seedSource = SeedSource.Preset(preset.id)))
            showSection(harness) { context, dispatcher -> ImageCandidateRow(context, dispatcher) }

            onNodeWithContentDescription(EYEDROPPER).performClick()
            waitForIdle()

            harness.openPanel shouldBe Panel.ImageEyedropper
            onNodeWithText("Pick any spot", substring = true).assertExists()
        }

    @Test
    fun starterSeed_leavesTheRowOut() =
        runComposeUiTest {
            val starter = Presets.starters.first()
            val harness = PosterHarness(ThemeDocument(seed = starter.seed, seedSource = SeedSource.Preset(starter.id)))
            showSection(harness) { context, dispatcher -> ImageCandidateRow(context, dispatcher) }

            onNodeWithContentDescription(EYEDROPPER).assertDoesNotExist()
        }

    @Test
    fun presets_openAsAPanel_andBackClosesThem_handingTheFocusBackToImage() =
        runComposeUiTest {
            val harness = PosterHarness(busy)
            showButton(harness)

            openPresets()
            harness.openPanel shouldBe Panel.Presets
            harness.actions shouldBe listOf(WorkspaceAction.OpenPanel(Panel.Presets))

            // Back reaches the workspace as the router's pop, which closes whatever panel is open.
            runOnUiThread { harness.dispatch(WorkspaceAction.ClosePanel) }
            waitForIdle()

            harness.openPanel shouldBe null
            onNodeWithText(PRESETS).assertDoesNotExist()
            onNodeWithText("Image").assertIsFocused()
            harness.document shouldBe busy
        }

    @Test
    fun starterCards_drawEveryStartersScheme_andNameAContrastOffStandard() =
        runComposeUiTest {
            val harness = PosterHarness(busy)
            showButton(harness)

            openPresets()

            onAllNodesWithTag(STARTER_CHIP_TAG, useUnmergedTree = true).assertCountEquals(Presets.starters.size)
            onAllNodesWithTag(STARTER_SKELETON_TAG, useUnmergedTree = true).assertCountEquals(0)
            Presets.starters.size shouldBe 8
            onNodeWithText("Ink, TonalSpot, Medium contrast").assertExists()
            onNodeWithText("Baseline, TonalSpot").assertExists()
        }

    private fun ComposeUiTest.openPresets() {
        onNodeWithText("Image").performClick()
        onNodeWithText(PRESETS).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.showButton(harness: PosterHarness) {
        showSection(harness) { context, dispatcher -> ImageMenuButton(context, dispatcher) }
    }

    /**
     * Opens the picker from the Image menu and chooses the card named [name].
     */
    private fun ComposeUiTest.choose(name: String) {
        openPresets()
        onNodeWithText(name).performScrollTo().performClick()
        waitForIdle()
    }
}
