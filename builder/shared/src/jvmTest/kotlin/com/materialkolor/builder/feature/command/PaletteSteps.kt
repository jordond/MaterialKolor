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
import com.materialkolor.builder.feature.workspace.Panel
import org.jetbrains.compose.resources.stringResource

/**
 * Boots the builder on [harness] for a command palette test. [onCategories] hears each category's
 * title, as the builder's strings read it, each time the page composes.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.boot(
    harness: CommandHarness,
    onCategories: (Map<CommandCategory, String>) -> Unit = {},
) {
    with(harness) { show(probe = { onCategories(categoryTitles()) }) }
}

/**
 * Each command category's title, as the builder's strings read it.
 */
@Composable
internal fun categoryTitles(): Map<CommandCategory, String> =
    CommandCategory.entries.associateWith { category -> stringResource(category.title) }

/**
 * Opens the command palette the way its command does.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.openPalette(harness: CommandHarness) {
    runOnUiThread { harness.workspace.openPanel(Panel.Palette) }
    waitForIdle()
}

/**
 * The palette's search field.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.field(): SemanticsNodeInteraction = onNode(hasSetTextAction() and InPalette)

/**
 * Types [text] into the palette's search field.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.search(text: String) {
    field().requestFocus()
    field().performTextInput(text)
    waitForIdle()
}

/**
 * Presses Enter in the palette's search field.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.enter() {
    field().performKeyInput { pressKey(Key.Enter) }
    waitForIdle()
}

/**
 * The palette's rows, top first, by their labels.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.rowLabels(): List<String> =
    onAllNodes(PaletteRow)
        .fetchSemanticsNodes()
        .sortedBy { node -> node.positionInRoot.y }
        .map { node ->
            node.config[SemanticsProperties.Text]
                .first()
                .text
        }

/**
 * The palette row that reads [label], and [supporting] under it when there is one.
 */
internal fun rowMatcher(
    label: String,
    supporting: String? = null,
): SemanticsMatcher {
    val labelled = PaletteRow and hasText(label)
    return if (supporting == null) labelled else labelled and hasText(supporting)
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
