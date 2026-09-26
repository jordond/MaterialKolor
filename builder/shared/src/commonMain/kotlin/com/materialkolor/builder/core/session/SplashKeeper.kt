package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.platform.BootSplash
import com.materialkolor.builder.core.platform.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps the next boot's splash up to date for [ProjectSession].
 *
 * The session hands it the colors of every document it shows or saves. It also follows the
 * chrome's appearance, so a reload right after the appearance changes paints the new one, with no
 * edit in between.
 *
 * @param[scope] The app scope, where the appearance is followed.
 */
internal class SplashKeeper(
    private val preferences: PreferencesRepository,
    private val environment: Environment,
    scope: CoroutineScope,
) {
    // The splash last written, so a change of appearance can write it again. The UI thread writes it
    // and the app scope reads it, and on the web both are the one thread.
    private var splash: BootSplash? = null

    init {
        scope.launch {
            preferences.preferences.collect { prefs ->
                val last = splash
                if (last != null && last.appearance != prefs.appearance) {
                    write(last.copy(appearance = prefs.appearance))
                }
            }
        }
    }

    /**
     * Keep [colors] for the next boot's splash, with the seed and the chrome's appearance.
     */
    fun write(colors: SessionColors) {
        val appearance = preferences.preferences.value.appearance
        write(BootSplash(colors.splashLight, colors.splashDark, colors.splashSeed, appearance))
    }

    private fun write(next: BootSplash) {
        splash = next
        environment.writeSplash(next)
    }
}
