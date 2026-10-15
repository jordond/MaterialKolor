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
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Builds the shape CMP 1.12.1 gives the viewport, a shadow root holding the canvas and, while a
 * text field is focused, the hidden backing input beside it.
 *
 * The repair's deferred work goes into [deferred] and document focus comes from [pageFocused], so
 * nothing depends on the browser window. Every check runs once the pending observer callbacks have.
 */
class FocusRepairTest {
    private lateinit var viewport: HTMLElement
    private lateinit var shadow: ShadowRoot
    private lateinit var layer: HTMLElement
    private lateinit var canvas: HTMLElement
    private lateinit var field: HTMLElement
    private val deferred = mutableListOf<() -> Unit>()
    private var pageFocused = true

    @BeforeTest
    fun buildViewport() {
        dropFocus()
        viewport = attached()
        shadow = viewport.child().attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
        layer = document.createElement("div") as HTMLElement
        shadow.appendChild(layer)
        canvas = layer.child("canvas")
        canvas.setAttribute("tabindex", "0")
        field = layer.backingField()

        countFocusCalls(canvas)
        FocusRepair.install(
            viewport = viewport,
            defer = { action -> deferred += action },
            hasDocumentFocus = { pageFocused },
        )
    }

    @AfterTest
    fun removeViewport() {
        viewport.remove()
        dropFocus()
    }

    @Test
    fun losingFocusToNothingRefocusesTheCanvas(): Promise<JsAny?> {
        field.dispatchFocusOut(relatedTarget = null)

        return afterRepair { assertTrue(shadow.focused == canvas) }
    }

    @Test
    fun losingFocusToAnotherElementLeavesFocusAlone(): Promise<JsAny?> {
        val button = attached("button")

        field.dispatchFocusOut(relatedTarget = button)

        return afterRepair {
            button.remove()
            assertFalse(shadow.focused == canvas)
            assertEquals(0, focusCallsOf(canvas))
        }
    }

    @Test
    fun focusAlreadyOnAnotherPageElementIsNotStolen(): Promise<JsAny?> {
        val button = attached("button")
        button.focus()

        field.dispatchFocusOut(relatedTarget = null)

        return afterRepair {
            val active = document.activeElement
            button.remove()
            assertTrue(active == button)
        }
    }

    @Test
    fun anInputThatIsNotTheBackingFieldIsIgnored(): Promise<JsAny?> {
        field.classList.remove(FocusRepair.BACKING_FIELD_CLASS)

        field.dispatchFocusOut(relatedTarget = null)

        return afterRepair { assertFalse(shadow.focused == canvas) }
    }

    /**
     * WebKit and Gecko drop focus to `<body>` without a `focusout` when the focused field goes.
     */
    @Test
    fun aRemovalWithoutFocusOutRefocusesTheCanvas(): Promise<JsAny?> {
        field.dispatchFocusIn()

        field.remove()

        return afterRepair {
            assertTrue(shadow.focused == canvas)
            assertEquals(1, focusCallsOf(canvas))
        }
    }

    /**
     * A field opened from the canvas takes focus inside the shadow root, where the viewport never
     * hears it, so focus coming into the canvas is what starts the watch.
     */
    @Test
    fun aRemovalAfterFocusCameIntoTheCanvasRefocusesTheCanvas(): Promise<JsAny?> {
        canvas.dispatchFocusIn()

        field.remove()

        return afterRepair {
            assertTrue(shadow.focused == canvas)
            assertEquals(1, focusCallsOf(canvas))
        }
    }

    /**
     * A Tab from one text field to the next, the way CMP does it within one task.
     */
    @Test
    fun aMoveToAnotherBackingFieldDoesNotPassThroughTheCanvas(): Promise<JsAny?> {
        field.dispatchFocusIn()

        field.dispatchFocusOut(relatedTarget = null)
        field.remove()
        val next = layer.backingField()
        next.focus()

        return afterRepair {
            assertTrue(shadow.focused == next)
            assertEquals(0, focusCallsOf(canvas))
        }
    }

    @Test
    fun aRemovalSeenByBothWatchersRefocusesOnce(): Promise<JsAny?> {
        field.dispatchFocusIn()

        field.dispatchFocusOut(relatedTarget = null)
        field.remove()

        return afterRepair {
            assertTrue(shadow.focused == canvas)
            assertEquals(1, focusCallsOf(canvas))
        }
    }

    @Test
    fun aRemovalWhileThePageIsInAnotherWindowLeavesFocusAlone(): Promise<JsAny?> {
        pageFocused = false
        field.dispatchFocusIn()

        field.remove()

        return afterRepair { assertEquals(0, focusCallsOf(canvas)) }
    }

    /**
     * Runs [check] once the pending observer callbacks and the repair's deferred work have run.
     * Observer callbacks were queued as microtasks before this one, so they come first.
     */
    private fun afterRepair(check: () -> Unit): Promise<JsAny?> =
        Promise<JsAny?> { resolve, _ -> resolve(null) }.then { _ ->
            while (deferred.isNotEmpty()) deferred.removeAt(0).invoke()
            check()
            null
        }

    private fun HTMLElement.backingField(): HTMLElement {
        val input = child("input")
        input.classList.add(FocusRepair.BACKING_FIELD_CLASS)
        return input
    }

    private fun HTMLElement.dispatchFocusIn() {
        dispatchEvent(FocusEvent("focusin", FocusEventInit(bubbles = true, composed = true)))
    }

    private fun HTMLElement.dispatchFocusOut(relatedTarget: EventTarget?) {
        val init = FocusEventInit(relatedTarget = relatedTarget, bubbles = true, composed = true)
        dispatchEvent(FocusEvent("focusout", init))
    }
}

/**
 * Counts the calls to [element]'s `focus`, whether or not the browser fires a focus event for them.
 */
private fun countFocusCalls(element: HTMLElement): Unit =
    js(
        "(() => { element.focusCalls = 0; const focus = element.focus.bind(element); " +
            "element.focus = (options) => { element.focusCalls += 1; focus(options); }; })()",
    )

private fun focusCallsOf(element: HTMLElement): Int = js("element.focusCalls")
