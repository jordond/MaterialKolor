package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.audit.AuditReason
import com.materialkolor.builder.engine.audit.AuditSuggestion
import com.materialkolor.builder.engine.resolve.ThemeResult
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ContrastTabTest {
    private val material = ThemeDocument.Default.copy(library = Library.Material3, expressive = false)

    /** Primary and onPrimary pinned a shade apart in light mode, a pair that fails outright. */
    private val failing = material.copy(
        pins = mapOf(
            Role.Primary to RolePin(light = Argb(0x777777)),
            Role.OnPrimary to RolePin(light = Argb(0x787878)),
        ),
    )

    @Test
    fun material_split_showsEveryAuditRow() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            showContrast(result, PreviewMode.Split)

            onAllNodesWithTag(CONTRAST_ROW_TAG).assertCountEquals(result.audit.rows(PreviewMode.Split).size)
        }

    @Test
    fun failuresOnly_hidesThePassingRows() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(failing)
            val failures = result.audit.rows(PreviewMode.Split).filterNot { row -> row.passes }
            failures.shouldNotBeEmpty()
            showContrast(result, PreviewMode.Split)

            onNodeWithText("Failures only").performClick()
            waitForIdle()

            onAllNodesWithTag(CONTRAST_ROW_TAG).assertCountEquals(failures.size)
            onNodeWithText(ALL_PASS).assertDoesNotExist()
        }

    @Test
    fun allPass_saysSoAboveTheList_andAloneUnderTheFilter() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(contrast = ContrastLevel.High))
            result.audit.rows(PreviewMode.Split).all { row -> row.passes } shouldBe true
            showContrast(result, PreviewMode.Split)

            onNodeWithText(ALL_PASS).assertExists()
            onAllNodesWithTag(CONTRAST_ROW_TAG).assertCountEquals(result.audit.rows(PreviewMode.Split).size)

            onNodeWithText("Failures only").performClick()
            waitForIdle()

            onNodeWithText(ALL_PASS).assertExists()
            onAllNodesWithTag(CONTRAST_ROW_TAG).assertCountEquals(0)
        }

    @Test
    fun failingRow_saysWhyAndWhatToTry() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(failing)
            showContrast(result, PreviewMode.Light)

            val row = hasTestTag(CONTRAST_ROW_TAG) and
                hasContentDescription("onPrimary on primary", substring = true) and
                hasContentDescription("Under 3 to 1, so text is hard to read at any size.", substring = true) and
                hasContentDescription("Try another pinned color, or unpin the role.", substring = true)
            onAllNodes(row).assertCountEquals(1)
            onNodeWithText(ALL_PASS).assertDoesNotExist()
        }

    @Test
    fun fluent_showsTwoPairsPerMode() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(library = Library.Fluent))
            showContrast(result, PreviewMode.Split)

            onAllNodesWithTag(CONTRAST_ROW_TAG).assertCountEquals(4)
            onAllNodes(hasContentDescription("onAccentPrimary on primary", substring = true)).assertCountEquals(2)
            onAllNodes(hasContentDescription("onAccentSecondary on primary", substring = true)).assertCountEquals(2)
        }

    @Test
    fun reasonsAndSuggestions_areTheStringsNamedByTheirKeys() {
        for (reason in AuditReason.entries) reason.text.key shouldBe reason.key
        for (suggestion in AuditSuggestion.entries) suggestion.text.key shouldBe suggestion.key
    }

    private fun ComposeUiTest.showContrast(
        result: ThemeResult,
        mode: PreviewMode,
    ) {
        setContent { DataTabTheme(result) { ContrastTab(result, mode, filter = null) } }
        waitForIdle()
    }

    private companion object {
        const val ALL_PASS = "Every pair passes AA at this contrast level"
    }
}
