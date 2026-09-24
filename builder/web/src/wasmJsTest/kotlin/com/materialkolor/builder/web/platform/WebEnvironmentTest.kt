@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.platform

import com.materialkolor.builder.web.interop.A11yLiveRegion
import com.materialkolor.builder.web.interop.nextFrame
import kotlinx.browser.document
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals

// b-221c
class WebEnvironmentTest {
    @Test
    fun announceFillsThePageLiveRegionAfterAFrame(): Promise<JsAny?> {
        WebEnvironment().announce("Copied Theme.kt")

        return nextFrame().then { _ ->
            val region = document.getElementById(A11yLiveRegion.ELEMENT_ID)
            assertEquals("Copied Theme.kt", region?.textContent)
            null
        }
    }
}
