package com.materialkolor.builder.domain.color

/**
 * Gives a color a short name such as "Burnt Ember", for the label beside a swatch.
 *
 * The names are our own, a little over 300 of them spread across hue, chroma and tone. A color takes the name of
 * whichever one sits nearest in OKLab, where distance follows how different two colors look, so a color between
 * two names gets the one it resembles more. A name describes a color rather than identifying it, and many colors
 * share each one.
 */
public object ColorNames {
    public fun nameOf(argb: Argb): String {
        val target = argb.toOklab()
        return entries.minBy { entry -> entry.oklab.distanceSquaredTo(target) }.name
    }

    /**
     * Every name with its color, in the order [ColorNameData] lists them.
     */
    internal val entries: List<NamedColor> by lazy {
        ColorNameData.flatMap { line -> line.split(", ").map(::namedColorOf) }
    }
}

/**
 * One entry from [ColorNameData].
 */
internal class NamedColor(
    val argb: Argb,
    val name: String,
) {
    val oklab: Oklab = argb.toOklab()
}

private fun namedColorOf(entry: String): NamedColor {
    require(entry.length > HexLength + 1 && entry[HexLength] == ' ') { "Expected \"RRGGBB Name\", got \"$entry\"" }
    return NamedColor(argb = Argb.fromHex(entry.substring(0, HexLength)), name = entry.substring(HexLength + 1))
}

private const val HexLength: Int = 6
