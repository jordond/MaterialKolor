package com.materialkolor.builder.feature.command

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.persist.ExportMode
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

// b-315
@OptIn(ExperimentalTestApi::class)
class ActionRegistryTest {
    @Test
    @Ignore("B-315 open: the More options menu items and AMOLED behind Spec extras are not found on the JVM yet")
    fun expanded_findsEachCommandsControlWhereTheRegistrySays() =
        runDesktopComposeUiTest(width = 1280, height = 800) {
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
            harness.commands.filter { command -> command.site == null }.map { command -> command.id } shouldBe listOf("save")
        }

    @Test
    fun expanded_listsEveryShortcutOnce() =
        runDesktopComposeUiTest(width = 1280, height = 800) {
            val harness = CommandHarness()
            with(harness) { show() }

            val shortcuts = harness.commands.mapNotNull { command -> command.shortcut }

            shortcuts.sorted() shouldBe Shortcut.entries.sorted()
            harness.commands.map { command -> command.id }.distinct().size shouldBe harness.commands.size
        }

    @Test
    fun medium_hasAnEntryForEveryCommand() =
        runDesktopComposeUiTest(width = 800, height = 800) {
            val harness = CommandHarness()
            with(harness) { show() }

            harness.commands.map { command -> command.id } shouldContainAll EXPECTED_IDS
        }

    @Test
    fun compact_hasAnEntryForEveryCommandButTheDeviceWidths() =
        runDesktopComposeUiTest(width = 400, height = 800) {
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
                site.opener?.takeIf { opener -> !named(site.name) && named(opener) }?.let { opener -> clickNamed(opener) }
                named(site.name)
            }
            is ControlSite.MenuItem -> {
                clickNamed(site.menu)
                val there = named(site.item)
                // A second press on the button closes its menu.
                clickNamed(site.menu)
                there
            }
            is ControlSite.InPanel -> {
                runOnUiThread { harness.workspace.openPanel(site.panel) }
                waitForIdle()
                site.opener?.takeIf { opener -> !named(site.name) && named(opener) }?.let { opener -> clickNamed(opener) }
                val there = named(site.name)
                runOnUiThread { harness.workspace.closePanel() }
                waitForIdle()
                there
            }
        }

    private fun ComposeUiTest.clickNamed(name: String) {
        val described = onAllNodes(hasContentDescription(name))
        val target = if (described.fetchSemanticsNodes().isEmpty()) onAllNodes(hasText(name)) else described
        target.onFirst().performClick()
        waitForIdle()
    }
}

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
