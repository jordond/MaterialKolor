@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import org.w3c.dom.Element
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.MutationRecord
import org.w3c.dom.Node
import org.w3c.dom.asList
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Takes the live region off the root of the semantics mirror Compose keeps for assistive tech.
 *
 * CMP 1.12.1 marks `#cmp_a11y_root` as `aria-live="polite"` and rebuilds the whole mirror on every
 * state change, about 405 nodes for one switch toggle, so a screen reader could read the page again
 * after every click. The builder announces through its own region in [A11yLiveRegion] instead.
 *
 * CMP writes the attribute once, when the window is built, and never again, so one pass is enough
 * and nothing watches the attribute afterwards.
 */
internal object MirrorRoot {
    /**
     * The id CMP gives the mirror's root element.
     */
    const val ELEMENT_ID = "cmp_a11y_root"

    private val liveAttributes = listOf("aria-live", "aria-relevant", "aria-atomic")

    /**
     * Scrubs the mirror root under [viewport] now if it is there, or once the viewport's host shows up.
     *
     * `ComposeViewport` builds its DOM once Skiko is ready, which can be after `main` returns, so a
     * missing root means waiting for the viewport's children to arrive. CMP builds the host, its
     * shadow root and the mirror in one task, so the watch ends after the first batch that adds a
     * child to [viewport], found or not. A root that never comes, with accessibility off or after a
     * CMP bump renames the id, then costs one look instead of one on every later change.
     */
    fun install(viewport: Element) {
        if (scrub(viewport)) return
        MutationObserver { records, observer ->
            if (scrub(viewport) || records.addsChildTo(viewport)) observer.disconnect()
        }.observe(viewport, subtreeChanges())
    }

    /**
     * Removes the live region attributes from the mirror root under [viewport], if it exists yet.
     */
    private fun scrub(viewport: Element): Boolean {
        val root = viewport.findMirrorRoot() ?: return false
        liveAttributes.forEach { name -> root.removeAttribute(name) }
        return true
    }

    /**
     * The mirror root, either in the light DOM or one shadow root down, which is where CMP puts it.
     */
    private fun Element.findMirrorRoot(): Element? =
        querySelector("#$ELEMENT_ID")
            ?: querySelectorAll("*")
                .asList()
                .firstNotNullOfOrNull { node -> (node as? Element)?.shadowRoot?.getElementById(ELEMENT_ID) }

    /**
     * Whether any of these records put an element straight under [parent].
     */
    private fun JsArray<MutationRecord>.addsChildTo(parent: Node): Boolean =
        (0 until length)
            .mapNotNull { index -> get(index) }
            .any { record -> record.target == parent && record.addedNodes.asList().any { node -> node is Element } }
}

/**
 * Options for watching children anywhere under a node. The `MutationObserverInit` factory in
 * kotlinx-browser passes `null` for the options it leaves out, and Chrome rejects a null
 * `attributeFilter`.
 */
@JsFun("() => ({ childList: true, subtree: true })")
private external fun subtreeChanges(): MutationObserverInit
