package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.data.Creation
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.data.ProjectRepository
import com.materialkolor.builder.core.platform.BootSplash
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.history.History
import com.materialkolor.builder.domain.link.DecodeResult
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.HistoryRecord
import com.materialkolor.builder.domain.persist.ProjectRecord
import com.materialkolor.builder.domain.persist.ProjectViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The one owner of the open project, its document and its undo history.
 *
 * Everything it publishes lives in one [SessionState], so showing another project swaps the
 * document, its ref, view, history, conflict and save status in one write. Models read [state], or
 * one of [document], [shown], [history], [project], [viewState], [conflict] and [saveStatus] seen
 * through it, and nothing else holds document state. The rules for when an edit saves and what
 * another tab's save does are in [editOutcome] and [incomingSave]. Every change goes through
 * [edit], [undo], [redo] or [jumpTo]. Every rename goes through [rename], so the record the session holds
 * never writes an old name back. The History list reads the steps from [timeline] when it needs them.
 *
 * Committed edits are saved [AUTOSAVE_DELAY_MILLIS] after the last one, so a drag saves once after
 * it is released, and [SessionWrites] puts them into storage. [flush] writes whatever is waiting
 * straight away, on lifecycle STOP and on `pagehide`. A project opened from a link stays
 * [ProjectRef.Transient] and unsaved until the first edit, which saves it under the link's project
 * name or [sharedThemeName]. Each project keeps its own waiting save, so one that did not land
 * survives opening another project and goes out again on the next flush.
 *
 * When another tab saves the open project, its document comes in as an undo step, so Undo brings
 * this tab's back. If this tab edited in the last [CONFLICT_WINDOW_MILLIS] a [Conflict] comes up
 * instead, and this tab's saves wait until [resolveConflict] settles it. A newer save from the other
 * tab while the conflict is up takes the conflict's place and is never taken in silently.
 *
 * A conflict never loses anyone's work. When this tab has to save with a conflict up, on
 * [flush] or before it opens another project, it keeps its own document. The other tab gets that
 * save as an ordinary change from another tab, a conflict there if it edited lately or an undo step
 * otherwise, so its work can still be brought back.
 *
 * [colorsOf] resolves a theme, so it only ever runs on the caller's thread, the UI thread, and the
 * colors go along with the save it schedules. Changes from other tabs are applied on the thread
 * that opened the project for the same reason. The app scope only writes.
 *
 * Deleting the open project is up to the drawer, which opens another project first. A save that
 * lands after a delete is dropped by the repository, and the session does not notice.
 *
 * Preference writes, the last open project and what a legacy link asked for, are best effort. A
 * full storage is ignored there, with no prune and no retry.
 *
 * @param[colorsOf] The thumbnail and splash colors of a theme.
 * @param[sharedThemeName] What a theme from a link without a name is saved as.
 * @param[scope] The app scope, where saves run.
 * @param[now] The time in epoch milliseconds, for merging undo steps, the time each step keeps and the
 * conflict window.
 */
internal class ProjectSession(
    private val projects: ProjectRepository,
    private val preferences: PreferencesRepository,
    private val environment: Environment,
    private val colorsOf: (ThemeDocument) -> SessionColors,
    private val sharedThemeName: suspend () -> String,
    private val scope: CoroutineScope,
    private val now: () -> Long,
) {
    private val current = MutableStateFlow(
        OpenProject(id = null, name = "", held = null, EmptyCoroutineContext, scope.coroutineContext[Job]),
    )
    private val writeLock = Mutex()
    private var steps = History()
    private var lastEditAt: Long? = null

    // The document last handed to autosave, or the one the project opened with, and when the edit
    // that made it happened. Only the UI thread reads or writes them.
    private var committed = ThemeDocument.Default
    private var committedEditAt: Long? = null

    private val _state = MutableStateFlow(SessionState.initial())

    private val writes = SessionWrites(
        projects = projects,
        tabId = environment.tabId,
        lock = writeLock,
        showing = { current.value },
        conflicted = { _state.value.conflict != null },
        viewState = { _state.value.view },
        materialized = ::materialized,
        setStatus = { status -> _state.update { state -> state.copy(saveStatus = status) } },
    )
    private val autosave = Autosave(scope, keyOf = { save -> save.project }, write = writes::write)
    private val viewAutosave = Autosave(scope, keyOf = { pending -> pending.project }, write = writes::writeView)

    // The splash last written, so a change of appearance can write it again. The UI thread writes it
    // and the app scope reads it, and on the web both are the one thread.
    private var splash: BootSplash? = null

    init {
        // A reload right after the appearance changes paints the new one, with no edit in between.
        scope.launch {
            preferences.preferences.collect { prefs ->
                val last = splash
                if (last != null && last.appearance != prefs.appearance) {
                    writeSplash(last.copy(appearance = prefs.appearance))
                }
            }
        }
    }

    /**
     * Everything the session publishes about the open project, as one value.
     */
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /**
     * The theme being edited, with the number of the project it belongs to.
     */
    val shown: StateFlow<ShownDocument> = state.derived { state -> ShownDocument(state.document, state.generation) }

    /**
     * The theme being edited. It is [shown] without the number.
     */
    val document: StateFlow<ThemeDocument> = state.derived { state -> state.document }

    /**
     * What undo and redo can do right now.
     */
    val history: StateFlow<HistoryState> = state.derived { state -> state.history }

    /**
     * Which project is open.
     */
    val project: StateFlow<ProjectRef> = state.derived { state -> state.project }

    /**
     * How the preview of the open project is set up.
     */
    val viewState: StateFlow<ProjectViewState> = state.derived { state -> state.view }

    /**
     * Another tab's save that clashes with an edit made here, or null.
     */
    val conflict: StateFlow<Conflict?> = state.derived { state -> state.conflict }

    /**
     * Whether the open project is saved. A failure comes after the repository pruned and tried again.
     */
    val saveStatus: StateFlow<SaveStatus> = state.derived { state -> state.saveStatus }

    /**
     * The open project's name, the one its next save writes. It follows renames and other tabs.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val projectName: Flow<String> =
        current.flatMapLatest { open -> open.facts.map { facts -> facts.name }.distinctUntilChanged() }

    /**
     * Open the project [BootResolver] picks for [route], and apply what a legacy link asked for.
     *
     * `/` reads only the drawer's index, and a link reads every listed project to match against
     * them. The project this tab had open before a reload comes from [Environment.readTabProject].
     * Afterwards the app puts `/` back in the address bar.
     *
     * Call it from a coroutine on the UI dispatcher, never from `runBlocking`. The session keeps the
     * caller's dispatcher to apply other tabs' changes on and resolves themes on the caller's thread,
     * so a blocking boot would keep a dispatcher whose event loop is gone and resolve off the UI
     * thread.
     *
     * @return Why the address did not open what it asked for, or null when it did.
     */
    suspend fun boot(route: Route): BootNotice? {
        val tabProjectId = environment.readTabProject()
        val lastProjectId = preferences.current().lastProjectId
        val plan = BootResolver.resolve(route, tabProjectId, lastProjectId, projects.savedProjects(route))
        when (val start = plan.start) {
            is BootStart.Reopen -> if (!open(start.id)) startNew(ThemeDocument.Default, ProjectViewState())
            is BootStart.Shared -> showShared(start)
            BootStart.New -> startNew(ThemeDocument.Default, ProjectViewState())
        }
        plan.previewMode?.let { mode -> updateView { view -> view.copy(mode = mode) } }
        plan.packageName?.let { name ->
            preferences.updateExportPrefs(ExportTarget.Material3) { prefs -> prefs.copy(packageName = name) }
        }
        return plan.notice
    }

    /**
     * Make [change] to the document and record it for undo.
     *
     * A [EditPhase.Dragging] step only moves the document. The save waits for the release. A release
     * that lands back on the document last committed, a cancelled picker or a slider dragged back to
     * its start, saves nothing and does not count as an edit. Neither does a discrete change that
     * leaves the document as it was. A project opened from a link stays unsaved through both.
     */
    fun edit(
        change: DocumentChange,
        phase: EditPhase,
    ) {
        val before = _state.value.document
        val after = change.apply(before)
        val time = now()
        steps.record(before, after, change, phase, time)
        showStep(after)
        when (editOutcome(phase, before, after, committed)) {
            EditOutcome.ReleasedOntoCommitted -> lastEditAt = committedEditAt
            EditOutcome.Unchanged -> Unit
            EditOutcome.Drag -> lastEditAt = time
            EditOutcome.Commit -> {
                lastEditAt = time
                commit(after)
            }
        }
    }

    /**
     * Step back once. Nothing happens when there is nothing to undo.
     */
    fun undo() {
        moveTo(steps.undo() ?: return)
    }

    /**
     * Step forward once. Nothing happens when there is nothing to redo.
     */
    fun redo() {
        moveTo(steps.redo() ?: return)
    }

    /**
     * Move to [cursor] in the [timeline] at once, as that many undos or redos in one go. It saves
     * and counts as an edit the way Undo does. A cursor past the steps does nothing, since a click from
     * a list that another tab's change just moved can be stale, and nor does the one the history is at.
     */
    fun jumpTo(cursor: Int) {
        if (cursor !in 0..steps.size) return
        moveTo(steps.jumpTo(cursor) ?: return)
    }

    /**
     * The steps as they stand now, built when asked and never published on each frame of a drag.
     */
    fun timeline(): Timeline {
        val entries = steps.entries
        return Timeline(
            cursor = steps.cursor,
            now = now(),
            start = entries.firstOrNull()?.before ?: _state.value.document,
            steps = entries,
        )
    }

    /**
     * Save what is waiting, keeping this tab's document over an open conflict, then open the project
     * [id] with its history and view state.
     *
     * @return False when there is no such project, and the open one stays.
     */
    suspend fun open(id: String): Boolean {
        flushAll()
        val record = projects.load(id) ?: return false
        val saved = projects.loadHistory(id)
        val view = projects.viewState(id)
        val open = openHere(id, record.name, held = record)
        open.facts.update { facts -> facts.copy(savedHistory = saved) }
        show(open, ProjectRef.Persisted(id), record.document, History(saved.entries), view)
        rememberLastProject(id)
        return true
    }

    /**
     * Open the theme a share [code] carries.
     *
     * A saved project whose document encodes to the same code opens instead, so the same link opened
     * twice is the same project. Finding it reads every listed project. What is waiting is saved
     * first, keeping this tab's document over an open conflict.
     *
     * @return Why the code could not be opened, or null when it was.
     */
    suspend fun openShared(code: String): BootNotice? {
        val link = when (val result = ShareCodec.decode(code)) {
            is DecodeResult.Ok -> result
            DecodeResult.UnknownVersion -> return BootNotice.NewerVersion
            DecodeResult.Corrupt -> return BootNotice.InvalidLink
        }
        flushAll()
        val local = BootResolver.matching(link.document, projects.listedRecords(), preferredId = current.value.facts.value.id)
        if (local != null && open(local.id)) return null
        showShared(BootStart.Shared(code, link.document, link.projectName))
        return null
    }

    /**
     * Save what is waiting, keeping this tab's document over an open conflict, then start a project
     * from the defaults or, with [copyCurrent], from this one.
     */
    suspend fun newProject(copyCurrent: Boolean) {
        val shown = _state.value
        val document = if (copyCurrent) shown.document else ThemeDocument.Default
        val view = if (copyCurrent) shown.view else ProjectViewState()
        flushAll()
        startNew(document, view)
    }

    /**
     * Call the project [id] [name]. When it is the open project its next save keeps the new name, and
     * when the rename does not land it keeps the old one.
     *
     * @return Why the rename did not land, or null when it did.
     */
    suspend fun rename(
        id: String,
        name: String,
    ): StoreError? =
        writeLock.withLock {
            val open = current.value.takeIf { open -> open.facts.value.id == id }
            val before = open?.facts?.getAndUpdate { facts -> facts.copy(name = name) }?.name
            val error = projects.rename(id, name)
            if (error == null) {
                open?.facts?.update { facts ->
                    facts.copy(held = facts.held?.let { held -> held.copy(name = name, revision = held.revision + 1) })
                }
            } else if (open != null && before != null) {
                // A name another tab saved in the meantime stays.
                open.facts.update { facts -> if (facts.name == name) facts.copy(name = before) else facts }
            }
            error
        }

    /**
     * Save a project opened from a link now rather than on its first edit, for "Save to my
     * projects", along with anything else waiting. A project that is saved already only flushes.
     */
    fun saveTransient(): Job {
        val shown = _state.value
        if (shown.project is ProjectRef.Transient) commit(shown.document)
        return flush()
    }

    /**
     * Change how the preview is set up, saved once the changes stop.
     */
    fun updateView(block: (ProjectViewState) -> ProjectViewState) {
        val view = _state.updateAndGet { state -> state.copy(view = block(state.view)) }.view
        viewAutosave.schedule(PendingView(current.value, view))
    }

    /**
     * Settle the [conflict], keeping this tab's document when [keepMine] is true or taking the other
     * tab's as an undo step when it is false.
     */
    fun resolveConflict(keepMine: Boolean) {
        val conflict = _state.value.conflict ?: return
        val open = current.value
        if (keepMine) {
            val mine = _state.updateAndGet { state -> state.copy(conflict = null) }.document
            open.facts.update { facts -> facts.copy(held = conflict.theirs) }
            commit(mine)
        } else {
            // Adopting clears the conflict in the same write as the document it brings.
            adopt(open, conflict.theirs)
        }
    }

    /**
     * Write whatever is waiting now, for every project, keeping this tab's document over an open
     * conflict. The write starts on the caller's thread before this returns, so `pagehide` can use it.
     */
    fun flush(): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) { flushAll() }

    /**
     * Settle every waiting save. Everything up to the first suspension runs on the caller's thread,
     * the UI thread, because the conflict and the autosave timers belong to it.
     */
    private suspend fun flushAll() {
        if (_state.value.conflict != null) resolveConflict(keepMine = true)
        autosave.cancelTimer()
        viewAutosave.cancelTimer()
        autosave.flush()
        viewAutosave.flush()
    }

    private fun moveTo(document: ThemeDocument) {
        lastEditAt = now()
        showStep(document)
        commit(document)
    }

    /**
     * Write the splash colors of [document] and schedule its save.
     */
    private fun commit(document: ThemeDocument) {
        committed = document
        committedEditAt = lastEditAt
        val colors = colorsOf(document)
        writeSplash(colors)
        val sequence = writes.nextSave()
        autosave.schedule(PendingSave(current.value, sequence, document, HistoryRecord(steps.persisted()), colors))
    }

    /**
     * Keep [colors] for the next boot's splash, with the seed and the chrome's appearance.
     */
    private fun writeSplash(colors: SessionColors) {
        val appearance = preferences.preferences.value.appearance
        writeSplash(BootSplash(colors.splashLight, colors.splashDark, colors.splashSeed, appearance))
    }

    private fun writeSplash(next: BootSplash) {
        splash = next
        environment.writeSplash(next)
    }

    /**
     * Show [document] in place of the one showing, in the same project, along with what undo and redo
     * can do now that the steps moved. With [clearConflict] the conflict goes away in the same write.
     */
    private fun showStep(
        document: ThemeDocument,
        clearConflict: Boolean = false,
    ) {
        val history = historyState()
        _state.update { state ->
            state.copy(
                document = document,
                history = history,
                conflict = if (clearConflict) null else state.conflict,
            )
        }
    }

    private fun historyState(): HistoryState = HistoryState(steps.canUndo, steps.canRedo, steps.undoLabel, steps.redoLabel)

    private suspend fun show(
        open: OpenProject,
        ref: ProjectRef,
        document: ThemeDocument,
        steps: History,
        view: ProjectViewState,
        colors: SessionColors = colorsOf(document),
    ) {
        current.value.job.cancel()
        current.value = open
        this.steps = steps
        lastEditAt = null
        committed = document
        committedEditAt = null
        // One write, so no reader sees the new document with the old project's ref, view or conflict.
        _state.value = SessionState(
            project = ref,
            document = document,
            generation = _state.value.generation + 1,
            history = historyState(),
            view = view,
            conflict = null,
            saveStatus = SaveStatus.Idle,
        )
        writeSplash(colors)
        val id = open.facts.value.id
        environment.writeTabProject(id)
        id?.let { watching -> watch(open, watching) }
    }

    private suspend fun showShared(shared: BootStart.Shared) {
        val name = shared.projectName?.takeIf { name -> name.isNotBlank() } ?: sharedThemeName()
        val open = openHere(id = null, name, held = null)
        show(open, ProjectRef.Transient(shared.code), shared.document, History(), ProjectViewState())
    }

    /**
     * Create a project from [document], or show it unsaved when storage will not take it.
     */
    private suspend fun startNew(
        document: ThemeDocument,
        view: ProjectViewState,
    ) {
        val name = ColorNames.nameOf(document.seed)
        val colors = colorsOf(document)
        when (val creation = projects.create(name, document, colors.previewColors)) {
            is Creation.Created -> {
                val id = creation.record.id
                if (view != ProjectViewState()) projects.saveViewState(id, view)
                val open = openHere(id, name, held = creation.record)
                open.facts.update { facts -> facts.copy(savedHistory = HistoryRecord()) }
                show(open, ProjectRef.Persisted(id), document, History(), view, colors)
                rememberLastProject(id)
            }
            is Creation.Failed -> {
                val open = openHere(id = null, name, held = null)
                val code = runCatching { ShareCodec.encode(document, name) }.getOrDefault("")
                show(open, ProjectRef.Transient(code), document, History(), view, colors)
                writes.resetStatus(SaveStatus.Failed(creation.error))
            }
        }
    }

    private fun watch(
        open: OpenProject,
        id: String,
    ) {
        scope.launch(open.job + open.context) {
            projects.changes(id).collect { incoming -> onSavedElsewhere(open, incoming) }
        }
    }

    private fun onSavedElsewhere(
        open: OpenProject,
        incoming: ProjectRecord,
    ) {
        if (current.value !== open) return
        val shown = _state.value
        val outcome = incomingSave(
            held = open.facts.value.held,
            incoming = incoming,
            tabId = environment.tabId,
            document = shown.document,
            conflicted = shown.conflict != null,
            lastEditAt = lastEditAt,
            now = now(),
        )
        when (outcome) {
            IncomingSave.Ignore -> Unit
            IncomingSave.Matches -> {
                open.facts.update { facts -> facts.copy(name = incoming.name, held = incoming) }
                _state.update { state -> state.copy(conflict = null) }
            }
            IncomingSave.RaiseConflict -> {
                open.facts.update { facts -> facts.copy(name = incoming.name) }
                _state.update { state -> state.copy(conflict = Conflict(incoming)) }
            }
            IncomingSave.Adopt -> adopt(open, incoming)
        }
    }

    /**
     * Take the other tab's [theirs] as an undo step, so Undo brings this tab's document back. It only
     * runs with no conflict up or while settling one, so the conflict goes away with the document.
     */
    private fun adopt(
        open: OpenProject,
        theirs: ProjectRecord,
    ) {
        val mine = _state.value.document
        open.facts.update { facts -> facts.copy(name = theirs.name, held = theirs) }
        steps.record(mine, theirs.document, DocumentChange.Replace(theirs.document), EditPhase.Discrete, now())
        showStep(theirs.document, clearConflict = true)
        commit(theirs.document)
    }

    /**
     * Take the id a project from a link got on its first save. While it is still showing, it becomes
     * the tab's project and other tabs' saves to it are watched.
     */
    private suspend fun materialized(
        open: OpenProject,
        id: String,
    ) {
        if (current.value === open) {
            _state.update { state -> state.copy(project = ProjectRef.Persisted(id)) }
            environment.writeTabProject(id)
            watch(open, id)
        }
        rememberLastProject(id)
    }

    private suspend fun rememberLastProject(id: String) {
        preferences.update { prefs -> prefs.copy(lastProjectId = id) }
    }

    /**
     * A project whose changes from other tabs come back to the caller's dispatcher, the UI thread.
     */
    private suspend fun openHere(
        id: String?,
        name: String,
        held: ProjectRecord?,
    ): OpenProject {
        val context = currentCoroutineContext()[ContinuationInterceptor] ?: EmptyCoroutineContext
        return OpenProject(id, name, held, context, scope.coroutineContext[Job])
    }
}
