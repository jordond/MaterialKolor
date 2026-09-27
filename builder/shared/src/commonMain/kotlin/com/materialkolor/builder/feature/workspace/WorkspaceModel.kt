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
import com.materialkolor.builder.core.session.SessionState
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
import com.materialkolor.builder.feature.topbar.LibraryChoice
import com.materialkolor.builder.feature.topbar.libraryPick
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
 * arrive through the session's state.
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
    // Where the poster's copy buttons write, straight from the click with no hop through here.
    val clipboard: Clipboard,
    private val router: Router,
    private val resolver: ThemeResolver,
) : StateViewModel<WorkspaceModel.State>(
        State(
            session = session.state.value,
            capabilities = capabilitiesOf(session.state.value.document),
            preferences = preferences.preferences.value,
        ),
    ) {
    init {
        // The session publishes the open project as one value, so no state here ever pairs one
        // project's document with another project's number or view, whatever order things run in.
        session.state.mergeState { state, open -> state.following(open) }
        preferences.preferences.mergeState { state, prefs -> state.copy(preferences = prefs) }
        router.overlayPops.mergeState { state, _ -> state.copy(panel = null, pickerTarget = null, timeline = null) }
        session.projectName.mergeState { state, name -> state.copy(projectName = name) }
    }

    /**
     * Make [change] to the document.
     */
    fun edit(
        change: DocumentChange,
        phase: EditPhase,
    ) {
        session.edit(change, phase)
        syncSession()
    }

    /**
     * Move the document to [choice] as one discrete edit, with the style following M3 Expressive
     * the way [libraryPick] says. It reads the steps an undo would walk back, so after an undo the
     * pick sees the history as it now stands. Picking the choice the document is on does nothing.
     */
    fun pickLibrary(choice: LibraryChoice) {
        val document = session.document.value
        if (LibraryChoice.of(document) == choice) return
        val timeline = session.timeline()
        edit(libraryPick(choice, document, timeline.steps.take(timeline.cursor)), EditPhase.Discrete)
    }

    /**
     * Step back once.
     */
    fun undo() {
        session.undo()
        syncSession()
    }

    /**
     * Step forward once.
     */
    fun redo() {
        session.redo()
        syncSession()
    }

    /**
     * Move straight to the step [cursor] of the history.
     */
    fun jumpTo(cursor: Int) {
        session.jumpTo(cursor)
        syncSession()
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
        updateState { state -> state.copy(rampHighlight = null) }
    }

    /**
     * Only the preview's mode moves. The chrome keeps its appearance.
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

    /**
     * Open the Fine-tune sheet at [section], or at its top, the locks, when [section] is null.
     */
    fun openFineTune(section: FineTuneSection? = null) {
        updateState { state -> state.copy(fineTune = section ?: FineTuneSection.Locks) } // b-521
    }

    fun closeFineTune() { // b-521
        updateState { state -> state.copy(fineTune = null) }
    }

    fun setVision(vision: VisionSimulation) {
        updateState { state -> state.copy(vision = vision) }
    }

    fun setInspect(on: Boolean) {
        updateState { state -> state.copy(inspect = on) }
    }

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
                    fullscreen = state.fullscreen && panel != Panel.History,
                    // b-521 The Projects drawer covers the poster, so the Fine-tune sheet shuts under it.
                    fineTune = state.fineTune.takeIf { panel != Panel.Projects },
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
     * Only the chrome's appearance moves. The preview keeps its mode.
     */
    fun setAppearance(appearance: Appearance) {
        updatePreferences { prefs -> prefs.copy(appearance = appearance) }
    }

    fun setMotionOverride(motion: MotionOverride) {
        updatePreferences { prefs -> prefs.copy(motion = motion) }
    }

    fun setPosterCollapsed(collapsed: Boolean) {
        if (collapsed) closeFineTune() // b-521
        updatePreferences { prefs -> prefs.copy(posterCollapsed = collapsed) }
    }

    /**
     * Collapse or open the poster shown in [mode]. On the narrow Medium rail the poster opens over the
     * canvas for this session only, and the stored choice stays with the docked poster.
     */
    fun setPosterCollapsed(
        collapsed: Boolean,
        mode: PosterMode,
    ) {
        if (mode == PosterMode.Rail72) {
            if (collapsed) closeFineTune() // b-521
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
        // The tab keeps the dismissal as well, so the hint stays shut when storage turns the write down.
        updateState { state -> state.copy(sessionDismissedHints = state.sessionDismissedHints + id) }
        updatePreferences { prefs -> prefs.copy(dismissedHints = prefs.dismissedHints + id) }
    }

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
    private fun syncSession() {
        val open = session.state.value
        updateState { state -> state.following(open) }
    }

    /**
     * This state holding the session's [open] value. The capabilities are worked out again only when
     * the document changed, and the Fine-tune sheet shuts when another project shows. The steps are
     * built again only when the document, the project or the history moved, so a change of view or
     * save status leaves them be.
     */
    private fun State.following(open: SessionState): State {
        if (open == this.session) return this
        val next = copy(
            session = open,
            capabilities = if (open.document == document) capabilities else capabilitiesOf(open.document),
            fineTune = fineTune.takeIf { open.generation == projectGeneration }, // b-521
        )
        val moved = open.document != document || open.generation != projectGeneration || open.history != history
        return if (moved) next.withTimeline() else next
    }

    /**
     * This state with the session's steps while the History list is open, and without them once it is
     * not, so nothing builds the list while no one can see it.
     */
    private fun State.withTimeline(): State =
        when {
            panel == Panel.History -> copy(timeline = this@WorkspaceModel.session.timeline())
            timeline != null -> copy(timeline = null)
            else -> this
        }

    private fun updateView(block: (ProjectViewState) -> ProjectViewState) {
        session.updateView(block)
        val open = session.state.value
        updateState { state -> state.following(open) }
    }

    private fun updatePreferences(block: (Preferences) -> Preferences) {
        viewModelScope.launch { preferences.update(block) }
    }

    /**
     * @property[session] The session's value for the open project, held whole so the document, its
     * project number, history, view and save status always come from the same moment.
     * @property[capabilities] How each control shows up for [document]'s target, style and spec,
     * worked out again whenever the document changes.
     * @property[preferences] What this browser remembers across projects.
     * @property[panel] The panel that is open, or null.
     * @property[pickerTarget] What the open picker edits, or null when it is closed.
     * @property[vision] The color vision simulated over the canvas. It resets on reload.
     * @property[inspect] Whether the inspect overlay is on.
     * @property[fullscreen] Whether the poster and the top bar are hidden.
     * @property[projectName] The open project's name, empty until the session has opened one.
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
     * @property[fineTune] The section the Fine-tune sheet is open at, or null while it is shut. It
     * lives for this session only. Collapsing the poster, opening the Projects drawer or showing
     * another project shuts it.
     */
    @Immutable
    data class State(
        val session: SessionState,
        val capabilities: Capabilities,
        val preferences: Preferences,
        val panel: Panel? = null,
        val pickerTarget: PickerTarget? = null,
        val vision: VisionSimulation = VisionSimulation.None,
        val inspect: Boolean = false,
        val fullscreen: Boolean = false,
        val projectName: String = "",
        val rampHighlight: RampHighlight? = null,
        val sessionDismissedHints: Set<String> = emptySet(),
        val visionMenuOpen: Boolean = false,
        val grayscaleHeld: Boolean = false,
        val posterOverCanvas: Boolean = false,
        val timeline: Timeline? = null,
        val fineTune: FineTuneSection? = null,
    ) {
        /**
         * The document as stored, with every setting kept. Anything that resolves or exports reads
         * it through `forTarget` first.
         */
        val document: ThemeDocument
            get() = session.document

        /**
         * What undo and redo would do.
         */
        val history: HistoryState
            get() = session.history

        /**
         * How the preview is set up, saved with the project.
         */
        val view: ProjectViewState
            get() = session.view

        /**
         * Whether the open project's latest changes are saved.
         */
        val saveStatus: SaveStatus
            get() = session.saveStatus

        /**
         * Counts the projects this tab has shown, one more each time another opens. It changes in
         * the same state as the document the new project brings, so anything that belongs to one
         * project can tell a new project from an edit. Both come from the session's one value, so
         * the pair holds whatever order the collectors run in.
         */
        val projectGeneration: Int
            get() = session.generation

        /**
         * What [document] exports to.
         */
        val target: ExportTarget
            get() = ExportTarget.of(document.library, document.expressive)

        /**
         * Whether the poster shows as the rail in [mode]. Below 840 dp at Medium it starts as the rail
         * and opens over the canvas when asked, and a docked poster keeps the stored choice.
         */
        fun posterCollapsed(mode: PosterMode): Boolean =
            if (mode == PosterMode.Rail72) !posterOverCanvas else preferences.posterCollapsed
    }

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
