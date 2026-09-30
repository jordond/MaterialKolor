package com.materialkolor.builder.feature.command

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.WIDTH
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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
internal class CommandPaletteTest : PaletteTestBase() {
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

            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.graph.session.document.value.style == Style.TonalSpot }
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

            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.graph.session.document.value.seed == Argb(0x0B6E4F) }
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

            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.graph.session.document.value.seed == Argb(0x8A2BE2) }
        }

    @Test
    fun aStyleName_leadsWithThatStyle() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            openPalette()

            search("vibrant")
            rowLabels().first() shouldBe "Use style Vibrant"
            enter()

            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.graph.session.document.value.style == Style.Vibrant }
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
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.workspace.state.value.preferences.posterCollapsed }
            openPalette()

            search("key colors")
            rowLabels().first() shouldBe "Go to Key colors"
            enter()

            waitUntil(timeoutMillis = WAIT_MILLIS) { !harness.workspace.state.value.preferences.posterCollapsed }
            harness.workspace.state.value.fineTune shouldBe FineTuneSection.KeyColors
        }

    @Test
    fun goTo_opensTheFineTuneSheetAtASectionItHolds_andSeedOnlyOpensThePoster() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.setPosterCollapsed(true) }
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.workspace.state.value.preferences.posterCollapsed }

            openPalette()
            search("seed")
            onNode(rowMatcher("Go to Seed")).performScrollTo().performClick()
            waitUntil(timeoutMillis = WAIT_MILLIS) { !harness.workspace.state.value.preferences.posterCollapsed }
            // The closed palette leaves the tree, and its search with it, before it opens again.
            waitForIdle()
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

            waitUntil(timeoutMillis = WAIT_MILLIS) {
                harness.workspace.state.value.preferences.motion ==
                    MotionOverride.Reduce
            }
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
            waitUntil(timeoutMillis = WAIT_MILLIS) { named("Saved") }
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
}
