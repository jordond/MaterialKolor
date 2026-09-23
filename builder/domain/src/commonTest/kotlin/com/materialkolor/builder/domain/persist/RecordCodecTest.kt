package com.materialkolor.builder.domain.persist

import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
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
    fun schema_everyRecord_isStillInItsFirstShape() {
        val schemas = listOf(
            ProjectRecord.Codec.schema,
            ProjectIndex.Codec.schema,
            HistoryRecord.Codec.schema,
            ProjectViewState.Codec.schema,
            Preferences.Codec.schema,
        )

        assertEquals(List(schemas.size) { 0 }, schemas)
    }

    @Test
    fun encode_defaultPreferences_writesEveryFieldOut() {
        val expected = """{"schema":0,"data":{"appearance":"System","motion":"System","hueLock":false,""" +
            """"styleLock":true,"seedLock":false,"dismissedHints":[],"firstExportDone":false,""" +
            """"posterCollapsed":false,"lastProjectId":null,"persistRequested":false,"exportPrefs":{}}}"""

        assertEquals(expected, Preferences.Codec.encode(Preferences()))
    }

    @Test
    fun encode_defaultExportPrefs_writesEveryOptionOut() {
        val prefs = Preferences().withExportPrefs(ExportTarget.Fluent, ExportPrefs())
        val exportPrefs = dataOf(Preferences.Codec.encode(prefs)).getValue("exportPrefs").jsonObject
        val fluent = exportPrefs.getValue("Fluent").jsonObject

        assertEquals(elementNames(ExportPrefs.serializer().descriptor), fluent.keys)
        assertEquals(JsonPrimitive(ExportPrefs.DEFAULT_PACKAGE_NAME), fluent["packageName"])
        assertEquals(JsonPrimitive(ExportPrefs.DEFAULT_ANIMATION_DURATION_MS), fluent["animationDurationMs"])
    }

    @Test
    fun encode_projectWithTheDefaultDocument_namesEveryDefault() {
        val record = ProjectRecord(
            id = "p1",
            name = "Cactus",
            document = ThemeDocument.Default,
            revision = 1L,
            writerTab = "tab-1",
        )

        val document = dataOf(ProjectRecord.Codec.encode(record)).getValue("document").jsonObject

        assertEquals(JsonPrimitive(ThemeDocument.Default.style.name), document["style"])
        assertEquals(JsonPrimitive(ThemeDocument.Default.themeName), document["themeName"])
        assertEquals(JsonPrimitive(ThemeDocument.Default.motionScheme.name), document["motionScheme"])
        assertEquals(elementNames(ThemeDocument.serializer().descriptor), document.keys)
    }

    @Test
    fun encode_historyOfDefaultDocuments_namesEveryDefault() {
        val entry = HistoryEntry(
            before = ThemeDocument.Default,
            after = ThemeDocument.Default,
            label = ChangeLabel(ChangeKind.entries.first()),
        )

        val entries = dataOf(HistoryRecord.Codec.encode(HistoryRecord(listOf(entry)))).getValue("entries").jsonArray
        val stored = entries.single().jsonObject

        listOf("before", "after").forEach { side ->
            assertEquals(elementNames(ThemeDocument.serializer().descriptor), stored.getValue(side).jsonObject.keys)
        }
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

    private fun dataOf(text: String): JsonObject =
        Json
            .parseToJsonElement(text)
            .jsonObject
            .getValue("data")
            .jsonObject

    private fun elementNames(descriptor: SerialDescriptor): Set<String> =
        List(descriptor.elementsCount) { index -> descriptor.getElementName(index) }.toSet()

    private fun <T> assertRoundTrip(
        codec: RecordCodec<T>,
        value: T,
    ) {
        assertEquals(value, assertOk(codec.decode(codec.encode(value))))
    }
}
