package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebKeyboardForTest
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Names: List<String> = listOf("M3", "M3 Expressive", "Unstyled", "Fluent", "Custom")

private const val RowTag = "row"

/**
 * The skins whose segmented row can size each option to its label, the web's keyboard habits on and
 * off for the expressive one, since those put each option in a box of its own.
 */
private val Cases: List<Triple<String, Skin, Boolean>> = listOf(
    Triple("expressive", Skin(SkinLibrary.Material3, expressive = true), false),
    Triple("expressive, web keyboard", Skin(SkinLibrary.Material3, expressive = true), true),
    Triple("custom", Skin(SkinLibrary.Custom, expressive = false), false),
)

/**
 * A segmented row shares its width evenly by default, and with equal widths off each option is as
 * wide as its own label, side by side with no overlap.
 */
@OptIn(ExperimentalTestApi::class)
class SegmentedWidthsTest {
    @Test
    fun segmented_equalWidths_givesEveryOptionTheSameWidth() {
        forEachCase(equalWidths = true) { widths, _ ->
            widths.forEach { width -> width.toDouble() shouldBe (widths.first().toDouble() plusOrMinus 1.0) }
        }
    }

    @Test
    fun segmented_ownWidths_givesALongLabelMoreRoomAndKeepsTheOptionsApart() {
        forEachCase(equalWidths = false) { widths, lefts ->
            widths[1] shouldBeGreaterThan widths[0] + 8f
            lefts.zipWithNext().forEachIndexed { index, (left, next) ->
                next shouldBeGreaterThan left + widths[index] - 1f
            }
        }
    }

    private fun forEachCase(
        equalWidths: Boolean,
        check: (widths: List<Float>, lefts: List<Float>) -> Unit,
    ) {
        for ((name, skin, webKeyboard) in Cases) {
            withClue(name) {
                runComposeUiTest {
                    setContent {
                        ControlsHarness(skin) {
                            WebKeyboard(on = webKeyboard) { Row(equalWidths) }
                        }
                    }
                    val options = onAllNodes(hasRole(Role.RadioButton) and hasAnyAncestor(hasTestTag(RowTag)))
                        .fetchSemanticsNodes()
                        .sortedBy { node -> node.boundsInRoot.left }
                    options.size shouldBe Names.size
                    check(options.map { node -> node.size.width.toFloat() }, options.map { node -> node.boundsInRoot.left })
                }
            }
        }
    }
}

@Composable
private fun Row(equalWidths: Boolean) {
    BuilderSegmented(
        options = Names,
        selected = Names.first(),
        onSelect = {},
        label = "Library",
        modifier = Modifier.testTag(RowTag),
        equalWidths = equalWidths,
    ) { option -> option }
}

@OptIn(KitTestApi::class)
@Composable
private fun WebKeyboard(
    on: Boolean,
    content: @Composable () -> Unit,
) {
    if (on) ProvideWebKeyboardForTest(content) else content()
}
