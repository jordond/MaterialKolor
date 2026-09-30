package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.BootSplash
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.SITE_ORIGIN
import com.materialkolor.builder.domain.persist.DeviceWidth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile

/**
 * An [Environment] a test can set, that remembers what the builder asked of it.
 */
internal class FakeEnvironment(
    override val tabId: String = "tab",
    override var eyeDropperAvailable: Boolean = false,
    override var storageAvailable: Boolean = true,
    override val siteOrigin: String = SITE_ORIGIN,
) : Environment {
    override val prefersDark: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reducedMotion: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val coarsePointer: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * The width a new project frames its preview at, a tablet unless a test says otherwise.
     */
    @Volatile
    override var defaultDeviceWidth: DeviceWidth = DeviceWidth.Tablet

    /**
     * What the eye dropper picks next, null for a cancel.
     */
    @Volatile
    var screenColor: Argb? = null

    /**
     * Whether the platform agrees to keep stored data.
     */
    @Volatile
    var persistGranted: Boolean = true

    /**
     * How many times persistent storage was asked for.
     */
    @Volatile
    var persistRequests: Int = 0
        private set

    /**
     * Whether the boot splash was removed.
     */
    @Volatile
    var splashHidden: Boolean = false
        private set

    /**
     * Every browser chrome tint, oldest first.
     */
    val themeColors: List<Argb>
        get() = themeColorsKept.value

    private val themeColorsKept = MutableStateFlow<List<Argb>>(emptyList())

    /**
     * Every splash written, oldest first.
     */
    val splashes: List<BootSplash>
        get() = splashesKept.value

    private val splashesKept = MutableStateFlow<List<BootSplash>>(emptyList())

    /**
     * How many times the eye dropper was opened.
     */
    @Volatile
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
        themeColorsKept.update { kept -> kept + argb }
    }

    override fun writeSplash(splash: BootSplash) {
        splashesKept.update { kept -> kept + splash }
    }

    override suspend fun requestPersist(): Boolean {
        persistRequests++
        return persistGranted
    }

    /**
     * The project this tab has open, as a reload would find it.
     */
    @Volatile
    var tabProject: String? = null

    override fun readTabProject(): String? = tabProject

    override fun writeTabProject(id: String?) {
        tabProject = id
    }

    /**
     * Emit on this to hide the page, as closing the tab would.
     */
    override val pageHides: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)

    /**
     * Everything read out to a screen reader, oldest first.
     */
    val announcements: List<String>
        get() = announcementsKept.value

    private val announcementsKept = MutableStateFlow<List<String>>(emptyList())

    override fun announce(message: String) {
        announcementsKept.update { kept -> kept + message }
    }

    override val browser: String = FAKE_BROWSER

    /**
     * Every path the page was asked to reload at, oldest first.
     */
    val reloads: List<String>
        get() = reloadsKept.value

    private val reloadsKept = MutableStateFlow<List<String>>(emptyList())

    override fun reload(path: String) {
        reloadsKept.update { kept -> kept + path }
    }

    /**
     * Whether a reload loads anything, true as on the web.
     */
    @Volatile
    override var canReload: Boolean = true

    /**
     * Every timing mark left, oldest first.
     */
    val marks: List<String>
        get() = marksKept.value

    private val marksKept = MutableStateFlow<List<String>>(emptyList())

    override fun mark(name: String) {
        marksKept.update { kept -> kept + name }
    }
}

/**
 * What every [FakeEnvironment] says it runs in.
 */
internal const val FAKE_BROWSER: String = "FakeBrowser/1.0 (Test OS)"
