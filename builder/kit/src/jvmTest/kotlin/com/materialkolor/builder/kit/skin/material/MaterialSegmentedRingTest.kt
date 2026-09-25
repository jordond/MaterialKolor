package com.materialkolor.builder.kit.skin.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebKeyboardForTest
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.RingPixelsNeeded
import com.materialkolor.builder.kit.control.hasRole
import com.materialkolor.builder.kit.control.shouldCoverEverySide
import com.materialkolor.builder.kit.control.shouldRingAllTheWayRound
import com.materialkolor.builder.kit.control.shouldRingEverySide
import com.materialkolor.builder.kit.control.shouldShowRing
import com.materialkolor.builder.kit.control.tabOntoRing
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A row of five, so the middle option has a neighbour on each side that is not an end.
 */
private val Libraries: List<String> = listOf("Material 3", "Expressive", "Unstyled", "Fluent", "Custom")

/**
 * The first, a middle and the last option, the three places a ring meets its neighbours differently.
 */
private val Chosen: List<String> = listOf(Libraries.first(), Libraries[2], Libraries.last())

private const val RowTag = "row"

/**
 * An option of the segmented row tagged [RowTag].
 */
private val RowOption: SemanticsMatcher = hasRole(Role.RadioButton) and hasAnyAncestor(hasTestTag(RowTag))

/**
 * The expressive segmented row's focus ring, which its neighbours must never paint over, with the
 * web's keyboard habits on and off. With them on each option sits in a box of its own to keep the
 * written order, which is where a raised option used to rise over nothing.
 */
@OptIn(ExperimentalTestApi::class)
class MaterialSegmentedRingTest {
    @Test
    fun segmented_expressive_webKeyboardOnAndOff_ringsTheFirstMiddleAndLastOnEverySide() {
        for (keyboard in listOf(false, true)) {
            for (chosen in Chosen) {
                withClue("web keyboard $keyboard, $chosen chosen") {
                    runComposeUiTest {
                        val capture = tabOntoRing(Skin(Library.Material3, expressive = true)) {
                            WebKeyboard(on = keyboard) { LibraryRow(chosen) }
                        }
                        onNode(isFocused()).fetchSemanticsNode().optionText() shouldBe chosen
                        capture.shouldShowRing(RingPixelsNeeded)
                        capture.shouldRingEverySide()
                        capture.shouldCoverEverySide()
                        capture.shouldRingAllTheWayRound()
                    }
                }
            }
        }
    }

    @Test
    fun segmented_expressive_webKeyboardOn_readsInWrittenOrderWhileAnOptionHasFocus() {
        for (chosen in Chosen) {
            withClue("$chosen chosen") {
                runComposeUiTest {
                    tabOntoRing(Skin(Library.Material3, expressive = true)) {
                        WebKeyboard(on = true) { LibraryRow(chosen) }
                    }
                    onNode(isFocused()).fetchSemanticsNode().optionText() shouldBe chosen
                    onAllNodes(RowOption).fetchSemanticsNodes().map { node -> node.optionText() } shouldBe Libraries
                }
            }
        }
    }
}

/**
 * The library switcher's row with [chosen] picked, tagged [RowTag].
 */
@Composable
private fun LibraryRow(chosen: String) {
    BuilderSegmented(
        options = Libraries,
        selected = chosen,
        onSelect = {},
        label = "Library",
        modifier = Modifier.testTag(RowTag),
    ) { option -> option }
}

/**
 * Turns the web's keyboard habits on around [content] when [on] is set.
 */
@OptIn(KitTestApi::class)
@Composable
private fun WebKeyboard(
    on: Boolean,
    content: @Composable () -> Unit,
) {
    if (on) ProvideWebKeyboardForTest(content) else content()
}

/**
 * The label an option shows, as its merged text reads.
 */
private fun SemanticsNode.optionText(): String? =
    config.getOrNull(SemanticsProperties.Text)?.joinToString { text -> text.text }
