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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorNames
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
import com.materialkolor.builder.kit.shell.WorkspaceShell
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/** The seed field's pause before it commits (F-05). */
private const val COMMIT_DELAY = 400L

private val Seed = Argb(0x6750A4)

/** A phone held upright, which gets the poster sheet. */
private const val PHONE_WIDTH = 400
private const val PHONE_HEIGHT = 800

/** The phone the sheet peek is checked on, upright and on its side (D38). */
private const val PEEK_PHONE_SHORT = 390
private const val PEEK_PHONE_LONG = 844

/** How much of the poster sheet shows at peek, upright and on its side, as the kit's shell sets it. */
private val UprightPeek: Dp = 344.dp
private val SidewaysPeek: Dp = 96.dp

private const val ALL_LOCKED = "The seed and the style are both locked"

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
    fun seedHero_documentSwappedWhileFocused_showsTheNewSeedAfterBlurWithoutEditing() =
        runComposeUiTest {
            var document by mutableStateOf(ThemeDocument(seed = Seed, seedSource = SeedSource.Typed))
            showPoster(document = { document })

            seedField().requestFocus()
            mainClock.autoAdvance = false
            seedField().performTextReplacement("#00ff00")
            mainClock.advanceTimeByFrame()
            // An undo, or a seed picked elsewhere, lands while the field still holds a draft.
            document = document.copy(seed = Argb(0x1E88E5), seedSource = SeedSource.Picked)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(COMMIT_DELAY * 2)
            mainClock.autoAdvance = true
            runOnIdle { focus.clearFocus() }
            waitForIdle()

            actions.shouldBeEmpty()
            fieldText() shouldBe "#1E88E5"
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

            // b-221f
            // Each copy also carries its own button, for the manual copy dialog to hand focus back to.
            val copies = actions.map { action -> (action as WorkspaceAction.CopyText).copy(returnFocusTo = null) }
            copies shouldBe listOf(
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

            // b-307
            // Pick also carries its own button, for the picker to hand focus back to.
            val pick = actions.first() as WorkspaceAction.OpenPicker
            pick.returnFocusTo shouldNotBe null
            listOf(pick.copy(returnFocusTo = null)) + actions.drop(1) shouldBe
                listOf(WorkspaceAction.OpenPicker(PickerTarget.Seed), WorkspaceAction.OpenImagePicker)
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
    fun posterHeader_projects_readsAsProjectsAndTheName() =
        runComposeUiTest {
            showPoster(projectName = "Ocean")

            onNodeWithContentDescription("Projects, Ocean").assertExists()
        }

    @Test
    fun posterHeader_saveStatus_showsEachStatusAsABadge() =
        runComposeUiTest {
            var status: SaveStatus by mutableStateOf(SaveStatus.Pending)
            showPoster(saveStatus = { status })
            onNodeWithText("Saving", useUnmergedTree = true).assertExists()

            status = SaveStatus.Failed(StoreError.QuotaExceeded)
            waitForIdle()
            onNodeWithText("Not saved", useUnmergedTree = true).assertExists()

            status = SaveStatus.Idle
            waitForIdle()
            onNodeWithText("Saved", useUnmergedTree = true).assertExists()
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
    fun posterRail_everythingLocked_shuffleIsOffAndItsTooltipSaysWhy() =
        runComposeUiTest {
            // The headless tooltip, which opens on hover alone.
            val locked = Preferences(styleLock = true, seedLock = true)
            showPoster(preferences = locked, rail = true, library = Library.Custom)

            onNodeWithContentDescription("Shuffle").assertIsNotEnabled()
            onNodeWithContentDescription("Shuffle").performMouseInput { enter(center) }
            waitForIdle()

            onNodeWithText(ALL_LOCKED, substring = true).assertExists()
        }

    @Test
    fun posterSheet_uprightPhone_peekShowsTheSeedRowAndShuffle() =
        runDesktopComposeUiTest(width = PEEK_PHONE_SHORT, height = PEEK_PHONE_LONG) {
            showPoster(coarsePointer = true, shell = true)

            assertPeekShows(UprightPeek)
            assertInPeek(hasText("Pick"), UprightPeek)
            assertInPeek(hasText("Image"), UprightPeek)
        }

    @Test
    fun posterSheet_phoneOnItsSide_peekShowsTheSeedRowAndShuffle() =
        runDesktopComposeUiTest(width = PEEK_PHONE_LONG, height = PEEK_PHONE_SHORT) {
            showPoster(coarsePointer = true, shell = true)

            assertPeekShows(SidewaysPeek)
        }

    @Test
    fun infoButton_seed_opensItsExplanationWithoutADocsLink() =
        runComposeUiTest {
            showPoster()
            onNodeWithText("The one color every palette grows from", substring = true).assertDoesNotExist()

            onNodeWithContentDescription("What is the seed?").performClick()
            waitForIdle()

            onNodeWithText("The one color every palette grows from", substring = true).assertExists()
            onNodeWithText("Read more in the docs").assertDoesNotExist()
        }

    // b-221f
    @Test
    fun infoButton_seed_readsCollapsedThenExpandedOnceOpened() =
        runComposeUiTest {
            showPoster()
            val info = onNodeWithContentDescription("What is the seed?")
            info.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))

            info.performClick()
            waitForIdle()

            info.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        }

    /**
     * The poster on its surface, the way the shell draws it, or inside the shell itself with
     * [shell], so a phone gets the sheet at its peek. Edits land on the document straight away, as
     * the workspace would, so a commit that matches the seed never shows up twice.
     */
    private fun ComposeUiTest.showPoster(
        document: () -> ThemeDocument? = { null },
        preferences: Preferences = Preferences(),
        projectName: String = "",
        saveStatus: () -> SaveStatus = { SaveStatus.Idle },
        rail: Boolean = false,
        library: Library = Library.Material3,
        coarsePointer: Boolean = false,
        shell: Boolean = false,
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
            val state = workspaceState(shown, preferences, projectName, saveStatus())
            BuilderTheme(
                skin = Skin(library = library, expressive = false),
                result = result,
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(coarsePointer = coarsePointer, modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalThemeResult provides result) {
                        if (shell) {
                            WorkspaceShell(
                                posterColors = result.poster,
                                posterCollapsed = false,
                                poster = { asRail -> PosterPanel(state, asRail, dispatcher) },
                                topBar = {},
                                canvas = {},
                                dock = {},
                            )
                        } else {
                            PosterSurface(result.poster) {
                                PosterPanel(state = state, rail = rail, dispatcher = dispatcher)
                            }
                        }
                    }
                }
            }
        }
    }

    /** The seed row, the seed's hex and name, and Shuffle all sit inside the sheet's [peek]. */
    private fun ComposeUiTest.assertPeekShows(peek: Dp) {
        assertInPeek(hasText(Seed.toHex()), peek)
        assertInPeek(hasText(ColorNames.nameOf(Seed)), peek)
        assertInPeek(hasContentDescription("Shuffle"), peek)
    }

    /**
     * Some node [matcher] finds sits wholly inside the bottom [peek] of the window. The hero lower
     * down the sheet repeats the hex and the name, so only one of them has to. Bounds in the root
     * are clipped to what shows, so a node cut off by the window edge comes up shorter than it is.
     */
    private fun ComposeUiTest.assertInPeek(
        matcher: SemanticsMatcher,
        peek: Dp,
    ) {
        val bottom = onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val top = bottom - with(density) { peek.toPx() }
        val inPeek = onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().filter { node ->
            val shown = node.boundsInRoot
            shown.top >= top && shown.bottom <= bottom && shown.height >= node.size.height - 1f
        }
        inPeek.shouldNotBeEmpty()
    }

    // b-304
    private fun ComposeUiTest.seedField() =
        onNode(hasSetTextAction() and hasContentDescription("Seed color, any format"))

    private fun ComposeUiTest.fieldText(): String =
        seedField().fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun typedSeed(rgb: Int): WorkspaceAction =
        WorkspaceAction.Edit(DocumentChange.SetSeed(Argb(rgb), SeedSource.Typed), EditPhase.Discrete)

    private fun workspaceState(
        document: ThemeDocument,
        preferences: Preferences,
        projectName: String,
        saveStatus: SaveStatus,
    ): WorkspaceModel.State =
        WorkspaceModel.State(
            document = document,
            capabilities = capabilitiesOf(document),
            history = HistoryState(),
            view = ProjectViewState(),
            preferences = preferences,
            projectName = projectName,
            saveStatus = saveStatus,
        )
}
