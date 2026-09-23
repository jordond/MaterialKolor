package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.data.Creation
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.data.ProjectRepository
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The one owner of the open project, its document and its undo history.
 *
 * Models read [document], [history], [project] and [viewState] and merge them as they need, and
 * nothing else holds document state. Every change goes through [edit], [undo] or [redo]. Every
 * rename goes through [rename], so the record the session holds never writes an old name back.
 *
 * Committed edits are saved [AUTOSAVE_DELAY_MILLIS] after the last one, so a drag saves once after
 * it is released. [flush] writes whatever is waiting straight away, on lifecycle STOP and on
 * `pagehide`. A project opened from a link stays [ProjectRef.Transient] and unsaved until the first
 * edit, which saves it under the link's project name or [SHARED_THEME_NAME].
 *
 * When another tab saves the open project, its document comes in as an undo step, so Undo brings
 * this tab's back. If this tab edited in the last [CONFLICT_WINDOW_MILLIS] a [Conflict] comes up
 * instead, and this tab's saves wait until [resolveConflict] settles it.
 *
 * [colorsOf] resolves a theme, so it only ever runs on the caller's thread, the UI thread, and the
 * colors go along with the save it schedules. Changes from other tabs are applied on the thread
 * that opened the project for the same reason. The app scope only writes.
 *
 * Deleting the open project is up to the drawer, which opens another project first. A save that
 * lands after a delete is dropped by the repository, and the session does not notice.
 *
 * Preference writes, the last open project and what a legacy link asked for, are best effort. A
 * full storage is ignored there, with no prune and no retry (D33).
 *
 * @param[colorsOf] The thumbnail and splash colors of a theme.
 * @param[scope] The app scope, where saves run.
 * @param[now] The time in milliseconds, for merging undo steps and for the conflict window.
 */
internal class ProjectSession(
    private val projects: ProjectRepository,
    private val preferences: PreferencesRepository,
    private val environment: Environment,
    private val colorsOf: (ThemeDocument) -> SessionColors,
    private val scope: CoroutineScope,
    private val now: () -> Long,
) {
    private val current = MutableStateFlow(OpenProject(id = null, name = "", held = null, EmptyCoroutineContext))
    private val writeLock = Mutex()
    private val autosave = Autosave(scope, write = ::write)
    private val viewAutosave = Autosave(scope, write = ::writeView)
    private var steps = History()
    private var lastEditAt: Long? = null

    private val _document = MutableStateFlow(ThemeDocument.Default)
    private val _history = MutableStateFlow(HistoryState())
    private val _project = MutableStateFlow<ProjectRef>(ProjectRef.Transient(ShareCodec.encode(ThemeDocument.Default)))
    private val _viewState = MutableStateFlow(ProjectViewState())
    private val _conflict = MutableStateFlow<Conflict?>(null)
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)

    /** The theme being edited. */
    val document: StateFlow<ThemeDocument> = _document.asStateFlow()

    /** What undo and redo can do right now. */
    val history: StateFlow<HistoryState> = _history.asStateFlow()

    /** Which project is open. */
    val project: StateFlow<ProjectRef> = _project.asStateFlow()

    /** How the preview of the open project is set up. */
    val viewState: StateFlow<ProjectViewState> = _viewState.asStateFlow()

    /** Another tab's save that clashes with an edit made here, or null. */
    val conflict: StateFlow<Conflict?> = _conflict.asStateFlow()

    /** Whether the open project is saved. A failure comes after the repository pruned and tried again. */
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    /**
     * Open the project [BootResolver] picks for [route], and apply what a legacy link asked for.
     *
     * Every listed project is read to match a link against them. Afterwards the app puts `/` back in
     * the address bar.
     *
     * @param[tabProjectId] The project this tab had open before a reload, or null.
     * @return Why the address did not open what it asked for, or null when it did.
     */
    suspend fun boot(
        route: Route,
        tabProjectId: String?,
    ): BootNotice? {
        val plan = BootResolver.resolve(route, tabProjectId, preferences.current().lastProjectId, listedRecords())
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
     * A [EditPhase.Dragging] step only moves the document. The save waits for the release.
     */
    fun edit(
        change: DocumentChange,
        phase: EditPhase,
    ) {
        val before = _document.value
        val after = change.apply(before)
        val time = now()
        steps.record(before, after, change, phase, time)
        lastEditAt = time
        _document.value = after
        publishHistory()
        if (phase == EditPhase.Dragging || (phase == EditPhase.Discrete && after == before)) return
        commit(after)
    }

    /** Step back once. Nothing happens when there is nothing to undo. */
    fun undo() {
        moveTo(steps.undo() ?: return)
    }

    /** Step forward once. Nothing happens when there is nothing to redo. */
    fun redo() {
        moveTo(steps.redo() ?: return)
    }

    /**
     * Save what is waiting, then open the project [id] with its history and view state.
     *
     * @return False when there is no such project, and the open one stays.
     */
    suspend fun open(id: String): Boolean {
        flushAll()
        val record = projects.load(id) ?: return false
        val saved = projects.loadHistory(id)
        val view = projects.viewState(id)
        val open = OpenProject(id, record.name, held = record, callerContext())
        open.savedHistory.value = saved
        show(open, ProjectRef.Persisted(id), record.document, History(saved.entries), view)
        rememberLastProject(id)
        return true
    }

    /**
     * Open the theme a share [code] carries.
     *
     * A saved project whose document encodes to the same code opens instead, so the same link opened
     * twice is the same project. Finding it reads every listed project.
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
        val local = BootResolver.matching(link.document, listedRecords(), preferredId = current.value.id.value)
        if (local != null && open(local.id)) return null
        showShared(BootStart.Shared(code, link.document, link.projectName))
        return null
    }

    /** Save what is waiting, then start a project from the defaults or, with [copyCurrent], from this one. */
    suspend fun newProject(copyCurrent: Boolean) {
        val document = if (copyCurrent) _document.value else ThemeDocument.Default
        val view = if (copyCurrent) _viewState.value else ProjectViewState()
        flushAll()
        startNew(document, view)
    }

    /**
     * Call the project [id] [name]. When it is the open project its next save keeps the new name.
     *
     * @return Why the rename did not land, or null when it did.
     */
    suspend fun rename(
        id: String,
        name: String,
    ): StoreError? =
        writeLock.withLock {
            val open = current.value.takeIf { open -> open.id.value == id }
            open?.name?.value = name
            val error = projects.rename(id, name)
            if (error == null) open?.held?.update { held -> held?.copy(name = name, revision = held.revision + 1) }
            error
        }

    /** Change how the preview is set up, saved once the changes stop. */
    fun updateView(block: (ProjectViewState) -> ProjectViewState) {
        val state = _viewState.updateAndGet(block)
        viewAutosave.schedule(PendingView(current.value, state))
    }

    /**
     * Settle the [conflict], keeping this tab's document when [keepMine] is true or taking the other
     * tab's as an undo step when it is false.
     */
    fun resolveConflict(keepMine: Boolean) {
        val conflict = _conflict.value ?: return
        val open = current.value
        _conflict.value = null
        if (keepMine) {
            open.held.value = conflict.theirs
            commit(_document.value)
        } else {
            adopt(open, conflict.theirs)
        }
    }

    /** Write whatever is waiting now. The write starts on the caller's thread, so `pagehide` can use it. */
    fun flush(): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) { flushAll() }

    private suspend fun flushAll() {
        autosave.flush()
        viewAutosave.flush()
    }

    private fun moveTo(document: ThemeDocument) {
        lastEditAt = now()
        _document.value = document
        publishHistory()
        commit(document)
    }

    /** Write the splash colors of [document] and schedule its save. */
    private fun commit(document: ThemeDocument) {
        val colors = colorsOf(document)
        environment.writeSplashColors(colors.splashLight, colors.splashDark)
        _saveStatus.value = SaveStatus.Pending
        autosave.schedule(PendingSave(current.value, document, HistoryRecord(steps.persisted()), colors))
    }

    private fun publishHistory() {
        _history.value = HistoryState(steps.canUndo, steps.canRedo, steps.undoLabel, steps.redoLabel)
    }

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
        _conflict.value = null
        _document.value = document
        _viewState.value = view
        _project.value = ref
        publishHistory()
        environment.writeSplashColors(colors.splashLight, colors.splashDark)
        open.id.value?.let { id -> watch(open, id) }
    }

    private suspend fun showShared(shared: BootStart.Shared) {
        val name = shared.projectName?.takeIf { name -> name.isNotBlank() } ?: SHARED_THEME_NAME
        val open = OpenProject(id = null, name, held = null, callerContext())
        show(open, ProjectRef.Transient(shared.code), shared.document, History(), ProjectViewState())
    }

    /** Create a project from [document], or show it unsaved when storage will not take it. */
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
                val open = OpenProject(id, name, held = creation.record, callerContext())
                open.savedHistory.value = HistoryRecord()
                show(open, ProjectRef.Persisted(id), document, History(), view, colors)
                rememberLastProject(id)
            }
            is Creation.Failed -> {
                _saveStatus.value = SaveStatus.Failed(creation.error)
                val open = OpenProject(id = null, name, held = null, callerContext())
                val code = runCatching { ShareCodec.encode(document, name) }.getOrDefault("")
                show(open, ProjectRef.Transient(code), document, History(), view, colors)
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
        if (current.value !== open || incoming.writerTab == environment.tabId) return
        val held = open.held.value
        if (held != null && incoming.revision <= held.revision) return
        open.name.value = incoming.name
        if (incoming.document == _document.value) {
            open.held.value = incoming
            _conflict.value = null
            return
        }
        val editedLately = lastEditAt?.let { at -> now() - at < CONFLICT_WINDOW_MILLIS } == true
        if (editedLately) _conflict.value = Conflict(incoming) else adopt(open, incoming)
    }

    /** Take the other tab's [theirs] as an undo step, so Undo brings this tab's document back. */
    private fun adopt(
        open: OpenProject,
        theirs: ProjectRecord,
    ) {
        val mine = _document.value
        open.held.value = theirs
        open.name.value = theirs.name
        steps.record(mine, theirs.document, DocumentChange.Replace(theirs.document), EditPhase.Discrete, now())
        _document.value = theirs.document
        publishHistory()
        commit(theirs.document)
    }

    private suspend fun write(save: PendingSave): Boolean {
        if (_conflict.value != null && current.value === save.project) return false
        val error = writeLock.withLock { persist(save) }
        _saveStatus.value = when {
            error != null -> SaveStatus.Failed(error)
            autosave.hasPending -> SaveStatus.Pending
            else -> SaveStatus.Idle
        }
        return error == null
    }

    private suspend fun persist(save: PendingSave): StoreError? {
        val id = save.project.id.value ?: return materialize(save)
        saveRecord(id, save)?.let { error -> return error }
        return saveHistory(id, save)
    }

    /** Save a transient project for the first time, under the name it was opened with. */
    private suspend fun materialize(save: PendingSave): StoreError? {
        val open = save.project
        val creation = projects.create(open.name.value, save.document, save.colors.previewColors)
        val record = when (creation) {
            is Creation.Created -> creation.record
            is Creation.Failed -> return creation.error
        }
        open.id.value = record.id
        open.held.value = record
        val showing = current.value === open
        val view = if (showing) _viewState.value else ProjectViewState()
        if (view != ProjectViewState()) projects.saveViewState(record.id, view)
        if (showing) {
            _project.value = ProjectRef.Persisted(record.id)
            watch(open, record.id)
        }
        rememberLastProject(record.id)
        return saveHistory(record.id, save)
    }

    private suspend fun saveRecord(
        id: String,
        save: PendingSave,
    ): StoreError? {
        val open = save.project
        val name = open.name.value
        val held = open.held.value
        if (held != null && held.document == save.document && held.name == name) return null
        val record = held?.copy(name = name, document = save.document)
            ?: ProjectRecord(id, name, save.document, revision = 0, writerTab = environment.tabId)
        val error = projects.save(record, save.colors.previewColors)
        if (error == null) {
            val saved = record.copy(revision = record.revision + 1)
            open.held.update { latest -> if (latest === held) saved else latest }
        }
        return error
    }

    private suspend fun saveHistory(
        id: String,
        save: PendingSave,
    ): StoreError? {
        val open = save.project
        if (open.savedHistory.value == save.history) return null
        val error = projects.saveHistory(id, save.history)
        if (error == null) open.savedHistory.value = save.history
        return error
    }

    private suspend fun writeView(pending: PendingView): Boolean {
        // A transient project has nowhere to keep its view yet. Its first save writes it.
        val id = pending.project.id.value ?: return true
        return projects.saveViewState(id, pending.state) == null
    }

    private suspend fun rememberLastProject(id: String) {
        preferences.update { prefs -> prefs.copy(lastProjectId = id) }
    }

    private suspend fun listedRecords(): List<ProjectRecord> =
        projects.index
            .first()
            .projects
            .mapNotNull { meta -> projects.load(meta.id) }

    /** The dispatcher the caller runs on, so work for it can come back to the same thread. */
    private suspend fun callerContext(): CoroutineContext =
        currentCoroutineContext()[ContinuationInterceptor] ?: EmptyCoroutineContext

    /**
     * The project on screen. It is told apart by identity, so a save that finishes late can check
     * whether its project is still the one showing.
     *
     * @param[context] The thread changes from other tabs are applied on.
     */
    private inner class OpenProject(
        id: String?,
        name: String,
        held: ProjectRecord?,
        val context: CoroutineContext,
    ) {
        /** The project's id, null until a transient project is first saved. */
        val id = MutableStateFlow(id)

        /** The name the next save writes. */
        val name = MutableStateFlow(name)

        /** The record as this tab last saved or read it, null before the first save. */
        val held = MutableStateFlow(held)

        /** The history as it was last saved, so one that did not change is not written again. */
        val savedHistory = MutableStateFlow<HistoryRecord?>(null)

        /** Watches other tabs while the project is showing. */
        val job = SupervisorJob(scope.coroutineContext[Job])
    }

    private class PendingSave(
        val project: OpenProject,
        val document: ThemeDocument,
        val history: HistoryRecord,
        val colors: SessionColors,
    )

    private class PendingView(
        val project: OpenProject,
        val state: ProjectViewState,
    )
}
