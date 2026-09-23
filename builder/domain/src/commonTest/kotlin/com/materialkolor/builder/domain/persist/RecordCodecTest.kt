package com.materialkolor.builder.domain.persist

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordCodecTest {
    private val fixtures = PersistFixtures()

    @Test
    fun projectRecord_filledRecord_survivesARoundTrip() {
        assertRoundTrip(ProjectRecord.Codec, fixtures.project())
    }

    @Test
    fun projectIndex_filledIndex_survivesARoundTrip() {
        assertRoundTrip(ProjectIndex.Codec, fixtures.index())
        assertRoundTrip(ProjectIndex.Codec, ProjectIndex())
    }

    @Test
    fun historyRecord_fullHistory_survivesARoundTrip() {
        assertRoundTrip(HistoryRecord.Codec, fixtures.history())
        assertRoundTrip(HistoryRecord.Codec, HistoryRecord())
    }

    @Test
    fun projectViewState_filledState_survivesARoundTrip() {
        assertRoundTrip(ProjectViewState.Codec, fixtures.viewState())
        assertRoundTrip(ProjectViewState.Codec, ProjectViewState())
    }

    @Test
    fun preferences_filledPreferences_surviveARoundTrip() {
        assertRoundTrip(Preferences.Codec, fixtures.preferences())
        assertRoundTrip(Preferences.Codec, Preferences())
    }

    @Test
    fun encode_defaultPreferences_writesTheCurrentSchemaAroundAnEmptyRecord() {
        assertEquals("""{"schema":1,"data":{}}""", Preferences.Codec.encode(Preferences()))
    }

    @Test
    fun encode_everyRecord_writesTheSchemaItsCodecReports() {
        val written = listOf(
            ProjectRecord.Codec.schema to ProjectRecord.Codec.encode(fixtures.project()),
            ProjectIndex.Codec.schema to ProjectIndex.Codec.encode(fixtures.index()),
            HistoryRecord.Codec.schema to HistoryRecord.Codec.encode(fixtures.history(size = 3)),
            ProjectViewState.Codec.schema to ProjectViewState.Codec.encode(fixtures.viewState()),
            Preferences.Codec.schema to Preferences.Codec.encode(fixtures.preferences()),
        )

        written.forEach { (schema, text) ->
            val envelope = Json.parseToJsonElement(text).jsonObject
            assertEquals(setOf("schema", "data"), envelope.keys)
            assertEquals(JsonPrimitive(schema), envelope["schema"])
        }
    }

    @Test
    fun decode_unknownKeysAtEveryLevel_areIgnored() {
        val project = fixtures.project()
        val envelope = Json.parseToJsonElement(ProjectRecord.Codec.encode(project)).jsonObject
        val data = envelope.getValue("data").jsonObject
        val document = data.getValue("document").jsonObject
        val padded = JsonObject(
            envelope + mapOf(
                "writtenBy" to JsonPrimitive("builder 7"),
                "data" to JsonObject(
                    data + mapOf(
                        "pinned" to JsonPrimitive(true),
                        "document" to JsonObject(document + ("sparkle" to JsonPrimitive(3))),
                    ),
                ),
            ),
        )

        assertEquals(project, assertOk(ProjectRecord.Codec.decode(padded.toString())))
    }

    @Test
    fun decode_viewStateWithAFieldItNoLongerHas_keepsTheRest() {
        val text = """{"schema":0,"data":{"mode":"Light","posterCollapsed":true}}"""

        assertEquals(ProjectViewState(mode = PreviewMode.Light), assertOk(ProjectViewState.Codec.decode(text)))
    }

    private fun <T> assertRoundTrip(
        codec: RecordCodec<T>,
        value: T,
    ) {
        assertEquals(value, assertOk(codec.decode(codec.encode(value))))
    }
}
