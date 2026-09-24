package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.a11y.LocalWebKeyboard
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

/** Shows [content] in [skin] with the state fold as [folds] and the web's keyboard as [keyboard]. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showOnWeb(
    skin: Skin,
    folds: Boolean,
    keyboard: Boolean,
    content: @Composable () -> Unit,
) {
    setContent {
        ControlsHarness(skin) {
            CompositionLocalProvider(
                LocalFoldsStateIntoName provides folds,
                LocalWebKeyboard provides keyboard,
                content = content,
            )
        }
    }
}

/** A scroll area with more lines than fit, tagged [tag], holding [inside] above the lines. */
@Composable
private fun OverflowingArea(
    tag: String,
    tabStop: Boolean = true,
    inside: @Composable () -> Unit = {},
) {
    BuilderScrollArea(Modifier.testTag(tag).height(120.dp).width(240.dp), tabStop = tabStop) {
        inside()
        repeat(30) { line -> BuilderText("Line $line") }
    }
}

/** Tabs once from the node tagged [from] and lets the focus land. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.tabFrom(from: String) {
    onNodeWithTag(from).requestFocus()
    waitForIdle()
    onNodeWithTag(from).performKeyInput { pressKey(Key.Tab) }
    waitForIdle()
}

private val TakesFocus: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)

private val TakesNoFocus: SemanticsMatcher = SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused)

@OptIn(ExperimentalTestApi::class)
class WebKeyboardTest {
    @Test
    fun scrollArea_foldOnKeyboardOff_everySkin_staysOutOfTheTabOrder() =
        forEachSkin { _, skin ->
            showOnWeb(skin, folds = true, keyboard = false) { OverflowingArea("long") }
            waitForIdle()

            onNodeWithTag("long").assert(TakesNoFocus)
        }

    @Test
    fun scrollArea_keyboardOnFoldOff_everySkin_isATabStopAndNamesStayUnfolded() =
        forEachSkin { _, skin ->
            showOnWeb(skin, folds = false, keyboard = true) {
                Column {
                    BuilderIconButton({}, IconId.Undo, "Undo", Modifier.testTag("undo"), enabled = false)
                    BuilderButton({}, "Before", Modifier.testTag("before"))
                    OverflowingArea("long")
                }
            }
            waitForIdle()

            onNodeWithTag("undo").assert(hasContentDescriptionExactly("Undo"))
            onNodeWithTag("long").assert(TakesFocus)
            tabFrom("before")
            onNodeWithTag("long").assertIsFocused()
        }

    /**
     * The web's keyboard without the fold, as a test outside the kit gets it, still keeps Material's
     * segmented buttons in the order they are written, flat and expressive, and leaves their names as
     * they read off the web.
     */
    @Test
    fun materialSegmented_keyboardOnFoldOff_bothFlavours_keepsWritingOrderAndNamesUnfolded() {
        for (expressive in listOf(false, true)) {
            withClue("expressive $expressive") {
                runComposeUiTest {
                    showOnWeb(Skin(Library.Material3, expressive), folds = false, keyboard = true) {
                        BuilderSegmented(
                            options = listOf("Light", "Split", "Dark"),
                            selected = "Split",
                            onSelect = {},
                            label = "Preview mode",
                            modifier = Modifier.testTag("mode"),
                        ) { option -> option }
                    }
                    waitForIdle()

                    onNodeWithTag("mode").assert(hasContentDescriptionExactly("Preview mode"))
                    val options = onAllNodes(hasRole(Role.RadioButton) and hasAnyAncestor(hasTestTag("mode")))
                        .fetchSemanticsNodes()
                    options.map { node ->
                        node.config[SemanticsProperties.Text].joinToString { text -> text.text }
                    } shouldContainExactly listOf("Light", "Split", "Dark")
                    options
                        .map { node -> node.config.getOrNull(SemanticsProperties.ContentDescription) }
                        .shouldContainExactly(null, null, null)
                }
            }
        }
    }

    @Test
    fun scrollArea_tabStopOff_keyboardOn_everySkin_leavesTabToTheButtonInside() =
        forEachSkin { _, skin ->
            showOnWeb(skin, folds = true, keyboard = true) {
                Column {
                    BuilderButton({}, "Before", Modifier.testTag("before"))
                    OverflowingArea("long", tabStop = false) {
                        BuilderButton({}, "Inside", Modifier.testTag("inside"))
                    }
                }
            }
            waitForIdle()

            onNodeWithTag("long").assert(TakesNoFocus)
            tabFrom("before")
            onNodeWithTag("inside").assertIsFocused()
        }
}
