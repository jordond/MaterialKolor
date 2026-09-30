package com.materialkolor.builder.feature.command

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.WIDTH
import com.materialkolor.builder.feature.export.ExportModel
import com.materialkolor.builder.feature.export.ExportOutcome
import com.materialkolor.builder.feature.workspace.Panel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * C and Shift+C inside the export sheet copy its files, from inside the key press.
 */
@OptIn(ExperimentalTestApi::class)
class ExportSheetKeysTest {
    private val harness = CommandHarness()

    private lateinit var export: ExportModel

    @AfterTest
    fun tearDown() {
        harness.close()
    }

    @Test
    fun c_copiesThePickedFileBeforeTheHandlerReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val ready = openSheet()

            pressInSheet { pressKey(Key.C) }

            harness.platform.clipboard.texts shouldBe listOf(ready.files.first().text)
        }

    @Test
    fun shiftC_copiesEveryFileBeforeTheHandlerReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val ready = openSheet()

            pressInSheet { withKeyDown(Key.ShiftLeft) { pressKey(Key.C) } }

            harness.platform.clipboard.texts shouldBe listOf(ready.allText)
        }

    /**
     * Boots the builder, opens the export sheet and says what it exports.
     */
    private fun ComposeUiTest.openSheet(): ExportOutcome.Ready {
        with(harness) { show(probe = { export = metroViewModel() }) }
        runOnUiThread { harness.workspace.openPanel(Panel.Export) }
        waitUntil(timeoutMillis = WAIT_MILLIS) { named("Copy all") }
        waitForIdle()
        return export.outcome().shouldBeInstanceOf<ExportOutcome.Ready>()
    }

    /**
     * Presses keys on what has focus in the sheet, with no frame or task run after them.
     */
    private fun ComposeUiTest.pressInSheet(block: KeyInjectionScope.() -> Unit) {
        mainClock.autoAdvance = false
        onAllNodes(isFocused()).onLast().performKeyInput(block)
    }
}
