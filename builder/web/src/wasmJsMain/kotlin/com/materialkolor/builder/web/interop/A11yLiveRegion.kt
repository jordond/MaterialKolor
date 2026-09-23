package com.materialkolor.builder.web.interop

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * The page's own polite live region, for messages a screen reader should hear once, like a toast.
 *
 * It sits straight under `<body>`, outside the canvas and outside the Compose semantics mirror, so
 * the mirror rebuilding itself never touches it. It is visually hidden but stays in the
 * accessibility tree.
 */
internal object A11yLiveRegion {
    /** The id of the region element, so a second install finds the first one. */
    const val ELEMENT_ID = "builder_live_region"

    private var pendingFrame: Int? = null

    /** Adds the region to the page if it is not there yet, and returns it. */
    fun install(): HTMLElement {
        val existing = document.getElementById(ELEMENT_ID) as? HTMLElement
        if (existing != null) return existing
        val region = document.createElement("div") as HTMLElement
        region.id = ELEMENT_ID
        region.setAttribute("role", "status")
        region.setAttribute("aria-live", "polite")
        region.style.cssText = VISUALLY_HIDDEN
        document.body?.appendChild(region)
        return region
    }

    /**
     * Reads [message] out through the region.
     *
     * The region is cleared first and the message goes in on the next frame, so the same message
     * twice in a row is still a change and gets announced again.
     */
    fun announce(message: String) {
        val region = install()
        pendingFrame?.let { frame -> window.cancelAnimationFrame(frame) }
        region.textContent = ""
        pendingFrame =
            window.requestAnimationFrame { _ ->
                pendingFrame = null
                region.textContent = message
            }
    }
}

/** Keeps the region out of sight and out of layout without hiding it from assistive tech. */
private const val VISUALLY_HIDDEN =
    "position: absolute; width: 1px; height: 1px; margin: -1px; padding: 0; border: 0; " +
        "overflow: hidden; clip-path: inset(50%); white-space: nowrap;"
