package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.persist.StorageKeys
import com.materialkolor.builder.web.interop.fadeOutSplash
import com.materialkolor.builder.web.interop.localStorageWorks
import com.materialkolor.builder.web.interop.localStorageWrite
import com.materialkolor.builder.web.interop.mediaQueryState
import com.materialkolor.builder.web.interop.onPageHide
import com.materialkolor.builder.web.interop.pageHasEyeDropper
import com.materialkolor.builder.web.interop.requestPersistentStorage
import com.materialkolor.builder.web.interop.sessionStorageRead
import com.materialkolor.builder.web.interop.sessionStorageWrite
import com.materialkolor.builder.web.interop.writeThemeColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // B-302 opens the EyeDropper.
    override suspend fun pickScreenColor(): Argb? = null

    override fun hideSplash() = fadeOutSplash(SPLASH_FADE_MILLIS)

    override fun setThemeColor(argb: Argb) = writeThemeColor(argb.toHex())

    /**
     * Keep [light] and [dark] under `mk:splash` for `boot.js`, which paints them before any code
     * loads.
     *
     * The text is plain JSON, `{"light":-16777216,"dark":-1}` for black and white. Each value is the
     * color as a signed 32 bit ARGB integer, so an opaque color is negative, and `value & 0xFFFFFF`
     * is its RGB. A full or blocked storage just means the next boot shows the default splash.
     */
    override fun writeSplashColors(
        light: Argb,
        dark: Argb,
    ) {
        localStorageWrite(StorageKeys.SPLASH, """{"light":${light.value},"dark":${dark.value}}""")
    }

    override suspend fun requestPersist(): Boolean = requestPersistentStorage()

    // b-215a
    override fun readTabProject(): String? = sessionStorageRead(TAB_PROJECT_KEY)

    override fun writeTabProject(id: String?) = sessionStorageWrite(TAB_PROJECT_KEY, id)
}

private const val SPLASH_FADE_MILLIS = 200

// sessionStorage, so it outlives a reload and goes with the tab.
private const val TAB_PROJECT_KEY = "mk:tab-project"
