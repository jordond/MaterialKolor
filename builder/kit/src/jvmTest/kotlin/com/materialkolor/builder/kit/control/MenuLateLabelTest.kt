package com.materialkolor.builder.kit.control

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import kotlin.test.Test

// b-315e

/**
 * An open menu's rows follow their labels in every skin with overlays in the page, a label that
 * lands as the host first draws the menu included. On the web a string still loading lands that
 * way, and the More options menu kept its appearance rows blank and nameless.
 */
@OptIn(ExperimentalTestApi::class)
class MenuLateLabelTest {
    @Test
    fun menu_inTree_rowTakesALabelThatLandsAsTheMenuFirstDraws() =
        forEachSkin { _, skin ->
            var label by mutableStateOf("")
            var open by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalFoldsStateIntoName provides true) {
                    HostOverlays(skin, inTree = true) {
                        val items = listOf(
                            BuilderMenuItem(label, {}, selected = true),
                            BuilderMenuItem("Help", {}),
                        )
                        BuilderMenu(open, { open = false }, items) { BuilderText("More options") }
                    }
                }
            }
            waitForIdle()
            mainClock.autoAdvance = false
            open = true
            // The menu composes in this frame, and the host draws it in the next, as the label lands.
            mainClock.advanceTimeByFrame()
            label = "Use the system appearance"
            mainClock.autoAdvance = true
            waitForIdle()
            onNode(hasText("Help")).assertExists()
            onNode(hasText("Use the system appearance")).assertExists()
            onNode(hasContentDescription("Use the system appearance, menu item, checked")).assertExists()
        }
}
