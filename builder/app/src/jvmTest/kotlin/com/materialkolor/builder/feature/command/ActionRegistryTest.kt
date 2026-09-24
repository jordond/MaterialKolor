package com.materialkolor.builder.feature.command

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.persist.ExportMode
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-315
@OptIn(ExperimentalTestApi::class)
class ActionRegistryTest {
    /**
     * Every command but Save has a control, and this finds each one, opening the menu, the panel or
     * the disclosure it sits in first. Save is the one exception, since the builder saves on its own
     * and only the keys and the palette ask it to save now.
     */
    @Test
    fun expanded_findsEachCommandsControlWhereTheRegistrySays() =
        runDesktopComposeUiTest(width = 1280, height = HEIGHT) {
            val harness = CommandHarness()
            with(harness) { show() }
            val missing = mutableListOf<String>()

            // Menus first, so no panel's closing hand-off lands while a menu is open.
            val order = harness.commands.sortedBy { command -> if (command.site is ControlSite.MenuItem) 0 else 1 }
            order.filter { command -> !command.id.startsWith("export.variants") }.forEach { command ->
                if (!found(harness, command)) missing += "${command.id} at ${command.site}"
            }
            runOnUiThread { harness.command("export.mode.${ExportMode.Frozen.name}").run() }
            waitForIdle()
            harness.commands.filter { command -> command.id.startsWith("export.variants") }.forEach { command ->
                if (!found(harness, command)) missing += "${command.id} at ${command.site}"
            }

            missing.joinToString("\n") shouldBe ""
            val withoutControl = harness.commands.filter { command -> command.site == null }
            withoutControl.map { command -> command.id } shouldBe listOf("save")
        }

    @Test
    fun expanded_listsEveryShortcutOnce() =
        runDesktopComposeUiTest(width = 1280, height = HEIGHT) {
            val harness = CommandHarness()
            with(harness) { show() }

            val shortcuts = harness.commands.mapNotNull { command -> command.shortcut }

            // b-315c
            // V and the held B work the dock straight from the keys.
            shortcuts.sorted() shouldBe Shortcut.entries.filter { shortcut -> shortcut.inRegistry }.sorted()
            harness.commands
                .map { command -> command.id }
                .distinct()
                .size shouldBe harness.commands.size
        }

    @Test
    fun medium_hasAnEntryForEveryCommand() =
        runDesktopComposeUiTest(width = 800, height = HEIGHT) {
            val harness = CommandHarness()
            with(harness) { show() }

            harness.commands.map { command -> command.id } shouldContainAll EXPECTED_IDS
        }

    @Test
    fun compact_hasAnEntryForEveryCommandButTheDeviceWidths() =
        runDesktopComposeUiTest(width = 400, height = HEIGHT) {
            val harness = CommandHarness()
            with(harness) { show() }
            val ids = harness.commands.map { command -> command.id }

            ids shouldContainAll EXPECTED_IDS.filterNot { id -> id.startsWith("deviceWidth") }
            ids.filter { id -> id.startsWith("deviceWidth") }.shouldBeEmpty()
        }

    /** Whether [command]'s control is on screen, opening its menu, panel or disclosure first. */
    private fun ComposeUiTest.found(
        harness: CommandHarness,
        command: Command,
    ): Boolean =
        when (val site = command.site) {
            null -> {
                true
            }
            is ControlSite.Direct -> {
                openIfClosed(site.opener, site.name)
                count(site.name) > 0
            }
            is ControlSite.MenuItem -> {
                // Counted, since a menu item may share its name with a control outside the menu.
                val closed = count(site.item)
                clickNamed(site.menu)
                val there = count(site.item) > closed
                closeMenu(site, closed)
                there
            }
            is ControlSite.InPanel -> {
                runOnUiThread { harness.workspace.openPanel(site.panel) }
                waitForIdle()
                openIfClosed(site.opener, site.name)
                val there = count(site.name) > 0
                runOnUiThread { harness.workspace.closePanel() }
                waitForIdle()
                there
            }
        }

    /** Opens the disclosure titled [opener] when there is one and [name] is not drawn yet. */
    private fun ComposeUiTest.openIfClosed(
        opener: String?,
        name: String,
    ) {
        if (opener != null && count(name) == 0) clickNamed(opener)
    }

    /**
     * Closes the menu [site] opened the way a mouse does, with a press on the page's bare bottom
     * corner, or else with a second press on its button, until its item is down to [closed] again.
     */
    private fun ComposeUiTest.closeMenu(
        site: ControlSite.MenuItem,
        closed: Int,
    ) {
        // The page's own root, not the menu's, which is a window of its own on the desktop.
        val roots = onAllNodes(isRoot())
        val page = roots.fetchSemanticsNodes().withIndex().maxBy { (_, root) -> root.size.width * root.size.height }
        roots[page.index].performMouseInput { click(Offset(1f, HEIGHT - 1f)) }
        waitForIdle()
        if (count(site.item) > closed) {
            // The menu's items take focus, and Esc on one closes it.
            val item = onAllNodes(hasText(site.item) and hasClickAction() and InWorkspace).onLast()
            item.requestFocus()
            item.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
        }
        if (count(site.item) > closed) clickNamed(site.menu)
    }

    /** How many workspace nodes, in any window, read [name] as their text or content description. */
    private fun ComposeUiTest.count(name: String): Int =
        onAllNodes((hasText(name) or hasContentDescription(name)) and InWorkspace, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .size

    /**
     * Clicks the workspace control named [name] through its click action, the way assistive
     * technology does. The top bar's More options button reports empty bounds here, so a press at
     * its middle would land on the bare page instead.
     */
    private fun ComposeUiTest.clickNamed(name: String) {
        val clickable = hasClickAction() and InWorkspace
        val described = onAllNodes(hasContentDescription(name) and clickable)
        val shown = onAllNodes(hasText(name) and clickable)
        val node = (if (described.fetchSemanticsNodes().isEmpty()) shown else described).onFirst()
        node.performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
    }
}

private const val HEIGHT = 800

/** Every command id the registry has at Expanded on the default theme. */
private val EXPECTED_IDS = listOf(
    "palette",
    "cheatSheet",
    "singleKeys",
    "help",
    "about",
    "github",
    "undo",
    "redo",
    "shuffle",
    "lock.Hue",
    "lock.Style",
    "lock.Seed",
    "copySeed",
    "addImage",
    "library.M3",
    "library.Expressive",
    "library.Unstyled",
    "library.Fluent",
    "library.Custom",
    "style.TonalSpot",
    "style.Vibrant",
    "previewMode.next",
    "previewMode.Light",
    "previewMode.Split",
    "previewMode.Dark",
    "tab.previous",
    "tab.next",
    "tab.App",
    "tab.Contrast",
    "inspect",
    "deviceWidth.next",
    "deviceWidth.Phone",
    "deviceWidth.Tablet",
    "deviceWidth.Desktop",
    "vision.None",
    "vision.Achromatopsia",
    "fullscreen",
    "poster",
    "projects",
    "newProject",
    "save",
    "share",
    "copyLink",
    "export",
    "export.copyAll",
    "export.download",
    "export.mode.Dynamic",
    "export.mode.Frozen",
    "export.versionCatalog",
    "export.animate",
    "export.variants.StandardOnly",
    "export.variants.AllContrasts",
    "appearance.next",
    "appearance.System",
    "appearance.Light",
    "appearance.Dark",
    "motion.System",
    "motion.Reduce",
    "motion.Full",
)
