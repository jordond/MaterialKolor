@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.FocusEvent
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Puts DOM focus back on the Compose canvas when a text field hands it to nobody.
 *
 * While a Compose text field is focused, CMP 1.12.1 keeps DOM focus on a hidden `input` or
 * `textarea` with the `compose-backing-field` class, next to the canvas. When Tab moves Compose
 * focus on, CMP removes that element and DOM focus falls to `<body>`, so the next Enter or Space
 * never reaches Compose. This listens for the hidden element losing focus to nothing and focuses
 * the canvas again, without scrolling.
 *
 * Focus that goes to another element on the page is left alone, and so is the window losing focus,
 * since the document still counts the field as focused then and it comes back with the window.
 */
internal object FocusRepair {
    /** The class CMP puts on its hidden text input. */
    const val BACKING_FIELD_CLASS = "compose-backing-field"

    /** Watches [viewport] for its hidden text input losing focus to nothing. */
    fun install(viewport: Element) {
        viewport.addEventListener("focusout", ::onFocusOut)
    }

    private fun onFocusOut(event: Event) {
        if (event !is FocusEvent || event.relatedTarget != null) return
        val field = event.originalTarget() as? HTMLElement ?: return
        if (!field.classList.contains(BACKING_FIELD_CLASS)) return
        val active = document.activeElement
        if (active != null && active != document.body) return
        val canvas = field.parentElement?.querySelector("canvas") as? HTMLCanvasElement ?: return
        focusWithoutScroll(canvas)
    }

    /**
     * The element the event started on. The field sits in the viewport's shadow root, so the
     * target a listener outside it sees is the shadow host.
     */
    private fun Event.originalTarget(): Element? = composedPath()[0] as? Element
}

@JsFun("(element) => element.focus({ preventScroll: true })")
private external fun focusWithoutScroll(element: HTMLElement)
