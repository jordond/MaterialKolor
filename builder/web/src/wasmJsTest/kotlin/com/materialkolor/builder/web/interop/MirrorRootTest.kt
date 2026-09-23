@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.OPEN
import org.w3c.dom.ShadowRootInit
import org.w3c.dom.ShadowRootMode
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MirrorRootTest {
    @Test
    fun aRootAlreadyThereIsScrubbed() {
        val viewport = attached()
        val root = viewport.child().asMirrorRoot()

        MirrorRoot.install(viewport)

        root.assertScrubbed()
        viewport.remove()
    }

    @Test
    fun aRootInsideTheViewportShadowRootIsScrubbed() {
        val viewport = attached()
        val shadow = viewport.child().attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
        val root = (document.createElement("div") as HTMLElement).asMirrorRoot()
        shadow.appendChild(root)

        MirrorRoot.install(viewport)

        root.assertScrubbed()
        viewport.remove()
    }

    @Test
    fun aRootThatArrivesLaterIsScrubbed(): Promise<JsAny?> {
        val viewport = attached()
        MirrorRoot.install(viewport)
        val root = viewport.child().asMirrorRoot()

        return nextFrame().then { _ ->
            root.assertScrubbed()
            viewport.remove()
            null
        }
    }

    /** The way CMP does it, a host arrives with the root already inside its open shadow root. */
    @Test
    fun aRootInsideAHostThatArrivesLaterIsScrubbed(): Promise<JsAny?> {
        val viewport = attached()
        MirrorRoot.install(viewport)
        val root = viewport.hostWithMirrorRoot()

        return nextFrame().then { _ ->
            root.assertScrubbed()
            viewport.remove()
            null
        }
    }

    @Test
    fun aHostWithoutARootEndsTheWatch(): Promise<JsAny?> {
        val viewport = attached()
        MirrorRoot.install(viewport)
        viewport.child()

        lateinit var root: HTMLElement
        val late: Promise<JsAny?> = nextFrame().then { _ ->
            root = viewport.hostWithMirrorRoot()
            nextFrame()
        }
        return late.then { _ ->
            assertEquals("polite", root.getAttribute("aria-live"))
            viewport.remove()
            null
        }
    }

    /** Appends a host to this element and fills its open shadow root with a mirror root, in one go. */
    private fun HTMLElement.hostWithMirrorRoot(): HTMLElement {
        val shadow = child().attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
        val root = (document.createElement("div") as HTMLElement).asMirrorRoot()
        shadow.appendChild(root)
        return root
    }

    /**
     * Dresses this element the way CMP 1.12.1 dresses `#cmp_a11y_root`, plus the two live region
     * attributes it leaves to their defaults.
     */
    private fun HTMLElement.asMirrorRoot(): HTMLElement {
        id = MirrorRoot.ELEMENT_ID
        setAttribute("role", "presentation")
        setAttribute("aria-live", "polite")
        setAttribute("aria-relevant", "additions text")
        setAttribute("aria-atomic", "false")
        return this
    }

    private fun HTMLElement.assertScrubbed() {
        assertNull(getAttribute("aria-live"))
        assertNull(getAttribute("aria-relevant"))
        assertNull(getAttribute("aria-atomic"))
        assertEquals("presentation", getAttribute("role"))
    }
}
