package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.dynamiccolor.DynamicScheme
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test

private val Seed = Argb(0x6750A4)

private const val TERTIARY_FIELD = "Tertiary seed, any format"

private const val KEEP_STYLE = "Keep the style when shuffling"

/**
 * How many frames the chip drag test moves the seed for.
 */
private const val DRAG_FRAMES = 4

@OptIn(ExperimentalTestApi::class)
class StyleChipsTest {
    @Test
    fun chips_redrawnWithTheSameInputs_askTheResolverOncePerChip() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            val asked = mutableListOf<Pair<SchemeInputs, DynamicScheme>>()
            showSection(harness) { context, dispatcher ->
                StyleChips(context, dispatcher, lookup = { inputs, isDark ->
                    harness.resolver.scheme(inputs, isDark).also { scheme -> asked += inputs to scheme }
                })
            }
            waitForIdle()
            asked shouldHaveSize Style.entries.size

            // A rename leaves every chip's inputs alone, so no chip asks again.
            harness.document = harness.document.copy(themeName = "Ocean")
            waitForIdle()
            asked shouldHaveSize Style.entries.size

            harness.document = harness.document.copy(seed = Argb(0x1E88E5))
            waitForIdle()
            asked shouldHaveSize Style.entries.size * 2
        }

    @Test
    fun chips_aSchemeChange_drawsOneChipAFrameAndAllOnceItSettles() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            val asked = mutableListOf<SchemeInputs>()
            showSection(harness) { context, dispatcher ->
                StyleChips(
                    context = context,
                    dispatcher = dispatcher,
                    lookup = { inputs, isDark -> harness.resolver.scheme(inputs, isDark).also { asked += inputs } },
                    pause = { withFrameNanos {} },
                )
            }
            waitForIdle()
            asked shouldHaveSize Style.entries.size

            // A drag lands a new seed every frame, and no frame draws more than one chip for it.
            mainClock.autoAdvance = false
            var last = Seed
            repeat(DRAG_FRAMES) { step ->
                last = Argb(0x1E88E5 + step)
                harness.document = harness.document.copy(seed = last)
                mainClock.advanceTimeByFrame()
                asked.size shouldBeLessThanOrEqual Style.entries.size + step + 1
            }

            mainClock.autoAdvance = true
            waitForIdle()
            asked.filter { inputs -> inputs.seed == last }.map { inputs -> inputs.style } shouldContainExactlyInAnyOrder
                Style.entries
        }

    @Test
    fun chips_currentStyle_hitsTheSchemeTheOpenThemeAlreadyBuilt() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            val asked = mutableMapOf<Style, DynamicScheme>()
            showSection(harness) { context, dispatcher ->
                StyleChips(context, dispatcher, lookup = { inputs, isDark ->
                    harness.resolver.scheme(inputs, isDark).also { scheme -> asked[inputs.style] = scheme }
                })
            }
            waitForIdle()

            asked.getValue(Style.TonalSpot) shouldBeSameInstanceAs harness.resolver.resolve(harness.document).light
        }

    @Test
    fun chips_click_picksTheStyleBehindARevealAsOneUndoEntry() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> StyleChipsSection(context, dispatcher) }

            onNode(hasContentDescription("Vibrant,", substring = true)).performClick()
            waitForIdle()

            val reveal = harness.actions.single().shouldBeTypeOf<WorkspaceAction.EditWithReveal>()
            reveal.change shouldBe DocumentChange.SetStyle(Style.Vibrant)
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun chips_arrowsMoveFocusWithoutPicking_enterPicks() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> StyleChipsSection(context, dispatcher) }

            onNode(hasContentDescription("TonalSpot,", substring = true)).requestFocus()
            onNode(hasContentDescription("TonalSpot,", substring = true)).performKeyInput {
                pressKey(Key.DirectionRight)
            }
            waitForIdle()

            onNode(hasContentDescription("Neutral,", substring = true)).assertIsFocused()
            harness.actions.shouldBeEmpty()

            onNode(hasContentDescription("Neutral,", substring = true)).performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            harness.actions
                .single()
                .shouldBeTypeOf<WorkspaceAction.EditWithReveal>()
                .change shouldBe
                DocumentChange.SetStyle(Style.Neutral)
        }

    @Test
    fun chips_homeAndEnd_jumpToTheEndsWithoutPicking() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> StyleChipsSection(context, dispatcher) }

            onNode(hasContentDescription("TonalSpot,", substring = true)).requestFocus()
            onNode(hasContentDescription("TonalSpot,", substring = true)).performKeyInput { pressKey(Key.MoveEnd) }
            waitForIdle()

            onNode(hasContentDescription("Cmf,", substring = true)).assertIsFocused()

            onNode(hasContentDescription("Cmf,", substring = true)).performKeyInput { pressKey(Key.MoveHome) }
            waitForIdle()

            onNode(hasContentDescription("TonalSpot,", substring = true)).assertIsFocused()
            harness.actions.shouldBeEmpty()
        }

    @Test
    fun cmfField_underFluent_saysWhyAndTakesNoInput() =
        runComposeUiTest {
            val document = ThemeDocument(seed = Seed, style = Style.Cmf, library = Library.Fluent)
            val harness = PosterHarness(document)
            showSection(harness) { context, dispatcher -> StyleChipsSection(context, dispatcher) }

            onNodeWithText(TERTIARY_FIELD, useUnmergedTree = true).assertExists()
            onAllNodes(hasSetTextAction()).fetchSemanticsNodes().shouldBeEmpty()
            onNodeWithText("Fluent builds one accent ramp", substring = true).assertExists()
            onNodeWithText("For Fluent, the style only changes the chroma of the accent ramp.").assertExists()
        }

    @Test
    fun cmfField_showsOnlyForCmf() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> StyleChipsSection(context, dispatcher) }

            onAllNodes(hasSetTextAction()).fetchSemanticsNodes().shouldBeEmpty()

            harness.document = harness.document.copy(style = Style.Cmf)
            waitForIdle()
            onAllNodes(hasSetTextAction()).fetchSemanticsNodes() shouldHaveSize 1
            onNodeWithText(TERTIARY_FIELD, useUnmergedTree = true).assertExists()
            onNodeWithText("Derived from your seed until you type or pick one").assertExists()
        }

    @Test
    fun cmfField_setSeed_offersToDeriveAgain() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed, style = Style.Cmf, cmfTertiarySeed = Argb(0x00897B)))
            showSection(harness) { context, dispatcher -> StyleChipsSection(context, dispatcher) }

            onNodeWithText("Derive from seed").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(DocumentChange.SetCmfSeed(null), EditPhase.Discrete),
            )
        }

    @Test
    fun keepOnShuffle_readsTheStyleLockAndFlipsIt() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            harness.preferences = Preferences(styleLock = true)
            showSection(harness) { context, dispatcher -> StyleChipsSection(context, dispatcher) }

            val keep = onNodeWithContentDescription(KEEP_STYLE)
            keep.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            keep.performClick()
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.SetLock(ShuffleLock.Style, on = false))
        }

    @Test
    fun styleWords_tenStyles_readAsTenDistinctLines() =
        runComposeUiTest {
            val descriptions = mutableListOf<String>()
            val tooltips = mutableListOf<String>()
            setContent {
                Style.entries.forEach { style ->
                    descriptions += stringResource(styleDescription(style))
                    tooltips += stringResource(styleTooltip(style))
                }
            }
            waitForIdle()

            descriptions.toSet() shouldHaveSize 10
            tooltips.toSet() shouldHaveSize 10
            descriptions.toSet().intersect(tooltips.toSet()).shouldBeEmpty()
            (descriptions + tooltips).filter { text -> text.isBlank() }.shouldBeEmpty()
        }
}
