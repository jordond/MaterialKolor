@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.browser.document
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class A11yLiveRegionTest {
    @Test
    fun theRegionIsCreatedOnceAsAPoliteStatusUnderTheBody() {
        val first = A11yLiveRegion.install()
        val second = A11yLiveRegion.install()

        assertTrue(first == second)
        assertEquals(1, document.querySelectorAll("#${A11yLiveRegion.ELEMENT_ID}").length)
        assertTrue(first.parentElement == document.body)
        assertEquals("status", first.getAttribute("role"))
        assertEquals("polite", first.getAttribute("aria-live"))
    }

    @Test
    fun aMessageLandsInTheRegionOnTheNextFrame(): Promise<JsAny?> {
        val region = A11yLiveRegion.install()

        A11yLiveRegion.announce("Copied #6750A4")

        return nextFrame().then { _ ->
            assertEquals("Copied #6750A4", region.textContent)
            null
        }
    }

    @Test
    fun theSameMessageTwiceIsAnnouncedAgain(): Promise<JsAny?> {
        val region = A11yLiveRegion.install()
        A11yLiveRegion.announce("Copied #6750A4")

        return nextFrame()
            .then { _ ->
                A11yLiveRegion.announce("Copied #6750A4")
                // Cleared first, so the message coming back is a change a screen reader hears.
                assertEquals("", region.textContent)
                nextFrame()
            }.then { _ ->
                assertEquals("Copied #6750A4", region.textContent)
                null
            }
    }
}
