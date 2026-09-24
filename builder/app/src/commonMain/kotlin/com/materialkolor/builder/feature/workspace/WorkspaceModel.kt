package com.materialkolor.builder.feature.workspace

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FineTuneRow
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.shuffle.Shuffle
import com.materialkolor.builder.engine.shuffle.ShuffleResult
import com.materialkolor.builder.engine.shuffle.shuffleLocks
import com.materialkolor.builder.feature.canvas.VisionSimulation
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.topbar.raisesExpressiveSuggestion
import dev.stateholder.extensions.viewmodel.StateViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * The workspace's view of the open project, its history, how the preview is set up, the browser's
 * preferences and which panel is open.
 *
 * The capabilities always come from the document they sit next to. An edit made here updates both
 * before it returns, so a control the new target cannot use is disabled in the frame the target
 * changes, whatever dispatcher the model runs on. Changes from elsewhere, another tab or a boot,
 * arrive through the session's flows.
 *
 * Opening a panel adds one history entry to the address bar, so Back closes it.
 */
@Stable
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class WorkspaceModel(
    private val session: ProjectSession,
    private val preferences: PreferencesRepository,
    // b-221c
    // Where the poster's copy buttons write, straight from the click with no hop through here.
    val clipboard: Clipboard,
    private val router: Router,
    private val resolver: ThemeResolver,
) : StateViewModel<WorkspaceModel.State>(
        State(
            document = session.document.value,
            capabilities = capabilitiesOf(session.document.value),
            history = session.history.value,
            view = session.viewState.value,
            preferences = preferences.preferences.value,
            saveStatus = session.saveStatus.value,
            // b-221c
            projectGeneration = session.generation.value,
        ),
    ) {
    init {
        // b-221c
        // The generation moves before the document does, so a new document always comes with its number.
        session.document.mergeState { state, document ->
            state.withDocument(document).copy(projectGeneration = session.generation.value)
        }
        session.history.mergeState { state, history -> state.copy(history = history) }
        session.viewState.mergeState { state, view -> state.copy(view = view) }
        preferences.preferences.mergeState { state, prefs -> state.copy(preferences = prefs) }
        router.overlayPops.mergeState { state, _ -> state.copy(panel = null, pickerTarget = null) }
        session.projectName.mergeState { state, name -> state.copy(projectName = name) }
        session.saveStatus.mergeState { state, status -> state.copy(saveStatus = status) }
        // b-221c
        session.generation.mergeState { state, generation -> state.copy(projectGeneration = generation) }
    }

    /**
     * Make [change] to the document.
     *
     * A switch onto Expressive that leaves the style or the spec where Expressive themes rarely sit
     * raises the Expressive suggestion in the same update as the document. Behind a reveal that is
     * when the change lands, never the press before it, whatever sent the switch. Any other edit that
     * moves the document puts it away.
     */
    fun edit(
        change: DocumentChange,
        phase: EditPhase,
    ) {
        val before = session.document.value
        session.edit(change, phase)
        val after = session.document.value
        val suggesting = if (after == before) {
            state.value.expressiveSuggestion
        } else {
            raisesExpressiveSuggestion(change, before, after)
        }
        syncSession(suggesting)
    }

    /** Step back once. */
    fun undo() {
        session.undo()
        syncSession(expressiveSuggestion = false)
    }

    /** Step forward once. */
    fun redo() {
        session.redo()
        syncSession(expressiveSuggestion = false)
    }

    /** Put the Expressive suggestion away, after Apply or Keep mine. */
    fun dismissExpressiveSuggestion() {
        updateState { state -> state.copy(expressiveSuggestion = false) }
    }

    /**
     * Draw the next shuffle of the document, leaving alone what the locks in the preferences hold.
     * Null when the locks leave nothing to move.
     *
     * It resolves candidates on the shared resolver, so call it on the UI thread.
     */
    fun drawShuffle(random: Random = Random.Default): ShuffleResult.Shuffled? {
        val locks = preferences.preferences.value.shuffleLocks()
        return when (val result = Shuffle.next(random, session.document.value, locks, resolver)) {
            is ShuffleResult.Shuffled -> result
            ShuffleResult.NothingToShuffle -> null
        }
    }

    /**
     * Land [shuffle] on the document as it is now. It is one undo entry that never folds into the
     * shuffle before it.
     */
    fun applyShuffle(shuffle: ShuffleResult.Shuffled) {
        edit(DocumentChange.Replace(shuffle.applyTo(session.document.value)), EditPhase.Discrete)
    }

    fun setPreviewTab(tab: PreviewTab) {
        updateView { view -> view.copy(tab = tab) }
    }

    /** Only the preview's mode moves. The chrome keeps its appearance (F-04). */
    fun setPreviewMode(mode: PreviewMode) {
        updateView { view -> view.copy(mode = mode) }
    }

    fun setSplitFraction(fraction: Float) {
        updateView { view -> view.copy(splitFraction = fraction.coerceIn(0f, 1f)) }
    }

    fun setDeviceWidth(width: DeviceWidth) {
        updateView { view -> view.copy(deviceWidth = width) }
    }

    fun setFineTuneRowOpen(
        row: FineTuneRow,
        open: Boolean,
    ) {
        updateView { view ->
            view.copy(openFineTuneRows = if (open) view.openFineTuneRows + row else view.openFineTuneRows - row)
        }
    }

    fun setVision(vision: VisionSimulation) {
        updateState { state -> state.copy(vision = vision) }
    }

    fun setInspect(on: Boolean) {
        updateState { state -> state.copy(inspect = on) }
    }

    fun toggleFullscreen() {
        updateState { state -> state.copy(fullscreen = !state.fullscreen) }
    }

    /** Open [panel], adding a history entry only when nothing else was open. */
    fun openPanel(panel: Panel) {
        val open = state.value.panel
        if (open == panel) return
        if (open == null) router.pushOverlay(panel.name)
        updateState { state -> state.copy(panel = panel, pickerTarget = null) }
    }

    /** Open the picker on [target]. */
    fun openPicker(target: PickerTarget) {
        openPanel(Panel.Picker)
        updateState { state -> state.copy(pickerTarget = target) }
    }

    /** Close the open panel and drop the history entry it added. */
    fun closePanel() {
        if (state.value.panel == null) return
        updateState { state -> state.copy(panel = null, pickerTarget = null) }
        router.popOverlay()
    }

    /** Only the chrome's appearance moves. The preview keeps its mode (F-04). */
    fun setAppearance(appearance: Appearance) {
        updatePreferences { prefs -> prefs.copy(appearance = appearance) }
    }

    fun setMotionOverride(motion: MotionOverride) {
        updatePreferences { prefs -> prefs.copy(motion = motion) }
    }

    fun setPosterCollapsed(collapsed: Boolean) {
        updatePreferences { prefs -> prefs.copy(posterCollapsed = collapsed) }
    }

    fun setLock(
        lock: ShuffleLock,
        on: Boolean,
    ) {
        updatePreferences { prefs ->
            when (lock) {
                ShuffleLock.Hue -> prefs.copy(hueLock = on)
                ShuffleLock.Style -> prefs.copy(styleLock = on)
                ShuffleLock.Seed -> prefs.copy(seedLock = on)
            }
        }
    }

    fun dismissHint(id: String) {
        updatePreferences { prefs -> prefs.copy(dismissedHints = prefs.dismissedHints + id) }
    }

    /** Read the session back into the state at once, so nothing waits on a collector. */
    private fun syncSession(expressiveSuggestion: Boolean) {
        val document = session.document.value
        val history = session.history.value
        updateState { state ->
            state.withDocument(document).copy(history = history, expressiveSuggestion = expressiveSuggestion)
        }
    }

    private fun updateView(block: (ProjectViewState) -> ProjectViewState) {
        session.updateView(block)
        val view = session.viewState.value
        updateState { state -> state.copy(view = view) }
    }

    private fun updatePreferences(block: (Preferences) -> Preferences) {
        viewModelScope.launch { preferences.update(block) }
    }

    /**
     * @property[document] The document as stored, with every setting kept. Anything that resolves
     * or exports reads it through `forTarget` first.
     * @property[capabilities] How each control shows up for [document]'s target, style and spec.
     * @property[history] What undo and redo would do.
     * @property[view] How the preview is set up, saved with the project.
     * @property[preferences] What this browser remembers across projects.
     * @property[panel] The panel that is open, or null.
     * @property[pickerTarget] What the open picker edits, or null when it is closed.
     * @property[vision] The color vision simulated over the canvas. It resets on reload.
     * @property[inspect] Whether the inspect overlay is on.
     * @property[fullscreen] Whether the poster and the top bar are hidden.
     * @property[projectName] The open project's name, empty until the session has opened one.
     * @property[saveStatus] Whether the open project's latest changes are saved.
     * @property[projectGeneration] Counts the projects this tab has shown, one more each time another
     * opens. It changes in the same state as the document the new project brings, so anything that
     * belongs to one project can tell a new project from an edit.
     * @property[expressiveSuggestion] Whether the top bar offers the Expressive style on the 2025
     * spec after a switch to Expressive (F-03).
     */
    @Immutable
    data class State(
        val document: ThemeDocument,
        val capabilities: Capabilities,
        val history: HistoryState,
        val view: ProjectViewState,
        val preferences: Preferences,
        val panel: Panel? = null,
        val pickerTarget: PickerTarget? = null,
        val vision: VisionSimulation = VisionSimulation.None,
        val inspect: Boolean = false,
        val fullscreen: Boolean = false,
        val projectName: String = "",
        val saveStatus: SaveStatus = SaveStatus.Idle,
        // b-221c
        val projectGeneration: Int = 0,
        val expressiveSuggestion: Boolean = false,
    ) {
        /** What [document] exports to. */
        val target: ExportTarget
            get() = ExportTarget.of(document.library, document.expressive)

        /** This state showing [document], its capabilities worked out in the same step. */
        fun withDocument(document: ThemeDocument): State =
            if (document == this.document) this else copy(document = document, capabilities = capabilitiesOf(document))
    }
}

/** The capabilities of [document]'s target, read with the spec its style really runs. */
internal fun capabilitiesOf(document: ThemeDocument): Capabilities =
    Capabilities.of(
        library = document.library,
        expressive = document.expressive,
        style = document.style,
        effectiveSpec = EffectiveSpec.of(style = document.style, requested = document.spec),
    )
