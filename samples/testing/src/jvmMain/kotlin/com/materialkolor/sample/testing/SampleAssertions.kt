package com.materialkolor.sample.testing

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import com.materialkolor.sample.shared.ui.SampleTags
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Asserts that the node tagged [tag], or a node inside it, shows exactly [text].
 */
internal fun SemanticsNodeInteractionsProvider.assertText(
    tag: String,
    text: String,
) {
    onNodeWithTag(tag, useUnmergedTree = true).assert(hasText(text) or hasAnyDescendant(hasText(text)))
}

/**
 * Asserts that a node tagged [tag] is on screen.
 */
internal fun SemanticsNodeInteractionsProvider.assertPresent(tag: String) {
    onNodeWithTag(tag, useUnmergedTree = true).assertExists()
}

/**
 * Asserts that no node is tagged [tag].
 */
internal fun SemanticsNodeInteractionsProvider.assertAbsent(tag: String) {
    onNodeWithTag(tag, useUnmergedTree = true).assertDoesNotExist()
}

/**
 * Asserts that of the [options], only [selected] is selected.
 */
internal fun <T> SemanticsNodeInteractionsProvider.assertOnlySelected(
    options: List<T>,
    selected: T,
    tag: (T) -> String,
) {
    for (option in options) {
        val node = onNodeWithTag(tag(option))
        if (option == selected) node.assertIsSelected() else node.assertIsNotSelected()
    }
}

/**
 * Asserts that the progress bar, or a node inside it, reports [expected] as its current value.
 */
internal fun SemanticsNodeInteractionsProvider.assertProgress(expected: Float) {
    val node = onNodeWithTag(SampleTags.Progress, useUnmergedTree = true).fetchSemanticsNode()
    val info = assertNotNull(node.findProgress(), "No ProgressBarRangeInfo on or inside ${SampleTags.Progress}")
    assertEquals(expected, info.current, absoluteTolerance = 0.001f, message = "Progress")
}

/**
 * Matches a text field whose own text, not its placeholder, is exactly [text].
 */
internal fun hasEditableText(text: String): SemanticsMatcher =
    SemanticsMatcher("EditableText is '$text'") { node ->
        node.config.getOrNull(SemanticsProperties.EditableText)?.text == text
    }

private fun SemanticsNode.findProgress(): ProgressBarRangeInfo? =
    config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        ?: children.firstNotNullOfOrNull { child -> child.findProgress() }
