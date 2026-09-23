package com.materialkolor.builder.feature.workspace

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.kit.skin.Skin
import dev.stateholder.extensions.viewmodel.StateViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey

/**
 * What the root needs to dress the builder, the chrome's appearance and its motion, and the boot
 * that opens the first project.
 *
 * The chrome's appearance comes from the preferences and the system, never from the preview mode,
 * and nothing here writes the preview mode (F-04). The skin is not held here. The root derives it
 * from the same collected document it resolves the theme from, so there is one skin source.
 */
@Stable
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class AppModel(
    private val session: ProjectSession,
    private val router: Router,
    preferences: PreferencesRepository,
    environment: Environment,
) : StateViewModel<AppModel.State>(
        State(
            appearance = preferences.preferences.value.appearance,
            motion = preferences.preferences.value.motion,
            systemDark = environment.prefersDark.value,
            systemReducedMotion = environment.reducedMotion.value,
            coarsePointer = environment.coarsePointer.value,
        ),
    ) {
    private var booted = false

    init {
        preferences.preferences.mergeState { state, prefs ->
            state.copy(appearance = prefs.appearance, motion = prefs.motion)
        }
        environment.prefersDark.mergeState { state, dark -> state.copy(systemDark = dark) }
        environment.reducedMotion.mergeState { state, reduced -> state.copy(systemReducedMotion = reduced) }
        environment.coarsePointer.mergeState { state, coarse -> state.copy(coarsePointer = coarse) }
    }

    /**
     * Open the project the address asks for, then put `/` back in the address bar. Only the first
     * call does anything.
     *
     * Call it from the UI dispatcher, never from `runBlocking`, since the session keeps the caller's
     * dispatcher and resolves themes on its thread.
     */
    suspend fun boot() {
        if (booted) return
        booted = true
        val notice = session.boot(router.initial)
        router.replaceHome()
        updateState { state -> state.copy(bootNotice = notice) }
    }

    /** Write whatever is waiting to be saved, when the app goes to the background. */
    fun flush() {
        session.flush()
    }

    /**
     * @property[appearance] What the chrome's appearance is set to.
     * @property[motion] What the chrome's motion is set to.
     * @property[systemDark] Whether the system is in dark mode right now.
     * @property[systemReducedMotion] Whether the system asks for less motion right now.
     * @property[coarsePointer] Whether the main pointer is a finger.
     * @property[bootNotice] Why the address did not open what it asked for, or null.
     */
    @Immutable
    data class State(
        val appearance: Appearance,
        val motion: MotionOverride,
        val systemDark: Boolean,
        val systemReducedMotion: Boolean,
        val coarsePointer: Boolean,
        val bootNotice: BootNotice? = null,
    ) {
        /** Whether the chrome is dark, following the system live when [appearance] says so. */
        val isDark: Boolean
            get() = when (appearance) {
                Appearance.System -> systemDark
                Appearance.Light -> false
                Appearance.Dark -> true
            }

        /** Whether the chrome keeps motion to a minimum, the system's wish unless [motion] overrides it. */
        val reducedMotion: Boolean
            get() = when (motion) {
                MotionOverride.System -> systemReducedMotion
                MotionOverride.Reduce -> true
                MotionOverride.Full -> false
            }
    }
}

/** The skin [document] is edited in, its own library and flavor. */
internal fun skinOf(document: ThemeDocument): Skin = Skin(library = document.library, expressive = document.expressive)
