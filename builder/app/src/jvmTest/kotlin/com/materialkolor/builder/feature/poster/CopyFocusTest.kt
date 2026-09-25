package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

private val Plain = ThemeDocument(seed = Argb(0x6750A4))

// b-306c

/**
 * The hero's copy buttons count as on screen only while the hero shows them, so the manual copy
 * dialog a refused copy opens hands focus back to nothing once they have gone (AR-09).
 */
@OptIn(ExperimentalTestApi::class)
class CopyFocusTest {
    @Test
    fun copyKotlin_heroGoneWhileTheDialogIsOpen_handsFocusToNothing() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            val focus = PosterFocus()
            var heroShown by mutableStateOf(true)
            showSection(harness) { context, dispatcher ->
                if (heroShown) SeedHero(context, dispatcher, focus = focus)
            }

            onNodeWithContentDescription("Copy Kotlin").performClick()
            waitForIdle()
            val opener = (harness.actions.last() as WorkspaceAction.CopyText).returnFocusTo
            focus.returnFocusFor(opener) shouldBeSameInstanceAs focus.copyKotlin.requester

            heroShown = false
            waitForIdle()

            focus.returnFocusFor(opener).shouldBeNull()
        }

    @Test
    fun copyHex_heroOnScreen_handsFocusBackToIt() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            val focus = PosterFocus()
            showSection(harness) { context, dispatcher -> SeedHero(context, dispatcher, focus = focus) }

            onNodeWithContentDescription("Copy hex").performClick()
            waitForIdle()

            val opener = (harness.actions.last() as WorkspaceAction.CopyText).returnFocusTo
            focus.returnFocusFor(opener) shouldBeSameInstanceAs focus.copyHex.requester
        }

    @Test
    fun returnFocusFor_anyOtherOpener_comesBackAsItIs() {
        val focus = PosterFocus()
        val tile = FocusRequester()

        focus.returnFocusFor(tile) shouldBeSameInstanceAs tile
        focus.returnFocusFor(null) shouldBe null
    }
}
