package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.RampSet
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import dev.stateholder.dispatcher.Dispatcher
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PalettesTabTest {
    private val material = ThemeDocument.Default.copy(library = Library.Material3, expressive = false)
    private val brand = Accent(name = "Brand", seed = Argb(0xB3261E))

    @Test
    fun split_2021_showsEachRampOnceWithBothModesMarkers() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(spec = SpecVersion.Spec2021))
            for (palette in RampSet.Palettes) {
                result.ramps[palette, false].steps shouldBe result.ramps[palette, true].steps
            }
            showPalettes(result, PreviewMode.Split)

            onNodeWithText("Same in light and dark").assertExists()
            for (title in PaletteTitles.values) onAllNodesWithText(title).assertCountEquals(1)
            onNodeWithText("primary light").assertExists()
            onNodeWithText("primary dark").assertExists()
            onNodeWithText("Light").assertDoesNotExist()
            onNodeWithText("Dark").assertDoesNotExist()
        }

    @Test
    fun split_2025_showsTheRampsThatDifferPerMode() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(spec = SpecVersion.Spec2025))
            val differing = RampSet.Palettes.filter { palette ->
                result.ramps[palette, false].steps != result.ramps[palette, true].steps
            }
            differing.shouldNotBeEmpty()
            showPalettes(result, PreviewMode.Split)

            onNodeWithText("Light").assertExists()
            onNodeWithText("Dark").assertExists()
            for (palette in differing) onAllNodesWithText(PaletteTitles.getValue(palette)).assertCountEquals(2)
        }

    @Test
    fun light_showsSixRampsAndTheAccentWithItsParts() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(accents = listOf(brand)))
            showPalettes(result, PreviewMode.Light)

            for (title in PaletteTitles.values) onAllNodesWithText(title).assertCountEquals(1)
            onAllNodesWithText("Brand").assertCountEquals(1)
            for (part in listOf("color", "onColor", "container", "onContainer")) {
                onAllNodesWithText(part).assertCountEquals(1)
            }
            onNodeWithText("Same in light and dark").assertDoesNotExist()
        }

    @Test
    fun split_accent_showsOnceWithEachModesParts() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(spec = SpecVersion.Spec2025, accents = listOf(brand)))
            showPalettes(result, PreviewMode.Split)

            onAllNodesWithText("Brand").assertCountEquals(1)
            onNodeWithText("container light").assertExists()
            onNodeWithText("container dark").assertExists()
        }

    @Test
    fun stop_copiesItsHex() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            val actions = showPalettes(result, PreviewMode.Light)
            val stop = result.ramps[KeyColor.Primary, false].steps.first { step -> step.tone == 40 }

            onAllNodesWithContentDescription("tone 40, ${stop.argb.toHex()}")[0].performClick()
            waitForIdle()

            actions.sent.last() shouldBe WorkspaceAction.CopyText(stop.argb.toHex(), "Primary tone 40")
        }

    @Test
    fun highlight_role_outlinesThePaletteItPickedFrom() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            val palette = result.ramps
                .mode(false)
                .first { ramp -> ramp.markers.any { marker -> marker.role == Role.Surface } }
                .palette
            val highlight = RampHighlight(RampTarget.OfRole(Role.Surface, isDark = false), generation = 3)
            showPalettes(result, PreviewMode.Light, highlight, generation = 3)

            onNodeWithText("surface sits on this ramp").assertIsDisplayed()
            onNode(hasTestTag(PICKED_RAMP_TAG) and hasAnyDescendant(hasText(PaletteTitles.getValue(palette))))
                .assertExists()
        }

    @Test
    fun highlight_fromAnEarlierProject_picksOutNothing() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            val highlight = RampHighlight(RampTarget.OfRole(Role.Primary, isDark = false), generation = 1)
            showPalettes(result, PreviewMode.Light, highlight, generation = 2)

            onNodeWithTag(PICKED_RAMP_TAG).assertDoesNotExist()
            onNodeWithText("primary sits on this ramp").assertDoesNotExist()
        }

    @Test
    fun highlight_accentThatIsGone_picksOutNothing() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(accents = listOf(brand)))
            val gone = RampTarget.OfAccent(AccentSlot(index = 3, part = AccentPart.Color), isDark = false)
            showPalettes(result, PreviewMode.Light, RampHighlight(gone, generation = 0), generation = 0)

            onNodeWithTag(PICKED_RAMP_TAG).assertDoesNotExist()
            onNodeWithText("Brand").assertExists()
        }

    @Test
    fun highlight_belowTheFold_scrollsIntoView() =
        runDesktopComposeUiTest(width = TABS_PHONE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(accents = listOf(brand)))
            val slot = AccentSlot(index = 0, part = AccentPart.Container)
            val highlight = RampHighlight(RampTarget.OfAccent(slot, isDark = true), generation = 0)
            showPalettes(result, PreviewMode.Split, highlight, generation = 0)

            onNodeWithText("Brand container sits on this ramp").assertIsDisplayed()
        }

    @Test
    fun showOnRamp_fromTheRolesPopover_picksOutTheRampOnPalettes() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            val view = ProjectViewState(tab = PreviewTab.Roles, mode = PreviewMode.Light)
            var state by mutableStateOf(
                WorkspaceModel.State(
                    document = material,
                    capabilities = capabilitiesOf(material),
                    history = HistoryState(),
                    view = view,
                    preferences = Preferences(),
                    projectGeneration = 4,
                ),
            )
            // The two actions the model handles for this, handled the way it does.
            val dispatcher = Dispatcher<WorkspaceAction> { action ->
                state = when (action) {
                    is WorkspaceAction.ShowOnRamp -> state.copy(
                        view = state.view.copy(tab = PreviewTab.Palettes),
                        rampHighlight = RampHighlight(action.target, state.projectGeneration),
                    )
                    is WorkspaceAction.SetPreviewTab -> state.copy(
                        view = state.view.copy(tab = action.tab),
                        rampHighlight = null,
                    )
                    else -> state
                }
            }
            setContent { DataTabTheme(result) { CanvasArea(state, PaddingValues(), dispatcher) } }
            waitForIdle()

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).performClick()
            waitForIdle()
            onNodeWithText("Show on ramp").performClick()
            waitForIdle()

            state.view.tab shouldBe PreviewTab.Palettes
            onNodeWithText("primary sits on this ramp").assertIsDisplayed()
            onNode(hasTestTag(PICKED_RAMP_TAG) and hasAnyDescendant(hasText("Primary"))).assertExists()
        }

    private fun ComposeUiTest.showPalettes(
        result: ThemeResult,
        mode: PreviewMode,
        highlight: RampHighlight? = null,
        generation: Int = 0,
    ): TabActions {
        val actions = TabActions()
        setContent {
            DataTabTheme(result) {
                PalettesTab(result, mode, filter = null, highlight, generation, dispatcher = actions.dispatcher)
            }
        }
        waitForIdle()
        return actions
    }
}

/** The heading over each palette's ramp. */
internal val PaletteTitles: Map<KeyColor, String> = mapOf(
    KeyColor.Primary to "Primary",
    KeyColor.Secondary to "Secondary",
    KeyColor.Tertiary to "Tertiary",
    KeyColor.Neutral to "Neutral",
    KeyColor.NeutralVariant to "Neutral variant",
    KeyColor.Error to "Error",
)
