package com.materialkolor.builder.domain.persist

import com.materialkolor.builder.domain.history.History
import com.materialkolor.builder.domain.history.HistoryEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test

class QuarantineTest {
    private val fixtures = PersistFixtures()

    @Test
    fun decode_textThatIsNotJson_isUnreadable() {
        val broken = listOf("", "   ", "{", "not json", """{"schema":1,"data":""", "\u0000\u0001")

        broken.forEach { text -> assertQuarantined(QuarantineReason.Unreadable, Preferences.Codec.decode(text)) }
    }

    @Test
    fun decode_jsonThatIsNotAnEnvelope_isUnreadable() {
        val notEnvelopes = listOf(
            "[]",
            "42",
            "null",
            """{"data":{}}""",
            """{"schema":1}""",
            """{"schema":"one","data":{}}""",
            """{"schema":1.5,"data":{}}""",
            """{"schema":-1,"data":{}}""",
        )

        notEnvelopes.forEach { text ->
            assertQuarantined(QuarantineReason.Unreadable, Preferences.Codec.decode(text))
        }
    }

    @Test
    fun decode_schemaNewerThanTheBuilder_isQuarantinedAsNewer() {
        val codecs = listOf(
            ProjectRecord.Codec,
            ProjectIndex.Codec,
            HistoryRecord.Codec,
            ProjectViewState.Codec,
            Preferences.Codec,
            SwatchCodec,
        )

        codecs.forEach { codec ->
            val text = """{"schema":${codec.schema + 1},"data":{}}"""
            assertQuarantined(QuarantineReason.NewerSchema, codec.decode(text))
        }
    }

    @Test
    fun decode_dataThatIsNotAnObject_isTheWrongShape() {
        val texts = listOf(
            """{"schema":0,"data":[]}""",
            """{"schema":0,"data":"project"}""",
            """{"schema":0,"data":null}""",
        )

        texts.forEach { text -> assertQuarantined(QuarantineReason.WrongShape, ProjectRecord.Codec.decode(text)) }
    }

    @Test
    fun decode_fieldsOfTheWrongType_areTheWrongShape() {
        val wrongShapes = listOf(
            projectJson(document = null),
            projectJson(id = "5"),
            projectJson(document = """{"seed":"purple"}"""),
            projectJson(revision = "\"x\""),
            projectJson(document = """{"seed":"#6750A4","style":"Glitter"}"""),
        )

        assertOk(ProjectRecord.Codec.decode(projectJson()))
        wrongShapes.forEach { text -> assertQuarantined(QuarantineReason.WrongShape, ProjectRecord.Codec.decode(text)) }
    }

    @Test
    fun decode_recordThatBreaksItsOwnRules_isTheWrongShape() {
        val entries = List(History.PERSISTED + 1) { index -> fixtures.entry(index) }
        val entriesJson = Json.encodeToString(ListSerializer(HistoryEntry.serializer()), entries)
        val tooLong = """{"schema":0,"data":{"entries":$entriesJson}}"""
        val threeColors = """{"schema":0,"data":{"projects":[{"id":"p","name":"n","createdAt":1,"updatedAt":2,""" +
            """"previewColors":["#FFFFFF","#000000","#6750A4"]}]}}"""
        val splitPastTheEdge = """{"schema":0,"data":{"splitFraction":1.5}}"""

        assertQuarantined(QuarantineReason.WrongShape, HistoryRecord.Codec.decode(tooLong))
        assertQuarantined(QuarantineReason.WrongShape, ProjectIndex.Codec.decode(threeColors))
        assertQuarantined(QuarantineReason.WrongShape, ProjectViewState.Codec.decode(splitPastTheEdge))
    }

    @Test
    fun decode_schema0DataTheStepCannotFollow_failsItsMigration() {
        val texts = listOf(
            """{"schema":0,"data":{"name":"Cactus","color":{"hex":"#6750A4"}}}""",
            """{"schema":0,"data":{"name":"Cactus","color":42}}""",
        )

        texts.forEach { text -> assertQuarantined(QuarantineReason.MigrationFailed, SwatchCodec.decode(text)) }
    }

    @Test
    fun decode_truncatedAtEveryLength_neverThrows() {
        val text = ProjectRecord.Codec.encode(fixtures.project())

        (0 until text.length).forEach { length ->
            assertQuarantined(QuarantineReason.Unreadable, ProjectRecord.Codec.decode(text.take(length)))
        }
    }

    private fun projectJson(
        id: String = "\"p1\"",
        document: String? = """{"seed":"#6750A4"}""",
        revision: String = "1",
    ): String {
        val documentField = document?.let { json -> ""","document":$json""" }.orEmpty()
        return """{"schema":0,"data":{"id":$id,"name":"A"$documentField,"revision":$revision,"writerTab":"t"}}"""
    }
}
