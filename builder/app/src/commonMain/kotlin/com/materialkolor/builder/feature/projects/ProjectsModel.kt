package com.materialkolor.builder.feature.projects

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.data.Creation
import com.materialkolor.builder.core.data.DeletedProject
import com.materialkolor.builder.core.data.Deletion
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.data.ProjectRepository
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.persist.ProjectMeta
import dev.stateholder.extensions.viewmodel.StateViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * The projects drawer, the saved projects newest first, and the banners that hang off the open
 * project, a clash with another tab and a theme opened from a link.
 *
 * Every rename goes through the session, so the open project's next save keeps the new name. A
 * delete stays undoable for [UNDO_WINDOW_MILLIS], and deleting the open project opens the newest
 * other one first, or a fresh one when there is none. Persistent storage is asked for once, when the
 * drawer first lists a second project, however it got there.
 */
@Stable
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class ProjectsModel(
    private val session: ProjectSession,
    private val projects: ProjectRepository,
    private val preferences: PreferencesRepository,
    private val environment: Environment,
    private val clock: Clock,
) : StateViewModel<ProjectsModel.State>(
        State(
            open = session.project.value,
            conflict = session.conflict.value != null,
            storageAvailable = environment.storageAvailable,
        ),
    ) {
    private var undoTimer: Job? = null
    private var persistAsked = false

    init {
        session.project.mergeState { state, project -> state.copy(open = project) }
        session.conflict.mergeState { state, conflict -> state.copy(conflict = conflict != null) }
        viewModelScope.launch {
            projects.index.collect { index ->
                val listed = index.projects.sortedByDescending { meta -> meta.updatedAt }
                updateState { state -> state.copy(projects = listed) }
                if (listed.size >= PERSIST_AT) requestPersistOnce()
            }
        }
    }

    /** The time now in milliseconds since the epoch, for each project's age. */
    fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    fun handle(action: ProjectsAction) {
        when (action) {
            is ProjectsAction.Open -> open(action.id)
            is ProjectsAction.New -> newProject(action.copyCurrent)
            is ProjectsAction.Rename -> rename(action.id, action.name)
            is ProjectsAction.Duplicate -> duplicate(action.id, action.name)
            is ProjectsAction.Delete -> delete(action.id)
            ProjectsAction.UndoDelete -> undoDelete()
            is ProjectsAction.Search -> updateState { state -> state.copy(query = action.query) }
            is ProjectsAction.ResolveConflict -> session.resolveConflict(action.keepMine)
            ProjectsAction.SaveShared -> saveShared()
            ProjectsAction.ProblemShown -> updateState { state -> state.copy(problem = null) }
        }
    }

    private fun open(id: String) {
        viewModelScope.launch {
            if (!session.open(id)) report(ProjectsProblem.NotOpened)
        }
    }

    private fun newProject(copyCurrent: Boolean) {
        viewModelScope.launch {
            session.newProject(copyCurrent)
            if (session.saveStatus.value is SaveStatus.Failed) report(ProjectsProblem.NotCreated)
        }
    }

    private fun rename(
        id: String,
        name: String,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (session.rename(id, trimmed) != null) report(ProjectsProblem.NotRenamed)
        }
    }

    /** Copy what is saved of [id], after anything still waiting to be saved has gone out. */
    private fun duplicate(
        id: String,
        name: String,
    ) {
        viewModelScope.launch {
            session.flush().join()
            if (projects.duplicate(id, name.trim().ifEmpty { name }) is Creation.Failed) {
                report(ProjectsProblem.NotCreated)
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            if (state.value.openId == id) moveOffDoomed(id)
            when (val deletion = projects.delete(id)) {
                is Deletion.Deleted -> holdForUndo(deletion.project)
                Deletion.NotListed -> Unit
                is Deletion.Failed -> report(ProjectsProblem.NotDeleted)
            }
        }
    }

    /** Open the newest project other than [id], or a fresh one when [id] is the only one. */
    private suspend fun moveOffDoomed(id: String) {
        val next = state.value.projects.firstOrNull { meta -> meta.id != id }
        if (next != null && session.open(next.id)) return
        session.newProject(copyCurrent = false)
    }

    private fun holdForUndo(deleted: DeletedProject) {
        undoTimer?.cancel()
        updateState { state -> state.copy(pendingDeletion = deleted) }
        undoTimer = viewModelScope.launch {
            delay(UNDO_WINDOW_MILLIS)
            updateState { state ->
                if (state.pendingDeletion ===
                    deleted
                ) {
                    state.copy(pendingDeletion = null)
                } else {
                    state
                }
            }
        }
    }

    private fun undoDelete() {
        val deleted = state.value.pendingDeletion ?: return
        undoTimer?.cancel()
        undoTimer = null
        updateState { state -> state.copy(pendingDeletion = null) }
        viewModelScope.launch {
            if (projects.restore(deleted) != null) report(ProjectsProblem.NotRestored)
        }
    }

    private fun saveShared() {
        viewModelScope.launch {
            session.saveTransient().join()
            if (session.saveStatus.value is SaveStatus.Failed) report(ProjectsProblem.NotCreated)
        }
    }

    /**
     * Ask the platform to keep stored data, once for this browser. The preference is written before
     * the ask, so a reload halfway through never asks again.
     */
    private suspend fun requestPersistOnce() {
        if (persistAsked || !environment.storageAvailable) return
        persistAsked = true
        if (preferences.current().persistRequested) return
        if (preferences.update { prefs -> prefs.copy(persistRequested = true) } != null) return
        environment.requestPersist()
    }

    private fun report(problem: ProjectsProblem) {
        updateState { state -> state.copy(problem = problem) }
    }

    /**
     * @property[projects] Every saved project, the most recently saved first.
     * @property[query] What the search field holds.
     * @property[open] The open project.
     * @property[conflict] Whether another tab's save clashes with an edit made here.
     * @property[storageAvailable] Whether anything saved here outlives the session.
     * @property[pendingDeletion] The project deleted last, while its undo is still up.
     * @property[problem] Something storage turned down, for a toast, or null.
     */
    @Immutable
    data class State(
        val projects: List<ProjectMeta> = emptyList(),
        val query: String = "",
        val open: ProjectRef? = null,
        val conflict: Boolean = false,
        val storageAvailable: Boolean = true,
        val pendingDeletion: DeletedProject? = null,
        val problem: ProjectsProblem? = null,
    ) {
        /** The open project's id, or null when it is not saved. */
        val openId: String?
            get() = (open as? ProjectRef.Persisted)?.id

        /** Whether the open theme came from a link and is not saved yet. */
        val transient: Boolean
            get() = open is ProjectRef.Transient

        /** Whether there are enough projects for the search field to show. */
        val searchable: Boolean
            get() = projects.size > SEARCH_THRESHOLD

        /** The projects the list shows, narrowed by [query] while the search field shows. */
        val shown: List<ProjectMeta>
            get() {
                val needle = query.trim()
                if (!searchable || needle.isEmpty()) return projects
                return projects.filter { meta -> meta.name.contains(needle, ignoreCase = true) }
            }
    }
}

/**
 * Something storage turned down, told to the user in a toast.
 */
internal enum class ProjectsProblem {
    NotOpened,
    NotCreated,
    NotRenamed,
    NotDeleted,
    NotRestored,
}

/** How long a deleted project can be brought back. */
internal const val UNDO_WINDOW_MILLIS: Long = 8_000

/** How many projects the drawer lists before it offers a search. */
internal const val SEARCH_THRESHOLD: Int = 8

/** The number of saved projects at which persistent storage is asked for. */
private const val PERSIST_AT: Int = 2
