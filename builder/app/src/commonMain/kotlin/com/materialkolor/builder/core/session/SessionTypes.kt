package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.persist.ProjectMeta
import com.materialkolor.builder.domain.persist.ProjectRecord

/**
 * The open project.
 */
internal sealed interface ProjectRef {
    /** A saved project. */
    data class Persisted(
        val id: String,
    ) : ProjectRef

    /**
     * A theme that is not saved yet, from a link or from a new project storage would not take.
     *
     * @property[fromCode] The share code it came from.
     */
    data class Transient(
        val fromCode: String,
    ) : ProjectRef
}

/**
 * What the undo and redo buttons can do.
 *
 * @property[undoLabel] What undo would take back, or null when there is nothing.
 * @property[redoLabel] What redo would bring back, or null when there is nothing.
 */
internal data class HistoryState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoLabel: ChangeLabel? = null,
    val redoLabel: ChangeLabel? = null,
)

/**
 * Another tab saved the open project while this tab was editing it. The UI offers Load theirs and
 * Keep mine.
 *
 * @property[theirs] What the other tab saved.
 */
internal data class Conflict(
    val theirs: ProjectRecord,
)

/**
 * Whether the open project's latest changes are saved.
 */
internal sealed interface SaveStatus {
    /** Everything is saved. */
    data object Idle : SaveStatus

    /** A save is waiting or under way. */
    data object Pending : SaveStatus

    /** The last save did not land, even after the repository made room and tried again. */
    data class Failed(
        val error: StoreError,
    ) : SaveStatus
}

/**
 * The colors a save needs from a resolved theme.
 *
 * @property[previewColors] The four colors of the drawer thumbnail.
 * @property[splashLight] The chrome surface in light mode, for the next boot's splash.
 * @property[splashDark] The chrome surface in dark mode, for the next boot's splash.
 */
internal data class SessionColors(
    val previewColors: List<Argb>,
    val splashLight: Argb,
    val splashDark: Argb,
) {
    init {
        require(previewColors.size == ProjectMeta.PREVIEW_COLORS) {
            "A thumbnail has ${ProjectMeta.PREVIEW_COLORS} colors, got ${previewColors.size}"
        }
    }
}

/** How recent an edit in this tab has to be for another tab's save to raise a [Conflict]. */
internal const val CONFLICT_WINDOW_MILLIS: Long = 2_000
