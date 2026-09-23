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
     * The saved revision is one past whatever is stored, and the index entry is stamped with the
     * time, so the drawer and other tabs can tell it moved.
     */
    suspend fun save(
        record: ProjectRecord,
        previewColors: List<Argb>,
    ): StoreError? {
        requirePreviewColors(previewColors)
        val error = write(record.id) {
            projectStore(record.id).update { stored ->
                record.copy(revision = maxOf(stored.revision, record.revision) + 1, writerTab = tabId)
            }
        }
        if (error != null) return error
        return write(record.id) { indexStore.update { index -> index.withSaved(record, previewColors, now()) } }
    }

    /** Call the project [id] [name]. Nothing happens when there is no such project. */
    suspend fun rename(
        id: String,
        name: String,
    ): StoreError? {
        val record = load(id) ?: return null
        val meta = meta(id) ?: return null
        return save(record.copy(name = name), meta.previewColors)
    }

    /**
     * Copy the project [id] under a new id as [name], with its view state but not its history.
     *
     * Null when there is no such project.
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
        if (view == ProjectViewState()) return created
        return write(copy.id) { viewStore(copy.id).update { view } }?.let(Creation::Failed) ?: created
    }

    /**
     * Remove the project [id] with its history and view state, and hand back what [restore] needs to
     * undo it.
     *
     * Null when the drawer does not list [id] or storage would not take the change, and then nothing
     * was removed. A project whose record can no longer be read can still be deleted.
     */
    suspend fun delete(id: String): DeletedProject? {
        val projects = indexStore.get().projects
        val position = projects.indexOfFirst { meta -> meta.id == id }
        if (position < 0) return null
        val deleted = DeletedProject(
            meta = projects[position],
            position = position,
            record = load(id),
            history = loadHistory(id),
            viewState = viewState(id),
        )
        val error = write(id) { indexStore.update { index -> index.without(id) } }
        if (error != null) return null
        projectStore(id).delete()
        historyStore(id).delete()
        viewStore(id).delete()
        return deleted
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
        val error = write(record.id) { projectStore(record.id).update { record } }
            ?: write(record.id) { indexStore.update { index -> index.withSaved(record, previewColors, now()) } }
        return if (error == null) Creation.Created(record) else Creation.Failed(error)
    }

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

private fun ProjectIndex.withSaved(
    record: ProjectRecord,
    previewColors: List<Argb>,
    time: Long,
): ProjectIndex {
    val listed = projects.find { meta -> meta.id == record.id }
    val meta = ProjectMeta(
        id = record.id,
        name = record.name,
        createdAt = listed?.createdAt ?: time,
        updatedAt = time,
        library = record.document.library,
        expressive = record.document.expressive,
        previewColors = previewColors,
    )
    if (listed == null) return copy(projects = projects + meta)
    return copy(projects = projects.map { other -> if (other.id == record.id) meta else other })
}

private fun ProjectIndex.without(id: String): ProjectIndex =
    copy(projects = projects.filterNot { meta -> meta.id == id })

private fun ProjectIndex.withRestored(deleted: DeletedProject): ProjectIndex {
    if (projects.any { meta -> meta.id == deleted.meta.id }) return this
    val position = deleted.position.coerceAtMost(projects.size)
    return copy(projects = projects.take(position) + deleted.meta + projects.drop(position))
}

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
