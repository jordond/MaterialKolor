package com.materialkolor.builder.preview.inklet

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.preview.GalleryHarness
import com.materialkolor.builder.preview.GalleryWhole
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.TextToolbarProbe
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.pressEveryControl
import com.materialkolor.builder.preview.rightClickAndLongPressEveryField
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class InkletGalleryTest {
    /**
     * The D40 guard. With the builder's motion frozen the pen's boil clock and the indeterminate
     * progress hold still, so the gallery settles, and no control, right click or long press on a
     * text field opens a popup or a window.
     */
    @Test
    fun gallery_motionFrozen_settlesAndOpensNoPopupOrWindow() =
        runComposeUiTest {
            val composed = mutableSetOf<String>()
            val toolbar = TextToolbarProbe()
            val spec = LightSpec.on(Library.Inklet)
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalTextToolbar provides toolbar) {
                    GalleryHarness(spec, state, GalleryWhole, composed)
                }
            }
            waitForIdle()
            composed shouldBe InkletCards.map { card -> card.title }.toSet()

            pressEveryControl()
            // A word to select, so the field has a context menu and a text toolbar to open.
            rightClickAndLongPressEveryField(onAllNodes(hasSetTextAction() and hasText("Destination")), "Lisbon")
            toolbar.shown shouldBe 0
        }
}
