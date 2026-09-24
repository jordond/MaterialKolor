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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor
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
 * edit, which saves it under the link's project name or [sharedThemeName]. Each project keeps its
 * own waiting save, so one that did not land survives opening another project and goes out again
 * on the next flush.
 *
 * When another tab saves the open project, its document comes in as an undo step, so Undo brings
 * this tab's back. If this tab edited in the last [CONFLICT_WINDOW_MILLIS] a [Conflict] comes up
 * instead, and this tab's saves wait until [resolveConflict] settles it. A newer save from the other
 * tab while the conflict is up takes the conflict's place and is never taken in silently.
 *
 * A conflict never loses anyone's work (D36). When this tab has to save with a conflict up, on
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
 * full storage is ignored there, with no prune and no retry (D33).
 *
 * @param[colorsOf] The thumbnail and splash colors of a theme.
 * @param[sharedThemeName] What a theme from a link without a name is saved as.
 * @param[scope] The app scope, where saves run.
 * @param[now] The time in milliseconds, for merging undo steps and for the conflict window.
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
    private val autosave = Autosave(scope, keyOf = { save -> save.project }, write = ::write)
    private val viewAutosave = Autosave(scope, keyOf = { pending -> pending.project }, write = ::writeView)
    private var steps = History()
    private var lastEditAt: Long? = null

    /** The number of the newest save handed to autosave. Only the UI thread reads or writes it. */
    private var newestSave = 0L

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

    // b-216b

    /** The open project's name, the one its next save writes. It follows renames and other tabs. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val projectName: Flow<String> = current.flatMapLatest { open -> open.name }

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
        val plan = BootResolver.resolve(route, tabProjectId, lastProjectId, savedProjects(route))
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
        open.savedHistory.value = saved
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
        val local = BootResolver.matching(link.document, listedRecords(), preferredId = current.value.id.value)
        if (local != null && open(local.id)) return null
        showShared(BootStart.Shared(code, link.document, link.projectName))
        return null
    }

    /**
     * Save what is waiting, keeping this tab's document over an open conflict, then start a project
     * from the defaults or, with [copyCurrent], from this one.
     */
    suspend fun newProject(copyCurrent: Boolean) {
        val document = if (copyCurrent) _document.value else ThemeDocument.Default
        val view = if (copyCurrent) _viewState.value else ProjectViewState()
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
            val open = current.value.takeIf { open -> open.id.value == id }
            val before = open?.name?.getAndUpdate { name }
            val error = projects.rename(id, name)
            if (error == null) {
                open?.held?.update { held -> held?.copy(name = name, revision = held.revision + 1) }
            } else if (open != null && before != null) {
                // A name another tab saved in the meantime stays.
                open.name.compareAndSet(expect = name, update = before)
            }
            error
        }

    // b-310

    /**
     * Save a project opened from a link now rather than on its first edit, for "Save to my
     * projects", along with anything else waiting. A project that is saved already only flushes.
     */
    fun saveTransient(): Job {
        if (_project.value is ProjectRef.Transient) commit(_document.value)
        return flush()
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
        if (_conflict.value != null) resolveConflict(keepMine = true)
        autosave.cancelTimer()
        viewAutosave.cancelTimer()
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
        newestSave++
        autosave.schedule(PendingSave(current.value, newestSave, document, HistoryRecord(steps.persisted()), colors))
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
        _saveStatus.value = SaveStatus.Idle
        _document.value = document
        _viewState.value = view
        _project.value = ref
        publishHistory()
        environment.writeSplashColors(colors.splashLight, colors.splashDark)
        environment.writeTabProject(open.id.value)
        open.id.value?.let { id -> watch(open, id) }
    }

    private suspend fun showShared(shared: BootStart.Shared) {
        val name = shared.projectName?.takeIf { name -> name.isNotBlank() } ?: sharedThemeName()
        val open = openHere(id = null, name, held = null)
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
                val open = openHere(id, name, held = creation.record)
                open.savedHistory.value = HistoryRecord()
                show(open, ProjectRef.Persisted(id), document, History(), view, colors)
                rememberLastProject(id)
            }
            is Creation.Failed -> {
                val open = openHere(id = null, name, held = null)
                val code = runCatching { ShareCodec.encode(document, name) }.getOrDefault("")
                show(open, ProjectRef.Transient(code), document, History(), view, colors)
                _saveStatus.value = SaveStatus.Failed(creation.error)
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
        // A conflict that is up takes the newer save, so Keep mine and Load theirs both see it.
        val conflicted = _conflict.value != null
        val editedLately = lastEditAt?.let { at -> now() - at < CONFLICT_WINDOW_MILLIS } == true
        if (conflicted || editedLately) _conflict.value = Conflict(incoming) else adopt(open, incoming)
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
        withContext(save.project.context) { report(save, error) }
        return error == null
    }

    /**
     * Show how [save] went. It runs on the thread that edits, so it cannot cover the Pending of a
     * newer save, and only the newest save of the open project reports.
     */
    private fun report(
        save: PendingSave,
        error: StoreError?,
    ) {
        if (current.value !== save.project || save.sequence != newestSave) return
        _saveStatus.value = if (error != null) SaveStatus.Failed(error) else SaveStatus.Idle
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
            environment.writeTabProject(record.id)
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

    /** What boot needs about the saved projects for [route], the index alone unless it is a link. */
    private suspend fun savedProjects(route: Route): SavedProjects {
        if (!BootResolver.readsRecords(route)) return SavedProjects.Listed(listedIds())
        return SavedProjects.Read(listedRecords())
    }

    private suspend fun listedIds(): List<String> =
        projects.index
            .first()
            .projects
            .map { meta -> meta.id }

    private suspend fun listedRecords(): List<ProjectRecord> = listedIds().mapNotNull { id -> projects.load(id) }

    /** A project whose changes from other tabs come back to the caller's dispatcher, the UI thread. */
    private suspend fun openHere(
        id: String?,
        name: String,
        held: ProjectRecord?,
    ): OpenProject {
        val context = currentCoroutineContext()[ContinuationInterceptor] ?: EmptyCoroutineContext
        return OpenProject(id, name, held, context, scope.coroutineContext[Job])
    }
}
