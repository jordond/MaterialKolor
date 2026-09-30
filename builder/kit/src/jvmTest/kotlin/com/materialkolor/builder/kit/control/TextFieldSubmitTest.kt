package com.materialkolor.builder.kit.control

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

private const val Field = "field"
private const val Elsewhere = "elsewhere"

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class TextFieldSubmitTest {
    @Test
    fun submit_firesOnEveryEnter_neverWhileComposing_andNeverOnLeaving() =
        forEverySkin { variant ->
            val commits = mutableListOf<String>()
            val submits = mutableListOf<String>()
            var value by mutableStateOf("")
            var session: PlatformTextInputMethodRequest? = null
            val watcher = PlatformTextInputInterceptor { request, nextHandler ->
                session = request
                nextHandler.startInputMethod(request)
            }
            setSkinnedContent(variant) {
                InterceptPlatformTextInput(watcher) {
                    Column {
                        BuilderTextField(
                            value = value,
                            onCommit = { text ->
                                commits += text
                                value = text
                            },
                            label = "Search",
                            modifier = Modifier.testTag(Field),
                            onSubmit = { text -> submits += text },
                        )
                        Box(Modifier.size(24.dp).testTag(Elsewhere).focusable())
                    }
                }
            }

            onNodeWithTag(Field).requestFocus()
            onNodeWithTag(Field).performTextReplacement("tsp")
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            commits shouldBe listOf("tsp")
            submits shouldBe listOf("tsp")

            // A clean draft still submits.
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            submits shouldBe listOf("tsp", "tsp")

            waitForIdle()
            val request = checkNotNull(session)
            runOnUiThread { request.onEditCommand(listOf(SetComposingTextCommand("ka", 1))) }
            waitForIdle()
            request.value().composition shouldNotBe null
            onNodeWithTag(Field).performKeyInput { pressKey(Key.Enter) }
            submits shouldBe listOf("tsp", "tsp")

            onNodeWithTag(Elsewhere).requestFocus()
            waitForIdle()
            submits shouldBe listOf("tsp", "tsp")
        }
}
