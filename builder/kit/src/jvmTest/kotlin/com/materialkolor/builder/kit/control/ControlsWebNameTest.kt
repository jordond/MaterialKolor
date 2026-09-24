package com.materialkolor.builder.kit.control

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Shows [content] in [skin] with the web's fold turned on. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showFolded(
    skin: Skin,
    content: @Composable () -> Unit,
) {
    setContent {
        ControlsHarness(skin) {
            CompositionLocalProvider(LocalFoldsStateIntoName provides true, content = content)
        }
    }
}

/** Matches a node whose last text is [last]. */
private fun hasLastText(last: String): SemanticsMatcher =
    SemanticsMatcher("last text is $last") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.lastOrNull() == AnnotatedString(last)
    }

@OptIn(ExperimentalTestApi::class)
class ControlsWebNameTest {
    @Test
    fun disabledActions_flagOn_everySkin_carryTheDisabledNote() =
        forEachSkin { _, skin ->
            showFolded(skin) {
                Column {
                    BuilderButton({}, "Export", Modifier.testTag("export"), enabled = false)
                    BuilderButton({}, "Share", Modifier.testTag("share"))
                    BuilderIconButton({}, IconId.Undo, "Undo", Modifier.testTag("undo"), enabled = false)
                    BuilderIconButton({}, IconId.Redo, "Redo", Modifier.testTag("redo"))
                    BuilderCard(Modifier.testTag("ocean"), onClick = {}, enabled = false) { BuilderText("Ocean card") }
                    BuilderCard(Modifier.testTag("forest"), onClick = {}) { BuilderText("Forest card") }
                }
            }

            onNodeWithTag("export").assert(hasContentDescriptionExactly("Export, disabled"))
            onNodeWithTag("share").assert(hasNoContentDescription())
            onNodeWithTag("undo").assert(hasContentDescriptionExactly("Undo, disabled"))
            onNodeWithTag("redo").assert(hasContentDescriptionExactly("Redo"))
            onNodeWithTag("ocean").assert(hasText("Ocean card")).assert(hasLastText("disabled"))
            onNodeWithTag("forest").assert(hasLastText("Forest card"))
        }

    @Test
    fun menuRows_flagOn_everySkin_foldDisabledAndSelected() =
        forEachSkin { _, skin ->
            showFolded(skin) {
                BuilderMenu(
                    expanded = true,
                    onDismissRequest = {},
                    items = listOf(
                        BuilderMenuItem("Duplicate", {}),
                        BuilderMenuItem("Archive", {}, enabled = false),
                        BuilderMenuItem("Dark", {}, selected = true),
                        BuilderMenuItem("Light", {}, selected = false),
                    ),
                ) { BuilderText("Project") }
            }
            waitForIdle()

            onNode(hasText("Duplicate") and hasRole(Role.Button)).assert(hasNoContentDescription())
            onNode(hasText("Archive")).assert(hasContentDescriptionExactly("Archive, disabled"))
            onNode(hasText("Dark") and isSelected()).assert(hasContentDescriptionExactly("Dark, selected"))
            onNode(hasText("Light") and isNotSelected()).assert(hasContentDescriptionExactly("Light, not selected"))
        }

    @Test
    fun fields_flagOn_everySkin_areNamedWithTheirDisabledNote() =
        forEachSkin { _, skin ->
            showFolded(skin) {
                Column {
                    BuilderTextField("Sunset", {}, "Theme name", enabled = false)
                    BuilderHexField(Argb(0x6750A4), { _, _ -> }, "Seed", { "Not a color" }, { "Changed" })
                }
            }

            val field = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
            onNode(field and hasContentDescriptionExactly("Theme name, disabled")).assertExists()
            onNode(field and hasContentDescriptionExactly("Seed")).assertExists()
        }

    @Test
    fun segmented_flagOn_everySkin_readsInWritingOrder() =
        forEachSkin { _, skin ->
            showFolded(skin) {
                BuilderSegmented(
                    options = listOf("Light", "Split", "Dark"),
                    selected = "Split",
                    onSelect = {},
                    label = "Preview mode",
                    modifier = Modifier.testTag("mode"),
                ) { it }
            }

            val names = onAllNodes(hasRole(Role.RadioButton) and hasAnyAncestor(hasTestTag("mode")))
                .fetchSemanticsNodes()
                .map { node -> node.config[SemanticsProperties.ContentDescription].single() }
            names shouldContainExactly listOf(
                "Light, radio, not selected",
                "Split, radio, selected",
                "Dark, radio, not selected",
            )
        }

    @Test
    fun segmentedAndChips_everySkin_selectOnFocusOffMovesFocusAndEnterOrSpaceChooses() =
        forEachSkin { _, skin ->
            val picks = mutableListOf<String>()
            setContent {
                ControlsHarness(skin) {
                    Column {
                        var mode by remember { mutableStateOf("Light") }
                        BuilderSegmented(
                            options = listOf("Light", "Split", "Dark"),
                            selected = mode,
                            onSelect = { next ->
                                picks += next
                                mode = next
                            },
                            label = "Preview mode",
                            selectOnFocus = false,
                        ) { it }
                        var style by remember { mutableStateOf("Tonal spot") }
                        BuilderChoiceChips(
                            options = listOf("Tonal spot", "Vibrant"),
                            selected = style,
                            onSelect = { next ->
                                picks += next
                                style = next
                            },
                            label = "Scheme style",
                            selectOnFocus = false,
                        ) { it }
                    }
                }
            }
            val light = onNode(hasText("Light") and hasRole(Role.RadioButton))
            light.requestFocus()
            light.performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            val split = onNode(hasText("Split") and hasRole(Role.RadioButton))
            split.assertIsFocused().assert(isNotSelected())
            picks shouldBe emptyList()
            split.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            split.assertIsFocused().assert(isSelected())

            val tonal = onNode(hasText("Tonal spot") and hasRole(Role.RadioButton))
            tonal.requestFocus()
            tonal.performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            val vibrant = onNode(hasText("Vibrant") and hasRole(Role.RadioButton))
            vibrant.assertIsFocused().assert(isNotSelected())
            vibrant.performKeyInput { pressKey(Key.Spacebar) }
            waitForIdle()
            vibrant.assert(isSelected())
            picks shouldBe listOf("Split", "Vibrant")
        }

    @Test
    fun segmented_everySkin_selectOnFocusOnChoosesWithTheArrow() =
        forEachSkin { _, skin ->
            val picks = mutableListOf<String>()
            setContent {
                ControlsHarness(skin) {
                    var mode by remember { mutableStateOf("Light") }
                    BuilderSegmented(
                        options = listOf("Light", "Split"),
                        selected = mode,
                        onSelect = { next ->
                            picks += next
                            mode = next
                        },
                        label = "Preview mode",
                    ) { it }
                }
            }
            val light = onNode(hasText("Light") and hasRole(Role.RadioButton))
            light.requestFocus()
            light.performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()

            picks shouldBe listOf("Split")
            onNode(hasText("Split") and hasRole(Role.RadioButton)).assertIsFocused().assert(isSelected())
        }

    @Test
    fun scrollArea_flagOn_everySkin_takesFocusOnlyWhenItOverflowsAndScrollsOnKeys() =
        forEachSkin { _, skin ->
            var scroll: ScrollState? = null
            showFolded(skin) {
                Column {
                    val state = rememberScrollState()
                    scroll = state
                    BuilderScrollArea(Modifier.testTag("long").height(120.dp).width(240.dp), state) {
                        repeat(30) { line -> BuilderText("Line $line") }
                    }
                    BuilderScrollArea(Modifier.testTag("short").height(120.dp).width(240.dp)) {
                        BuilderText("One line")
                    }
                }
            }
            waitForIdle()
            val focusable = SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)
            onNodeWithTag("short").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
            val long = onNodeWithTag("long").assert(focusable)
            long.requestFocus()
            long.assertIsFocused()

            val state = checkNotNull(scroll)
            long.performKeyInput { pressKey(Key.DirectionDown) }
            waitForIdle()
            val stepped = state.value
            stepped shouldBeGreaterThan 0
            long.performKeyInput { pressKey(Key.PageDown) }
            waitForIdle()
            val paged = state.value
            paged shouldBeGreaterThan stepped
            long.performKeyInput { pressKey(Key.PageUp) }
            waitForIdle()
            state.value shouldBeLessThan paged
        }

    @Test
    fun scrollArea_flagOff_everySkin_staysOutOfTheTabOrder() =
        forEachSkin { _, skin ->
            setContent {
                ControlsHarness(skin) {
                    BuilderScrollArea(Modifier.testTag("long").height(120.dp).width(240.dp)) {
                        repeat(30) { line -> BuilderText("Line $line") }
                    }
                }
            }

            onNodeWithTag("long").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        }
}
