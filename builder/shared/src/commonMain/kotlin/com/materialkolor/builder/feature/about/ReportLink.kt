package com.materialkolor.builder.feature.about

import androidx.compose.runtime.Immutable

/**
 * What a problem report says about where it came from. About copies it with Copy details and
 * sends it with Report a problem.
 *
 * @property[builderVersion] The builder's own version.
 * @property[materialKolorVersion] The MaterialKolor version every export uses.
 * @property[browser] The browser and the system it runs on.
 * @property[themeLink] The open theme's link without the project name, since issues are public, or
 *   null when the theme is too big to fit in one.
 */
@Immutable
internal data class ReportDetails(
    val builderVersion: String,
    val materialKolorVersion: String,
    val browser: String,
    val themeLink: String?,
)

/**
 * The details as the lines of an issue, one fact a line.
 *
 * The labels stay in English whatever language the builder speaks, since they are read on the
 * project's issue tracker rather than in the app.
 */
internal fun ReportDetails.text(): String =
    buildString {
        appendLine("Builder: $builderVersion")
        appendLine("MaterialKolor: $materialKolorVersion")
        appendLine("Browser: $browser")
        append("Theme: ${themeLink ?: "too big to link"}")
    }

/**
 * The address that opens a new issue on [issuesUrl] with [details] already in its body and the
 * title left for the reporter.
 */
internal fun reportUrl(
    issuesUrl: String,
    details: ReportDetails,
): String = "$issuesUrl/new?title=&body=${percentEncode(details.text())}"

/**
 * [text] made safe for a query value. Every UTF-8 byte outside the unreserved letters, digits and
 * `-._~` becomes `%XX`, so a space, a line break, `&`, `=`, `#` and `+` all arrive as they were.
 */
internal fun percentEncode(text: String): String =
    buildString {
        text.encodeToByteArray().forEach { byte ->
            val code = byte.toInt() and BYTE_MASK
            if (code.isUnreserved()) {
                append(code.toChar())
            } else {
                append('%')
                append(HEX_DIGITS[code shr NIBBLE_BITS])
                append(HEX_DIGITS[code and NIBBLE_MASK])
            }
        }
    }

/**
 * Whether the byte [this] stands for itself in a URL, RFC 3986's unreserved set.
 */
private fun Int.isUnreserved(): Boolean =
    this in 'A'.code..'Z'.code ||
        this in 'a'.code..'z'.code ||
        this in '0'.code..'9'.code ||
        this == '-'.code ||
        this == '.'.code ||
        this == '_'.code ||
        this == '~'.code

private const val HEX_DIGITS = "0123456789ABCDEF"
private const val BYTE_MASK = 0xFF
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0x0F
