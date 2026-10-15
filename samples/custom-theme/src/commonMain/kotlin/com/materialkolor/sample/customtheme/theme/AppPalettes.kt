package com.materialkolor.sample.customtheme.theme

import com.materialkolor.MaterialKolors
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette

/**
 * The scheme generated from the seed plus a ramp for the paper and each spot ink.
 */
public data class AppPalettes(
    public val scheme: DynamicScheme,
    public val stock: TonalPalette,
    public val pink: TonalPalette,
    public val blue: TonalPalette,
    public val yellow: TonalPalette,
) {
    internal val tones: PrintTones = PrintTones(isDark = scheme.isDark)
}

internal fun AppPalettes.toColors(): AppColors {
    val kolors = MaterialKolors(scheme)

    return AppColors(
        isLight = !scheme.isDark,
        paper = stock.toneColor(tones.paper),
        paperShade = stock.toneColor(tones.paperShade),
        paperEdge = stock.toneColor(tones.paperEdge),
        ink = scheme.primaryPalette.toneColor(tones.ink),
        inkSoft = scheme.neutralVariantPalette.toneColor(tones.inkSoft),
        primary = kolors.primary(),
        onPrimary = kolors.onPrimary(),
        error = kolors.error(),
        onError = kolors.onError(),
        pink = pink.toneColor(tones.pink),
        onPink = pink.onTone(tones.pink),
        blue = blue.toneColor(tones.blue),
        onBlue = blue.onTone(tones.blue),
        yellow = yellow.toneColor(tones.yellow),
        onYellow = yellow.onTone(tones.yellow),
        highlight = yellow.toneColor(tones.highlight),
        scrim = kolors.scrim(),
    )
}

internal class PrintTones(
    isDark: Boolean,
) {
    val paper = if (isDark) 7 else 95
    val paperShade = if (isDark) 12 else 90
    val paperEdge = if (isDark) 30 else 78

    val ink = if (isDark) 92 else 18
    val inkSoft = if (isDark) 70 else 42

    val pink = if (isDark) 65 else 64
    val blue = if (isDark) 62 else 46
    val yellow = if (isDark) 85 else 88

    val highlight = if (isDark) 30 else 88
}
