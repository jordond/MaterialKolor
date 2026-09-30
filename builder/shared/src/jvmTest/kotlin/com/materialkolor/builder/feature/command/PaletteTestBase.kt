package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.workspace.Panel
import org.jetbrains.compose.resources.stringResource
import kotlin.test.AfterTest

/**
 * The command palette over the builder on fakes, with the steps its tests take to open it, search
 * and read its rows.
 */
@OptIn(ExperimentalTestApi::class)
internal abstract class PaletteTestBase {
    protected val harness: CommandHarness = CommandHarness()

    protected val platform: FakePlatform = harness.platform

    protected var categories: Map<CommandCategory, String> = emptyMap()
        private set

    @AfterTest
    fun tearDown() {
        harness.close()
    }

    protected fun ComposeUiTest.boot() {
        with(harness) { show(probe = { categories() }) }
    }

    @Composable
    protected fun categories() {
        categories = CommandCategory.entries.associateWith { category -> stringResource(category.title) }
    }

    protected fun ComposeUiTest.openPalette() {
        runOnUiThread { harness.workspace.openPanel(Panel.Palette) }
        waitForIdle()
    }

    protected fun ComposeUiTest.field(): SemanticsNodeInteraction = onNode(hasSetTextAction() and InPalette)

    protected fun ComposeUiTest.search(text: String) {
        field().requestFocus()
        field().performTextInput(text)
        waitForIdle()
    }

    protected fun ComposeUiTest.enter() {
        field().performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
    }

    /**
     * The palette's rows, top first, by their labels.
     */
    protected fun ComposeUiTest.rowLabels(): List<String> =
        onAllNodes(PaletteRow)
            .fetchSemanticsNodes()
            .sortedBy { node -> node.positionInRoot.y }
            .map { node ->
                node.config[SemanticsProperties.Text]
                    .first()
                    .text
            }

    protected fun rowMatcher(
        label: String,
        supporting: String? = null,
    ): SemanticsMatcher {
        val labelled = PaletteRow and hasText(label)
        return if (supporting == null) labelled else labelled and hasText(supporting)
    }
}

internal val InPalette: SemanticsMatcher = hasAnyAncestor(
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Command palette"),
)

internal val PaletteRow: SemanticsMatcher =
    hasClickAction() and InPalette and !hasSetTextAction() and !hasText("Close") and !hasContentDescription("Close")

/**
 * The top bar's Commands button, which only the page's own window holds.
 */
internal val CommandsButton: SemanticsMatcher =
    hasClickAction() and hasContentDescription("Command palette") and !InPalette
