package com.materialkolor.builder.domain.history

import com.materialkolor.builder.domain.DocumentArb
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun historyEntry_keys_stayWhereSavedHistoriesExpectThem() {
        val wireNames = listOf(
            HistoryEntry.serializer().descriptor to listOf("before", "after", "label"),
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
