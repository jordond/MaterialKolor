@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.ShadowRoot
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

/**
 * A fresh element of [tag] under `<body>`, standing in for the `#app` viewport or a page control.
 */
internal fun attached(tag: String = "div"): HTMLElement {
    val element = document.createElement(tag) as HTMLElement
    document.body!!.appendChild(element)
    return element
}

/**
 * A new [tag] element appended to this one.
 */
internal fun HTMLElement.child(tag: String = "div"): HTMLElement {
    val element = document.createElement(tag) as HTMLElement
    appendChild(element)
    return element
}

/**
 * Settles after the next animation frame, once every frame callback queued before it has run.
 */
internal fun nextFrame(): Promise<JsAny?> =
    Promise { resolve, _ ->
        window.requestAnimationFrame { _ -> resolve(null) }
    }

/**
 * Takes DOM focus off whatever holds it, so it sits on `<body>`.
 */
internal fun dropFocus() {
    (document.activeElement as? HTMLElement)?.blur()
}

/**
 * The element holding focus inside this shadow root, which the DOM bindings leave out.
 */
internal val ShadowRoot.focused: Element? get() = activeElementOf(this)

private fun activeElementOf(root: ShadowRoot): Element? = js("root.activeElement")
