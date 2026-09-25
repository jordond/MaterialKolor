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
import com.materialkolor.builder.core.session.Timeline
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.ExportPrefs
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
import com.materialkolor.builder.feature.canvas.RampHighlight
import com.materialkolor.builder.feature.canvas.RampTarget
import com.materialkolor.builder.feature.canvas.VisionSimulation
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.topbar.raisesExpressiveSuggestion
import com.materialkolor.builder.kit.layout.PosterMode
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
            document = session.shown.value.document, // b-229
            capabilities = capabilitiesOf(session.shown.value.document), // b-229
            history = session.history.value,
            view = session.viewState.value,
            preferences = preferences.preferences.value,
            saveStatus = session.saveStatus.value,
            // b-221c
            projectGeneration = session.shown.value.generation, // b-229
        ),
    ) {
    init {
        // b-221c
        // b-229
        // The session publishes the document and its project's number as one value, so no state here
        // ever pairs one project's document with another project's number, whatever order things run in.
        session.shown.mergeState { state, shown ->
            state.withDocument(shown.document).copy(projectGeneration = shown.generation).withTimeline() // b-508
        }
        session.history.mergeState { state, history -> state.copy(history = history).withTimeline() } // b-508
        session.viewState.mergeState { state, view -> state.copy(view = view) }
        preferences.preferences.mergeState { state, prefs -> state.copy(preferences = prefs) }
        router.overlayPops.mergeState { state, _ -> state.copy(panel = null, pickerTarget = null, timeline = null) }
        session.projectName.mergeState { state, name -> state.copy(projectName = name) }
        session.saveStatus.mergeState { state, status -> state.copy(saveStatus = status) }
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

    /**
     * Step back once.
     */
    fun undo() {
        session.undo()
        syncSession(expressiveSuggestion = false)
    }

    /**
     * Step forward once.
     */
    fun redo() {
        session.redo()
        syncSession(expressiveSuggestion = false)
    }

    // b-508

    /**
     * Move straight to the step [cursor] of the history, and put the Expressive suggestion away as Undo does.
     */
    fun jumpTo(cursor: Int) {
        session.jumpTo(cursor)
        syncSession(expressiveSuggestion = false)
    }

    /**
     * Put the Expressive suggestion away, after Apply or Keep mine.
     */
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
        updateState { state -> state.copy(rampHighlight = null) } // b-308
    }

    /**
     * Only the preview's mode moves. The chrome keeps its appearance (F-04).
     */
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

    // b-315c

    fun setVisionMenuOpen(open: Boolean) {
        updateState { state -> state.copy(visionMenuOpen = open) }
    }

    fun holdGrayscale(held: Boolean) {
        updateState { state -> state.copy(grayscaleHeld = held) }
    }

    fun toggleFullscreen() {
        updateState { state -> state.copy(fullscreen = !state.fullscreen) }
    }

    /**
     * Open [panel], adding a history entry only when nothing else was open. History hangs from the top
     * bar, which fullscreen hides, so opening it leaves fullscreen first.
     */
    fun openPanel(panel: Panel) {
        val open = state.value.panel
        if (open == panel) return
        if (open == null) router.pushOverlay(panel.name)
        updateState { state ->
            state
                .copy(
                    panel = panel,
                    pickerTarget = null,
                    fullscreen = state.fullscreen && panel != Panel.History, // b-508
                ).withTimeline()
        }
    }

    /**
     * Open the picker on [target].
     */
    fun openPicker(target: PickerTarget) {
        openPanel(Panel.Picker)
        updateState { state -> state.copy(pickerTarget = target) }
    }

    /**
     * Close the open panel and drop the history entry it added.
     */
    fun closePanel() {
        if (state.value.panel == null) return
        updateState { state -> state.copy(panel = null, pickerTarget = null, timeline = null) }
        router.popOverlay()
    }

    /**
     * Only the chrome's appearance moves. The preview keeps its mode (F-04).
     */
    fun setAppearance(appearance: Appearance) {
        updatePreferences { prefs -> prefs.copy(appearance = appearance) }
    }

    fun setMotionOverride(motion: MotionOverride) {
        updatePreferences { prefs -> prefs.copy(motion = motion) }
    }

    fun setPosterCollapsed(collapsed: Boolean) {
        updatePreferences { prefs -> prefs.copy(posterCollapsed = collapsed) }
    }

    // b-406g

    /**
     * Collapse or open the poster shown in [mode]. On the narrow Medium rail the poster opens over the
     * canvas for this session only, and the stored choice stays with the docked poster.
     */
    fun setPosterCollapsed(
        collapsed: Boolean,
        mode: PosterMode,
    ) {
        if (mode == PosterMode.Rail72) {
            updateState { state -> state.copy(posterOverCanvas = !collapsed) }
        } else {
            setPosterCollapsed(collapsed)
        }
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
        // b-314a
        // The tab keeps the dismissal as well, so the hint stays shut when storage turns the write down.
        updateState { state -> state.copy(sessionDismissedHints = state.sessionDismissedHints + id) }
        updatePreferences { prefs -> prefs.copy(dismissedHints = prefs.dismissedHints + id) }
    }

    // b-308

    /**
     * Show the Palettes tab with the ramp of [target] picked out, until the next tab switch. The
     * target keeps the project it was picked in, so the tab can drop it once another one opens.
     */
    fun showOnRamp(target: RampTarget) {
        updateView { view -> view.copy(tab = PreviewTab.Palettes) }
        updateState { state -> state.copy(rampHighlight = RampHighlight(target, state.projectGeneration)) }
    }

    /**
     * Read the session back into the state at once, so nothing waits on a collector.
     */
    private fun syncSession(expressiveSuggestion: Boolean) {
        val shown = session.shown.value // b-229
        val history = session.history.value
        updateState { state ->
            state
                .withDocument(shown.document)
                .copy(
                    projectGeneration = shown.generation, // b-229
                    history = history,
                    expressiveSuggestion = expressiveSuggestion,
                ).withTimeline() // b-508
        }
    }

    // b-508

    /**
     * This state with the session's steps while the History list is open, and without them once it is
     * not, so nothing builds the list while no one can see it.
     */
    private fun State.withTimeline(): State =
        when {
            panel == Panel.History -> copy(timeline = session.timeline())
            timeline != null -> copy(timeline = null)
            else -> this
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
     * belongs to one project can tell a new project from an edit. Both come from the session's one
     * `shown` value, so the pair holds whatever order the collectors run in.
     * @property[expressiveSuggestion] Whether the top bar offers the Expressive style on the 2025
     * spec after a switch to Expressive (F-03).
     * @property[rampHighlight] What the Palettes tab picks out after Show on ramp, with the project
     * it was picked in, or null. A tab switch clears it and nothing saves it.
     * @property[sessionDismissedHints] The hints closed in this tab, kept beside the stored ones so
     * a hint stays closed for the session when the preferences write fails.
     * @property[visionMenuOpen] Whether the dock's Vision menu is open, so V can open it too.
     * @property[grayscaleHeld] Whether B is held, which shows the canvas in grayscale over [vision].
     * @property[posterOverCanvas] Whether the poster is open over the canvas from the narrow Medium
     * rail. Every session starts with it shut and nothing saves it.
     * @property[timeline] Every step of the history and where the document sits among them, while
     * [panel] is the History list, or null while it is not. It follows the history, the document and
     * the panel, and nothing builds it while the list is closed.
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
        val rampHighlight: RampHighlight? = null, // b-308
        val sessionDismissedHints: Set<String> = emptySet(), // b-314a
        // b-315c
        val visionMenuOpen: Boolean = false,
        val grayscaleHeld: Boolean = false,
        val posterOverCanvas: Boolean = false, // b-406g
        val timeline: Timeline? = null, // b-508
    ) {
        /**
         * What [document] exports to.
         */
        val target: ExportTarget
            get() = ExportTarget.of(document.library, document.expressive)

        // b-406g

        /**
         * Whether the poster shows as the rail in [mode]. Below 840 dp at Medium it starts as the rail
         * and opens over the canvas when asked (spec section 7), and a docked poster keeps the stored
         * choice.
         */
        fun posterCollapsed(mode: PosterMode): Boolean =
            if (mode == PosterMode.Rail72) !posterOverCanvas else preferences.posterCollapsed

        /**
         * This state showing [document], its capabilities worked out in the same step.
         */
        fun withDocument(document: ThemeDocument): State =
            if (document == this.document) this else copy(document = document, capabilities = capabilitiesOf(document))
    }

    // b-306b

    /**
     * Turn color animation on or off in the export options of [target] alone. The document and its
     * history stay as they are.
     */
    fun setColorAnimation(
        target: ExportTarget,
        on: Boolean,
    ) {
        updateExportPrefs(target) { prefs -> prefs.copy(animate = on) }
    }

    /**
     * Set how long the color animation of [target] runs, in milliseconds, leaving every other target alone.
     */
    fun setColorAnimationDuration(
        target: ExportTarget,
        durationMs: Int,
    ) {
        require(durationMs > 0) { "An animation runs for some time, got $durationMs ms" }
        updateExportPrefs(target) { prefs -> prefs.copy(animationDurationMs = durationMs) }
    }

    private fun updateExportPrefs(
        target: ExportTarget,
        block: (ExportPrefs) -> ExportPrefs,
    ) {
        viewModelScope.launch { preferences.updateExportPrefs(target, block) }
    }
}

/**
 * The capabilities of [document]'s target, read with the spec its style really runs.
 */
internal fun capabilitiesOf(document: ThemeDocument): Capabilities =
    Capabilities.of(
        library = document.library,
        expressive = document.expressive,
        style = document.style,
        effectiveSpec = EffectiveSpec.of(style = document.style, requested = document.spec),
    )
