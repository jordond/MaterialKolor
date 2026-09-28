package com.materialkolor.builder.domain.link

import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * Where the builder is served, and so where its links point unless the page says otherwise.
 */
public const val SITE_ORIGIN: String = "https://materialkolor.com"

/**
 * Where a share code opens its theme on [SITE_ORIGIN], and where every export links back to.
 */
public const val SHARE_URL_PREFIX: String = "$SITE_ORIGIN/t/"

/**
 * The link to [document] called [projectName], the one Share gives and every export links back with.
 * It opens on [origin], so a link made on staging opens on staging.
 *
 * The code carries the document and the name and nothing else, so an export option such as the
 * package name never ends up in it. A document whose own accents do not fit in a code links to what
 * its target sees instead, still under [projectName], which for Fluent leaves the accents out. When
 * that does not fit either there is no link, and this is null.
 */
public fun shareLink(
    document: ThemeDocument,
    projectName: String,
    origin: String = SITE_ORIGIN,
): String? = fittedCode(document, projectName)?.let { code -> "$origin/t/$code" }

/**
 * The link to the card for [document] called [projectName], the PNG a chat app shows under the
 * link [shareLink] gives. It is served from [origin], so a card made on staging comes from staging.
 *
 * It carries the same code as [shareLink], so it is null exactly when that link is.
 */
public fun shareCardLink(
    document: ThemeDocument,
    projectName: String,
    origin: String = SITE_ORIGIN,
): String? = fittedCode(document, projectName)?.let { code -> "$origin/og/$code.png" }

/**
 * The code a link to [document] carries. It is the document's own code, or failing that the code
 * of what its target sees, or null when neither fits.
 */
private fun fittedCode(
    document: ThemeDocument,
    projectName: String,
): String? {
    val target = ExportTarget.of(document.library, document.expressive)
    return codeOrNull(document, projectName) ?: codeOrNull(document.forTarget(target), projectName)
}

/**
 * The share code for [document], or null when its accents do not fit in one.
 */
private fun codeOrNull(
    document: ThemeDocument,
    projectName: String,
): String? =
    try {
        ShareCodec.encode(document, projectName)
    } catch (_: IllegalArgumentException) {
        null
    }
