package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ProjectViewState

/**
 * Everything [ProjectSession] publishes about the open project, as one value.
 *
 * Showing another project replaces the whole value at once, so no reader ever sees one project's
 * document next to another project's ref, view or conflict.
 *
 * @property[project] Which project is open.
 * @property[document] The theme being edited.
 * @property[generation] Counts the projects this tab has shown. It moves by one each time another
 * project shows, and saving a project opened from a link keeps it, since that is still the same project.
 * @property[history] What undo and redo can do right now.
 * @property[view] How the preview of the open project is set up.
 * @property[conflict] Another tab's save that clashes with an edit made here, or null.
 * @property[saveStatus] Whether the open project is saved.
 */
internal data class SessionState(
    val project: ProjectRef,
    val document: ThemeDocument,
    val generation: Int,
    val history: HistoryState,
    val view: ProjectViewState,
    val conflict: Conflict?,
    val saveStatus: SaveStatus,
) {
    companion object {
        /**
         * What a session holds before its first project shows.
         */
        fun initial(): SessionState =
            SessionState(
                project = ProjectRef.Transient(ShareCodec.encode(ThemeDocument.Default)),
                document = ThemeDocument.Default,
                generation = 0,
                history = HistoryState(),
                view = ProjectViewState(),
                conflict = null,
                saveStatus = SaveStatus.Idle,
            )
    }
}
