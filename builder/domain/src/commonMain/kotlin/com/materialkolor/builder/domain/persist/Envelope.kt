package com.materialkolor.builder.domain.persist

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * How every stored value is wrapped, the record together with the schema it was written in.
 *
 * The schema is what lets a newer builder read what an older one left behind. Reading runs the
 * [Migrations] from the stored schema up to the current one, and only then decodes the record.
 *
 * @property[schema] The schema the record was written in.
 * @property[data] The record itself, kept as plain JSON so it can be migrated before it is decoded.
 */
@Serializable
internal data class Envelope(
    @SerialName("schema")
    val schema: Int,
    @SerialName("data")
    val data: JsonElement,
)

/**
 * The JSON every stored value is written and read with.
 *
 * Unknown keys are skipped, so a field this builder has never heard of, or one an older schema had
 * and this one dropped, never stops a record from loading.
 */
internal val PersistJson: Json = Json { ignoreUnknownKeys = true }

/**
 * Writes [value] in the current schema of [migrations].
 */
internal fun <T> encodeEnvelope(
    value: T,
    serializer: KSerializer<T>,
    migrations: Migrations,
): String {
    val envelope = Envelope(schema = migrations.current, data = PersistJson.encodeToJsonElement(serializer, value))
    return PersistJson.encodeToString(Envelope.serializer(), envelope)
}

/**
 * Reads a stored value, bringing it up to date first.
 *
 * Nothing thrown while reading gets out. Text that is not an envelope, a schema this builder does
 * not know yet, a step that cannot follow the data and a record that still does not fit after
 * every step all come back as [DecodeOutcome.Quarantine], so the caller can move the value aside
 * instead of losing it.
 */
internal fun <T> decodeEnvelope(
    text: String,
    serializer: KSerializer<T>,
    migrations: Migrations,
): DecodeOutcome<T> {
    val envelope = readEnvelope(text) ?: return DecodeOutcome.Quarantine(QuarantineReason.Unreadable)
    if (envelope.schema < 0) return DecodeOutcome.Quarantine(QuarantineReason.Unreadable)
    if (envelope.schema > migrations.current) return DecodeOutcome.Quarantine(QuarantineReason.NewerSchema)
    val data = envelope.data as? JsonObject ?: return DecodeOutcome.Quarantine(QuarantineReason.WrongShape)
    val migrated = try {
        migrations.migrate(envelope.schema, data)
    } catch (error: IllegalArgumentException) {
        return DecodeOutcome.Quarantine(QuarantineReason.MigrationFailed)
    }
    return try {
        DecodeOutcome.Ok(PersistJson.decodeFromJsonElement(serializer, migrated))
    } catch (error: IllegalArgumentException) {
        DecodeOutcome.Quarantine(QuarantineReason.WrongShape)
    }
}

/**
 * The envelope in [text], or null when the text is not JSON or has no schema and data.
 *
 * Every failure kotlinx serialization reports is an [IllegalArgumentException], and so is a
 * `require` in a record's init block, which is why that is the one exception caught here and below.
 */
private fun readEnvelope(text: String): Envelope? =
    try {
        PersistJson.decodeFromString(Envelope.serializer(), text)
    } catch (error: IllegalArgumentException) {
        null
    }
