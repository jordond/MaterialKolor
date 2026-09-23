package com.materialkolor.builder.domain.color

import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Reads the text someone typed or pasted into the seed field.
 *
 * It takes hex as `#rgb`, `#rrggbb` or `#aarrggbb`, each with or without the `#`, hex as `0xrrggbb` or
 * `0xaarrggbb`, Compose code such as `Color(0xFF6750A4)`, the CSS functions `rgb()`, `rgba()`, `hsl()`, `hsla()`
 * and `oklch()` in comma or space syntax, and the 148 CSS named colors. Whitespace around the text and letter case
 * are ignored.
 *
 * Eight hex digits are read as `AARRGGBB`, alpha first, because that is how Android and the old builder write a
 * color. CSS does the opposite with `#rrggbbaa`, so four and eight digit CSS hex with alpha will not read the way a
 * browser would. Four digit hex is turned away rather than guessed at.
 *
 * A seed is always opaque. Any transparency is dropped and reported as [ParseNote.AlphaDropped], while a fully
 * opaque alpha is dropped quietly since nothing changes. A value outside its range is clamped and reported as
 * [ParseNote.Clamped], the way a browser treats `rgb(300, 0, 0)`. An `oklch()` color outside sRGB is reported the
 * same way. It keeps its lightness and hue and loses chroma, found by a binary search, until it fits, rather than
 * having each channel clipped, so the hue that was typed is the hue that comes out.
 *
 * Parsing never throws. Text that is not a color comes back as [ParseResult.Invalid] and should leave the seed alone.
 */
public object ColorInput {
    public fun parse(text: String): ParseResult {
        val input = normalize(text)
        return when {
            input.isEmpty() -> invalid(InvalidReason.Empty)
            input.startsWith('#') -> parseHex(input.substring(1), allowShort = true)
            input.startsWith("0x") -> parseHex(input.substring(2), allowShort = false)
            '(' in input || ')' in input -> parseFunction(input)
            else -> parseWord(input)
        }
    }
}

/**
 * What [ColorInput.parse] made of some text.
 */
public sealed interface ParseResult {
    /**
     * The text is a color.
     *
     * @property[argb] The color, always opaque.
     * @property[notes] What had to change for the color to fit, empty when it was read exactly as written.
     */
    public data class Ok(
        public val argb: Argb,
        public val notes: Set<ParseNote> = emptySet(),
    ) : ParseResult

    /**
     * The text is not a color, so the seed stays as it was.
     *
     * @property[reason] What went wrong, for the error under the field.
     */
    public data class Invalid(
        public val reason: InvalidReason,
    ) : ParseResult
}

/**
 * Something [ColorInput.parse] changed so the color would fit, worth telling the person who typed it.
 */
public enum class ParseNote {
    /** The text asked for some transparency, and a seed is always opaque. */
    AlphaDropped,

    /** A value was out of range, or an `oklch()` color was outside sRGB, and was pulled back inside. */
    Clamped,
}

/**
 * Why [ColorInput.parse] could not read some text as a color.
 */
public enum class InvalidReason {
    /** There is no text, or only whitespace. */
    Empty,

    /** Hex with the wrong number of digits, or with something in it that is not a hex digit. */
    BadHex,

    /** A function the field knows, such as `rgb()`, with the wrong number or kind of values, or left unclosed. */
    BadArguments,

    /** Something written like a function that the field does not know, such as `hwb()`. */
    UnknownFunction,

    /** A word that is not one of the CSS named colors. */
    UnknownName,

    /** Text that does not look like any of the accepted forms. */
    Unrecognized,
}

private fun normalize(text: String): String =
    buildString {
        text.trim().lowercase().forEach { char -> append(if (char.isWhitespace()) ' ' else char) }
    }

private fun ok(
    argb: Argb,
    notes: Set<ParseNote> = emptySet(),
): ParseResult = ParseResult.Ok(argb, notes)

private fun invalid(reason: InvalidReason): ParseResult = ParseResult.Invalid(reason)

private fun parseHex(
    digits: String,
    allowShort: Boolean,
): ParseResult {
    if (!HexDigits.matches(digits)) return invalid(InvalidReason.BadHex)
    return when (digits.length) {
        3 -> {
            if (allowShort) ok(Argb.fromHex(digits.doubleEachDigit())) else invalid(InvalidReason.BadHex)
        }
        6 -> {
            ok(Argb.fromHex(digits))
        }
        8 -> {
            val opaque = digits.substring(0, 2) == "ff"
            ok(Argb.fromHex(digits), if (opaque) emptySet() else setOf(ParseNote.AlphaDropped))
        }
        else -> {
            invalid(InvalidReason.BadHex)
        }
    }
}

private fun String.doubleEachDigit(): String = buildString { this@doubleEachDigit.forEach { append(it).append(it) } }

private fun parseWord(input: String): ParseResult {
    val isName = NamePattern.matches(input)
    val named = if (isName) CssColors.find(input.filter { it in 'a'..'z' }) else null
    return when {
        named != null -> ok(named)
        HexDigits.matches(input) -> parseHex(input, allowShort = true)
        isName -> invalid(InvalidReason.UnknownName)
        else -> invalid(InvalidReason.Unrecognized)
    }
}

private enum class ColorFunction {
    Rgb,
    Hsl,
    Oklch,
    Compose,
}

private fun colorFunctionOf(name: String): ColorFunction? =
    when (name) {
        "rgb", "rgba" -> ColorFunction.Rgb
        "hsl", "hsla" -> ColorFunction.Hsl
        "oklch" -> ColorFunction.Oklch
        "color" -> ColorFunction.Compose
        else -> null
    }

private fun parseFunction(input: String): ParseResult {
    val call = FunctionCall.matchEntire(input)
    val name = (call ?: FunctionStart.find(input))?.groupValues?.get(1) ?: return invalid(InvalidReason.Unrecognized)
    val function = colorFunctionOf(name) ?: return invalid(InvalidReason.UnknownFunction)
    val body = call?.groupValues?.get(2)?.trim() ?: return invalid(InvalidReason.BadArguments)
    return when (function) {
        ColorFunction.Compose -> parseComposeLiteral(body)
        ColorFunction.Rgb -> parseCssFunction(body, ::rgbOf)
        ColorFunction.Hsl -> parseCssFunction(body, ::hslOf)
        ColorFunction.Oklch -> parseCssFunction(body, ::oklchOf)
    }
}

/**
 * Read the `0xAARRGGBB` or `0xRRGGBB` literal inside Compose's `Color(...)`.
 */
private fun parseComposeLiteral(body: String): ParseResult =
    if (body.startsWith("0x")) {
        parseHex(body.substring(2), allowShort = false)
    } else {
        invalid(InvalidReason.BadArguments)
    }

private fun parseCssFunction(
    body: String,
    readChannels: (channels: List<String>, notes: MutableSet<ParseNote>) -> Argb?,
): ParseResult {
    val arguments = splitArguments(body) ?: return invalid(InvalidReason.BadArguments)
    val alpha = alphaOf(arguments.alpha) ?: return invalid(InvalidReason.BadArguments)
    val notes = mutableSetOf<ParseNote>()
    if (alpha < 1.0) notes += ParseNote.AlphaDropped
    val argb = readChannels(arguments.channels, notes) ?: return invalid(InvalidReason.BadArguments)
    return ok(argb, notes)
}

/**
 * The three channel values of a CSS color function and its optional alpha, still as text.
 */
private class Arguments(
    val channels: List<String>,
    val alpha: String?,
)

/**
 * Split `1, 2, 3`, `1, 2, 3, 0.5`, `1 2 3` or `1 2 3 / 0.5`, without mixing the comma and space forms.
 */
private fun splitArguments(body: String): Arguments? {
    if (',' in body) {
        if ('/' in body) return null
        val parts = body.split(',').map { it.trim() }
        if (parts.any { part -> part.isEmpty() || ' ' in part }) return null
        return when (parts.size) {
            3 -> Arguments(channels = parts, alpha = null)
            4 -> Arguments(channels = parts.take(3), alpha = parts[3])
            else -> null
        }
    }
    val halves = body.split('/')
    if (halves.size > 2) return null
    val channels = halves[0].split(' ').filter { it.isNotEmpty() }
    val alpha = halves.getOrNull(1)?.trim()
    if (channels.size != 3 || alpha?.let { it.isEmpty() || ' ' in it } == true) return null
    return Arguments(channels, alpha)
}

private fun rgbOf(
    channels: List<String>,
    notes: MutableSet<ParseNote>,
): Argb? {
    val values =
        channels.map { token ->
            val value = Quantity.of(token)?.scaled(percentOf = ByteMax) ?: return null
            notes.clamp(value, 0.0, ByteMax).roundToInt()
        }
    return Argb((values[0] shl 16) or (values[1] shl 8) or values[2])
}

private fun hslOf(
    channels: List<String>,
    notes: MutableSet<ParseNote>,
): Argb? {
    val hue = angleOf(channels[0]) ?: return null
    val saturation = percentOf(channels[1]) ?: return null
    val lightness = percentOf(channels[2]) ?: return null
    val rgb =
        ColorSpaces.hslToSrgb(
            hue = hue,
            saturation = notes.clamp(saturation, 0.0, 100.0) / 100,
            lightness = notes.clamp(lightness, 0.0, 100.0) / 100,
        )
    return ColorSpaces.srgbToArgb(rgb)
}

private fun oklchOf(
    channels: List<String>,
    notes: MutableSet<ParseNote>,
): Argb? {
    val lightness = Quantity.of(channels[0])?.scaled(percentOf = 1.0) ?: return null
    val chroma = Quantity.of(channels[1])?.scaled(percentOf = OklchChromaAtFullPercent) ?: return null
    val hue = angleOf(channels[2]) ?: return null
    val mapped =
        ColorSpaces.oklchToArgb(
            Oklch(
                l = notes.clamp(lightness, 0.0, 1.0),
                c = notes.clamp(chroma, 0.0, Double.MAX_VALUE),
                h = hue,
            ),
        )
    if (mapped.clamped) notes += ParseNote.Clamped
    return mapped.argb
}

/**
 * Read an alpha token as 0.0 to 1.0, where no token at all means opaque.
 */
private fun alphaOf(token: String?): Double? {
    if (token == null) return 1.0
    return Quantity.of(token)?.scaled(percentOf = 1.0)?.coerceIn(0.0, 1.0)
}

/**
 * Read an HSL saturation or lightness as 0 to 100, with or without the `%`.
 */
private fun percentOf(token: String): Double? = Quantity.of(token)?.scaled(percentOf = 100.0)

/**
 * Read a hue in degrees from 0 up to 360, as a bare number or with `deg`, `rad`, `grad` or `turn`.
 */
private fun angleOf(token: String): Double? {
    val quantity = Quantity.of(token) ?: return null
    val degrees =
        when (quantity.unit) {
            QuantityUnit.None, QuantityUnit.Degrees -> quantity.value
            QuantityUnit.Radians -> quantity.value * 180 / PI
            QuantityUnit.Gradians -> quantity.value * 0.9
            QuantityUnit.Turns -> quantity.value * 360
            QuantityUnit.Percent -> return null
        }
    if (!degrees.isFinite()) return null
    val turn = degrees % 360
    return if (turn < 0) turn + 360 else turn
}

private fun MutableSet<ParseNote>.clamp(
    value: Double,
    min: Double,
    max: Double,
): Double {
    if (value < min || value > max) add(ParseNote.Clamped)
    return value.coerceIn(min, max)
}

private enum class QuantityUnit {
    None,
    Percent,
    Degrees,
    Radians,
    Gradians,
    Turns,
}

/**
 * One number from inside a CSS function, with the unit written after it.
 */
private class Quantity(
    val value: Double,
    val unit: QuantityUnit,
) {
    /**
     * The value as a plain number, where 100% stands for [percentOf]. Angles are not plain numbers.
     */
    fun scaled(percentOf: Double): Double? =
        when (unit) {
            QuantityUnit.None -> value
            QuantityUnit.Percent -> value * percentOf / 100
            QuantityUnit.Degrees, QuantityUnit.Radians, QuantityUnit.Gradians, QuantityUnit.Turns -> null
        }

    companion object {
        fun of(token: String): Quantity? {
            val match = QuantityPattern.matchEntire(token) ?: return null
            val number = match.groupValues[1].removePrefix("+").toDoubleOrNull() ?: return null
            if (!number.isFinite()) return null
            val unit =
                when (match.groupValues[2]) {
                    "" -> QuantityUnit.None
                    "%" -> QuantityUnit.Percent
                    "deg" -> QuantityUnit.Degrees
                    "rad" -> QuantityUnit.Radians
                    "grad" -> QuantityUnit.Gradians
                    "turn" -> QuantityUnit.Turns
                    else -> return null
                }
            return Quantity(number, unit)
        }
    }
}

private const val ByteMax: Double = 255.0

/** What `100%` chroma means in `oklch()`, from CSS Color 4. */
private const val OklchChromaAtFullPercent: Double = 0.4

private val HexDigits = Regex("[0-9a-f]+")

private val NamePattern = Regex("[a-z]+(?:[ _-]+[a-z]+)*")

private val FunctionCall = Regex("([a-z]+) *\\((.*)\\)")

private val FunctionStart = Regex("^([a-z]+) *\\(")

private val QuantityPattern = Regex("([+-]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:e[+-]?[0-9]+)?)(%|deg|rad|grad|turn)?")
