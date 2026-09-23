@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.OPEN
import org.w3c.dom.ShadowRoot
import org.w3c.dom.ShadowRootInit
import org.w3c.dom.ShadowRootMode
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.FocusEventInit
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Builds the shape CMP 1.12.1 gives the viewport, a shadow root holding the canvas and, while a
 * text field is focused, the hidden backing input beside it.
 */
class FocusRepairTest {
    private lateinit var viewport: HTMLElement
    private lateinit var shadow: ShadowRoot
    private lateinit var canvas: HTMLElement
    private lateinit var field: HTMLElement

    @BeforeTest
    fun buildViewport() {
        dropFocus()
        viewport = attached()
        shadow = viewport.child().attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
        val layer = document.createElement("div") as HTMLElement
        shadow.appendChild(layer)
        canvas = layer.child("canvas")
        canvas.setAttribute("tabindex", "0")
        field = layer.child("input")
        field.classList.add(FocusRepair.BACKING_FIELD_CLASS)
        FocusRepair.install(viewport)
    }

    @AfterTest
    fun removeViewport() {
        viewport.remove()
        dropFocus()
    }

    @Test
    fun losingFocusToNothingRefocusesTheCanvas() {
        field.dispatchFocusOut(relatedTarget = null)

        assertTrue(shadow.focused == canvas)
    }

    @Test
    fun losingFocusToAnotherElementLeavesFocusAlone() {
        val button = attached("button")

        field.dispatchFocusOut(relatedTarget = button)

        assertFalse(shadow.focused == canvas)
        button.remove()
    }

    @Test
    fun focusAlreadyOnAnotherPageElementIsNotStolen() {
        val button = attached("button")
        button.focus()

        field.dispatchFocusOut(relatedTarget = null)

        assertTrue(document.activeElement == button)
        button.remove()
    }

    @Test
    fun anInputThatIsNotTheBackingFieldIsIgnored() {
        field.classList.remove(FocusRepair.BACKING_FIELD_CLASS)

        field.dispatchFocusOut(relatedTarget = null)

        assertFalse(shadow.focused == canvas)
    }

    private fun HTMLElement.dispatchFocusOut(relatedTarget: EventTarget?) {
        val init = FocusEventInit(relatedTarget = relatedTarget, bubbles = true, composed = true)
        dispatchEvent(FocusEvent("focusout", init))
    }
}
