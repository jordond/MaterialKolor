package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.materialkolor.builder.kit.icon.BuilderIcons
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.icon.LocalBuilderIcons
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** The skin's own icons, noting every glyph something draws, once for each time it is drawn. */
private class NotedIcons(
    private val icons: BuilderIcons,
    val drawn: MutableList<IconId>,
) : BuilderIcons {
    override fun get(id: IconId): ImageVector {
        drawn += id
        return icons[id]
    }
}

/**
 * Shows [panel] in [skin] between the Before and After buttons, with the overlays [inTree] or not,
 * and notes every glyph drawn into [drawn].
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showPanel(
    skin: Skin,
    inTree: Boolean,
    drawn: MutableList<IconId>,
    panel: @Composable () -> Unit,
) {
    setContent {
        HostOverlays(skin, inTree) {
            CompositionLocalProvider(LocalBuilderIcons provides NotedIcons(LocalBuilderIcons.current, drawn)) {
                Column {
                    BuilderButton({}, "Before", Modifier.testTag("before"))
                    panel()
                    BuilderButton({}, "After", Modifier.testTag("after"))
                }
            }
        }
    }
    waitForIdle()
}

/** The row labelled [label] that plays [role]. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.row(
    label: String,
    role: Role,
): SemanticsNodeInteraction = onNode(hasText(label) and hasRole(role))

/** Tabs from Before through [rows] one at a time and on to After. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.tabThrough(rows: List<Pair<String, Role>>) {
    onNodeWithTag("before").requestFocus()
    waitForIdle()
    for ((label, role) in rows) {
        onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        row(label, role).assertIsFocused()
    }
    onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
    waitForIdle()
    onNodeWithTag("after").assertIsFocused()
}

@OptIn(ExperimentalTestApi::class)
class OpenPanelsTest {
    @Test
    fun menuPanel_everySkin_eachWay_drawsOpenInPlace() =
        hostEachWay { skin, inTree ->
            val drawn = mutableListOf<IconId>()
            val picks = mutableListOf<String>()
            showPanel(skin, inTree, drawn) {
                BuilderMenuPanel(
                    items = listOf(
                        BuilderMenuItem("Rename", { picks += "Rename" }),
                        BuilderMenuItem("Light", { picks += "Light" }, selected = true),
                        BuilderMenuItem("Dark", { picks += "Dark" }, selected = false),
                    ),
                )
            }

            onAllNodes(isPopup()).assertCountEquals(0)
            row("Rename", Role.Button).assertExists()
            row("Light", Role.RadioButton).assertIsSelected()
            row("Dark", Role.RadioButton).assertIsNotSelected()
            drawn shouldContain IconId.Check
            withClue("checks drawn") { drawn.count { id -> id == IconId.Check } shouldBe 1 }

            row("Dark", Role.RadioButton).performClick()
            waitForIdle()
            picks shouldBe listOf("Dark")
            onAllNodes(isPopup()).assertCountEquals(0)

            tabThrough(listOf("Rename" to Role.Button, "Light" to Role.RadioButton, "Dark" to Role.RadioButton))
        }

    @Test
    fun selectPanel_everySkin_eachWay_drawsItsFieldOverTheOpenList() =
        hostEachWay { skin, inTree ->
            val drawn = mutableListOf<IconId>()
            var contrast by mutableStateOf("Standard")
            showPanel(skin, inTree, drawn) {
                BuilderSelectPanel(
                    label = "Contrast",
                    options = listOf("Reduced", "Standard", "High"),
                    selected = contrast,
                    onSelect = { picked -> contrast = picked },
                )
            }

            onAllNodes(isPopup()).assertCountEquals(0)
            onNode(hasRole(Role.DropdownList)).assert(hasStateDescription("Standard"))
            row("Standard", Role.RadioButton).assertIsSelected()
            row("High", Role.RadioButton).assertIsNotSelected()
            drawn shouldContain IconId.Check
            withClue("checks drawn") { drawn.count { id -> id == IconId.Check } shouldBe 1 }

            drawn.clear()
            row("High", Role.RadioButton).performClick()
            waitForIdle()
            contrast shouldBe "High"
            withClue("checks drawn for the new choice") { drawn.count { id -> id == IconId.Check } shouldBe 1 }
            row("High", Role.RadioButton).assertIsSelected()
            onNode(hasRole(Role.DropdownList)).assert(hasStateDescription("High"))
            onAllNodes(isPopup()).assertCountEquals(0)

            tabThrough(listOf("Reduced", "Standard", "High").map { option -> option to Role.RadioButton })
        }

    /**
     * The open list spans its field, less the edge it keeps round itself for its shadow, whether the
     * field's label or an option is the wider, and the panel holds its width from frame to frame
     * (R-B-221d).
     */
    @Test
    fun selectPanel_everySkin_listSpansItsFieldAndHoldsItsWidth() =
        forEachSkin { _, skin ->
            var wideOption by mutableStateOf(false)
            var edge = 0
            setContent {
                ControlsHarness(skin) {
                    edge = with(LocalDensity.current) {
                        LocalBuilderTokens.current.spacing.small
                            .roundToPx()
                    }
                    val options = if (wideOption) {
                        listOf(
                            "An option far wider than its label",
                            "Short",
                        )
                    } else {
                        listOf("A", "B")
                    }
                    BuilderSelectPanel(
                        label = if (wideOption) "Size" else "A label far wider than any of its options",
                        options = options,
                        selected = options.first(),
                        onSelect = {},
                    )
                }
            }
            for (wide in listOf(false, true)) {
                withClue(if (wide) "wide option" else "wide label") {
                    wideOption = wide
                    waitForIdle()
                    val field = onNode(hasRole(Role.DropdownList)).fetchSemanticsNode().size.width
                    val list = onNode(OpenList).fetchSemanticsNode().size.width
                    list shouldBe (field - 2 * edge)
                    repeat(3) { mainClock.advanceTimeByFrame() }
                    onNode(hasRole(Role.DropdownList)).fetchSemanticsNode().size.width shouldBe field
                }
            }
        }
}

/** The open list, the one node in a select's panel that scrolls down. */
private val OpenList: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
