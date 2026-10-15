@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.MutationRecord
import org.w3c.dom.Node
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.dom.events.FocusEvent
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Puts DOM focus back on the Compose canvas when a text field hands it to nobody.
 *
 * While a Compose text field is focused, CMP 1.12.1 keeps DOM focus on a hidden `input` or
 * `textarea` with the `compose-backing-field` class, next to the canvas. When Tab moves Compose
 * focus on, CMP removes that element and DOM focus falls to `<body>`, so the next Enter or Space
 * never reaches Compose. This focuses the canvas again, without scrolling.
 *
 * Two things notice the removal. Chromium fires `focusout` on the field as it goes, and WebKit and
 * Gecko fire nothing, so an observer on the field's container catches it there. The observer starts
 * on the first focus that comes into the canvas or a field from outside the viewport. A move from
 * the canvas to a field stays inside the viewport's shadow root and never reaches a listener on the
 * viewport, so waiting for the field alone would miss every field opened from the canvas, the
 * palette's search field after Cmd or Ctrl+K among them. Both look at the
 * focus again once the current task is done, so a Tab from one text field straight to the next
 * leaves focus on the new field instead of passing it through the canvas. Whichever of the two runs
 * second finds focus already on the canvas and leaves it be.
 *
 * Focus that goes to another element on the page is left alone, and so is the window losing focus,
 * since the document still counts the field as focused then and it comes back with the window.
 */
internal object FocusRepair {
    /**
     * The class CMP puts on its hidden text input.
     */
    const val BACKING_FIELD_CLASS = "compose-backing-field"

    /**
     * Watches [viewport] for its hidden text input going away and leaving focus with nobody.
     *
     * [defer] runs an action once the current task is done, and [hasDocumentFocus] says whether the
     * page holds focus. Tests pass their own to drive the repair without a real browser window.
     */
    fun install(
        viewport: Element,
        defer: (action: () -> Unit) -> Unit = ::runAsMicrotask,
        hasDocumentFocus: () -> Boolean = { document.hasFocus() },
    ) {
        val watcher = Watcher(defer, hasDocumentFocus)
        val removals = MutationObserver { records, _ -> watcher.onChildrenChanged(records) }
        viewport.addEventListener("focusin") { event -> watcher.watchContainer(event, removals) }
        viewport.addEventListener("focusout") { event -> watcher.onFocusOut(event) }
    }

    private class Watcher(
        private val defer: (action: () -> Unit) -> Unit,
        private val hasDocumentFocus: () -> Boolean,
    ) {
        /**
         * Starts watching the children of the element holding the canvas and its backing fields,
         * once either takes focus. CMP builds its DOM once Skiko is ready, so a focused canvas or
         * field is the first sure sign of where that element is. Observing the same element again
         * only replaces its options.
         */
        fun watchContainer(
            event: Event,
            removals: MutationObserver,
        ) {
            val container = (event.backingField() ?: event.canvasOrigin())?.parentElement ?: return
            removals.observe(container, childChanges())
        }

        /**
         * The field is still in place here, so the canvas beside it is found now and focused later.
         */
        fun onFocusOut(event: Event) {
            if (event !is FocusEvent || event.relatedTarget != null) return
            val canvas = event.backingField()?.parentElement?.canvas() ?: return
            defer { refocusIfLost(canvas) }
        }

        /**
         * Observer callbacks already run once the task that changed the container is done. Asking
         * for document focus keeps a removal while the user is in another window from pulling focus
         * here.
         */
        fun onChildrenChanged(records: JsArray<MutationRecord>) {
            if (!hasDocumentFocus()) return
            val removal = (0 until records.length)
                .mapNotNull { index -> records[index] }
                .firstOrNull { record -> record.removedNodes.asList().any { node -> node.isBackingField() } }
            val canvas = (removal?.target as? Element)?.canvas() ?: return
            refocusIfLost(canvas)
        }

        private fun refocusIfLost(canvas: HTMLCanvasElement) {
            val active = document.activeElement
            if (active != null && active != document.body) return
            focusWithoutScroll(canvas)
        }
    }

    /**
     * The backing field the event started on, if it started on one. The field sits in the
     * viewport's shadow root, so the target a listener outside it sees is the shadow host.
     */
    private fun Event.backingField(): Element? {
        val origin = composedPath()[0] as? Element ?: return null
        return origin.takeIf { element -> element.isBackingField() }
    }

    /**
     * The canvas the event started on, if it started on one.
     */
    private fun Event.canvasOrigin(): Element? = composedPath()[0] as? HTMLCanvasElement

    private fun Node.isBackingField(): Boolean = (this as? Element)?.classList?.contains(BACKING_FIELD_CLASS) == true

    private fun Element.canvas(): HTMLCanvasElement? = querySelector("canvas") as? HTMLCanvasElement
}

private fun focusWithoutScroll(element: HTMLElement): Unit = js("element.focus({ preventScroll: true })")

private fun runAsMicrotask(action: () -> Unit): Unit = js("queueMicrotask(action)")

/**
 * Options for watching an element's own children. The kotlinx-browser factory passes `null` for
 * the options it leaves out, and Chrome rejects a null `attributeFilter`.
 */
private fun childChanges(): MutationObserverInit = js("({ childList: true })")
