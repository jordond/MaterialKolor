package com.materialkolor.builder.core.data

import com.materialkolor.builder.core.platform.Store
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.HistoryRecord
import com.materialkolor.builder.domain.persist.ProjectIndex
import com.materialkolor.builder.domain.persist.ProjectMeta
import com.materialkolor.builder.domain.persist.ProjectRecord
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.domain.persist.StorageKey
import com.materialkolor.builder.domain.persist.StorageKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * The saved projects, each spread over its record, its undo history and its view state, and the
 * index the drawer lists them from.
 *
 * Every write returns the [StoreError] that stopped it, or null when it landed. When storage is
 * full the repository drops the histories of all but the [HISTORIES_KEPT] most recently updated
 * projects and tries once more, so a [StoreError.QuotaExceeded] that still comes back means the user
 * has to make room.
 *
 * The repository never resolves a theme. Whoever saves a project hands over its preview colors.
 *
 * @param[stores] Where the records are kept.
 * @param[tabId] This tab, written into every record it saves so its own writes can be told apart.
 * @param[now] The time in milliseconds since the epoch.
 * @param[newId] A fresh project id, not empty and without a colon.
 */
internal class ProjectRepository(
    private val stores: StoreFactory,
    private val tabId: String,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val newId: () -> String = { Uuid.random().toString() },
) {
    private val indexStore = stores.create(StorageKeys.INDEX, ProjectIndex.Codec, ProjectIndex())

    /** Every saved project, as the drawer lists them. */
    val index: Flow<ProjectIndex> = indexStore.data

    /** The project [id], or null when there is none or it could no longer be read. */
    suspend fun load(id: String): ProjectRecord? = projectStore(id).get().takeIf { record -> record.revision > UNSAVED }

    /**
     * Start a project called [name] holding [document], listed with [previewColors] as its
     * thumbnail.
     */
    suspend fun create(
        name: String,
        document: ThemeDocument,
        previewColors: List<Argb>,
    ): Creation {
        val record = ProjectRecord(newId(), name, document, revision = FIRST_REVISION, writerTab = tabId)
        return add(record, previewColors)
    }

    /**
     * Save [record] and list it with [previewColors] as its thumbnail.
     *
     * The saved revision is one past the larger of the stored revision and the one [record] carries,
     * and the index entry is stamped with the time, so the drawer and other tabs can tell it moved.
     *
     * Only a project the drawer lists is saved. An autosave that comes in after the project was
     * deleted is dropped, writes nothing and returns null. When the delete lands while the save is
     * under way, the save takes the record it wrote back out, so the project stays deleted.
     */
    suspend fun save(
        record: ProjectRecord,
        previewColors: List<Argb>,
    ): StoreError? {
        requirePreviewColors(previewColors)
        if (meta(record.id) == null) return null
        val error = write(record.id) {
            projectStore(record.id).update { stored ->
                record.copy(revision = maxOf(stored.revision, record.revision) + 1, writerTab = tabId)
            }
        }
        if (error != null) return error
        var listed = true
        val indexError = write(record.id) {
            indexStore.update { index ->
                listed = index.lists(record.id)
                index.withSaved(record, previewColors, now())
            }
        }
        if (listed) return indexError
        projectStore(record.id).delete()
        return null
    }

    /**
     * Call the project [id] [name]. Nothing happens when the drawer does not list it.
     *
     * Only the name changes, in the record and in its listing, so a save that lands just before keeps
     * its document. A listed project whose record can no longer be read is renamed in the drawer
     * only, and its record is left as it is.
     *
     * The session that has the project open holds a record of its own. It has to take the new name
     * into that record, or its next autosave writes the old name back. B-215 or the session owner
     * wires that.
     */
    suspend fun rename(
        id: String,
        name: String,
    ): StoreError? {
        if (meta(id) == null) return null
        if (load(id) != null) renameRecord(id, name)?.let { error -> return error }
        return write(id) { indexStore.update { index -> index.withName(id, name) } }
    }

    /**
     * Copy the project [id] under a new id as [name], with its view state but not its history.
     *
     * Null when there is no such project. The copy counts as created once it is saved and listed.
     * Its view state is copied after that if storage takes it, and when it does not the copy opens
     * with the default view.
     */
    suspend fun duplicate(
        id: String,
        name: String,
    ): Creation? {
        val source = load(id) ?: return null
        val meta = meta(id) ?: return null
        val copy = source.copy(id = newId(), name = name, revision = FIRST_REVISION, writerTab = tabId)
        val created = add(copy, meta.previewColors)
        if (created is Creation.Failed) return created
        val view = viewState(id)
        if (view != ProjectViewState()) write(copy.id) { viewStore(copy.id).update { view } }
        return created
    }

    /**
     * Remove the project [id] with its history and view state, and hand back what [restore] needs to
     * undo it.
     *
     * The project is gone once it leaves the index. Removing its record, history and view state
     * after that is best effort, and the first of them that fails comes back as
     * [Deletion.Deleted.cleanupError]. A project whose record can no longer be read can still be
     * deleted. Reading it sets that text aside first, but only as best the store can, so on a full
     * storage the text can stay under its key and go with the project.
     *
     * When a newer build saved the record, the history or the view state, it reads as missing or
     * empty here but is left where it is (D41). The delete is turned down with [Deletion.NewerBuild]
     * before anything is written, since removing it would lose it for good and an undo from here
     * could not put it back.
     */
    suspend fun delete(id: String): Deletion {
        val projects = indexStore.get().projects
        val position = projects.indexOfFirst { meta -> meta.id == id }
        if (position < 0) return Deletion.NotListed
        if (fromNewerBuild(id)) return Deletion.NewerBuild
        val record = load(id)
        val deleted = DeletedProject(
            meta = projects[position],
            position = position,
            record = record,
            history = loadHistory(id),
            viewState = viewState(id),
        )
        val error = write(id) { indexStore.update { index -> index.without(id) } }
        if (error != null) return Deletion.Failed(error)
        val cleanupError = listOfNotNull(
            projectStore(id).delete(),
            historyStore(id).delete(),
            viewStore(id).delete(),
        ).firstOrNull()
        return Deletion.Deleted(deleted, cleanupError)
    }

    /** Put a project [delete] removed back, where the drawer listed it. */
    suspend fun restore(deleted: DeletedProject): StoreError? {
        val id = deleted.meta.id
        val record = deleted.record
        if (record != null) write(id) { projectStore(id).update { record } }?.let { error -> return error }
        if (deleted.history != HistoryRecord()) {
            write(id) { historyStore(id).update { deleted.history } }?.let { error -> return error }
        }
        if (deleted.viewState != ProjectViewState()) {
            write(id) { viewStore(id).update { deleted.viewState } }?.let { error -> return error }
        }
        return write(id) { indexStore.update { index -> index.withRestored(deleted) } }
    }

    /** The undo history of project [id], empty when it has none. */
    suspend fun loadHistory(id: String): HistoryRecord = historyStore(id).get()

    /**
     * Save [history] as the undo history of project [id].
     *
     * Only the [HISTORIES_KEPT] most recently updated projects keep theirs, so this drops the others.
     * The history of [id] itself stays, since it is the project being worked on.
     */
    suspend fun saveHistory(
        id: String,
        history: HistoryRecord,
    ): StoreError? {
        val error = write(id) { historyStore(id).update { history } }
        pruneHistories(keep = id)
        return error
    }

    /** How the preview of project [id] was left, or the defaults when it never was. */
    suspend fun viewState(id: String): ProjectViewState = viewStore(id).get()

    /** Remember [state] as how the preview of project [id] was left. */
    suspend fun saveViewState(
        id: String,
        state: ProjectViewState,
    ): StoreError? = write(id) { viewStore(id).update { state } }

    /**
     * The project [id] each time another tab saves it. A delete in another tab shows up in [index]
     * instead.
     */
    fun changes(id: String): Flow<ProjectRecord> =
        stores.externalChanges
            .filter { key -> key == StorageKey.Project(id) }
            .mapNotNull { load(id) }

    private suspend fun add(
        record: ProjectRecord,
        previewColors: List<Argb>,
    ): Creation {
        requirePreviewColors(previewColors)
        write(record.id) { projectStore(record.id).update { record } }?.let { error -> return Creation.Failed(error) }
        val error = write(record.id) { indexStore.update { index -> index.withAdded(record, previewColors, now()) } }
        if (error == null) return Creation.Created(record)
        projectStore(record.id).delete()
        return Creation.Failed(error)
    }

    /**
     * Give the record of project [id] the name [name], leaving everything else it holds alone.
     *
     * When the record went away since it was last read, the update wrote the default in its place,
     * and that is taken back out.
     */
    private suspend fun renameRecord(
        id: String,
        name: String,
    ): StoreError? {
        var gone = false
        val error = write(id) {
            projectStore(id).update { stored ->
                gone = stored.revision == UNSAVED
                if (gone) stored else stored.copy(name = name, revision = stored.revision + 1, writerTab = tabId)
            }
        }
        if (error == null && gone) projectStore(id).delete()
        return error
    }

    /**
     * Whether a newer build saved the record, the history or the view state of project [id], which
     * this build leaves where they are (D41). Asking only reads.
     */
    suspend fun fromNewerBuild(id: String): Boolean =
        projectStore(id).fromNewerBuild() || historyStore(id).fromNewerBuild() || viewStore(id).fromNewerBuild()

    /** Run [attempt], and when storage is full drop the old histories and run it once more. */
    private suspend fun write(
        id: String,
        attempt: suspend () -> StoreError?,
    ): StoreError? {
        val error = attempt()
        if (error != StoreError.QuotaExceeded) return error
        pruneHistories(keep = id)
        return attempt()
    }

    private suspend fun pruneHistories(keep: String) {
        val projects = indexStore.get().projects
        val recent = projects
            .sortedByDescending { meta -> meta.updatedAt }
            .take(HISTORIES_KEPT)
            .mapTo(mutableSetOf()) { meta -> meta.id }
        projects
            .filter { meta -> meta.id != keep && meta.id !in recent }
            .forEach { meta -> historyStore(meta.id).delete() }
    }

    private suspend fun meta(id: String): ProjectMeta? = indexStore.get().projects.find { meta -> meta.id == id }

    private fun projectStore(id: String): Store<ProjectRecord> =
        stores.create(StorageKeys.project(id), ProjectRecord.Codec, unsaved(id))

    private fun historyStore(id: String): Store<HistoryRecord> =
        stores.create(StorageKeys.history(id), HistoryRecord.Codec, HistoryRecord())

    private fun viewStore(id: String): Store<ProjectViewState> =
        stores.create(StorageKeys.view(id), ProjectViewState.Codec, ProjectViewState())
}

/**
 * What [ProjectRepository.create] and [ProjectRepository.duplicate] came to.
 */
internal sealed interface Creation {
    /** The project was saved and listed. */
    data class Created(
        val record: ProjectRecord,
    ) : Creation

    /** Storage would not take the project. */
    data class Failed(
        val error: StoreError,
    ) : Creation
}

/**
 * What [ProjectRepository.delete] came to.
 */
internal sealed interface Deletion {
    /**
     * The project left the drawer.
     *
     * @property[project] What [ProjectRepository.restore] needs to put it back.
     * @property[cleanupError] The first error met while removing its record, history and view state,
     *   or null when they all went.
     */
    data class Deleted(
        val project: DeletedProject,
        val cleanupError: StoreError?,
    ) : Deletion

    /** The drawer does not list the project, so nothing was removed. */
    data object NotListed : Deletion

    /** A newer build saved the project, so this one leaves it where it is (D41). */
    data object NewerBuild : Deletion

    /** Storage would not take the change to the index, so nothing was removed. */
    data class Failed(
        val error: StoreError,
    ) : Deletion
}

/**
 * Everything a deleted project was, so [ProjectRepository.restore] can put it back while the undo
 * toast is up.
 *
 * @property[meta] How the drawer listed it.
 * @property[position] Where it sat in the index.
 * @property[record] The project, or null when it could no longer be read.
 * @property[history] Its undo history.
 * @property[viewState] How its preview was left.
 */
internal data class DeletedProject(
    val meta: ProjectMeta,
    val position: Int,
    val record: ProjectRecord?,
    val history: HistoryRecord,
    val viewState: ProjectViewState,
)

/** How many projects keep their undo history. */
internal const val HISTORIES_KEPT: Int = 10

private fun ProjectIndex.lists(id: String): Boolean = projects.any { meta -> meta.id == id }

/** The index with [record] listed at the end, created and updated at [time]. */
private fun ProjectIndex.withAdded(
    record: ProjectRecord,
    previewColors: List<Argb>,
    time: Long,
): ProjectIndex = copy(projects = projects + record.toMeta(previewColors, createdAt = time, updatedAt = time))

/** The index with the entry of [record] brought up to date at [time]. It never lists a project it did not. */
private fun ProjectIndex.withSaved(
    record: ProjectRecord,
    previewColors: List<Argb>,
    time: Long,
): ProjectIndex =
    copy(
        projects = projects.map { meta ->
            if (meta.id != record.id) return@map meta
            record.toMeta(previewColors, createdAt = meta.createdAt, updatedAt = time)
        },
    )

private fun ProjectIndex.withName(
    id: String,
    name: String,
): ProjectIndex = copy(projects = projects.map { meta -> if (meta.id == id) meta.copy(name = name) else meta })

private fun ProjectIndex.without(id: String): ProjectIndex =
    copy(projects = projects.filterNot { meta -> meta.id == id })

private fun ProjectIndex.withRestored(deleted: DeletedProject): ProjectIndex {
    if (lists(deleted.meta.id)) return this
    val position = deleted.position.coerceAtMost(projects.size)
    return copy(projects = projects.take(position) + deleted.meta + projects.drop(position))
}

private fun ProjectRecord.toMeta(
    previewColors: List<Argb>,
    createdAt: Long,
    updatedAt: Long,
): ProjectMeta =
    ProjectMeta(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        library = document.library,
        expressive = document.expressive,
        previewColors = previewColors,
    )

private fun requirePreviewColors(previewColors: List<Argb>) {
    require(previewColors.size == ProjectMeta.PREVIEW_COLORS) {
        "A thumbnail has ${ProjectMeta.PREVIEW_COLORS} colors, got ${previewColors.size}"
    }
}

/** What a project reads as before it is first saved, told apart by its revision. */
private fun unsaved(id: String): ProjectRecord =
    ProjectRecord(id, name = "", document = ThemeDocument(seed = DEFAULT_SEED), revision = UNSAVED, writerTab = "")

private const val UNSAVED: Long = 0

private const val FIRST_REVISION: Long = 1
