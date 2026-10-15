package com.materialkolor.builder.domain.persist

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MigrationsTest {
    @Test
    fun recordCodec_schema0Fixture_migratesToSchema1() {
        val expected = Swatch(name = "Cactus", colors = listOf("#6750A4"))

        assertEquals(1, SwatchCodec.schema)
        assertEquals(expected, assertOk(SwatchCodec.decode(SCHEMA_0_SWATCH)))
    }

    @Test
    fun recordCodec_migratedRecord_writesBackInSchema1() {
        val migrated = assertOk(SwatchCodec.decode(SCHEMA_0_SWATCH))
        val rewritten = Json.parseToJsonElement(SwatchCodec.encode(migrated)).jsonObject

        assertEquals(JsonPrimitive(1), rewritten["schema"])
        assertEquals(setOf("name", "colors"), rewritten.getValue("data").jsonObject.keys)
        assertEquals(migrated, assertOk(SwatchCodec.decode(rewritten.toString())))
    }

    @Test
    fun recordCodec_schema0WithoutTheOldKey_migratesToTheDefaults() {
        val text = """{"schema":0,"data":{"name":"Cactus"}}"""

        assertEquals(Swatch(name = "Cactus"), assertOk(SwatchCodec.decode(text)))
    }

    @Test
    fun recordCodec_currentSchema_skipsTheStep() {
        val text = """{"schema":1,"data":{"name":"Cactus","color":"#FFFFFF","colors":["#000000"]}}"""

        assertEquals(Swatch(name = "Cactus", colors = listOf("#000000")), assertOk(SwatchCodec.decode(text)))
    }

    @Test
    fun migrations_steps_runInOrderFromTheStoredSchema() {
        val migrations = Migrations(
            listOf(
                MigrationStep { data -> JsonObject(data + ("trail" to JsonPrimitive(trail(data) + "a"))) },
                MigrationStep { data -> JsonObject(data + ("trail" to JsonPrimitive(trail(data) + "b"))) },
                MigrationStep { data -> JsonObject(data + ("trail" to JsonPrimitive(trail(data) + "c"))) },
            ),
        )
        val start = JsonObject(mapOf("trail" to JsonPrimitive("")))

        assertEquals(3, migrations.current)
        assertEquals("abc", trail(migrations.migrate(schema = 0, data = start)))
        assertEquals("bc", trail(migrations.migrate(schema = 1, data = start)))
        assertEquals("", trail(migrations.migrate(schema = 3, data = start)))
    }

    @Test
    fun migrations_schemaOutOfRange_isRefused() {
        val data = JsonObject(emptyMap())

        assertFailsWith<IllegalArgumentException> { Migrations.None.migrate(schema = 1, data = data) }
        assertFailsWith<IllegalArgumentException> { Migrations.None.migrate(schema = -1, data = data) }
    }

    private fun trail(data: JsonObject): String = (data.getValue("trail") as JsonPrimitive).content

    private companion object {
        /**
         * A [Swatch] as schema 0 wrote it, one color under `color`, written by hand the way a
         * browser would still hold it.
         */
        val SCHEMA_0_SWATCH: String =
            """
            {
              "schema": 0,
              "data": {
                "name": "Cactus",
                "color": "#6750A4"
              }
            }
            """.trimIndent()
    }
}
