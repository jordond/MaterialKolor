package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.domain.color.Argb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

// stub
// B-301 reads the media queries, the EyeDropper API, the splash and persistent storage. Until then
// every preference reads false and every request does nothing.
internal class WebEnvironment : Environment {
    override val prefersDark: StateFlow<Boolean> = MutableStateFlow(false)
    override val reducedMotion: StateFlow<Boolean> = MutableStateFlow(false)
    override val coarsePointer: StateFlow<Boolean> = MutableStateFlow(false)
    override val eyeDropperAvailable: Boolean = false
    override val tabId: String = Random.nextLong().toULong().toString(radix = 36)

    // Stores are in memory until B-302, so nothing written survives a reload.
    override val storageAvailable: Boolean = false

    override suspend fun pickScreenColor(): Argb? = null

    override fun hideSplash() = Unit

    override fun setThemeColor(argb: Argb) = Unit

    override fun writeSplashColors(
        light: Argb,
        dark: Argb,
    ) = Unit

    override suspend fun requestPersist(): Boolean = false

    // b-215a
    // B-302 keeps this in sessionStorage. Until then a reload forgets the tab's project.
    private var tabProject: String? = null

    override fun readTabProject(): String? = tabProject

    override fun writeTabProject(id: String?) {
        tabProject = id
    }
}
