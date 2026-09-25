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
import com.materialkolor.builder.core.platform.Quarantined
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.persist.ProjectMeta
import com.materialkolor.builder.domain.persist.QuarantineReason
import dev.stateholder.extensions.viewmodel.StateViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * The projects drawer, the saved projects newest first, and the banners that hang off the open
 * project, a clash with another tab, a theme that is not saved yet and data a newer build saved.
 *
 * Every rename goes through the session, so the open project's next save keeps the new name. The
 * model keeps the last deletion for its undo toast until the next delete or its undo, with no timer
 * of its own, since the toast keeps its own time. An undo from an older toast still brings its
 * project back. Deleting the open project opens the newest other one once the delete lands, or a
 * fresh one when there is none, and a refused delete leaves it open. Persistent storage is asked for once, when the drawer first lists a second project,
 * however it got there.
 *
 * The model is the one collector of [StoreFactory.quarantined]. A record from a newer build raises
 * a banner asking for a reload, and text that could not be read at all raises a toast saying it was
 * set aside.
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
    stores: StoreFactory,
) : StateViewModel<ProjectsModel.State>(
        State(
            open = session.project.value,
            conflict = session.conflict.value != null,
            storageAvailable = environment.storageAvailable,
        ),
    ) {
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
        viewModelScope.launch {
            stores.quarantined.collect(::setAside)
        }
    }

    /**
     * The time now in milliseconds since the epoch, for each project's age.
     */
    fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    fun handle(action: ProjectsAction) {
        when (action) {
            is ProjectsAction.Open -> open(action.id)
            is ProjectsAction.New -> newProject(action.copyCurrent)
            is ProjectsAction.Rename -> rename(action.id, action.name)
            is ProjectsAction.Duplicate -> duplicate(action.id, action.name)
            is ProjectsAction.Delete -> delete(action.id)
            is ProjectsAction.UndoDelete -> undoDelete(action.deleted)
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

    /**
     * Copy what is saved of [id], after anything still waiting to be saved has gone out. A project
     * that is gone or can no longer be read has nothing to copy.
     */
    private fun duplicate(
        id: String,
        name: String,
    ) {
        viewModelScope.launch {
            session.flush().join()
            when (projects.duplicate(id, name.trim().ifEmpty { name })) {
                is Creation.Created -> Unit
                is Creation.Failed -> report(ProjectsProblem.NotCreated)
                null -> report(ProjectsProblem.NotDuplicated)
            }
        }
    }

    /**
     * Delete [id] once anything still waiting to be saved has gone out, so its undo holds the latest
     * edit. The open project is only left once the delete has landed, so a refused delete keeps it
     * open.
     */
    private fun delete(id: String) {
        viewModelScope.launch {
            session.flush().join()
            when (val deletion = projects.delete(id)) {
                is Deletion.Deleted -> {
                    updateState { state -> state.copy(lastDeletion = deletion.project) }
                    if (state.value.openId == id) moveOffDeleted(id)
                }
                Deletion.NotListed -> {
                    // The list no longer holds it, so nothing was removed and there is nothing to undo or report.
                }
                Deletion.NewerBuild -> {
                    report(ProjectsProblem.NotDeletedNewer)
                }
                is Deletion.Failed -> {
                    report(ProjectsProblem.NotDeleted)
                }
            }
        }
    }

    /**
     * Open the newest project other than [id], or a fresh one when [id] was the only one.
     */
    private suspend fun moveOffDeleted(id: String) {
        val next = state.value.projects.firstOrNull { meta -> meta.id != id }
        if (next != null && session.open(next.id)) return
        session.newProject(copyCurrent = false)
    }

    /**
     * Put [deleted] back where it was listed, whether or not it is still the last deletion.
     */
    private fun undoDelete(deleted: DeletedProject) {
        updateState { state -> if (state.lastDeletion == deleted) state.copy(lastDeletion = null) else state }
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

    /**
     * A record a newer build saved stays where it is and asks for a reload, while text that could not
     * be read at all has been moved to its quarantine key by now.
     */
    private fun setAside(quarantined: Quarantined) {
        when (quarantined.reason) {
            QuarantineReason.NewerSchema -> {
                updateState { state -> state.copy(newerData = true) }
            }
            QuarantineReason.Unreadable,
            QuarantineReason.MigrationFailed,
            QuarantineReason.WrongShape,
            -> {
                report(ProjectsProblem.SetAside)
            }
        }
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
     * @property[lastDeletion] The project deleted last, until the next delete or its undo, for the
     *   undo toast.
     * @property[newerData] Whether a newer build of the builder saved data this one leaves alone
     *   until a reload.
     * @property[problem] Something storage turned down, for a toast, or null.
     */
    @Immutable
    data class State(
        val projects: List<ProjectMeta> = emptyList(),
        val query: String = "",
        val open: ProjectRef? = null,
        val conflict: Boolean = false,
        val storageAvailable: Boolean = true,
        val lastDeletion: DeletedProject? = null,
        val newerData: Boolean = false,
        val problem: ProjectsProblem? = null,
    ) {
        /**
         * The open project's id, or null when it is not saved.
         */
        val openId: String?
            get() = (open as? ProjectRef.Persisted)?.id

        /**
         * Whether the open theme is not saved yet, one from a link or one storage turned down.
         */
        val transient: Boolean
            get() = open is ProjectRef.Transient

        /**
         * Whether there are enough projects for the search field to show.
         */
        val searchable: Boolean
            get() = projects.size > SEARCH_THRESHOLD

        /**
         * The projects the list shows, narrowed by [query] while the search field shows.
         */
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
    NotDuplicated,
    NotRenamed,
    NotDeleted,

    /**
     * A newer build saved the project, so only a reload can delete it.
     */
    NotDeletedNewer,
    NotRestored,

    /**
     * Saved data could not be read, so it was moved aside rather than lost.
     */
    SetAside,
}

/**
 * How many projects the drawer lists before it offers a search.
 */
internal const val SEARCH_THRESHOLD: Int = 8

/**
 * The number of saved projects at which persistent storage is asked for.
 */
private const val PERSIST_AT: Int = 2
