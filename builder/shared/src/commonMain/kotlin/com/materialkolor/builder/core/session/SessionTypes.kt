package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ProjectMeta
import com.materialkolor.builder.domain.persist.ProjectRecord
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The open project.
 */
internal sealed interface ProjectRef {
    /**
     * A saved project.
     */
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
 * The document showing and the number of the project it belongs to. Both are read from the one
 * [SessionState] the session publishes, so they always belong to the same project.
 *
 * @property[document] The theme being edited.
 * @property[generation] Counts the projects this tab has shown. It moves by one each time `open`,
 * `openShared` or `newProject` shows another. Saving a project opened from a link keeps its number,
 * since it is still the same project.
 */
internal data class ShownDocument(
    val document: ThemeDocument,
    val generation: Int,
)

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
 * Every step of the open project's history for the History list, a snapshot the session builds when
 * asked. [HistoryState] goes out on every frame of a drag, so the steps stay out of it.
 *
 * @property[cursor] How many of [steps] are applied. Zero is [start].
 * @property[now] The session clock when the snapshot was taken, in epoch milliseconds, to tell each
 * step's age by.
 * @property[start] The document before the oldest step, or the one showing when there are no steps.
 * @property[steps] Every step, oldest first. The ones past [cursor] were undone and can be jumped to
 * until the next edit drops them.
 */
internal data class Timeline(
    val cursor: Int,
    val now: Long,
    val start: ThemeDocument,
    val steps: List<HistoryEntry>,
) {
    init {
        require(cursor in 0..steps.size) { "Cursor is 0 to ${steps.size}, got $cursor" }
    }
}

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
    /**
     * Everything is saved.
     */
    data object Idle : SaveStatus

    /**
     * A save is waiting or under way.
     */
    data object Pending : SaveStatus

    /**
     * The last save did not land, even after the repository made room and tried again.
     */
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
 * @property[splashSeed] The theme's seed, for the poster on the next boot's splash.
 */
internal data class SessionColors(
    val previewColors: List<Argb>,
    val splashLight: Argb,
    val splashDark: Argb,
    val splashSeed: Argb,
) {
    init {
        require(previewColors.size == ProjectMeta.PREVIEW_COLORS) {
            "A thumbnail has ${ProjectMeta.PREVIEW_COLORS} colors, got ${previewColors.size}"
        }
    }
}

/**
 * How recent an edit in this tab has to be for another tab's save to raise a [Conflict].
 */
internal const val CONFLICT_WINDOW_MILLIS: Long = 2_000

/**
 * This flow seen through [transform], read and collected straight from it with no scope and no
 * dispatch in between, so its value always agrees with the value it came from.
 */
internal fun <T, R> StateFlow<T>.derived(transform: (T) -> R): StateFlow<R> = DerivedStateFlow(this, transform)

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class DerivedStateFlow<T, R>(
    private val source: StateFlow<T>,
    private val transform: (T) -> R,
) : StateFlow<R> {
    override val value: R
        get() = transform(source.value)

    override val replayCache: List<R>
        get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        source.map(transform).distinctUntilChanged().collect(collector)
        awaitCancellation()
    }
}
