package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.data.Creation
import com.materialkolor.builder.core.data.ProjectRepository
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.domain.persist.ProjectRecord
import com.materialkolor.builder.domain.persist.ProjectViewState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The write path of [ProjectSession], where its autosaves go into storage.
 *
 * It sets the save status the session publishes through [setStatus], and only the newest save of
 * the project showing reports into it. The first save of a project opened from a link gives it an id here. What that
 * changes about the open project, its ref, the tab's project and the watch on other tabs, is up to
 * the session through [materialized].
 *
 * @param[tabId] This tab, written into a record the first time this tab saves it.
 * @param[lock] Held around every write, and by the session around a rename, so the two never mix.
 * @param[showing] The project the session shows now.
 * @param[conflicted] Whether a conflict with another tab's save is up.
 * @param[viewState] How the preview of the project showing is set up.
 * @param[materialized] Called once a project from a link has its id, before its history is saved.
 * @param[setStatus] Publishes whether the open project is saved.
 */
internal class SessionWrites(
    private val projects: ProjectRepository,
    private val tabId: String,
    private val lock: Mutex,
    private val showing: () -> OpenProject,
    private val conflicted: () -> Boolean,
    private val viewState: () -> ProjectViewState,
    private val materialized: suspend (open: OpenProject, id: String) -> Unit,
    private val setStatus: (SaveStatus) -> Unit,
) {
    /**
     * The number of the newest save handed to autosave. Only the UI thread reads or writes it.
     */
    private var newestSave = 0L

    /**
     * Mark a new save as waiting, or as held while a conflict is up, and hand back its number. Call
     * it on the UI thread.
     */
    fun nextSave(): Long {
        setStatus(if (conflicted()) SaveStatus.Held else SaveStatus.Pending)
        return ++newestSave
    }

    /**
     * Show [status] straight away, when another project shows or a new one could not be saved.
     */
    fun resetStatus(status: SaveStatus) {
        setStatus(status)
    }

    /**
     * Write [save], or hold it back while a conflict is up for its project. A held save stops
     * reading as saving until the conflict is settled, which schedules a new save.
     */
    suspend fun write(save: PendingSave): Boolean {
        if (conflicted() && showing() === save.project) {
            withContext(save.project.context) { report(save, SaveStatus.Held) }
            return false
        }
        val error = lock.withLock { persist(save) }
        withContext(save.project.context) {
            report(save, if (error != null) SaveStatus.Failed(error) else SaveStatus.Idle)
        }
        return error == null
    }

    suspend fun writeView(pending: PendingView): Boolean {
        // A transient project has nowhere to keep its view yet. Its first save writes it.
        val id = pending.project.facts.value.id ?: return true
        return projects.saveViewState(id, pending.state) == null
    }

    /**
     * Show how [save] went. It runs on the thread that edits, so it cannot cover the Pending of a
     * newer save, and only the newest save of the open project reports.
     */
    private fun report(
        save: PendingSave,
        status: SaveStatus,
    ) {
        if (showing() !== save.project || save.sequence != newestSave) return
        setStatus(status)
    }

    private suspend fun persist(save: PendingSave): StoreError? {
        val id = save.project.facts.value.id ?: return materialize(save)
        saveRecord(id, save)?.let { error -> return error }
        return saveHistory(id, save)
    }

    /**
     * Save a transient project for the first time, under the name it was opened with.
     */
    private suspend fun materialize(save: PendingSave): StoreError? {
        val open = save.project
        val creation = projects.create(open.facts.value.name, save.document, save.colors.previewColors)
        val record = when (creation) {
            is Creation.Created -> creation.record
            is Creation.Failed -> return creation.error
        }
        open.facts.update { facts -> facts.copy(id = record.id, held = record) }
        val view = if (showing() === open) viewState() else ProjectViewState()
        if (view != ProjectViewState()) projects.saveViewState(record.id, view)
        materialized(open, record.id)
        return saveHistory(record.id, save)
    }

    private suspend fun saveRecord(
        id: String,
        save: PendingSave,
    ): StoreError? {
        val open = save.project
        val facts = open.facts.value
        val name = facts.name
        val held = facts.held
        if (held != null && held.document == save.document && held.name == name) return null
        val record = held?.copy(name = name, document = save.document)
            ?: ProjectRecord(id, name, save.document, revision = 0, writerTab = tabId)
        val error = projects.save(record, save.colors.previewColors)
        if (error == null) {
            val saved = record.copy(revision = record.revision + 1)
            open.facts.update { latest -> if (latest.held === held) latest.copy(held = saved) else latest }
        }
        return error
    }

    private suspend fun saveHistory(
        id: String,
        save: PendingSave,
    ): StoreError? {
        val open = save.project
        if (open.facts.value.savedHistory == save.history) return null
        val error = projects.saveHistory(id, save.history)
        if (error == null) open.facts.update { facts -> facts.copy(savedHistory = save.history) }
        return error
    }
}
