package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.input.SetComposingTextCommand
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.canvas.RampTarget
import com.materialkolor.builder.feature.workspace.FineTuneSection
import com.materialkolor.builder.feature.workspace.Panel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class CommandPaletteTest {
    private val harness = CommandHarness()
    private val platform = harness.platform
    private var categories: Map<CommandCategory, String> = emptyMap()

    // The category is the header over the row now, and the keys are keycaps at its end.
    @Test
    fun everyCommand_listsUnderItsCategoryWithItsKeys_andADisabledOneWithItsReason() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()
            val apple = isApple(platform.environment.browser)
            // All but its own row, which would only open what is already open.
            val palette = harness.command("palette")
            onAllNodes(rowMatcher(palette.label)).fetchSemanticsNodes().size shouldBe 0
            val headers = onAllNodes(isHeading() and InPalette).fetchSemanticsNodes()

            val missing = harness.commands.filter { command -> command.id != palette.id }.filter { command ->
                val category = categories.getValue(command.category)
                val keys = command.shortcut?.text(apple)
                val shows = when (val state = command.state) {
                    is CommandState.Disabled -> rowMatcher(command.label, state.reason)
                    CommandState.Enabled -> rowMatcher(command.label) and
                        (keys?.let(::hasContentDescription) ?: PaletteRow)
                }
                onAllNodes(shows).fetchSemanticsNodes().none { node ->
                    val header = headers
                        .filter { each -> each.positionInRoot.y < node.positionInRoot.y }
                        .maxByOrNull { each -> each.positionInRoot.y }
                    header
                        ?.config
                        ?.get(SemanticsProperties.Text)
                        ?.first()
                        ?.text == category
                }
            }

            missing.map { command -> command.id } shouldBe emptyList()
            harness.command("undo").state shouldBe CommandState.Disabled("Nothing to undo")
            onNode(rowMatcher(harness.command("undo").label, "Nothing to undo")).assertIsNotEnabled()
        }

    @Test
    fun aRunCommand_leadsTheNextEmptySearch() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()
            val copy = harness.command("copySeed").label
            rowLabels().first() shouldNotBe copy

            onNode(rowMatcher(copy)).performScrollTo().performClick()
            waitForIdle()
            harness.workspace.state.value.panel shouldBe null
            platform.clipboard.texts.size shouldBe 1
            openPalette()

            rowLabels().first() shouldBe copy
        }

    @Test
    fun tsp_findsTonalSpotFirst_andEnterUsesIt() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete) }
            openPalette()

            search("tsp")
            rowLabels().first() shouldBe "Use style TonalSpot"
            enter()

            waitUntil { harness.graph.session.document.value.style == Style.TonalSpot }
            harness.workspace.state.value.panel shouldBe null
        }

    @Test
    fun aColor_leadsWithSetSeed() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()

            search("#0B6E4F")
            rowLabels().first() shouldBe "Set seed to #0B6E4F"
            enter()

            waitUntil { harness.graph.session.document.value.seed == Argb(0x0B6E4F) }
        }

    @Test
    fun aShareLink_leadsWithOpenSharedTheme() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val shared = ThemeDocument.Default.copy(seed = Argb(0x8A2BE2))
            val code = ShareCodec.encode(shared, projectName = "Shared")
            openPalette()

            search("https://materialkolor.com/t/$code")
            rowLabels().first() shouldBe "Open shared theme"
            enter()

            waitUntil { harness.graph.session.document.value.seed == Argb(0x8A2BE2) }
        }

    @Test
    fun aStyleName_leadsWithThatStyle() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()

            search("vibrant")
            rowLabels().first() shouldBe "Use style Vibrant"
            enter()

            waitUntil { harness.graph.session.document.value.style == Style.Vibrant }
        }

    @Test
    fun aRoleName_showsItOnItsRamp() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()

            search("primary container")
            rowLabels().first() shouldBe "Show primaryContainer on ramp"
            enter()

            val state = harness.workspace.state.value
            state.view.tab shouldBe PreviewTab.Palettes
            state.rampHighlight?.target shouldBe RampTarget.OfRole(Role.PrimaryContainer, isDark = false)
        }

    @Test
    fun aSectionName_opensThePosterAtIt() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.setPosterCollapsed(true) }
            waitUntil { harness.workspace.state.value.preferences.posterCollapsed }
            openPalette()

            search("key colors")
            rowLabels().first() shouldBe "Go to Key colors"
            enter()

            waitUntil { !harness.workspace.state.value.preferences.posterCollapsed }
            harness.workspace.state.value.fineTune shouldBe FineTuneSection.KeyColors
        }

    @Test
    fun goTo_opensTheFineTuneSheetAtASectionItHolds_andSeedOnlyOpensThePoster() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.setPosterCollapsed(true) }
            waitUntil { harness.workspace.state.value.preferences.posterCollapsed }

            openPalette()
            search("seed")
            onNode(rowMatcher("Go to Seed")).performScrollTo().performClick()
            waitUntil { !harness.workspace.state.value.preferences.posterCollapsed }
            harness.workspace.state.value.fineTune shouldBe null

            openPalette()
            search("target options")
            onNode(rowMatcher("Go to Target options")).performScrollTo().performClick()
            waitForIdle()
            harness.workspace.state.value.fineTune shouldBe FineTuneSection.TargetOptions
        }

    @Test
    fun motionRows_markTheCurrentOne_andSetTheOverride() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()
            val system = harness.command("motion.${MotionOverride.System.name}").label
            val reduce = harness.command("motion.${MotionOverride.Reduce.name}").label

            onNode(rowMatcher(system)).assertIsSelected()
            onNode(rowMatcher(reduce)).assertIsNotSelected()
            onNode(rowMatcher(reduce)).performScrollTo().performClick()

            waitUntil { harness.workspace.state.value.preferences.motion == MotionOverride.Reduce }
        }

    @Test
    fun enterOnCopyShareLink_writesBeforeTheKeyHandlerReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()
            search("copy share link")
            rowLabels().first() shouldBe "Copy share link"
            // No frame and no task runs after the key, so only a write started inside it lands.
            mainClock.autoAdvance = false

            field().performKeyInput { pressKey(Key.Enter) }

            platform.clipboard.texts.size shouldBe 1
            platform.clipboard.texts.single() shouldContain "/t/"
        }

    @Test
    fun saveNow_stillSaysSaved_afterThePaletteHasGone() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()
            search("save now")
            rowLabels().first() shouldBe harness.command("save").label

            enter()

            harness.workspace.state.value.panel shouldBe null
            waitUntil { named("Saved") }
        }

    @Test
    fun enterWhileAnInputMethodComposes_runsNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            var session: PlatformTextInputMethodRequest? = null
            with(harness) { show(onTextInput = { request -> session = request }, probe = { categories() }) }
            session = null
            openPalette()
            search("copy share lin")
            waitUntil { session != null }
            val request = checkNotNull(session)
            runOnUiThread { request.onEditCommand(listOf(SetComposingTextCommand("k", 1))) }
            waitForIdle()
            request.value().composition shouldNotBe null
            rowLabels().first() shouldBe "Copy share link"

            field().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            platform.clipboard.texts shouldBe emptyList()
            harness.workspace.state.value.panel shouldBe Panel.Palette
        }

    @Test
    fun esc_throwsTheSearchAway_thenCloses() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()
            search("zzz")
            rowLabels() shouldBe emptyList()

            field().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            harness.workspace.state.value.panel shouldBe Panel.Palette
            rowLabels().first() shouldBe harness.commands.first { command -> command.id != "palette" }.label
            field().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe null
        }

    @Test
    fun downAndUp_moveBetweenTheFieldAndTheRows() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()
            search("shuffle")
            val shuffle = harness.command("shuffle").label

            field().performKeyInput { pressKey(Key.DirectionDown) }
            waitForIdle()
            onNode(rowMatcher(shuffle)).assertIsFocused()
            onNode(rowMatcher(shuffle)).performKeyInput { pressKey(Key.DirectionUp) }
            waitForIdle()

            field().assertIsFocused()
        }

    @Test
    fun closing_handsFocusBackToCommands_whenItsButtonOpenedIt() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val commands = onNode(
                hasClickAction() and hasContentDescription("Command palette") and !InPalette,
            )
            commands.performClick()
            waitForIdle()
            harness.workspace.state.value.panel shouldBe Panel.Palette

            field().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe null
            commands.assertIsFocused()
        }

    @Test
    fun closing_handsFocusBackToThePage_whenCtrlKOpenedIt() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } }
            harness.workspace.state.value.panel shouldBe Panel.Palette

            field().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            harness.workspace.state.value.panel shouldBe null
            onNode(hasClickAction() and hasContentDescription("Command palette")).assertIsNotFocused()
            val seed = harness.graph.session.document.value.seed
            keys { pressKey(Key.Spacebar) }

            harness.graph.session.document.value.seed shouldNotBe seed
        }

    /**
     * On the desktop the palette is a window of its own, so the page's holder keeps focus in the
     * page's window, the way it does on the web in the frames before the palette takes focus.
     */
    @Test
    fun esc_thatReachesThePage_closesThePalette() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            keys { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } }
            harness.workspace.state.value.panel shouldBe Panel.Palette

            onNode(isRoot() and hasAnyDescendant(CommandsButton)).performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe null
        }

    @Test
    fun ctrlSInsideThePalette_saves_andCtrlO_leavesItOpen() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()

            field().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.O) } }
            waitForIdle()
            harness.workspace.state.value.panel shouldBe Panel.Palette
            field().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.S) } }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe Panel.Palette
            // b-522 The poster's Projects button carries the save state now, and the open palette keeps
            // the poster out of the tree, so the button reads once the palette has closed.
            onNode(isRoot() and hasAnyDescendant(CommandsButton)).performKeyInput { pressKey(Key.Escape) }
            val saved = hasContentDescription(", saved", substring = true) and InWorkspace
            waitUntil { onAllNodes(saved, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        }

    @Test
    fun aClosedPalette_buildsNoRegistryOnADragStep() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            var builds = 0
            with(harness) { show(probe = { categories() }, registryBuilds = { builds++ }) }

            fun dragStepBuilds(seed: Int): Int {
                builds = 0
                runOnUiThread {
                    harness.workspace.edit(DocumentChange.SetSeed(Argb(seed), SeedSource.Typed), EditPhase.Dragging)
                }
                waitForIdle()
                return builds
            }

            // The page's own registry, as often as a drag step recomposes the page.
            val page = dragStepBuilds(0xFF1A73E8.toInt())
            openPalette()
            runOnUiThread { harness.workspace.closePanel() }
            waitForIdle()

            dragStepBuilds(0xFF6750A4.toInt()) shouldBe page
        }

    @Test
    fun theVisionMenuRow_closesThePalette_andOpensTheMenu() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()
            search("vision menu")
            rowLabels().first() shouldBe harness.command("visionMenu").label

            enter()

            harness.workspace.state.value.panel shouldBe null
            harness.workspace.state.value.visionMenuOpen shouldBe true
        }

    private fun ComposeUiTest.boot() {
        with(harness) { show(probe = { categories() }) }
    }

    @Composable
    private fun categories() {
        categories = CommandCategory.entries.associateWith { category -> stringResource(category.title) }
    }

    private fun ComposeUiTest.openPalette() {
        runOnUiThread { harness.workspace.openPanel(Panel.Palette) }
        waitForIdle()
    }

    private fun ComposeUiTest.field(): SemanticsNodeInteraction = onNode(hasSetTextAction() and InPalette)

    private fun ComposeUiTest.search(text: String) {
        field().requestFocus()
        field().performTextInput(text)
        waitForIdle()
    }

    private fun ComposeUiTest.enter() {
        field().performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
    }

    /**
     * The palette's rows, top first, by their labels.
     */
    private fun ComposeUiTest.rowLabels(): List<String> =
        onAllNodes(PaletteRow)
            .fetchSemanticsNodes()
            .sortedBy { node -> node.positionInRoot.y }
            .map { node ->
                node.config[SemanticsProperties.Text]
                    .first()
                    .text
            }

    private fun rowMatcher(
        label: String,
        supporting: String? = null,
    ): SemanticsMatcher {
        val labelled = PaletteRow and hasText(label)
        return if (supporting == null) labelled else labelled and hasText(supporting)
    }
}

private val InPalette: SemanticsMatcher = hasAnyAncestor(
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Command palette"),
)

private val PaletteRow: SemanticsMatcher =
    hasClickAction() and InPalette and !hasSetTextAction() and !hasText("Close") and !hasContentDescription("Close")

/**
 * The top bar's Commands button, which only the page's own window holds.
 */
private val CommandsButton: SemanticsMatcher =
    hasClickAction() and hasContentDescription("Command palette") and !InPalette
