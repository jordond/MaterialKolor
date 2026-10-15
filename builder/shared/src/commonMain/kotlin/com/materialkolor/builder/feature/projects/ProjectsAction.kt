package com.materialkolor.builder.feature.projects

import com.materialkolor.builder.core.data.DeletedProject

/**
 * Everything the projects drawer and its banners can ask of [ProjectsModel].
 */
internal sealed interface ProjectsAction {
    /**
     * Open the project [id].
     */
    data class Open(
        val id: String,
    ) : ProjectsAction

    /**
     * Start a project from the defaults, or as a copy of the open one with [copyCurrent].
     */
    data class New(
        val copyCurrent: Boolean,
    ) : ProjectsAction

    /**
     * Call the project [id] [name].
     */
    data class Rename(
        val id: String,
        val name: String,
    ) : ProjectsAction

    /**
     * Copy the project [id] as a new project called [name].
     */
    data class Duplicate(
        val id: String,
        val name: String,
    ) : ProjectsAction

    /**
     * Delete the project [id], with an undo in a toast.
     */
    data class Delete(
        val id: String,
    ) : ProjectsAction

    /**
     * Bring back [deleted], from the undo toast its delete raised, even when others were deleted since.
     */
    data class UndoDelete(
        val deleted: DeletedProject,
    ) : ProjectsAction

    /**
     * Narrow the list to the projects whose name holds [query].
     */
    data class Search(
        val query: String,
    ) : ProjectsAction

    /**
     * Settle a clash with another tab, keeping this tab's document or taking the other's.
     */
    data class ResolveConflict(
        val keepMine: Boolean,
    ) : ProjectsAction

    /**
     * Save a theme opened from a link to the drawer now, rather than on its first edit.
     */
    data object SaveShared : ProjectsAction

    /**
     * The toast for [ProjectsModel.State.problem] went up, so it can be forgotten.
     */
    data object ProblemShown : ProjectsAction
}
