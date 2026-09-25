package com.materialkolor.builder.feature.command

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-315
@OptIn(ExperimentalTestApi::class)
class KeymapTest {
    @Test
    fun isApple_readsTheBrowserString() {
        val table = mapOf(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Safari/605.1.15" to true,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15" to true,
            "Mozilla/5.0 (iPad; CPU OS 17_4 like Mac OS X) AppleWebKit/605.1.15" to true,
            "Java 21 (OpenJDK 64-Bit Server VM), Mac OS X 15.6 aarch64" to true,
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/128.0 Safari/537.36" to false,
            "Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0" to false,
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/128.0 Mobile" to false,
            "Java 21 (OpenJDK 64-Bit Server VM), Windows 11 10.0 amd64" to false,
            "FakeBrowser/1.0 (Test OS)" to false,
        )

        table.forEach { (browser, apple) -> (browser to isApple(browser)) shouldBe (browser to apple) }
    }

    @Test
    fun chordText_writesCmdOnAppleAndCtrlElsewhere() {
        Shortcut.Palette.text(apple = true) shouldBe "Cmd+K"
        Shortcut.Palette.text(apple = false) shouldBe "Ctrl+K"
        Shortcut.Projects.text(apple = false) shouldBe "P, Ctrl+O"
        Shortcut.StyleLock.text(apple = false) shouldBe "Shift+L"
        Shortcut.CheatSheet.text(apple = false) shouldBe "?"
    }

    @Test
    fun match_readsRealKeyPresses() =
        runDesktopComposeUiTest {
            fun matched(
                apple: Boolean = false,
                singleKeys: Boolean = true,
                press: KeyInjectionScope.() -> Unit,
            ): Shortcut? =
                captured(press)
                    .firstNotNullOfOrNull { event -> Shortcut.match(event, apple, singleKeys) }
                    ?.first

            matched { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } } shouldBe Shortcut.Palette
            matched(apple = true) { withKeyDown(Key.MetaLeft) { pressKey(Key.K) } } shouldBe Shortcut.Palette
            matched(apple = true) { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } } shouldBe null
            matched { pressKey(Key.L) } shouldBe Shortcut.HueLock
            matched { withKeyDown(Key.ShiftLeft) { pressKey(Key.L) } } shouldBe Shortcut.StyleLock
            matched { withKeyDown(Key.ShiftLeft) { pressKey(Key.Slash) } } shouldBe Shortcut.CheatSheet
            matched { pressKey(Key.LeftBracket) } shouldBe Shortcut.PreviousTab
            matched { withKeyDown(Key.CtrlLeft) { pressKey(Key.Y) } } shouldBe Shortcut.Redo
            val ctrlShiftZ = matched { withKeyDown(Key.CtrlLeft) { withKeyDown(Key.ShiftLeft) { pressKey(Key.Z) } } }
            ctrlShiftZ shouldBe Shortcut.Redo
            matched { withKeyDown(Key.CtrlLeft) { pressKey(Key.L) } } shouldBe null
            matched(singleKeys = false) { pressKey(Key.Spacebar) } shouldBe null
            matched(singleKeys = false) { withKeyDown(Key.CtrlLeft) { pressKey(Key.S) } } shouldBe Shortcut.Save
        }

    @Test
    fun clipboardChords_areNeverShortcuts() =
        runDesktopComposeUiTest {
            listOf(Key.V, Key.C, Key.X, Key.A).forEach { key ->
                val events = captured { withKeyDown(Key.CtrlLeft) { pressKey(key) } }
                val pressed = events.first { event -> event.type == KeyEventType.KeyDown && event.key == key }

                clipboardChord(pressed, apple = false) shouldBe true
                Shortcut.match(pressed, apple = false) shouldBe null
            }
        }

    private val events = mutableListOf<KeyEvent>()
    private var shown = false

    /**
     * The key downs [press] sends to a focused box, as the page would see them.
     */
    private fun ComposeUiTest.captured(press: KeyInjectionScope.() -> Unit): List<KeyEvent> {
        events.clear()
        if (!shown) showBox()
        onRoot().performKeyInput(press)
        waitForIdle()
        return events.toList()
    }

    private fun ComposeUiTest.showBox() {
        shown = true
        val requester = FocusRequester()
        setContent {
            Box(
                Modifier
                    .size(10.dp)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) events += event
                        false
                    }.focusRequester(requester)
                    .focusable(),
            )
        }
        runOnUiThread { requester.requestFocus() }
        waitForIdle()
    }
}
