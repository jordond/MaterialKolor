package com.materialkolor.builder.domain.history

import com.materialkolor.builder.domain.DocumentArb
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryEntryJsonTest {
    private val json = Json

    @Test
    fun historyEntry_randomEntries_surviveAJsonRoundTrip() {
        val arb = DocumentArb()
        val entries = ChangeKind.entries.flatMap { kind ->
            listOf(
                HistoryEntry(arb.nextDocument(), arb.nextDocument(), ChangeLabel(kind)),
                HistoryEntry(arb.nextDocument(), arb.nextDocument(), ChangeLabel(kind, detail = kind.name)),
            )
        }
        val serializer = ListSerializer(HistoryEntry.serializer())

        val text = json.encodeToString(serializer, entries)

        assertEquals(entries, json.decodeFromString(serializer, text))
    }

    // b-508
    @Test
    fun historyEntry_withTime_roundTrips() {
        val arb = DocumentArb()
        val entry = HistoryEntry(
            before = arb.nextDocument(),
            after = arb.nextDocument(),
            label = ChangeLabel(ChangeKind.Style, detail = "Vibrant"),
            at = 1_758_000_000_123L,
        )

        val text = json.encodeToString(HistoryEntry.serializer(), entry)

        assertEquals(entry, json.decodeFromString(HistoryEntry.serializer(), text))
    }

    @Test
    fun historyEntry_withoutTimeKey_readsAsNull() {
        val text = """{"before":{"seed":"#6750A4"},"after":{"seed":"#6750A4","amoled":true},""" +
            """"label":{"kind":"Amoled","detail":null}}"""

        val entry = json.decodeFromString(HistoryEntry.serializer(), text)

        assertNull(entry.at)
        assertEquals(ChangeLabel(ChangeKind.Amoled), entry.label)
        assertTrue(entry.after.amoled)
    }

    @Test
    fun historyEntry_keys_stayWhereSavedHistoriesExpectThem() {
        val wireNames = listOf(
            HistoryEntry.serializer().descriptor to listOf("before", "after", "label", "at"), // b-508
            ChangeLabel.serializer().descriptor to listOf("kind", "detail"),
        )

        wireNames.forEach { (descriptor, names) ->
            assertEquals(names, List(descriptor.elementsCount) { index -> descriptor.getElementName(index) })
        }
        assertEquals(
            ChangeKind.entries.map { kind -> kind.name },
            List(ChangeKind.entries.size) { index -> ChangeKind.serializer().descriptor.getElementName(index) },
        )
    }
}
