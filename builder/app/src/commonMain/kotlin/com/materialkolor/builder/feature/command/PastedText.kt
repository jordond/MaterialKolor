package com.materialkolor.builder.feature.command

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorInput
import com.materialkolor.builder.domain.color.ParseResult
import com.materialkolor.builder.domain.link.DecodeResult
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.RoutePath
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.Style as PaletteStyle

/**
 * What a piece of pasted or typed text is to the builder, for the command palette and for a paste
 * outside any text field (F-05, F-32, F-33).
 */
internal sealed interface PastedText {
    /** A color, to set the seed to. */
    data class Color(
        val argb: Argb,
    ) : PastedText

    /** A share link or a bare share code, to open. */
    data class Share(
        val code: String,
    ) : PastedText

    /** The name of a palette style, to switch to. */
    data class Style(
        val style: PaletteStyle,
    ) : PastedText
}

/**
 * What [text] is, or null when it is none of a color, a share link or code, or a style name.
 *
 * A color wins over everything else, so text that reads both ways sets the seed. A link counts on
 * any host as long as its path is `/t/<code>`, whether the code still reads or not, since opening it
 * says what is wrong. A bare code that reads counts at any length. One that only looks like it comes
 * from a newer builder counts after a style name, and only when it looks like a real code. Links of
 * the old builder are ignored. A style matches its enum name or [styleNames], the names the chips
 * show, with case, spaces, dashes and underscores ignored.
 */
internal fun classify(
    text: String,
    styleNames: Map<PaletteStyle, String> = emptyMap(),
): PastedText? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    (ColorInput.parse(trimmed) as? ParseResult.Ok)?.let { ok -> return PastedText.Color(ok.argb) }
    linkCodeOf(trimmed)?.let { code -> return PastedText.Share(code) }
    val decoded = bareDecode(trimmed)
    if (decoded is DecodeResult.Ok) return PastedText.Share(trimmed)
    styleOf(trimmed, styleNames)?.let { style -> return PastedText.Style(style) }
    if (decoded == DecodeResult.UnknownVersion && looksLikeACode(trimmed)) return PastedText.Share(trimmed)
    return null
}

private fun linkCodeOf(text: String): String? {
    if (text.any { char -> char.isWhitespace() }) return null
    val path = pathOf(text) ?: return null
    val route = RoutePath.parse(path.substringBefore('?').substringBefore('#'), query = "")
    return (route as? Route.Theme)?.code
}

/** [text] read as a bare share code, or null when it cannot be one, having a slash or a space. */
private fun bareDecode(text: String): DecodeResult? {
    if ('/' in text || text.any { char -> char.isWhitespace() }) return null
    return ShareCodec.decode(text)
}

/**
 * Whether [text], which reads as a code from a newer builder, looks like a real code. Almost any word
 * reads that way, so it also has to be as long as a code with a few sections and carry a digit or a
 * capital, which the bytes of a real code always do.
 */
private fun looksLikeACode(text: String): Boolean =
    text.length >= MIN_NEWER_CODE_LENGTH && text.any { char -> char.isDigit() || char.isUpperCase() }

private const val MIN_NEWER_CODE_LENGTH = 16

/** The path of [text] when it is a URL or starts with one, or null for a bare word. */
private fun pathOf(text: String): String? {
    val scheme = text.indexOf("://")
    return when {
        scheme >= 0 -> {
            val afterHost = text.indexOf('/', startIndex = scheme + SCHEME_MARK_LENGTH)
            if (afterHost < 0) "/" else text.substring(afterHost)
        }
        text.startsWith('/') -> {
            text
        }
        '/' in text -> {
            text.substring(text.indexOf('/'))
        }
        else -> {
            null
        }
    }
}

private const val SCHEME_MARK_LENGTH = 3

private fun styleOf(
    text: String,
    styleNames: Map<PaletteStyle, String>,
): PaletteStyle? {
    val wanted = folded(text)
    return PaletteStyle.entries.firstOrNull { style ->
        folded(style.name) == wanted || styleNames[style]?.let(::folded) == wanted
    }
}

private fun folded(text: String): String =
    text.filterNot { char -> char.isWhitespace() || char == '-' || char == '_' }.lowercase()
