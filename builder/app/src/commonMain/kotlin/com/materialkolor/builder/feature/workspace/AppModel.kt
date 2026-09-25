package com.materialkolor.builder.feature.workspace

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.kit.skin.Skin
import dev.stateholder.extensions.viewmodel.StateViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * What the root needs to theme the builder, the chrome's appearance and its motion, and the boot
 * that opens the first project.
 *
 * The chrome's appearance comes from the preferences and the system, never from the preview mode,
 * and nothing here writes the preview mode (F-04). The skin is not held here. The root derives it
 * from the same collected document it resolves the theme from, so there is one skin source.
 *
 * It also keeps what the banners above the workspace say for the whole session (F-38). Why the
 * address did not open what it asked for, until it is dismissed. A full storage, from the save that
 * failed after the repository made room until a save lands. No storage at all, until its banner is
 * closed. Where a reload does nothing, data a newer build saved, until its banner is dismissed.
 */
@Stable
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class AppModel(
    private val session: ProjectSession,
    private val router: Router,
    preferences: PreferencesRepository,
    private val environment: Environment,
) : StateViewModel<AppModel.State>(
        State(
            appearance = preferences.preferences.value.appearance,
            motion = preferences.preferences.value.motion,
            systemDark = environment.prefersDark.value,
            systemReducedMotion = environment.reducedMotion.value,
            coarsePointer = environment.coarsePointer.value,
            storageUnavailable = !environment.storageAvailable, // b-314b
            canReload = environment.canReload, // b-314ba
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
        // b-221c
        // Undispatched, since a hidden page may never run another task and the autosave waiting
        // would be lost with the tab.
        viewModelScope.launch(Dispatchers.Unconfined) {
            environment.pageHides.collect { session.flush() }
        }
        // b-314b
        session.saveStatus.mergeState { state, status ->
            state.copy(storageFull = storageFullAfter(state.storageFull, status))
        }
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
        val openedDefaults = session.document.value == ThemeDocument.Default // b-314b
        updateState { state -> state.copy(bootNotice = notice, openedDefaults = openedDefaults) }
    }

    // b-314b

    /**
     * Put the boot notice away for the rest of the session.
     */
    fun dismissBootNotice() {
        updateState { state -> state.copy(bootNotice = null) }
    }

    /**
     * Close the banner about a browser that keeps nothing, for the rest of the session.
     */
    fun dismissStorageUnavailable() {
        updateState { state -> state.copy(storageUnavailableDismissed = true) }
    }

    // b-314ba

    /**
     * Put the banner about a newer build's data away for the rest of the session.
     */
    fun dismissNewerData() {
        updateState { state -> state.copy(newerDataDismissed = true) }
    }

    /**
     * Load the address the page opened on again, the link a newer build wrote for example, so a
     * newer build can read it. The address bar says `/` by now (D15), so the path comes from the
     * route read at boot.
     */
    fun reloadLink() {
        environment.reload(pathOf(router.initial))
    }

    /**
     * Load the builder again at `/`, so a newer build picks up the data it saved here (D41).
     */
    fun reloadHome() {
        environment.reload(HOME_PATH)
    }

    /**
     * Write whatever is waiting to be saved, when the app goes to the background or the page hides.
     */
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
     * @property[openedDefaults] Whether boot opened the defaults rather than a saved theme.
     * @property[storageFull] Whether storage was still full after the repository made room, until a
     *   save lands or another project opens.
     * @property[storageUnavailable] Whether nothing saved here outlives the session.
     * @property[storageUnavailableDismissed] Whether the banner saying so was closed this session.
     * @property[canReload] Whether loading the page again does anything here, as it does on the web.
     * @property[newerDataDismissed] Whether the banner about a newer build's data was dismissed this
     *   session.
     */
    @Immutable
    data class State(
        val appearance: Appearance,
        val motion: MotionOverride,
        val systemDark: Boolean,
        val systemReducedMotion: Boolean,
        val coarsePointer: Boolean,
        val bootNotice: BootNotice? = null,
        // b-314b
        val openedDefaults: Boolean = false,
        val storageFull: Boolean = false,
        val storageUnavailable: Boolean = false,
        val storageUnavailableDismissed: Boolean = false,
        // b-314ba
        val canReload: Boolean = false,
        val newerDataDismissed: Boolean = false,
    ) {
        /**
         * Whether the chrome is dark, following the system live when [appearance] says so.
         */
        val isDark: Boolean
            get() = when (appearance) {
                Appearance.System -> systemDark
                Appearance.Light -> false
                Appearance.Dark -> true
            }

        /**
         * Whether the chrome keeps motion to a minimum, the system's wish unless [motion] overrides it.
         */
        val reducedMotion: Boolean
            get() = when (motion) {
                MotionOverride.System -> systemReducedMotion
                MotionOverride.Reduce -> true
                MotionOverride.Full -> false
            }
    }
}

// b-314b

/**
 * Whether storage is full after [status], given it was [full] before. A save that is waiting says
 * nothing yet, so the banner stays up through it.
 */
private fun storageFullAfter(
    full: Boolean,
    status: SaveStatus,
): Boolean =
    when (status) {
        SaveStatus.Idle -> false
        SaveStatus.Pending -> full
        is SaveStatus.Failed -> status.error == StoreError.QuotaExceeded
    }

/**
 * The path [route] was read from, a link's own `/t/` path or `/` for anything else.
 */
private fun pathOf(route: Route): String =
    when (route) {
        is Route.Theme -> THEME_PATH_PREFIX + route.code
        Route.Home, is Route.Legacy, is Route.Unknown -> HOME_PATH
    }

private const val HOME_PATH = "/"
private const val THEME_PATH_PREFIX = "/t/"

/**
 * The skin [document] is edited in, its own library and flavor.
 */
internal fun skinOf(document: ThemeDocument): Skin = Skin(library = document.library, expressive = document.expressive)
