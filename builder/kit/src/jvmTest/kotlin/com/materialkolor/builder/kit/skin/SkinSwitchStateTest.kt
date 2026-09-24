package com.materialkolor.builder.kit.skin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.HostOverlays
import com.materialkolor.builder.kit.control.hasOverlayPaneTitle
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SkinSwitchStateTest {
    @Test
    fun skinSwitch_eachWay_keepsARememberedCounterAndAnOpenDialog() {
        for (inTree in listOf(false, true)) {
            withClue(if (inTree) "in tree" else "in windows") {
                runComposeUiTest {
                    var skin by mutableStateOf(Skin(Library.Material3, expressive = false))
                    var counter: MutableIntState? = null
                    var open: MutableState<Boolean>? = null
                    setContent {
                        HostOverlays(skin, inTree) {
                            val remembered = remember { mutableIntStateOf(0) }
                            val dialog = remember { mutableStateOf(false) }
                            counter = remembered
                            open = dialog
                            BuilderDialog(dialog.value, { dialog.value = false }, "Rename") { BuilderText("Sunset") }
                        }
                    }
                    waitForIdle()
                    checkNotNull(counter).intValue = 2
                    checkNotNull(open).value = true
                    waitForIdle()
                    onNode(hasOverlayPaneTitle("Rename")).assertExists()
                    for (library in listOf(Library.Fluent, Library.Custom)) {
                        withClue(library.name) {
                            skin = Skin(library, expressive = false)
                            waitForIdle()
                            checkNotNull(counter).intValue shouldBe 2
                            checkNotNull(open).value shouldBe true
                            onNode(hasOverlayPaneTitle("Rename")).assertExists()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun materialTooltipOpenAcrossASkinSwap_inWindows_closesWithTheSwap() =
        runComposeUiTest {
            var skin by mutableStateOf(Skin(Library.Custom, expressive = false))
            setContent {
                HostOverlays(skin, inTree = false) {
                    Column {
                        Box(Modifier.testTag("library").size(40.dp).focusable())
                        BuilderTooltip("Undo last edit") {
                            Box(
                                Modifier
                                    .testTag("undo")
                                    .size(40.dp)
                                    .clickable { skin = Skin(Library.Custom, expressive = false) },
                            )
                        }
                    }
                }
            }
            skin = Skin(Library.Material3, expressive = false)
            waitForIdle()
            onNodeWithTag("library").requestFocus()
            onNodeWithTag("library").performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            onAllNodes(isPopup()).assertCountEquals(1)
            onNodeWithTag("undo").performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            skin.library shouldBe Library.Custom
            onAllNodes(isPopup()).assertCountEquals(0)
        }
}
