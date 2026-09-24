package com.materialkolor.builder.core.data

import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.HistoryRecord
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.ProjectMeta
import com.materialkolor.builder.domain.persist.ProjectRecord
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.domain.persist.StorageKeys
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectRepositoryTest {
    private var time = START
    private var ids = 0
    private val stores = InMemoryStoreFactory(now = { time })
    private val repository = ProjectRepository(stores, tabId = TAB, now = { time }, newId = { "p${++ids}" })

    @Test
    fun create_newProject_savesItAndListsIt() =
        runTest {
            val record = created("Sunset")

            record.revision shouldBe 1
            record.writerTab shouldBe TAB
            repository.load(record.id) shouldBe record
            repository.index.first().projects shouldBe listOf(
                ProjectMeta(record.id, "Sunset", createdAt = START, updatedAt = START, previewColors = PREVIEW),
            )
        }

    @Test
    fun save_later_bumpsTheRevisionAndIndexUpdatedAt() =
        runTest {
            val record = created()
            time += MINUTE

            repository.save(record.copy(document = DOCUMENT.copy(amoled = true)), OTHER_PREVIEW) shouldBe null

            val saved = repository.load(record.id).shouldNotBeNull()
            saved.revision shouldBe 2
            saved.document.amoled shouldBe true
            val meta = repository.index
                .first()
                .projects
                .single()
            meta.createdAt shouldBe START
            meta.updatedAt shouldBe START + MINUTE
            meta.previewColors shouldBe OTHER_PREVIEW
        }

    @Test
    fun save_afterDelete_writesNothingAndRestoreStillWorks() =
        runTest {
            val listed = List(3) { created().id }
            val record = repository.load(listed[1]).shouldNotBeNull()
            val deleted = deletedProject(record.id)

            repository.save(record.copy(document = DOCUMENT.copy(amoled = true)), PREVIEW) shouldBe null

            listedIds() shouldBe listOf(listed[0], listed[2])
            stores.textAt(StorageKeys.project(record.id)) shouldBe null
            repository.restore(deleted) shouldBe null
            listedIds() shouldBe listed
            repository.load(record.id) shouldBe record
        }

    @Test
    fun save_deleteLandsBeforeTheIndexUpdate_takesTheRecordBackOut() =
        runTest {
            val record = created()
            stores.beforeNextUpdate(StorageKeys.INDEX) { deletedProject(record.id) }

            repository.save(record.copy(document = DOCUMENT.copy(amoled = true)), PREVIEW) shouldBe null

            listedIds() shouldBe emptyList()
            stores.keys shouldBe setOf(StorageKeys.INDEX)
        }

    @Test
    fun saveViewStateAndHistory_afterDelete_writeNothing() =
        runTest {
            val record = created()
            deletedProject(record.id)

            repository.saveViewState(record.id, VIEW) shouldBe null
            repository.saveHistory(record.id, HISTORY) shouldBe null

            stores.keys shouldBe setOf(StorageKeys.INDEX)
        }

    @Test
    fun saveViewState_deleteLandsWhileItWrites_takesTheKeyBackOut() =
        runTest {
            val record = created()
            stores.beforeNextUpdate(StorageKeys.view(record.id)) { deletedProject(record.id) }

            repository.saveViewState(record.id, VIEW) shouldBe null

            listedIds() shouldBe emptyList()
            stores.keys shouldBe setOf(StorageKeys.INDEX)
        }

    @Test
    fun saveHistory_deleteLandsWhileItWrites_takesTheKeyBackOut() =
        runTest {
            val record = created()
            stores.beforeNextUpdate(StorageKeys.history(record.id)) { deletedProject(record.id) }

            repository.saveHistory(record.id, HISTORY) shouldBe null

            stores.keys shouldBe setOf(StorageKeys.INDEX)
        }

    @Test
    fun rename_listedProject_renamesTheRecordAndItsListing() =
        runTest {
            val record = created("Before")

            repository.rename(record.id, "After") shouldBe null

            repository.load(record.id)?.name shouldBe "After"
            repository.index
                .first()
                .projects
                .single()
                .name shouldBe "After"
        }

    @Test
    fun rename_saveLandsJustBefore_keepsTheSavedDocumentAndTheNewName() =
        runTest {
            val record = created("Before")
            val saved = DOCUMENT.copy(amoled = true)
            stores.beforeNextUpdate(StorageKeys.project(record.id)) {
                repository.save(record.copy(document = saved), PREVIEW) shouldBe null
            }

            repository.rename(record.id, "After") shouldBe null

            val stored = repository.load(record.id).shouldNotBeNull()
            stored.document shouldBe saved
            stored.name shouldBe "After"
            stored.revision shouldBe 3
            repository.index
                .first()
                .projects
                .single()
                .name shouldBe "After"
        }

    @Test
    fun rename_unreadableRecord_renamesOnlyTheListing() =
        runTest {
            val record = created("Before")
            stores.seed(StorageKeys.project(record.id), "{ broken")

            repository.rename(record.id, "After") shouldBe null

            repository.index
                .first()
                .projects
                .single()
                .name shouldBe "After"
            stores.textAt(StorageKeys.project(record.id)) shouldBe null
        }

    @Test
    fun create_indexWriteFails_removesTheRecordAgain() =
        runTest {
            stores.beforeNextUpdate(StorageKeys.INDEX) { stores.failNextUpdates(1, StoreError.Unavailable) }

            repository.create("Theme", DOCUMENT, PREVIEW) shouldBe Creation.Failed(StoreError.Unavailable)

            stores.keys shouldBe emptySet()
        }

    @Test
    fun duplicate_listedProject_copiesItAndItsViewUnderANewId() =
        runTest {
            val record = created("Original")
            repository.saveViewState(record.id, VIEW)

            val copy = repository.duplicate(record.id, "Copy").shouldBeInstanceOf<Creation.Created>().record

            copy.id shouldBe "p2"
            copy.name shouldBe "Copy"
            copy.document shouldBe record.document
            repository.viewState(copy.id) shouldBe VIEW
            repository.index
                .first()
                .projects
                .map { meta -> meta.id } shouldBe listOf(record.id, copy.id)
        }

    @Test
    fun duplicate_viewStateWriteFails_stillCreatesTheCopy() =
        runTest {
            val record = created("Original")
            repository.saveViewState(record.id, VIEW)
            stores.beforeNextUpdate(StorageKeys.view("p2")) { stores.failNextUpdates(1, StoreError.Unavailable) }

            val copy = repository.duplicate(record.id, "Copy").shouldBeInstanceOf<Creation.Created>().record

            repository.load(copy.id) shouldBe copy
            repository.viewState(copy.id) shouldBe ProjectViewState()
            listedIds() shouldBe listOf(record.id, copy.id)
        }

    @Test
    fun delete_thenRestore_bringsTheProjectBackWithItsHistory() =
        runTest {
            val record = created()
            repository.saveHistory(record.id, HISTORY)
            repository.saveViewState(record.id, VIEW)

            val deletion = repository.delete(record.id).shouldBeInstanceOf<Deletion.Deleted>()
            val deleted = deletion.project

            deletion.cleanupError shouldBe null
            repository.load(record.id) shouldBe null
            repository.index.first().projects shouldBe emptyList()
            stores.keys shouldBe setOf(StorageKeys.INDEX)

            repository.restore(deleted) shouldBe null

            repository.load(record.id) shouldBe record
            repository.loadHistory(record.id) shouldBe HISTORY
            repository.viewState(record.id) shouldBe VIEW
            repository.index.first().projects shouldBe listOf(deleted.meta)
        }

    @Test
    fun restore_projectFromTheMiddle_putsItBackWhereItWas() =
        runTest {
            val listed = List(3) { created().id }

            repository.restore(deletedProject(listed[1]))

            listedIds() shouldBe listed
        }

    @Test
    fun delete_unreadableRecord_stillRemovesTheListing() =
        runTest {
            val record = created()
            stores.seed(StorageKeys.project(record.id), "{ broken")

            repository.load(record.id) shouldBe null
            deletedProject(record.id).record shouldBe null
            repository.index.first().projects shouldBe emptyList()
        }

    @Test
    fun delete_recordANewerBuildSaved_isTurnedDownAndWritesNothing() =
        runTest {
            val record = created()
            repository.saveHistory(record.id, HISTORY)
            stores.seed(StorageKeys.project(record.id), NEWER_RECORD)
            val texts = stores.keys.associateWith { key -> stores.textAt(key) }

            repository.delete(record.id) shouldBe Deletion.NewerBuild

            stores.keys.associateWith { key -> stores.textAt(key) } shouldBe texts
            stores.textAt(StorageKeys.project(record.id)) shouldBe NEWER_RECORD
            listedIds() shouldBe listOf(record.id)
        }

    // b-310aa
    @Test
    fun delete_onlyTheHistoryFromANewerBuild_isTurnedDownAndWritesNothing() =
        runTest {
            val record = created()
            stores.seed(StorageKeys.history(record.id), NEWER_HISTORY)
            val texts = stores.keys.associateWith { key -> stores.textAt(key) }

            repository.fromNewerBuild(record.id) shouldBe true
            repository.delete(record.id) shouldBe Deletion.NewerBuild

            stores.keys.associateWith { key -> stores.textAt(key) } shouldBe texts
            repository.load(record.id) shouldBe record
            listedIds() shouldBe listOf(record.id)
        }

    @Test
    fun delete_notListed_returnsNotListed() =
        runTest {
            repository.delete("missing") shouldBe Deletion.NotListed
        }

    @Test
    fun delete_indexWriteRefused_removesNothing() =
        runTest {
            val record = created()
            stores.failNextUpdates(1, StoreError.Unavailable)

            repository.delete(record.id) shouldBe Deletion.Failed(StoreError.Unavailable)

            repository.load(record.id) shouldBe record
            listedIds() shouldBe listOf(record.id)
        }

    @Test
    fun delete_recordRemovalFails_stillDeletesAndReportsIt() =
        runTest {
            val record = created()
            repository.saveHistory(record.id, HISTORY)
            stores.failNextDeletes(1, StoreError.Unavailable)

            val deletion = repository.delete(record.id).shouldBeInstanceOf<Deletion.Deleted>()

            deletion.cleanupError shouldBe StoreError.Unavailable
            listedIds() shouldBe emptyList()
            stores.keys shouldBe setOf(StorageKeys.INDEX, StorageKeys.project(record.id))
        }

    @Test
    fun saveHistory_elevenProjects_keepsOnlyTheTenMostRecentlyUpdated() =
        runTest {
            val records = createdOverTime(count = 11)

            records.forEach { record -> repository.saveHistory(record.id, HISTORY) }

            records.count { record -> stores.textAt(StorageKeys.history(record.id)) != null } shouldBe HISTORIES_KEPT
            repository.loadHistory(records.first().id) shouldBe HistoryRecord()
        }

    @Test
    fun saveHistory_leastRecentlyUpdatedOfEleven_keepsItsOwnHistory() =
        runTest {
            val records = createdOverTime(count = 11)
            records.drop(1).forEach { record ->
                stores.seed(StorageKeys.history(record.id), HistoryRecord.Codec.encode(HISTORY))
            }

            repository.saveHistory(records.first().id, HISTORY) shouldBe null

            records.forEach { record -> repository.loadHistory(record.id) shouldBe HISTORY }
        }

    @Test
    fun save_quotaExceededOnce_prunesTheOldestHistoryAndRetries() =
        runTest {
            val records = createdOverTime(count = 11)
            records.forEach { record ->
                stores.seed(StorageKeys.history(record.id), HistoryRecord.Codec.encode(HISTORY))
            }
            stores.failNextUpdates(1, StoreError.QuotaExceeded)

            repository.save(records.last().copy(name = "Renamed"), PREVIEW) shouldBe null

            repository.load(records.last().id)?.name shouldBe "Renamed"
            repository.loadHistory(records.first().id) shouldBe HistoryRecord()
            records.drop(1).forEach { record -> repository.loadHistory(record.id) shouldBe HISTORY }
        }

    @Test
    fun save_quotaExceededAfterPruning_returnsQuotaExceeded() =
        runTest {
            val record = created()
            stores.failNextUpdates(2, StoreError.QuotaExceeded)

            repository.save(record.copy(name = "Renamed"), PREVIEW) shouldBe StoreError.QuotaExceeded

            repository.load(record.id) shouldBe record
        }

    @Test
    fun changes_anotherTabSavesTheProject_emitsTheirRecord() =
        runTest {
            val record = created()
            val seen = mutableListOf<ProjectRecord>()
            backgroundScope.launch { repository.changes(record.id).toList(seen) }
            runCurrent()
            val theirs = record.copy(name = "Theirs", revision = 2, writerTab = "other")

            stores.writeFromAnotherTab(StorageKeys.project(record.id), ProjectRecord.Codec.encode(theirs))
            stores.writeFromAnotherTab(StorageKeys.project("elsewhere"), ProjectRecord.Codec.encode(theirs))
            runCurrent()

            seen shouldBe listOf(theirs)
        }

    private suspend fun created(name: String = "Theme"): ProjectRecord =
        repository.create(name, DOCUMENT, PREVIEW).shouldBeInstanceOf<Creation.Created>().record

    private suspend fun deletedProject(id: String): DeletedProject =
        repository.delete(id).shouldBeInstanceOf<Deletion.Deleted>().project

    private suspend fun listedIds(): List<String> =
        repository.index
            .first()
            .projects
            .map { meta -> meta.id }

    private suspend fun createdOverTime(count: Int): List<ProjectRecord> =
        List(count) { index -> "Theme $index" }.map { name ->
            time += MINUTE
            created(name)
        }
}

private const val TAB = "this-tab"

private const val START = 1_700_000_000_000

private const val MINUTE = 60_000L

/** A project record from a build whose schema this one does not know yet. */
private const val NEWER_RECORD = """{"schema":999,"data":{"id":"p1"}}"""

private const val NEWER_HISTORY = """{"schema":999,"data":{"entries":[]}}"""

private val DOCUMENT = ThemeDocument(seed = DEFAULT_SEED)

private val PREVIEW = List(ProjectMeta.PREVIEW_COLORS) { index -> Argb(0x202020 * (index + 1)) }

private val OTHER_PREVIEW = List(ProjectMeta.PREVIEW_COLORS) { index -> Argb(0x101010 * (index + 1)) }

private val HISTORY = HistoryRecord(
    listOf(
        HistoryEntry(before = DOCUMENT, after = DOCUMENT.copy(amoled = true), label = ChangeLabel(ChangeKind.Seed)),
    ),
)

private val VIEW = ProjectViewState(tab = PreviewTab.Palettes, splitFraction = 0.3f)
