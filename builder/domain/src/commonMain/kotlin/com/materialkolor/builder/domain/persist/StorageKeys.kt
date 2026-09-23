package com.materialkolor.builder.domain.persist

/**
 * The names everything the builder saves is stored under.
 *
 * A project is spread over three keys, its record, its history and its view state, so the drawer
 * can list projects from the index alone and a heavy history is only read for the open project.
 */
public object StorageKeys {
    /** The [ProjectIndex], every project the drawer lists. */
    public const val INDEX: String = "mk:index"

    /** The browser's [Preferences]. */
    public const val PREFS: String = "mk:prefs"

    /** The light and dark splash colors of the open theme, plain JSON that `boot.js` reads before the app loads. */
    public const val SPLASH: String = "mk:splash"

    /** Where the [ProjectRecord] of project [id] lives. */
    public fun project(id: String): String = "$PROJECT_PREFIX${checkedId(id)}"

    /** Where the [HistoryRecord] of project [id] lives. */
    public fun history(id: String): String = "$HISTORY_PREFIX${checkedId(id)}"

    /** Where the [ProjectViewState] of project [id] lives. */
    public fun view(id: String): String = "$VIEW_PREFIX${checkedId(id)}"

    /**
     * Where a record that could not be read at [key] is moved to, stamped with [time] so a second
     * failure never lands on top of the first.
     *
     * @param[time] When it was moved, in milliseconds since the epoch.
     */
    public fun quarantine(
        key: String,
        time: Long,
    ): String = "$QUARANTINE_PREFIX$key:$time"

    private fun checkedId(id: String): String {
        require(id.isNotEmpty() && ':' !in id) { "A project id is not empty and has no colon, got \"$id\"" }
        return id
    }

    private const val PROJECT_PREFIX: String = "mk:project:"
    private const val HISTORY_PREFIX: String = "mk:history:"
    private const val VIEW_PREFIX: String = "mk:view:"
    private const val QUARANTINE_PREFIX: String = "mk:quarantine:"
}
