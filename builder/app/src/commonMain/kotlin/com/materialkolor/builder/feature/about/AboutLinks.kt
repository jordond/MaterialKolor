package com.materialkolor.builder.feature.about

// The addresses the builder links out to, kept here so each is written once.

/**
 * Where the builder's help pages live, or null while there are none.
 *
 * The owner decides F-36, whether the builder gets pages of its own and where. Until then this stays
 * null and Help offers no docs button, so nothing links to pages that do not exist yet.
 */
internal val HELP_PAGES_URL: String? = null

/**
 * The project on GitHub, which the top bar's overflow and About link to.
 */
internal const val GITHUB_URL = "https://github.com/jordond/materialkolor"

/**
 * The project's issue tracker, where Report a problem opens a new issue.
 */
internal const val ISSUES_URL = "$GITHUB_URL/issues"
