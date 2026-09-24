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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** The skin's own icons, noting every glyph something draws. */
private class NotedIcons(
    private val icons: BuilderIcons,
    val drawn: MutableSet<IconId>,
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
    drawn: MutableSet<IconId>,
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
            val drawn = mutableSetOf<IconId>()
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

            row("Dark", Role.RadioButton).performClick()
            waitForIdle()
            picks shouldBe listOf("Dark")
            onAllNodes(isPopup()).assertCountEquals(0)

            tabThrough(listOf("Rename" to Role.Button, "Light" to Role.RadioButton, "Dark" to Role.RadioButton))
        }

    @Test
    fun selectPanel_everySkin_eachWay_drawsItsFieldOverTheOpenList() =
        hostEachWay { skin, inTree ->
            val drawn = mutableSetOf<IconId>()
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

            row("High", Role.RadioButton).performClick()
            waitForIdle()
            contrast shouldBe "High"
            row("High", Role.RadioButton).assertIsSelected()
            onNode(hasRole(Role.DropdownList)).assert(hasStateDescription("High"))
            onAllNodes(isPopup()).assertCountEquals(0)

            tabThrough(listOf("Reduced", "Standard", "High").map { option -> option to Role.RadioButton })
        }
}
