package com.materialkolor.builder.domain.persist

import com.materialkolor.builder.domain.history.History
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectRecordTest {
    private val fixtures = PersistFixtures()

    @Test
    fun projectViewState_defaults_matchTheSpec() {
        val state = ProjectViewState()

        assertEquals(PreviewTab.App, state.tab)
        assertEquals(PreviewMode.Split, state.mode)
        assertEquals(0.5f, state.splitFraction)
        assertEquals(DeviceWidth.Tablet, state.deviceWidth)
        assertEquals(emptySet(), state.openFineTuneRows)
    }

    @Test
    fun projectViewState_splitOutsideTheFrame_isRefused() {
        listOf(-0.01f, 1.01f, Float.NaN).forEach { fraction ->
            assertFailsWith<IllegalArgumentException> { ProjectViewState(splitFraction = fraction) }
        }
        listOf(0f, 1f).forEach { fraction -> ProjectViewState(splitFraction = fraction) }
    }

    @Test
    fun historyRecord_moreThanHistoryPersists_isRefused() {
        assertEquals(History.PERSISTED, fixtures.history().entries.size)
        assertFailsWith<IllegalArgumentException> { fixtures.history(size = History.PERSISTED + 1) }
    }

    @Test
    fun historyRecord_whatHistoryPersists_readsBackIntoAHistory() {
        val record = fixtures.history(size = 3)

        val history = History(assertOk(HistoryRecord.Codec.decode(HistoryRecord.Codec.encode(record))).entries)

        assertEquals(record.entries, history.persisted())
    }

    @Test
    fun projectMeta_otherThanFourPreviewColors_isRefused() {
        val meta = fixtures.meta(index = 1)

        assertFailsWith<IllegalArgumentException> { meta.copy(previewColors = meta.previewColors.drop(1)) }
        assertFailsWith<IllegalArgumentException> { meta.copy(previewColors = meta.previewColors + meta.previewColors) }
    }
}
