package com.materialkolor.builder.domain.link

import androidx.compose.runtime.Immutable

/**
 * Where a URL points inside the builder.
 *
 * Once a link has been read the app puts `/` back in the address bar, and the panels open as
 * history states on the same URL, so these few are every route there is.
 */
@Immutable
public sealed interface Route {
    /**
     * The builder itself, with nothing to import.
     */
    @Immutable
    public data object Home : Route

    /**
     * A shared theme.
     *
     * @property[code] The share code, still to be read by [ShareCodec.decode].
     */
    @Immutable
    public data class Theme(
        public val code: String,
    ) : Route

    /**
     * A link from the previous builder, which kept the whole theme in the query string.
     *
     * @property[query] The query without its leading `?`, still to be read by [LegacyQuery.parse].
     */
    @Immutable
    public data class Legacy(
        public val query: String,
    ) : Route

    /**
     * A path the builder has no page for.
     *
     * @property[path] The path as it was asked for.
     */
    @Immutable
    public data class Unknown(
        public val path: String,
    ) : Route
}

/**
 * Reads the path and query of the address the builder was opened on.
 */
public object RoutePath {
    private const val THEME_PREFIX: String = "/t/"

    /**
     * The route [path] and [query] point at. The query may carry its leading `?` or not.
     *
     * The root with a query the old builder would have written is a [Route.Legacy]. Any other query
     * on the root, a campaign tag say, is ignored. A `/t/` path with one segment after it is a
     * [Route.Theme] whatever the query holds, and a trailing `/` on it is forgiven.
     */
    public fun parse(
        path: String,
        query: String,
    ): Route {
        if (path.isEmpty() || path == "/") {
            val legacy = query.removePrefix("?")
            return if (LegacyQuery.recognizes(legacy)) Route.Legacy(legacy) else Route.Home
        }
        if (path.startsWith(THEME_PREFIX)) {
            val code = path.removePrefix(THEME_PREFIX).removeSuffix("/")
            if (code.isNotEmpty() && '/' !in code) return Route.Theme(code)
        }
        return Route.Unknown(path)
    }
}
