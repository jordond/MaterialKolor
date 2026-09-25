@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.BootSplash
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.TimingMarks
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.SITE_ORIGIN
import com.materialkolor.builder.domain.persist.StorageKeys
import com.materialkolor.builder.web.interop.A11yLiveRegion
import com.materialkolor.builder.web.interop.Analytics
import com.materialkolor.builder.web.interop.fadeOutSplash
import com.materialkolor.builder.web.interop.localStorageWorks
import com.materialkolor.builder.web.interop.localStorageWrite
import com.materialkolor.builder.web.interop.locationOrigin
import com.materialkolor.builder.web.interop.mediaQueryState
import com.materialkolor.builder.web.interop.onPageHide
import com.materialkolor.builder.web.interop.pageHasEyeDropper
import com.materialkolor.builder.web.interop.pickColorOnScreen
import com.materialkolor.builder.web.interop.requestPersistentStorage
import com.materialkolor.builder.web.interop.sessionStorageRead
import com.materialkolor.builder.web.interop.sessionStorageWrite
import com.materialkolor.builder.web.interop.userAgent
import com.materialkolor.builder.web.interop.writeThemeColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.random.Random

/**
 * The page around the builder.
 *
 * The media queries are live, each backed by one `matchMedia` listener for the life of the page.
 */
internal class WebEnvironment : Environment {
    override val prefersDark: StateFlow<Boolean> = mediaQueryState("(prefers-color-scheme: dark)")
    override val reducedMotion: StateFlow<Boolean> = mediaQueryState("(prefers-reduced-motion: reduce)")
    override val coarsePointer: StateFlow<Boolean> = mediaQueryState("(pointer: coarse)")
    override val eyeDropperAvailable: Boolean = pageHasEyeDropper()

    // Fresh on every load and never stored. A duplicated tab copies sessionStorage, so an id kept
    // there would be shared by both tabs and each would take the other's saves for its own.
    override val tabId: String = Random.nextLong().toULong().toString(radix = 36)

    override val storageAvailable: Boolean = localStorageWorks()

    // b-301
    private val hides = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val pageHides: Flow<Unit> = hides.asSharedFlow()

    init {
        onPageHide { hides.tryEmit(Unit) }
        exposeToE2e()
    }

    // b-302
    // The eyedropper only opens inside a click, and it opens before this first suspends, so start
    // this undispatched from the click handler. Esc comes back as null and changes nothing.
    override suspend fun pickScreenColor(): Argb? {
        if (!eyeDropperAvailable) return null
        val hex = pickColorOnScreen() ?: return null
        return runCatching { Argb.fromHex(hex) }.getOrNull()
    }

    override fun hideSplash() = fadeOutSplash(SPLASH_FADE_MILLIS)

    override fun setThemeColor(argb: Argb) = writeThemeColor(argb.toHex())

    // b-501b

    /**
     * Keep [splash] under `mk:splash` as [BootSplash.toJson] for `boot.js`, which paints it before
     * any code loads. A full or blocked storage means the next boot shows the default splash.
     */
    override fun writeSplash(splash: BootSplash) {
        localStorageWrite(StorageKeys.SPLASH, splash.toJson())
    }

    override suspend fun requestPersist(): Boolean = requestPersistentStorage()

    // b-215a
    override fun readTabProject(): String? = sessionStorageRead(TAB_PROJECT_KEY)

    override fun writeTabProject(id: String?) = sessionStorageWrite(TAB_PROJECT_KEY, id)

    // b-221c
    override fun announce(message: String) = A11yLiveRegion.announce(message)

    // b-314
    override val browser: String = userAgent()

    // b-314b
    override fun reload(path: String) = assignLocation(path)

    // b-314ba
    override val canReload: Boolean = true

    // b-505
    // The page's own origin, as the Worker does for link previews, so each deploy links to itself.
    override val siteOrigin: String = locationOrigin() ?: SITE_ORIGIN

    // b-504
    // Analytics waits for the first frame, so only the builder's own files are on the critical
    // path (PB-10).
    override fun mark(name: String) {
        performanceMark(name)
        if (name == TimingMarks.FIRST_FRAME) Analytics.load()
    }
}

// b-504
// Only the newest mark of each name stays in the buffer, so the resolves of a long session do not
// pile up there. A PerformanceObserver still sees every one, which is how the perf run reads them.
private fun performanceMark(name: String): Unit =
    js(
        """{
        performance.clearMarks(name);
        performance.mark(name);
    }""",
    )

// b-314b
// Goes through history like a click on a link would, so back returns to the page it left.
private fun assignLocation(path: String): Unit = js("window.location.assign(path)")

private const val SPLASH_FADE_MILLIS = 200

// sessionStorage, so it outlives a reload and goes with the tab.
private const val TAB_PROJECT_KEY = "mk:tab-project"
