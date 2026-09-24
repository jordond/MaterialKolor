package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import dev.stateholder.dispatcher.Dispatcher
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.roundToInt
import kotlin.test.Test

/** A red loud enough that TonalSpot has to calm it. */
private val Seed = Argb(0xE53935)

@OptIn(ExperimentalTestApi::class)
class ContrastExplainerTest {
    @Test
    fun contrastReadout_namesTheLowestPairWithItsRatioAndBadge() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> ContrastSection(context, dispatcher) }
            val row = harness.resolver
                .resolve(harness.document)
                .audit
                .lowestPair(PreviewMode.Split)
            val document = harness.document
            val mode = if (row.isDark) "dark" else "light"
            val fg = row.pair.foreground.readoutName(document)
            val bg = row.pair.background.readoutName(document)

            onNodeWithText("Lowest in $mode, $fg on $bg at ${ratioText(row.ratio)} to 1").assertExists()
        }

    @Test
    fun contrastSlider_drag_movesThenLetsGoAsOneUndoEntry() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> ContrastSection(context, dispatcher) }

            onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.5f) }
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(DocumentChange.SetContrast(ContrastLevel.Medium), EditPhase.Dragging),
                WorkspaceAction.Edit(DocumentChange.SetContrast(ContrastLevel.Medium), EditPhase.Released),
            )
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun contrastField_typedLevel_landsAsIs() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> ContrastSection(context, dispatcher) }

            onNode(hasSetTextAction()).requestFocus()
            onNode(hasSetTextAction()).performTextReplacement("0.37")
            onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            harness.actions shouldBe listOf(
                WorkspaceAction.Edit(DocumentChange.SetContrast(ContrastLevel(37)), EditPhase.Discrete),
            )
        }

    @Test
    fun contrastField_outOfRange_saysSoAndNeverEdits() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> ContrastSection(context, dispatcher) }

            onNode(hasSetTextAction()).requestFocus()
            onNode(hasSetTextAction()).performTextReplacement("1.7")
            onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            harness.actions.shouldBeEmpty()
            onNodeWithText("Type a number from -1 to 1", useUnmergedTree = true).assertExists()
        }

    @Test
    fun explainerLine_calmerPrimary_saysSoAndOpensThePanel() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showSection(harness) { context, dispatcher -> LineAndExplainer(context, dispatcher) }

            onNodeWithText("Primary is a calmer take on your seed.").assertExists()
            onNodeWithText("Why?").performClick()
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.OpenPanel(Panel.Explainer))
            val chroma = harness.resolver
                .resolve(harness.document)
                .light.primaryPalette.chroma
                .roundToInt()
            onNodeWithText("TonalSpot caps primary chroma at $chroma", substring = true).assertExists()
        }

    @Test
    fun explainerLine_primaryPinnedToTheSeed_staysAway() =
        runComposeUiTest {
            val pinned = DocumentChange.SetPin(Role.Primary, PinMode.Light, Seed)
            val harness = PosterHarness(pinned.apply(ThemeDocument(seed = Seed)))
            showSection(harness) { context, dispatcher -> LineAndExplainer(context, dispatcher) }

            onNodeWithText("Why?").assertDoesNotExist()
        }

    @Test
    fun keepChroma_switchesToFidelityAsOneUndoEntry() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed), openPanel = Panel.Explainer)
            showSection(harness) { context, dispatcher -> LineAndExplainer(context, dispatcher) }

            onNodeWithText("Keep chroma").performClick()
            waitForIdle()

            harness.actions shouldContain WorkspaceAction.EditWithReveal(DocumentChange.SetStyle(Style.Fidelity), null)
            harness.actions shouldContain WorkspaceAction.ClosePanel
            harness.document.style shouldBe Style.Fidelity
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun useAsPrimaryOverride_setsThePrimaryKeyColorAsOneUndoEntry() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed), openPanel = Panel.Explainer)
            showSection(harness) { context, dispatcher -> LineAndExplainer(context, dispatcher) }

            onNodeWithText("Use as primary override").performClick()
            waitForIdle()

            harness.document.keyColors.primary shouldBe Seed
            harness.actions shouldContain
                WorkspaceAction.Edit(DocumentChange.SetKeyColor(KeyColor.Primary, Seed), EditPhase.Discrete)
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun matchExactly_writesThePinsAsOneUndoEntry() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed), openPanel = Panel.Explainer)
            val expected = MatchExactly.of(ThemeResolver().resolve(harness.document), Seed, pinDark = false)
            showSection(harness) { context, dispatcher -> LineAndExplainer(context, dispatcher) }

            val onPrimary = expected.onPrimaryLight.toHex()
            onNodeWithText("Pins primary ${Seed.toHex()} and onPrimary $onPrimary in light mode").assertExists()
            // b-503b
            // The explainer scrolls, and Match exactly sits at its foot.
            onNodeWithText("Match exactly").performScrollTo().performClick()
            waitForIdle()

            harness.document.pins[Role.Primary]?.light shouldBe Seed
            harness.document.pins[Role.OnPrimary]?.light shouldBe expected.onPrimaryLight
            harness.document.pins[Role.Primary]?.dark shouldBe null
            harness.undoEntries() shouldBe 1
        }

    @Test
    fun matchExactly_belowAa_warns() =
        runComposeUiTest {
            val resolver = ThemeResolver()
            val warned = (0x404040..0x909090 step 0x010101).map { rgb -> Argb(rgb) }.firstOrNull { gray ->
                MatchExactly.of(resolver.resolve(ThemeDocument(seed = gray)), gray, pinDark = false).warns
            }
            val passed = MatchExactly.of(resolver.resolve(ThemeDocument(seed = Seed)), Seed, pinDark = false)
            passed.warns shouldBe false
            warned shouldNotBe null
            val harness = PosterHarness(ThemeDocument(seed = warned!!), openPanel = Panel.Explainer)
            showSection(harness) { context, dispatcher -> LineAndExplainer(context, dispatcher) }

            onNodeWithText("Below 4.5 to 1", useUnmergedTree = true).assertExists()
            onNodeWithText("Small text on primary will be hard to read.").assertExists()
        }
}

// b-221f

/** The explainer line with the explainer the workspace hosts over it, as the two sit on screen. */
@Composable
private fun LineAndExplainer(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    PrimaryExplainerLine(context, dispatcher)
    ExplainerDialog(context, dispatcher)
}
