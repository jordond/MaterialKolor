package com.materialkolor.builder.domain.persist

import kotlinx.serialization.json.JsonObject

/**
 * One schema bump, turning a record written in one schema into the next.
 *
 * A step works on plain JSON because the Kotlin types only know the newest shape. It throws an
 * [IllegalArgumentException] when the data is not what its schema looked like, and the record is
 * then quarantined rather than guessed at.
 *
 * A step spells its keys out rather than reading them off the Kotlin types. It describes a format
 * that is already out in the world, so it must not move when the code does.
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
