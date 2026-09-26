package com.materialkolor.builder.domain.link

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument

/**
 * The preview an old link asked for. Only dark is ever asked for, see [LegacyImport.previewMode].
 */
public enum class LegacyPreviewMode {
    Dark,
}

/**
 * What an old builder link held, split into the theme and the two things that are not the theme.
 *
 * @property[document] The theme the link described, with everything it did not mention left at the defaults.
 * @property[previewMode] The preview the link asked for, or null when it did not ask for dark. The
 * old builder wrote `dark_mode=false` on every link where dark was off, whether or not anyone chose
 * it, so a light preview is never something a link asked for.
 * @property[packageName] The package the old builder exported to, or null when the link had none.
 * It belongs to the exporting browser's preferences, never to the document.
 */
public data class LegacyImport(
    public val document: ThemeDocument,
    public val previewMode: LegacyPreviewMode?,
    public val packageName: String?,
)

/**
 * Reads the query string the previous builder wrote into its links, so they keep opening.
 *
 * The parser is tolerant. A segment with no `=`, a value that is not hex, a style or spec it does
 * not know or a key it has never heard of is skipped on its own and the rest of the link still
 * counts. When a key appears twice the last one wins, the way the old builder read it.
 */
public object LegacyQuery {
    /**
     * The theme and settings [query] describes, with or without its leading `?`. Never throws.
     */
    public fun parse(query: String): LegacyImport {
        var legacy = LegacyImport(LegacyDefault, previewMode = null, packageName = null)
        for ((key, value) in segments(query)) legacy = legacy.applying(key, value) ?: legacy
        return legacy
    }

    /**
     * Whether [query] holds at least one key the old builder wrote, which is what tells an old
     * link apart from a plain visit that happens to carry a query.
     */
    internal fun recognizes(query: String): Boolean = segments(query).isNotEmpty()
}

/**
 * The theme an old link starts from. The old builder left the spec out of a link on 2021, its
 * default, so a link without one still means 2021 now that a new theme asks for the newest.
 */
private val LegacyDefault: ThemeDocument = ThemeDocument.Default.copy(spec = SpecVersion.Spec2021)

/**
 * Every key the old builder wrote into a link.
 */
private enum class LegacyKey(
    val key: String,
) {
    ColorSeed("color_seed"),
    ColorPrimary("color_primary"),
    ColorSecondary("color_secondary"),
    ColorTertiary("color_tertiary"),
    ColorError("color_error"),
    ColorNeutral("color_neutral"),
    ColorNeutralVariant("color_neutralvariant"),
    Style("style"),
    Contrast("contrast"),
    ColorSpec("color_spec"),
    DarkMode("dark_mode"),
    Amoled("is_amoled"),
    Expressive("expressive"),
    PackageName("package_name"),
    Misc("misc"),
    SelectedPresetId("selected_preset_id"),
    Destination("destination"),
}

private const val CMF_PREFIX: String = "Cmf:"

private val LEGACY_PRESET_IDS: Set<String> = setOf("res-0", "res-1", "res-2", "res-3", "res-4")

/**
 * This import with one segment applied, or null when the value is not one the old builder wrote.
 */
private fun LegacyImport.applying(
    key: LegacyKey,
    value: String,
): LegacyImport? =
    when (key) {
        LegacyKey.ColorSeed -> value.legacyColor()?.let { seed -> edit { copy(seed = seed) } }
        LegacyKey.ColorPrimary -> keyColor(KeyColor.Primary, value)
        LegacyKey.ColorSecondary -> keyColor(KeyColor.Secondary, value)
        LegacyKey.ColorTertiary -> keyColor(KeyColor.Tertiary, value)
        LegacyKey.ColorError -> keyColor(KeyColor.Error, value)
        LegacyKey.ColorNeutral -> keyColor(KeyColor.Neutral, value)
        LegacyKey.ColorNeutralVariant -> keyColor(KeyColor.NeutralVariant, value)
        LegacyKey.Style -> style(value)
        LegacyKey.Contrast -> value.legacyContrast()?.let { contrast -> edit { copy(contrast = contrast) } }
        LegacyKey.ColorSpec -> value.legacySpec()?.let { spec -> edit { copy(spec = spec) } }
        LegacyKey.DarkMode -> darkMode(value)
        LegacyKey.Amoled -> value.toBooleanStrictOrNull()?.let { amoled -> edit { copy(amoled = amoled) } }
        LegacyKey.Expressive -> expressive(value)
        LegacyKey.PackageName -> value.takeIf { name -> name.isNotBlank() }?.let { name -> copy(packageName = name) }
        LegacyKey.Misc -> null
        LegacyKey.SelectedPresetId -> preset(value)
        LegacyKey.Destination -> null
    }

private inline fun LegacyImport.edit(change: ThemeDocument.() -> ThemeDocument): LegacyImport =
    copy(document = document.change())

private fun LegacyImport.keyColor(
    slot: KeyColor,
    value: String,
): LegacyImport? = value.legacyColor()?.let { color -> edit { copy(keyColors = keyColors.with(slot, color)) } }

/**
 * The old builder wrote `false` on every link where dark mode was off, so only `true` says anything.
 */
private fun LegacyImport.darkMode(value: String): LegacyImport? {
    val dark = value.toBooleanStrictOrNull() ?: return null
    return copy(previewMode = if (dark) LegacyPreviewMode.Dark else null)
}

/**
 * Expressive only ever meant the Material 3 export with the expressive extras on.
 */
private fun LegacyImport.expressive(value: String): LegacyImport? {
    val enabled = value.toBooleanStrictOrNull() ?: return null
    return edit { copy(library = if (enabled) Library.Material3 else library, expressive = enabled) }
}

private fun LegacyImport.preset(value: String): LegacyImport? {
    if (value !in LEGACY_PRESET_IDS) return null
    return edit { copy(seedSource = SeedSource.Preset(id = value)) }
}

private fun LegacyImport.style(value: String): LegacyImport? {
    if (value.startsWith(CMF_PREFIX)) {
        val seed = value.removePrefix(CMF_PREFIX).legacyColor() ?: return null
        return edit { copy(style = Style.Cmf, cmfTertiarySeed = seed) }
    }
    val style = Style.entries.firstOrNull { entry -> entry.name == value } ?: return null
    return edit { copy(style = style) }
}

/**
 * A color written as six or eight hex digits with an optional `#`. The old builder always wrote
 * eight, alpha first, and the alpha is dropped the way every color in a document drops it.
 */
private fun String.legacyColor(): Argb? {
    val digits = removePrefix("#")
    if (digits.length != 6 && digits.length != 8) return null
    if (!digits.all { char -> char.hexValue() >= 0 }) return null
    return Argb.fromHex(digits)
}

/**
 * Any double the old builder could have written, at the named level nearest it.
 */
private fun String.legacyContrast(): ContrastLevel? {
    val value = toDoubleOrNull() ?: return null
    if (value.isNaN()) return null
    return ContrastLevel.nearest(value)
}

private fun String.legacySpec(): SpecVersion? = SpecVersion.entries.firstOrNull { spec -> spec.legacyName == this }

/**
 * The name the library's spec enum gave each version, which is what the old builder wrote.
 */
private val SpecVersion.legacyName: String
    get() =
        when (this) {
            SpecVersion.Spec2021 -> "SPEC_2021"
            SpecVersion.Spec2025 -> "SPEC_2025"
            SpecVersion.Spec2026 -> "SPEC_2026"
        }

/**
 * The segments of [query] this parser knows, in order, with their values percent decoded.
 */
private fun segments(query: String): List<Pair<LegacyKey, String>> =
    query.removePrefix("?").split('&').mapNotNull { segment ->
        val separator = segment.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        val name = segment.substring(0, separator).percentDecoded() ?: return@mapNotNull null
        val key = LegacyKey.entries.firstOrNull { entry -> entry.key == name } ?: return@mapNotNull null
        val value = segment.substring(separator + 1).percentDecoded() ?: return@mapNotNull null
        key to value
    }

/**
 * This text with every `%XX` escape turned back into its byte, or null when an escape is broken.
 *
 * A `+` stays a `+`, the old builder never wrote a space as one.
 */
private fun String.percentDecoded(): String? {
    if ('%' !in this) return this
    val bytes = mutableListOf<Byte>()
    var index = 0
    while (index < length) {
        if (this[index] == '%') {
            if (index + 2 > lastIndex) return null
            val high = this[index + 1].hexValue()
            val low = this[index + 2].hexValue()
            if (high < 0 || low < 0) return null
            bytes += ((high shl 4) or low).toByte()
            index += 3
        } else {
            val next = indexOf('%', startIndex = index).let { found -> if (found < 0) length else found }
            bytes += substring(index, next).encodeToByteArray().toList()
            index = next
        }
    }
    return bytes.toByteArray().decodeToString()
}

private fun Char.hexValue(): Int =
    when (this) {
        in '0'..'9' -> this - '0'
        in 'a'..'f' -> this - 'a' + 10
        in 'A'..'F' -> this - 'A' + 10
        else -> -1
    }
