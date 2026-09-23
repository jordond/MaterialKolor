package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.domain.color.Argb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * An [Environment] a test can set, that remembers what the builder asked of it.
 */
internal class FakeEnvironment(
    override val tabId: String = "tab",
    override var eyeDropperAvailable: Boolean = false,
    override var storageAvailable: Boolean = true,
) : Environment {
    override val prefersDark: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reducedMotion: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val coarsePointer: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** What the eye dropper picks next, null for a cancel. */
    var screenColor: Argb? = null

    /** Whether the platform agrees to keep stored data. */
    var persistGranted: Boolean = true

    /** How many times persistent storage was asked for. */
    var persistRequests: Int = 0
        private set

    /** Whether the boot splash was removed. */
    var splashHidden: Boolean = false
        private set

    /** Every browser chrome tint, oldest first. */
    val themeColors: MutableList<Argb> = mutableListOf()

    /** Every pair of splash colors written, light then dark, oldest first. */
    val splashColors: MutableList<Pair<Argb, Argb>> = mutableListOf()

    override suspend fun pickScreenColor(): Argb? = screenColor

    override fun hideSplash() {
        splashHidden = true
    }

    override fun setThemeColor(argb: Argb) {
        themeColors += argb
    }

    override fun writeSplashColors(
        light: Argb,
        dark: Argb,
    ) {
        splashColors += light to dark
    }

    override suspend fun requestPersist(): Boolean {
        persistRequests++
        return persistGranted
    }

    /** The project this tab has open, as a reload would find it. */
    var tabProject: String? = null

    override fun readTabProject(): String? = tabProject

    override fun writeTabProject(id: String?) {
        tabProject = id
    }

    // b-301
    override val pageHides: Flow<Unit> = emptyFlow()
}
