package com.materialkolor.builder.kit.a11y

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.ControlsHarness
import com.materialkolor.builder.kit.control.forEachSkin
import com.materialkolor.builder.kit.control.hasContentDescriptionExactly
import com.materialkolor.builder.kit.icon.IconId
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
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
}
