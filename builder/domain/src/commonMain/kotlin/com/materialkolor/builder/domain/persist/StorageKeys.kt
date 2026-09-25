package com.materialkolor.builder.domain.persist

/**
 * The names everything the builder saves is stored under.
 *
 * A project is spread over three keys, its record, its history and its view state, so the drawer
 * can list projects from the index alone and a heavy history is only read for the open project.
 */
public object StorageKeys {
    /**
     * The [ProjectIndex], every project the drawer lists.
     */
    public const val INDEX: String = "mk:index"

    /**
     * The browser's [Preferences].
     */
    public const val PREFS: String = "mk:prefs"

    /**
     * The light and dark splash colors of the open theme, plain JSON that `boot.js` reads before the app loads.
     */
    public const val SPLASH: String = "mk:splash"

    /**
     * Where the [ProjectRecord] of project [id] lives.
     */
    public fun project(id: String): String = "$PROJECT_PREFIX${checkedId(id)}"

    /**
     * Where the [HistoryRecord] of project [id] lives.
     */
    public fun history(id: String): String = "$HISTORY_PREFIX${checkedId(id)}"

    /**
     * Where the [ProjectViewState] of project [id] lives.
     */
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

    /**
     * What [key] names, read back off the key itself.
     *
     * The store runs a key another tab changed through this to learn which record moved. A key made
     * by [quarantine] and any key the builder does not own come back null, since the store never
     * reads either.
     */
    public fun parse(key: String): StorageKey? =
        when (key) {
            INDEX -> StorageKey.Index
            PREFS -> StorageKey.Prefs
            SPLASH -> StorageKey.Splash
            else -> parseProjectKey(key)
        }

    private fun parseProjectKey(key: String): StorageKey? {
        val id = key.substringAfterLast(ID_SEPARATOR)
        if (!isId(id)) return null
        return when (key.removeSuffix(id)) {
            PROJECT_PREFIX -> StorageKey.Project(id)
            HISTORY_PREFIX -> StorageKey.History(id)
            VIEW_PREFIX -> StorageKey.View(id)
            else -> null
        }
    }

    private fun checkedId(id: String): String {
        require(isId(id)) { "A project id is not empty and has no colon, got \"$id\"" }
        return id
    }

    private fun isId(id: String): Boolean = id.isNotEmpty() && ID_SEPARATOR !in id

    private const val ID_SEPARATOR: Char = ':'
    private const val PROJECT_PREFIX: String = "mk:project:"
    private const val HISTORY_PREFIX: String = "mk:history:"
    private const val VIEW_PREFIX: String = "mk:view:"
    private const val QUARANTINE_PREFIX: String = "mk:quarantine:"
}

/**
 * One of the builder's own records, as [StorageKeys.parse] reads it back off its key.
 */
public sealed interface StorageKey {
    /**
     * The [ProjectIndex] under [StorageKeys.INDEX].
     */
    public data object Index : StorageKey

    /**
     * The [Preferences] under [StorageKeys.PREFS].
     */
    public data object Prefs : StorageKey

    /**
     * The splash colors under [StorageKeys.SPLASH].
     */
    public data object Splash : StorageKey

    /**
     * The [ProjectRecord] of one project, under [StorageKeys.project].
     *
     * @property[id] The project it belongs to.
     */
    public data class Project(
        public val id: String,
    ) : StorageKey

    /**
     * The [HistoryRecord] of one project, under [StorageKeys.history].
     *
     * @property[id] The project it belongs to.
     */
    public data class History(
        public val id: String,
    ) : StorageKey

    /**
     * The [ProjectViewState] of one project, under [StorageKeys.view].
     *
     * @property[id] The project it belongs to.
     */
    public data class View(
        public val id: String,
    ) : StorageKey
}
