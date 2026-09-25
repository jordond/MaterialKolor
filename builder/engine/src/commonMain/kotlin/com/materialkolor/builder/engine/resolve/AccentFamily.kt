package com.materialkolor.builder.engine.resolve

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.export.AccentColors
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.ktx.ContrastThreshold
import com.materialkolor.ktx.from
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * One accent of the document, worked out into its four colors for both modes.
 *
 * The exported `ExtendedColors.kt` builds its ramp with `rememberTonalPalette`, whose body in core
 * is the chain below, and `ExportParityTest` checks the two agree. The seed is pulled toward the
 * theme seed with `harmonize` when the accent asks for it, a ramp is built from the result, and each
 * mode cuts its color and container off that ramp with `toneColor`. The two on colors come from
 * `onTone` at the accent's own threshold. Change one without the other and the preview stops
 * matching the export.
 *
 * @property[accent] The accent this family was built from.
 * @property[palette] The ramp every color of the family is cut from.
 * @property[light] The family in light mode.
 * @property[dark] The family in dark mode.
 */
@Immutable
public class AccentFamily internal constructor(
    public val accent: Accent,
    public val palette: TonalPalette,
    public val light: AccentColors,
    public val dark: AccentColors,
) {
    /**
     * The family's ramp at every tone in [RampSet.Tones], darkest first, laid out like a scheme
     * palette's steps. Both modes cut from this one ramp.
     */
    public val steps: ImmutableList<RampStep> =
        RampSet.Tones.map { tone -> RampStep(tone, Argb(palette.tone(tone))) }.toImmutableList()

    /**
     * The family in the mode [isDark] picks.
     */
    public fun mode(isDark: Boolean): AccentColors = if (isDark) dark else light

    /**
     * The color [part] names in the mode [isDark] picks.
     */
    public operator fun get(
        part: AccentPart,
        isDark: Boolean,
    ): Argb = mode(isDark).part(part)

    internal companion object {
        /**
         * Build the family for [accent] in a theme seeded with [themeSeed].
         */
        fun of(
            accent: Accent,
            themeSeed: Argb,
        ): AccentFamily {
            val seed = accent.seed.toColor()
            val source = if (accent.harmonize) seed.harmonize(themeSeed.toColor()) else seed
            val palette = TonalPalette.from(source)
            val threshold = accent.threshold.toCore()
            return AccentFamily(
                accent = accent,
                palette = palette,
                light = palette.colors(accent.light, threshold),
                dark = palette.colors(accent.dark, threshold),
            )
        }

        private fun TonalPalette.colors(
            tones: FamilyTones,
            threshold: ContrastThreshold,
        ): AccentColors =
            AccentColors(
                color = toneColor(tones.color).toDomain(),
                onColor = onTone(tones.color, threshold).toDomain(),
                container = toneColor(tones.container).toDomain(),
                onContainer = onTone(tones.container, threshold).toDomain(),
            )
    }
}

/**
 * Every accent of a document as a family, in the document's order.
 *
 * @property[families] One family per accent, so an [AccentSlot] index is a position in this list.
 */
@Immutable
public class AccentFamilies internal constructor(
    public val families: ImmutableList<AccentFamily>,
) {
    /**
     * The color [slot] names in the mode [isDark] picks.
     */
    public operator fun get(
        slot: AccentSlot,
        isDark: Boolean,
    ): Argb = families[slot.index][slot.part, isDark]

    internal companion object {
        /**
         * Build a family for every accent of [document], harmonized with its seed where asked.
         */
        fun from(document: ThemeDocument): AccentFamilies =
            AccentFamilies(
                families = document.accents
                    .map { accent -> AccentFamily.of(accent, themeSeed = document.seed) }
                    .toImmutableList(),
            )
    }
}

/**
 * The color of these four that [part] names.
 */
internal fun AccentColors.part(part: AccentPart): Argb =
    when (part) {
        AccentPart.Color -> color
        AccentPart.OnColor -> onColor
        AccentPart.Container -> container
        AccentPart.OnContainer -> onContainer
    }

/**
 * The library threshold for this document threshold.
 *
 * AAA maps to the normal text rule, since an on color carries body text.
 */
internal fun OnColorThreshold.toCore(): ContrastThreshold =
    when (this) {
        OnColorThreshold.AaNormal -> ContrastThreshold.WCAG_AA_NORMAL_TEXT
        OnColorThreshold.AaLarge -> ContrastThreshold.WCAG_AA_LARGE_TEXT
        OnColorThreshold.Aaa -> ContrastThreshold.WCAG_AAA_NORMAL_TEXT
    }

/**
 * The document color for a color the library handed back, which is always opaque here.
 */
internal fun Color.toDomain(): Argb = Argb(toArgb())
