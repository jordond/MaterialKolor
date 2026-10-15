package com.materialkolor.builder.domain.persist

import kotlinx.serialization.KSerializer

/**
 * Writes one kind of stored record as text and reads it back.
 *
 * Every record goes out wrapped with the schema it was written in, and comes back through the
 * migrations for its kind before it is decoded. Nothing about the JSON underneath leaks out, the
 * store only ever sees a string.
 *
 * Each record type carries its codec as `Codec` on its companion, for example
 * [ProjectRecord.Codec] and [Preferences.Codec].
 *
 * @param[settle] What a record that read cleanly passes through before it is handed out, such as
 * moving a contrast between the named levels onto the nearest one.
 */
public class RecordCodec<T> internal constructor(
    private val serializer: KSerializer<T>,
    private val migrations: Migrations,
    private val settle: (T) -> T = { value -> value },
) {
    /**
     * The schema this builder writes the record in.
     */
    public val schema: Int
        get() = migrations.current

    /**
     * [value] as the text to store.
     */
    public fun encode(value: T): String = encodeEnvelope(value, serializer, migrations)

    /**
     * The record stored as [text], or why it could not be read. This never throws.
     */
    public fun decode(text: String): DecodeOutcome<T> =
        when (val outcome = decodeEnvelope(text, serializer, migrations)) {
            is DecodeOutcome.Ok -> DecodeOutcome.Ok(settle(outcome.value))
            is DecodeOutcome.Quarantine -> outcome
        }
}
