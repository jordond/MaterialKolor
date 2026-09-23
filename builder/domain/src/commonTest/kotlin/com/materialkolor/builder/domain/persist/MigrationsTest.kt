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
    fun preferences_schema0Fixture_migratesToOneSetOfExportPrefsPerTarget() {
        val shared = ExportPrefs(
            packageName = "com.cactus.theme",
            multiplatform = false,
            mode = ExportMode.Frozen,
            frozenVariants = FrozenVariants.AllContrasts,
        )
        val expected = Preferences(
            appearance = Appearance.Dark,
            styleLock = false,
            dismissedHints = setOf("shuffle"),
            exportPrefs = ExportTarget.entries.associateWith { shared },
        )

        assertEquals(expected, assertOk(Preferences.Codec.decode(SCHEMA_0_PREFERENCES)))
    }

    @Test
    fun preferences_schema0Fixture_writesBackInSchema1() {
        val migrated = assertOk(Preferences.Codec.decode(SCHEMA_0_PREFERENCES))
        val rewritten = Json.parseToJsonElement(Preferences.Codec.encode(migrated)).jsonObject

        assertEquals(1, Preferences.Codec.schema)
        assertEquals(JsonPrimitive(1), rewritten["schema"])
        assertEquals(migrated, assertOk(Preferences.Codec.decode(rewritten.toString())))
    }

    @Test
    fun preferences_schema0WithoutExport_migratesToDefaults() {
        val text = """{"schema":0,"data":{"posterCollapsed":true}}"""

        assertEquals(Preferences(posterCollapsed = true), assertOk(Preferences.Codec.decode(text)))
    }

    @Test
    fun exportPrefsPerTarget_step_leavesTheOtherKeysAlone() {
        val before = JsonObject(
            mapOf(
                "hueLock" to JsonPrimitive(true),
                "export" to JsonObject(mapOf("animate" to JsonPrimitive(true))),
            ),
        )

        val after = ExportPrefsPerTarget.migrate(before)

        assertEquals(setOf("hueLock", "exportPrefs"), after.keys)
        assertEquals(JsonPrimitive(true), after["hueLock"])
        assertEquals(
            setOf("Material3", "Material3Expressive", "Unstyled", "Fluent", "Custom"),
            after.getValue("exportPrefs").jsonObject.keys,
        )
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
         * Preferences as schema 0 wrote them, one set of export options under `export` for every
         * target, written by hand the way a browser would still hold them.
         */
        val SCHEMA_0_PREFERENCES: String =
            """
            {
              "schema": 0,
              "data": {
                "appearance": "Dark",
                "styleLock": false,
                "dismissedHints": ["shuffle"],
                "export": {
                  "packageName": "com.cactus.theme",
                  "multiplatform": false,
                  "mode": "Frozen",
                  "frozenVariants": "AllContrasts"
                }
              }
            }
            """.trimIndent()
    }
}
