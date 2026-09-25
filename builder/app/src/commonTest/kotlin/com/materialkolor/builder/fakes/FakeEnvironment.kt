package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.BootSplash
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.SITE_ORIGIN
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An [Environment] a test can set, that remembers what the builder asked of it.
 */
internal class FakeEnvironment(
    override val tabId: String = "tab",
    override var eyeDropperAvailable: Boolean = false,
    override var storageAvailable: Boolean = true,
    // b-505
    override val siteOrigin: String = SITE_ORIGIN,
) : Environment {
    override val prefersDark: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reducedMotion: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val coarsePointer: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * What the eye dropper picks next, null for a cancel.
     */
    var screenColor: Argb? = null

    /**
     * Whether the platform agrees to keep stored data.
     */
    var persistGranted: Boolean = true

    /**
     * How many times persistent storage was asked for.
     */
    var persistRequests: Int = 0
        private set

    /**
     * Whether the boot splash was removed.
     */
    var splashHidden: Boolean = false
        private set

    /**
     * Every browser chrome tint, oldest first.
     */
    val themeColors: MutableList<Argb> = mutableListOf()

    // b-501b

    /**
     * Every splash written, oldest first.
     */
    val splashes: MutableList<BootSplash> = mutableListOf()

    // b-307

    /**
     * How many times the eye dropper was opened.
     */
    var screenPicks: Int = 0
        private set

    override suspend fun pickScreenColor(): Argb? {
        screenPicks++
        return screenColor
    }

    override fun hideSplash() {
        splashHidden = true
    }

    override fun setThemeColor(argb: Argb) {
        themeColors += argb
    }

    override fun writeSplash(splash: BootSplash) {
        splashes += splash
    }

    override suspend fun requestPersist(): Boolean {
        persistRequests++
        return persistGranted
    }

    /**
     * The project this tab has open, as a reload would find it.
     */
    var tabProject: String? = null

    override fun readTabProject(): String? = tabProject

    override fun writeTabProject(id: String?) {
        tabProject = id
    }

    // b-301
    // b-221c

    /**
     * Emit on this to hide the page, as closing the tab would.
     */
    override val pageHides: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)

    /**
     * Everything read out to a screen reader, oldest first.
     */
    val announcements: MutableList<String> = mutableListOf()

    override fun announce(message: String) {
        announcements += message
    }

    // b-314
    override val browser: String = FAKE_BROWSER

    // b-314b

    /**
     * Every path the page was asked to reload at, oldest first.
     */
    val reloads: MutableList<String> = mutableListOf()

    override fun reload(path: String) {
        reloads += path
    }

    // b-314ba

    /**
     * Whether a reload loads anything, true as on the web.
     */
    override var canReload: Boolean = true

    // b-504

    /**
     * Every timing mark left, oldest first.
     */
    val marks: MutableList<String> = mutableListOf()

    override fun mark(name: String) {
        marks += name
    }
}

/**
 * What every [FakeEnvironment] says it runs in.
 */
internal const val FAKE_BROWSER: String = "FakeBrowser/1.0 (Test OS)"
