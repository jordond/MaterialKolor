package com.materialkolor.builder.domain.persist

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * One schema bump, turning a record written in one schema into the next.
 *
 * A step works on plain JSON because the Kotlin types only know the newest shape. It throws an
 * [IllegalArgumentException] when the data is not what its schema looked like, and the record is
 * then quarantined rather than guessed at.
 */
internal fun interface MigrationStep {
    fun migrate(data: JsonObject): JsonObject
}

/**
 * The steps that bring one kind of record up to date, oldest first.
 *
 * The step at index n reads schema n and writes schema n + 1, so the schema a record is written in
 * today is the number of steps. A record that has never changed shape has no steps and is at
 * schema 0.
 */
internal class Migrations(
    private val steps: List<MigrationStep>,
) {
    /** The schema this builder writes. */
    val current: Int
        get() = steps.size

    /**
     * Runs every step from [schema] up to [current], leaving data that is already current alone.
     */
    fun migrate(
        schema: Int,
        data: JsonObject,
    ): JsonObject {
        require(schema in 0..current) { "Schema is 0 to $current, got $schema" }
        return steps.drop(schema).fold(data) { migrated, step -> step.migrate(migrated) }
    }

    companion object {
        /** For a record that is still in its first shape. */
        val None: Migrations = Migrations(emptyList())
    }
}

/**
 * Schema 0 to 1 of [Preferences].
 *
 * Schema 0 kept one set of export options under `export`, shared by every target. Schema 1 keeps a
 * set per target under `exportPrefs`, and each target starts from what the shared set said.
 *
 * The keys are written out rather than read off the Kotlin types on purpose. A step describes a
 * format that is already out in the world, so it must not move when the code does.
 */
internal val ExportPrefsPerTarget: MigrationStep =
    MigrationStep { data ->
        val shared = data[SCHEMA_0_EXPORT_KEY]?.jsonObject ?: return@MigrationStep data
        val perTarget = JsonObject(SCHEMA_1_EXPORT_TARGETS.associateWith { shared })
        JsonObject(data - SCHEMA_0_EXPORT_KEY + (SCHEMA_1_EXPORT_PREFS_KEY to perTarget))
    }

/** The steps for [Preferences], declared after the step it lists so it is set up in time. */
internal val PreferencesMigrations: Migrations = Migrations(listOf(ExportPrefsPerTarget))

private const val SCHEMA_0_EXPORT_KEY: String = "export"

private const val SCHEMA_1_EXPORT_PREFS_KEY: String = "exportPrefs"

private val SCHEMA_1_EXPORT_TARGETS: List<String> =
    listOf("Material3", "Material3Expressive", "Unstyled", "Fluent", "Custom")
