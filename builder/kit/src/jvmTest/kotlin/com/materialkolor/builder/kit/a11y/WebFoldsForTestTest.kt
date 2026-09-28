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
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.ControlsHarness
import com.materialkolor.builder.kit.control.forEachSkin
import com.materialkolor.builder.kit.control.hasContentDescriptionExactly
import com.materialkolor.builder.kit.icon.IconId
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, KitTestApi::class)
class WebFoldsForTestTest {
    @Test
    fun provideWebFoldsForTest_everySkin_foldsOnlyWhatItHoldsAndLeavesTheKeyboardAlone() =
        forEachSkin { _, skin ->
            var foldsInside: Boolean? = null
            var foldsOutside: Boolean? = null
            var keyboardInside: Boolean? = null
            setContent {
                ControlsHarness(skin) {
                    Column {
                        ProvideWebFoldsForTest {
                            foldsInside = foldsValueIntoName
                            keyboardInside = LocalWebKeyboard.current
                            BuilderIconButton({}, IconId.Undo, "Undo", Modifier.testTag("inside"), enabled = false)
                        }
                        foldsOutside = foldsValueIntoName
                        BuilderIconButton({}, IconId.Redo, "Redo", Modifier.testTag("outside"), enabled = false)
                    }
                }
            }

            onNodeWithTag("inside").assert(hasContentDescriptionExactly("Undo, disabled"))
            onNodeWithTag("outside").assert(hasContentDescriptionExactly("Redo"))
            foldsInside shouldBe true
            foldsOutside shouldBe false
            keyboardInside shouldBe false
        }

    @Test
    fun provideWebKeyboardForTest_everySkin_turnsOnOnlyTheKeyboardForWhatItHolds() =
        forEachSkin { _, skin ->
            var keyboardInside: Boolean? = null
            var keyboardOutside: Boolean? = null
            var foldsInside: Boolean? = null
            setContent {
                ControlsHarness(skin) {
                    Column {
                        ProvideWebKeyboardForTest {
                            keyboardInside = LocalWebKeyboard.current
                            foldsInside = foldsValueIntoName
                            OverflowingArea("inside")
                        }
                        keyboardOutside = LocalWebKeyboard.current
                        OverflowingArea("outside")
                    }
                }
            }

            keyboardInside shouldBe true
            keyboardOutside shouldBe false
            foldsInside shouldBe false
            onNodeWithTag("inside").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
            onNodeWithTag("outside").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
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
