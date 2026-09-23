package com.materialkolor.builder.engine.resolve

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.palettes.TonalPalette
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * One stop on a ramp.
 *
 * @property[tone] The HCT tone of the stop, one of [RampSet.Tones].
 * @property[argb] The palette's color at that tone.
 */
@Immutable
public data class RampStep(
    public val tone: Int,
    public val argb: Argb,
)

/**
 * A tone a role picked from a ramp.
 *
 * Roles rarely land on one of the fixed stops once contrast or the 2025 spec moves them, so the
 * marker carries the exact tone rather than the nearest stop.
 *
 * @property[role] The role that picked this tone.
 * @property[tone] The HCT tone the role resolved to.
 * @property[argb] The color the role resolved to.
 */
@Immutable
public data class RampMarker(
    public val role: Role,
    public val tone: Double,
    public val argb: Argb,
)

/**
 * One of a scheme's six tonal palettes, laid out as the stops the builder shows.
 *
 * @property[palette] Which palette this is.
 * @property[keyColor] The palette's key color.
 * @property[keyTone] The HCT tone of [keyColor].
 * @property[steps] The palette at every tone in [RampSet.Tones], in that order.
 * @property[markers] Every role that picked its tone from this palette, in [Role] order.
 */
@Immutable
public data class Ramp(
    public val palette: KeyColor,
    public val keyColor: Argb,
    public val keyTone: Double,
    public val steps: ImmutableList<RampStep>,
    public val markers: ImmutableList<RampMarker>,
)

/**
 * The six tonal palettes of a theme in both modes.
 *
 * Markers come from the generated schemes, before AMOLED or pins, because they show where a role
 * sits on its palette and a pinned color does not sit on any.
 *
 * @property[light] The ramps of the light scheme, in [Palettes] order.
 * @property[dark] The ramps of the dark scheme, in [Palettes] order.
 */
@Immutable
public class RampSet internal constructor(
    public val light: ImmutableList<Ramp>,
    public val dark: ImmutableList<Ramp>,
) {
    /** The ramps of the mode [isDark] picks. */
    public fun mode(isDark: Boolean): ImmutableList<Ramp> = if (isDark) dark else light

    /** The ramp for [palette] in the mode [isDark] picks. */
    public operator fun get(
        palette: KeyColor,
        isDark: Boolean,
    ): Ramp = mode(isDark).first { ramp -> ramp.palette == palette }

    public companion object {
        /** The tones every ramp is laid out at, darkest first. */
        public val Tones: ImmutableList<Int> =
            persistentListOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 50, 60, 70, 80, 90, 95, 98, 99, 100)

        /** The palettes a ramp set holds, in the order they are shown. */
        public val Palettes: ImmutableList<KeyColor> =
            persistentListOf(
                KeyColor.Primary,
                KeyColor.Secondary,
                KeyColor.Tertiary,
                KeyColor.Neutral,
                KeyColor.NeutralVariant,
                KeyColor.Error,
            )

        /** Lay out the palettes of [light] and [dark] and mark the tones their roles picked. */
        internal fun from(
            light: DynamicScheme,
            dark: DynamicScheme,
        ): RampSet = RampSet(light = ramps(light), dark = ramps(dark))

        private fun ramps(scheme: DynamicScheme): ImmutableList<Ramp> {
            val markers = markers(scheme)
            return Palettes
                .map { palette ->
                    val tonal = scheme.palette(palette)
                    Ramp(
                        palette = palette,
                        keyColor = Argb(tonal.keyColor.toInt()),
                        keyTone = tonal.keyColor.tone,
                        steps = Tones.map { tone -> RampStep(tone, Argb(tonal.tone(tone))) }.toImmutableList(),
                        markers = markers[palette].orEmpty().toImmutableList(),
                    )
                }.toImmutableList()
        }

        /**
         * Every role of [scheme] grouped by the palette it reads from.
         *
         * A role names its palette as a function of the scheme, and every core role hands back one
         * of the scheme's own six, so the answer is matched back by identity alone. Equality would
         * not do, since a Monochrome scheme holds equal neutral and neutral variant palettes. A role
         * that matches none of the six is a bug in the mapping and stops the build.
         */
        private fun markers(scheme: DynamicScheme): Map<KeyColor, List<RampMarker>> {
            val colors = MaterialDynamicColors()
            return Role.entries
                .map { role ->
                    val color = role.dynamicColor(colors)
                    val picked = color.palette(scheme)
                    val owner = checkNotNull(Palettes.firstOrNull { palette -> scheme.palette(palette) === picked }) {
                        "$role reads a palette that is none of the scheme's six"
                    }
                    owner to RampMarker(role, color.getHct(scheme).tone, Argb(color.getArgb(scheme)))
                }.groupBy(keySelector = { pick -> pick.first }, valueTransform = { pick -> pick.second })
        }

        private fun DynamicScheme.palette(palette: KeyColor): TonalPalette =
            when (palette) {
                KeyColor.Primary -> primaryPalette
                KeyColor.Secondary -> secondaryPalette
                KeyColor.Tertiary -> tertiaryPalette
                KeyColor.Error -> errorPalette
                KeyColor.Neutral -> neutralPalette
                KeyColor.NeutralVariant -> neutralVariantPalette
            }
    }
}
