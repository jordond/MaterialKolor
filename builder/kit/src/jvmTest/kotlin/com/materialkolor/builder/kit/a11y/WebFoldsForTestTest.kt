package com.materialkolor.builder.kit.a11y

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.ControlsHarness
import com.materialkolor.builder.kit.control.hasContentDescriptionExactly
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, KitTestApi::class)
class WebFoldsForTestTest {
    @Test
    fun webSeams_eachTurnsOnOnlyItsOwnBehaviourForWhatItHolds() =
        runComposeUiTest {
            setContent {
                ControlsHarness(Skin(SkinLibrary.Material3, expressive = false)) {
                    Column {
                        ProvideWebFoldsForTest {
                            BuilderIconButton({}, IconId.Undo, "Undo", Modifier.testTag("folds"), enabled = false)
                            OverflowingArea("folds-area")
                        }
                        ProvideWebKeyboardForTest {
                            BuilderIconButton({}, IconId.Undo, "Undo", Modifier.testTag("keyboard"), enabled = false)
                            OverflowingArea("keyboard-area")
                        }
                        BuilderIconButton({}, IconId.Redo, "Redo", Modifier.testTag("outside"), enabled = false)
                        OverflowingArea("outside-area")
                    }
                }
            }

            onNodeWithTag("folds").assert(hasContentDescriptionExactly("Undo, disabled"))
            onNodeWithTag("keyboard").assert(hasContentDescriptionExactly("Undo"))
            onNodeWithTag("outside").assert(hasContentDescriptionExactly("Redo"))
            onNodeWithTag("keyboard-area").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
            onNodeWithTag("folds-area").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
            onNodeWithTag("outside-area").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        }
}

/**
 * A scroll area tagged [tag] with more lines than fit, which the web's keyboard makes a Tab stop.
 */
@Composable
private fun OverflowingArea(tag: String) {
    BuilderScrollArea(Modifier.testTag(tag).height(96.dp).width(240.dp)) {
        repeat(30) { line -> BuilderText("Line $line") }
    }
}
