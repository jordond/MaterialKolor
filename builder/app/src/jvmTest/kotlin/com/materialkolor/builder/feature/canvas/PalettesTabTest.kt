package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.materialkolor.builder.kit.a11y.Announcer
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
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
            // b-513
            onNode(hasMark("primary") and hasMark("surfaceTint") and hasAnyAncestor(hasContentDescription("Light")))
                .assertExists()
            onNode(hasMark("primary") and hasAnyAncestor(hasContentDescription("Dark"))).assertExists()
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
                onAllNodes(hasMark(part)).assertCountEquals(1) // b-513
            }
            onNodeWithText("Same in light and dark").assertDoesNotExist()
        }

    @Test
    fun split_accent_showsOnceWithEachModesParts() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(spec = SpecVersion.Spec2025, accents = listOf(brand)))
            showPalettes(result, PreviewMode.Split)

            onAllNodesWithText("Brand").assertCountEquals(1)
            // b-513
            onNode(hasMark("container") and hasAnyAncestor(hasContentDescription("Light"))).assertExists()
            onNode(hasMark("container") and hasAnyAncestor(hasContentDescription("Dark"))).assertExists()
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

    // b-308ba
    @Test
    fun highlight_anEditAfterIt_leavesTheScrollAlone() =
        runDesktopComposeUiTest(width = TABS_PHONE, height = TABS_HEIGHT) {
            val document = material.copy(accents = listOf(brand))
            var result by mutableStateOf(resolvedFor(document))
            val slot = AccentSlot(index = 0, part = AccentPart.Container)
            val highlight = RampHighlight(RampTarget.OfAccent(slot, isDark = false), generation = 0)
            setContent {
                DataTabTheme(result) {
                    PalettesTab(result, PreviewMode.Light, filter = null, highlight, 0, TabActions().dispatcher)
                }
            }
            waitForIdle()
            val label = onNodeWithText("Brand container sits on this ramp")
            label.assertIsDisplayed()
            onNodeWithText("Primary").performScrollTo()
            waitForIdle()
            label.assertIsNotDisplayed()

            result = resolvedFor(document.copy(seed = Argb(0x00696B)))
            waitForIdle()

            onNodeWithText("Primary").assertIsDisplayed()
            label.assertIsNotDisplayed()
        }

    @Test
    fun highlight_accentPickedInDark_showsInLight() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(accents = listOf(brand)))
            val slot = AccentSlot(index = 0, part = AccentPart.Color)
            showPalettes(result, PreviewMode.Light, RampHighlight(RampTarget.OfAccent(slot, isDark = true), 0))

            onNode(hasTestTag(PICKED_RAMP_TAG) and hasAnyDescendant(hasText("Brand"))).assertExists()
        }

    @Test
    fun highlight_2021RampPickedInDark_showsInLight() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(spec = SpecVersion.Spec2021))
            showPalettes(result, PreviewMode.Light, RampHighlight(RampTarget.OfRole(Role.Primary, isDark = true), 0))

            onNodeWithText("primary sits on this ramp").assertExists()
            onNode(hasTestTag(PICKED_RAMP_TAG) and hasAnyDescendant(hasText("Primary"))).assertExists()
        }

    @Test
    fun highlight_2025RampThatDiffersPickedInDark_staysOutOfLight() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(spec = SpecVersion.Spec2025))
            val palette = RampSet.Palettes.first { palette ->
                result.ramps[palette, false].steps != result.ramps[palette, true].steps
            }
            val dark = result.ramps[palette, true]
            val role = dark.markers.first().role
            showPalettes(result, PreviewMode.Light, RampHighlight(RampTarget.OfRole(role, isDark = true), 0))

            onNodeWithTag(PICKED_RAMP_TAG).assertDoesNotExist()
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
            val announced = mutableListOf<String>()
            setContent {
                DataTabTheme(result) {
                    CompositionLocalProvider(LocalAnnouncer provides Announcer { message -> announced += message }) {
                        CanvasArea(state, PaddingValues(), dispatcher)
                    }
                }
            }
            waitForIdle()

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).performClick()
            waitForIdle()
            onNodeWithText("Show on ramp").performClick()
            waitForIdle()

            state.view.tab shouldBe PreviewTab.Palettes
            onNodeWithText("primary sits on this ramp").assertIsDisplayed()
            onNode(hasTestTag(PICKED_RAMP_TAG) and hasAnyDescendant(hasText("Primary"))).assertExists()
            // b-308ba
            announced shouldBe listOf("primary sits on this ramp")
            val first = result.ramps[KeyColor.Primary, false].steps.first()
            val stop = "tone ${first.tone}, ${first.argb.toHex()}"
            onNode(hasContentDescription(stop) and hasAnyAncestor(hasTestTag(PICKED_RAMP_TAG))).assertIsFocused()
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

// b-513

/**
 * A tag under a ramp, as in "40 · primary, surfaceTint", that names [name] among what landed on its tone.
 */
private fun hasMark(name: String): SemanticsMatcher =
    SemanticsMatcher("a tone tag naming $name") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text ->
            name in text.text.substringAfter(" · ", missingDelimiterValue = "").split(", ")
        }
    }

/**
 * The heading over each palette's ramp.
 */
internal val PaletteTitles: Map<KeyColor, String> = mapOf(
    KeyColor.Primary to "Primary",
    KeyColor.Secondary to "Secondary",
    KeyColor.Tertiary to "Tertiary",
    KeyColor.Neutral to "Neutral",
    KeyColor.NeutralVariant to "Neutral variant",
    KeyColor.Error to "Error",
)
