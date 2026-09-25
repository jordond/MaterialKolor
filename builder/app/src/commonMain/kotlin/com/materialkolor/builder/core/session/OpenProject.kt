package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.HistoryRecord
import com.materialkolor.builder.domain.persist.ProjectRecord
import com.materialkolor.builder.domain.persist.ProjectViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext

/**
 * A project [ProjectSession] has shown. It is told apart by identity, so a save that finishes late
 * can check whether its project is still the one showing, and waiting saves are kept per project.
 *
 * @param[context] The thread changes from other tabs and the results of saves are applied on.
 * @param[parent] The app scope's job, so watching stops with the app.
 */
internal class OpenProject(
    id: String?,
    name: String,
    held: ProjectRecord?,
    val context: CoroutineContext,
    parent: Job?,
) {
    /**
     * The project's id, null until a transient project is first saved.
     */
    val id = MutableStateFlow(id)

    /**
     * The name the next save writes.
     */
    val name = MutableStateFlow(name)

    /**
     * The record as this tab last saved or read it, null before the first save.
     */
    val held = MutableStateFlow(held)

    /**
     * The history as it was last saved, so one that did not change is not written again.
     */
    val savedHistory = MutableStateFlow<HistoryRecord?>(null)

    /**
     * Watches other tabs while the project is showing.
     */
    val job = SupervisorJob(parent)
}

/**
 * A save of [project] waiting for autosave.
 *
 * @property[sequence] Counts up with every save handed to autosave, so the newest one is known.
 */
internal class PendingSave(
    val project: OpenProject,
    val sequence: Long,
    val document: ThemeDocument,
    val history: HistoryRecord,
    val colors: SessionColors,
)

/**
 * A view state of [project] waiting for autosave.
 */
internal class PendingView(
    val project: OpenProject,
    val state: ProjectViewState,
)
