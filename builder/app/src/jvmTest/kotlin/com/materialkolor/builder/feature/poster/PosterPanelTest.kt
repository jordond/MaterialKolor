package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.shell.PosterSurface
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** The seed field's pause before it commits (F-05). */
private const val COMMIT_DELAY = 400L

private val Seed = Argb(0x6750A4)

/** A phone held upright, which gets the poster sheet. */
private const val PHONE_WIDTH = 400
private const val PHONE_HEIGHT = 800

@OptIn(ExperimentalTestApi::class)
class PosterPanelTest {
    private val actions = mutableListOf<WorkspaceAction>()
    private lateinit var focus: FocusManager

    @Test
    fun seedHero_enter_editsTheSeedAsTyped() =
        runComposeUiTest {
            showPoster()

            seedField().requestFocus()
            seedField().performTextReplacement("#00ff00")
            seedField().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            actions shouldBe listOf(typedSeed(0x00FF00))
            fieldText() shouldBe "#00FF00"
        }

    @Test
    fun seedHero_leavingTheField_editsTheSeedAsTyped() =
        runComposeUiTest {
            showPoster()

            seedField().requestFocus()
            seedField().performTextReplacement("red")
            runOnIdle { focus.clearFocus() }
            waitForIdle()

            actions shouldBe listOf(typedSeed(0xFF0000))
        }

    @Test
    fun seedHero_pauseAfterAValidKeystroke_editsOnceTheDelayIsUp() =
        runComposeUiTest {
            showPoster()

            seedField().requestFocus()
            mainClock.autoAdvance = false
            seedField().performTextReplacement("#1e88e5")
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(COMMIT_DELAY - 100)
            actions.shouldBeEmpty()
            mainClock.advanceTimeBy(150)
            mainClock.advanceTimeByFrame()

            actions shouldBe listOf(typedSeed(0x1E88E5))
        }

    @Test
    fun seedHero_escape_putsTheSeedBackAndNeverEdits() =
        runComposeUiTest {
            showPoster()

            seedField().requestFocus()
            mainClock.autoAdvance = false
            seedField().performTextReplacement("#00ff00")
            mainClock.advanceTimeByFrame()
            seedField().performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(COMMIT_DELAY * 2)

            fieldText() shouldBe Seed.toHex()
            actions.shouldBeEmpty()
        }

    @Test
    fun seedHero_invalidText_showsTheErrorAndNeverEdits() =
        runComposeUiTest {
            showPoster()

            seedField().requestFocus()
            seedField().performTextReplacement("#12345")
            mainClock.advanceTimeBy(COMMIT_DELAY * 3)
            seedField().performKeyInput { pressKey(Key.Enter) }
            runOnIdle { focus.clearFocus() }
            waitForIdle()

            actions.shouldBeEmpty()
            onNodeWithText("Hex takes 3, 6 or 8 digits", useUnmergedTree = true).assertExists()
        }

    @Test
    fun seedHero_pastedHex_editsTheSeedAsTyped() =
        runComposeUiTest {
            showPoster()

            seedField().requestFocus()
            mainClock.autoAdvance = false
            // A paste replaces the whole text in one go, stray spaces and all.
            seedField().performTextReplacement("  #2E7D32 ")
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(COMMIT_DELAY + 100)
            mainClock.advanceTimeByFrame()

            actions shouldBe listOf(typedSeed(0x2E7D32))
        }

    @Test
    fun seedHero_eachSource_showsWhereTheSeedCameFrom() =
        runComposeUiTest {
            var document by mutableStateOf(ThemeDocument(seed = Seed, seedSource = SeedSource.Typed))
            showPoster(document = { document })
            onNodeWithText("Typed", useUnmergedTree = true).assertExists()

            document = document.copy(seedSource = SeedSource.Image("sunset.png"))
            waitForIdle()
            onNodeWithText("From sunset.png", useUnmergedTree = true).assertExists()

            document = document.copy(seedSource = SeedSource.Shuffled)
            waitForIdle()
            onNodeWithText("Shuffled", useUnmergedTree = true).assertExists()
        }

    @Test
    fun seedHero_copyButtons_copyTheHexAndTheKotlinLiteral() =
        runComposeUiTest {
            showPoster()

            onNodeWithText("Copy hex").performClick()
            onNodeWithText("Copy Kotlin").performClick()
            waitForIdle()

            actions shouldBe listOf(
                WorkspaceAction.CopyText("#6750A4", "seed hex"),
                WorkspaceAction.CopyText("Color(0xFF6750A4)", "seed as Kotlin"),
            )
        }

    @Test
    fun seedActions_shuffle_asksTheWorkspaceForOneShuffle() =
        runComposeUiTest {
            showPoster()

            onNodeWithText("Shuffle").performClick()
            waitForIdle()

            actions shouldBe listOf(WorkspaceAction.Shuffle(origin = null))
        }

    @Test
    fun seedActions_seedAndStyleLocked_showsTheHintAndTurnsShuffleOff() =
        runComposeUiTest {
            showPoster(preferences = Preferences(styleLock = true, seedLock = true))

            onNodeWithText("The seed and the style are both locked", substring = true).assertExists()
            onNodeWithText("Shuffle").assertIsNotEnabled()
        }

    @Test
    fun seedActions_lockToggle_setsThatLock() =
        runComposeUiTest {
            showPoster(preferences = Preferences(hueLock = false, styleLock = true))

            onNodeWithText("Lock hue").performClick()
            onNodeWithText("Lock style").performClick()
            waitForIdle()

            actions shouldBe listOf(
                WorkspaceAction.SetLock(ShuffleLock.Hue, on = true),
                WorkspaceAction.SetLock(ShuffleLock.Style, on = false),
            )
        }

    @Test
    fun seedActions_pickAndImage_openThePickerOnTheSeedAndTheImagePicker() =
        runComposeUiTest {
            showPoster()

            onNodeWithText("Pick").performClick()
            onNodeWithText("Image").performClick()
            waitForIdle()

            actions shouldBe listOf(WorkspaceAction.OpenPicker(PickerTarget.Seed), WorkspaceAction.OpenImagePicker)
        }

    @Test
    fun posterHeader_projects_showsTheNameAndSaveStateAndOpensProjects() =
        runComposeUiTest {
            showPoster(projectName = "Ocean")

            onNodeWithText("Saved", useUnmergedTree = true).assertExists()
            onNodeWithText("Ocean").performClick()
            waitForIdle()

            actions shouldBe listOf(WorkspaceAction.OpenPanel(Panel.Projects))
        }

    @Test
    fun posterHeader_collapse_asksForTheRail() =
        runComposeUiTest {
            showPoster()

            onNodeWithContentDescription("Collapse the poster").performClick()
            waitForIdle()

            actions shouldBe listOf(WorkspaceAction.SetPosterCollapsed(collapsed = true))
        }

    @Test
    fun posterHeader_onAPhone_hasNoCollapseButton() =
        runDesktopComposeUiTest(width = PHONE_WIDTH, height = PHONE_HEIGHT) {
            showPoster()

            onNodeWithText("MaterialKolor").assertExists()
            onNodeWithContentDescription("Collapse the poster").assertDoesNotExist()
        }

    @Test
    fun posterRail_buttons_shuffleOpenProjectsAndOpenThePosterAgain() =
        runComposeUiTest {
            showPoster(rail = true)

            onNodeWithContentDescription("Seed #6750A4").assertExists()
            onNodeWithContentDescription("Shuffle").performClick()
            onNodeWithContentDescription("Projects").performClick()
            onNodeWithContentDescription("Open the poster").performClick()
            waitForIdle()

            actions shouldBe listOf(
                WorkspaceAction.Shuffle(origin = null),
                WorkspaceAction.OpenPanel(Panel.Projects),
                WorkspaceAction.SetPosterCollapsed(collapsed = false),
            )
        }

    @Test
    fun infoButton_seed_opensItsExplanationAndDocsLink() =
        runComposeUiTest {
            showPoster()
            onNodeWithText("The one color every palette grows from", substring = true).assertDoesNotExist()

            onNodeWithContentDescription("What is the seed?").performClick()
            waitForIdle()

            onNodeWithText("The one color every palette grows from", substring = true).assertExists()
            onNodeWithText("Read more in the docs").assertExists()
        }

    /**
     * The poster on its surface, the way the shell draws it. Edits land on the document straight
     * away, as the workspace would, so a commit that matches the seed never shows up twice.
     */
    private fun ComposeUiTest.showPoster(
        document: () -> ThemeDocument? = { null },
        preferences: Preferences = Preferences(),
        projectName: String = "",
        rail: Boolean = false,
    ) {
        val resolver = ThemeResolver()
        setContent {
            focus = LocalFocusManager.current
            var edited by remember { mutableStateOf(ThemeDocument(seed = Seed, seedSource = SeedSource.Typed)) }
            val shown = document() ?: edited
            val dispatcher = rememberDispatcher<WorkspaceAction> { action ->
                actions += action
                if (action is WorkspaceAction.Edit) edited = action.change.apply(edited)
            }
            val result = remember(shown) { resolver.resolve(shown) }
            BuilderTheme(
                skin = Skin(library = Library.Material3, expressive = false),
                result = result,
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalThemeResult provides result) {
                        PosterSurface(result.poster) {
                            PosterPanel(
                                state = workspaceState(shown, preferences, projectName),
                                rail = rail,
                                dispatcher = dispatcher,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.seedField() = onNode(hasSetTextAction())

    private fun ComposeUiTest.fieldText(): String =
        seedField().fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun typedSeed(rgb: Int): WorkspaceAction =
        WorkspaceAction.Edit(DocumentChange.SetSeed(Argb(rgb), SeedSource.Typed), EditPhase.Discrete)

    private fun workspaceState(
        document: ThemeDocument,
        preferences: Preferences,
        projectName: String,
    ): WorkspaceModel.State =
        WorkspaceModel.State(
            document = document,
            capabilities = capabilitiesOf(document),
            history = HistoryState(),
            view = ProjectViewState(),
            preferences = preferences,
            projectName = projectName,
        )
}
