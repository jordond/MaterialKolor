package com.materialkolor.builder.domain.persist

/**
 * What reading a stored record came to.
 */
public sealed interface DecodeOutcome<out T> {
    /**
     * The record read cleanly, after whatever migrations it needed.
     *
     * @property[value] The record, in the shape this builder writes.
     */
    public data class Ok<out T>(
        public val value: T,
    ) : DecodeOutcome<T>

    /**
     * The record could not be read. The store moves it aside and tells the user, it is never
     * deleted.
     *
     * @property[reason] Why the record could not be read.
     */
    public data class Quarantine(
        public val reason: QuarantineReason,
    ) : DecodeOutcome<Nothing>
}

/**
 * Why a stored record was set aside instead of read.
 */
public enum class QuarantineReason {
    /**
     * The text is not JSON, or not an envelope with a schema and data.
     */
    Unreadable,

    /**
     * A newer builder wrote it, in a schema this one has no steps for yet.
     */
    NewerSchema,

    /**
     * A migration step could not make sense of the data it was handed.
     */
    MigrationFailed,

    /**
     * The data went through every step and still does not fit the record.
     */
    WrongShape,
}
