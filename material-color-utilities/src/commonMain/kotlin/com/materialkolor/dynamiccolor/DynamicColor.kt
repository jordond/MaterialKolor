/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.materialkolor.dynamiccolor

import com.materialkolor.contrast.Contrast
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import dev.drewhamilton.poko.Poko
import kotlin.math.abs
import kotlin.math.round

/**
 * A color that adjusts itself based on UI state, represented by DynamicScheme.
 *
 * This color automatically adjusts to accommodate a desired contrast level, or other adjustments
 * such as differing in light mode versus dark mode, or what the theme is, or what the color that
 * produced the theme is, etc.
 *
 * Colors without backgrounds do not change tone when contrast changes. Colors with backgrounds
 * become closer to their background as contrast lowers, and further when contrast increases.
 *
 * Prefer the static constructors. They provide a much more simple interface, such as requiring
 * just a hexcode, or just a hexcode and a background.
 *
 * Ultimately, each component necessary for calculating a color, adjusting it for a desired
 * contrast level, and ensuring it has a certain lightness/tone difference from another color, is
 * provided by a function that takes a DynamicScheme and returns a value. This ensures ultimate
 * flexibility, any desired behavior of a color for any design system, but it usually unnecessary.
 * See the default constructor for more information.
 *
 * @constructor _Strongly_ prefer using one of the convenience constructors. This class is arguably too
 * flexible to ensure it can support any scenario. Functional arguments allow overriding without
 * risks that come with subclasses.
 *
 * For example, the default behavior of adjust tone at max contrast to be at a 7.0 ratio with
 * its background is principled and matches accessibility guidance. That does not mean it's the
 * desired approach for _every_ design system, and every color pairing, always, in every case.
 *
 * For opaque colors (colors with alpha = 100%).
 *
 * @param name The name of the dynamic color.
 * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
 * defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing
 * a tonal palette, when contrast adjustments are made, intended chroma can be preserved.
 * @param tone Function that provides a tone, given a DynamicScheme.
 * @param isBackground Whether this dynamic color is a background, with some other color as the
 * foreground.
 * @param background The background of the dynamic color (as a function of a `DynamicScheme`), if
 * it exists.
 * @param secondBackground A second background of the dynamic color (as a function of a
 * `DynamicScheme`), if it exists.
 * @param contrastCurve A `ContrastCurve` object specifying how its contrast against its
 * background should behave in various contrast levels options.
 * @param toneDeltaPair A `ToneDeltaPair` object specifying a tone delta constraint between two
 * colors. One of them must be the color being constructed.
 * @param opacity A function returning the opacity of a color, as a number between 0 and 1.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@Poko
public class DynamicColor(
    public val name: String,
    public val palette: (DynamicScheme) -> TonalPalette,
    public val tone: (DynamicScheme) -> Double,
    public val isBackground: Boolean,
    public val chromaMultiplier: ((DynamicScheme) -> Double?)? = null,
    public val background: ((DynamicScheme) -> DynamicColor?)? = null,
    public val secondBackground: ((DynamicScheme) -> DynamicColor?)? = null,
    public val contrastCurve: ((DynamicScheme) -> ContrastCurve?)? = null,
    public val toneDeltaPair: ((DynamicScheme) -> ToneDeltaPair?)? = null,
    public val opacity: ((DynamicScheme) -> Double?)? = null,
) {
    private val hctCache: HashMap<DynamicScheme, Hct> = HashMap()

    /**
     * Returns an ARGB integer (i.e. a hex code).
     *
     * @param scheme Defines the conditions of the user interface, for example, whether or not it is
     * dark mode or light mode, and what the desired contrast level is.
     */
    public fun getArgb(scheme: DynamicScheme): Int {
        val argb: Int = getHct(scheme).toInt()
        val percentage = opacity?.invoke(scheme)
        if (opacity == null || percentage == null) {
            return argb
        }

        val alpha = round(percentage * 255).toInt().coerceIn(0, 255)
        return argb and 0x00ffffff or (alpha shl 24)
    }

    /**
     * Returns an HCT object.
     *
     * @param scheme Defines the conditions of the user interface, for example, whether or not it is
     * dark mode or light mode, and what the desired contrast level is.
     */
    public fun getHct(scheme: DynamicScheme): Hct {
        val cachedAnswer = hctCache[scheme]
        if (cachedAnswer != null) {
            return cachedAnswer
        }
        // This is crucial for aesthetics: we aren't simply the taking the standard color
        // and changing its tone for contrast. Rather, we find the tone for contrast, then
        // use the specified chroma from the palette to construct a new color.
        //
        // For example, this enables colors with standard tone of T90, which has limited chroma, to
        // "recover" intended chroma as contrast increases.
        val answer: Hct = ColorSpecs.get(scheme.specVersion).getHct(scheme, this)
        // NOMUTANTS--trivial test with onerous dependency injection requirement.
        if (hctCache.size > 4) {
            hctCache.clear()
        }
        // NOMUTANTS--trivial test with onerous dependency injection requirement.
        hctCache[scheme] = answer
        return answer
    }

    /**
     * Returns the tone in HCT, ranging from 0 to 100, of the resolved color given scheme.
     */
    public fun getTone(scheme: DynamicScheme): Double =
        ColorSpecs.get(scheme.specVersion).getTone(scheme, this)

    public fun toBuilder(): Builder =
        Builder()
            .setName(this.name)
            .setPalette(this.palette)
            .setTone(this.tone)
            .setIsBackground(this.isBackground)
            .setChromaMultiplier(this.chromaMultiplier)
            .setBackground(this.background)
            .setSecondBackground(this.secondBackground)
            .setContrastCurve(this.contrastCurve)
            .setToneDeltaPair(this.toneDeltaPair)
            .setOpacity(this.opacity)

    public companion object {
        /**
         * A convenience constructor for DynamicColor.
         *
         * _Strongly_ prefer using one of the convenience constructors. This class is arguably too
         * flexible to ensure it can support any scenario. Functional arguments allow overriding without
         * risks that come with subclasses.
         *
         * For example, the default behavior of adjust tone at max contrast to be at a 7.0 ratio with
         * its background is principled and matches accessibility guidance. That does not mean it's the
         * desired approach for _every_ design system, and every color pairing, always, in every case.
         *
         * For opaque colors (colors with alpha = 100%).
         *
         * For colors that are not backgrounds, and do not have backgrounds.
         *
         * @param name The name of the dynamic color.
         * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
         * defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing
         * a tonal palette, when contrast adjustments are made, intended chroma can be preserved.
         * @param tone Function that provides a tone, given a DynamicScheme.
         */
        public fun fromPalette(
            name: String,
            palette: (DynamicScheme) -> TonalPalette,
            tone: (DynamicScheme) -> Double,
        ): DynamicColor =
            DynamicColor(
                name = name,
                palette = palette,
                tone = tone,
                isBackground = false,
                background = null,
                secondBackground = null,
                contrastCurve = null,
                toneDeltaPair = null,
            )

        /**
         * A convenience constructor for DynamicColor.
         *
         * _Strongly_ prefer using one of the convenience constructors. This class is arguably too
         * flexible to ensure it can support any scenario. Functional arguments allow overriding without
         * risks that come with subclasses.
         *
         * For example, the default behavior of adjust tone at max contrast to be at a 7.0 ratio with
         * its background is principled and matches accessibility guidance. That does not mean it's the
         * desired approach for _every_ design system, and every color pairing, always, in every case.
         *
         * For opaque colors (colors with alpha = 100%).
         *
         * For colors that do not have backgrounds.
         *
         * @param name The name of the dynamic color.
         * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
         * defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing
         * a tonal palette, when contrast adjustments are made, intended chroma can be preserved.
         * @param tone Function that provides a tone, given a DynamicScheme.
         * @param isBackground Whether this dynamic color is a background, with some other color as the
         * foreground.
         */
        public fun fromPalette(
            name: String,
            palette: (DynamicScheme) -> TonalPalette,
            tone: (DynamicScheme) -> Double,
            isBackground: Boolean,
        ): DynamicColor =
            DynamicColor(
                name = name,
                palette = palette,
                tone = tone,
                isBackground = isBackground,
                background = null,
                secondBackground = null,
                contrastCurve = null,
                toneDeltaPair = null,
            )

        /**
         * Create a DynamicColor from a hex code.
         *
         * Result has no background; thus no support for increasing/decreasing contrast for a11y.
         *
         * @param name The name of the dynamic color.
         * @param argb The source color from which to extract the hue and chroma.
         */
        public fun fromArgb(
            name: String,
            argb: Int,
        ): DynamicColor {
            val hct: Hct = Hct.fromInt(argb)
            val palette = TonalPalette.fromInt(argb)
            return fromPalette(name, { palette }, { hct.tone })
        }

        /**
         * Given a background tone, find a foreground tone, while ensuring they reach a contrast ratio
         * that is as close to ratio as possible.
         */
        public fun foregroundTone(
            bgTone: Double,
            ratio: Double,
        ): Double {
            val lighterTone: Double = Contrast.lighterUnsafe(bgTone, ratio)
            val darkerTone: Double = Contrast.darkerUnsafe(bgTone, ratio)
            val lighterRatio: Double = Contrast.ratioOfTones(lighterTone, bgTone)
            val darkerRatio: Double = Contrast.ratioOfTones(darkerTone, bgTone)
            val preferLighter = tonePrefersLightForeground(bgTone)
            return if (preferLighter) {
                // "Negligible difference" handles an edge case where the initial contrast ratio is high
                // (ex. 13.0), and the ratio passed to the function is that high ratio, and both the lighter
                // and darker ratio fails to pass that ratio.
                //
                // This was observed with Tonal Spot's On Primary Container turning black momentarily between
                // high and max contrast in light mode. PC's standard tone was T90, OPC's was T10, it was
                // light mode, and the contrast level was 0.6568521221032331.
                val negligibleDifference =
                    abs(lighterRatio - darkerRatio) < 0.1 &&
                        lighterRatio < ratio &&
                        darkerRatio < ratio
                if (lighterRatio >= ratio || lighterRatio >= darkerRatio || negligibleDifference) {
                    lighterTone
                } else {
                    darkerTone
                }
            } else {
                if (darkerRatio >= ratio || darkerRatio >= lighterRatio) darkerTone else lighterTone
            }
        }

        /**
         * Adjust a tone down such that white has 4.5 contrast, if the tone is reasonably close to
         * supporting it.
         */
        public fun enableLightForeground(tone: Double): Double =
            if (tonePrefersLightForeground(tone) && !toneAllowsLightForeground(tone)) {
                49.0
            } else {
                tone
            }

        /**
         * People prefer white foregrounds on ~T60-70. Observed over time, and also by Andrew Somers
         * during research for APCA.
         *
         * T60 used as to create the smallest discontinuity possible when skipping down to T49 in order
         * to ensure light foregrounds.
         *
         * Since `tertiaryContainer` in dark monochrome scheme requires a tone of 60, it should not be
         * adjusted. Therefore, 60 is excluded here.
         */
        public fun tonePrefersLightForeground(tone: Double): Boolean = round(tone) < 60

        /** Tones less than ~T50 always permit white at 4.5 contrast.  */
        public fun toneAllowsLightForeground(tone: Double): Boolean = round(tone) <= 49

        public fun getInitialToneFromBackground(
            background: ((DynamicScheme) -> DynamicColor?)? = null,
        ): (DynamicScheme) -> Double {
            if (background == null) {
                return { scheme -> 50.0 }
            }
            return { scheme: DynamicScheme? ->
                if (scheme == null) 50.0 else background(scheme)?.getTone(scheme) ?: 50.0
            }
        }

        /** Builder for [DynamicColor].  */
        public class Builder {
            private var name: String? = null
            private var palette: ((DynamicScheme) -> TonalPalette)? = null
            private var tone: ((DynamicScheme) -> Double)? = null
            private var isBackground = false
            private var chromaMultiplier: ((DynamicScheme) -> Double?)? = null
            private var background: ((DynamicScheme) -> DynamicColor?)? = null
            private var secondBackground: ((DynamicScheme) -> DynamicColor?)? = null
            private var contrastCurve: ((DynamicScheme) -> ContrastCurve?)? = null
            private var toneDeltaPair: ((DynamicScheme) -> ToneDeltaPair?)? = null
            private var opacity: ((DynamicScheme) -> Double?)? = null

            public fun setName(name: String): Builder =
                apply {
                    this.name = name
                }

            public fun setPalette(palette: (DynamicScheme) -> TonalPalette): Builder =
                apply {
                    this.palette = palette
                }

            public fun setTone(tone: (DynamicScheme) -> Double): Builder =
                apply {
                    this.tone = tone
                }

            public fun setIsBackground(isBackground: Boolean): Builder =
                apply {
                    this.isBackground = isBackground
                }

            public fun setChromaMultiplier(
                chromaMultiplier: ((DynamicScheme) -> Double?)?,
            ): Builder =
                apply {
                    this.chromaMultiplier = chromaMultiplier
                }

            public fun setBackground(background: ((DynamicScheme) -> DynamicColor?)?): Builder =
                apply {
                    this.background = background
                }

            public fun setSecondBackground(
                secondBackground: ((DynamicScheme) -> DynamicColor?)?,
            ): Builder = apply { this.secondBackground = secondBackground }

            public fun setContrastCurve(
                contrastCurve: ((DynamicScheme) -> ContrastCurve?)?,
            ): Builder =
                apply {
                    this.contrastCurve = contrastCurve
                }

            public fun setToneDeltaPair(
                toneDeltaPair: ((DynamicScheme) -> ToneDeltaPair?)?,
            ): Builder =
                apply {
                    this.toneDeltaPair = toneDeltaPair
                }

            public fun setOpacity(opacity: ((DynamicScheme) -> Double?)?): Builder =
                apply {
                    this.opacity = opacity
                }

            public fun extendSpecVersion(
                specVersion: ColorSpec.SpecVersion,
                extendedColor: DynamicColor,
            ): Builder {
                validateExtendedColor(specVersion, extendedColor)

                return Builder()
                    .setName(this.name!!)
                    .setIsBackground(this.isBackground)
                    .setPalette { s: DynamicScheme ->
                        val palette =
                            if (s.specVersion ==
                                specVersion
                            ) {
                                extendedColor.palette
                            } else {
                                this.palette
                            }
                        palette?.invoke(s) ?: extendedColor.palette(s)
                    }.setTone { s: DynamicScheme ->
                        val tone = if (s.specVersion ==
                            specVersion
                        ) {
                            extendedColor.tone
                        } else {
                            this.tone
                        }
                        tone?.invoke(s) ?: extendedColor.tone(s)
                    }.setChromaMultiplier { s: DynamicScheme ->
                        val chromaMultiplier = if (s.specVersion == specVersion) {
                            extendedColor.chromaMultiplier
                        } else {
                            this.chromaMultiplier
                        }
                        if (chromaMultiplier != null) chromaMultiplier(s) else 1.0
                    }.setBackground { s: DynamicScheme ->
                        val background =
                            if (s.specVersion ==
                                specVersion
                            ) {
                                extendedColor.background
                            } else {
                                this.background
                            }
                        background?.invoke(s)
                    }.setSecondBackground { s: DynamicScheme ->
                        val secondBackground =
                            if (s.specVersion == specVersion) {
                                extendedColor.secondBackground
                            } else {
                                this.secondBackground
                            }
                        secondBackground?.invoke(s)
                    }.setContrastCurve { s: DynamicScheme ->
                        val contrastCurve =
                            if (s.specVersion ==
                                specVersion
                            ) {
                                extendedColor.contrastCurve
                            } else {
                                this.contrastCurve
                            }
                        contrastCurve?.invoke(s)
                    }.setToneDeltaPair { s: DynamicScheme ->
                        val toneDeltaPair =
                            if (s.specVersion ==
                                specVersion
                            ) {
                                extendedColor.toneDeltaPair
                            } else {
                                this.toneDeltaPair
                            }
                        toneDeltaPair?.invoke(s)
                    }.setOpacity { s: DynamicScheme ->
                        val opacity =
                            if (s.specVersion ==
                                specVersion
                            ) {
                                extendedColor.opacity
                            } else {
                                this.opacity
                            }
                        opacity?.invoke(s)
                    }
            }

            public fun build(): DynamicColor {
                require(!(background == null && secondBackground != null)) {
                    "Color $name has secondBackground defined, but background is not defined."
                }
                require(!(background == null && contrastCurve != null)) {
                    "Color $name has contrastCurve defined, but background is not defined."
                }
                require(!(background != null && contrastCurve == null)) {
                    "Color $name has background defined, but contrastCurve is not defined."
                }

                val tone = this.tone ?: getInitialToneFromBackground(this.background)
                return DynamicColor(
                    name = this.name!!,
                    palette = this.palette!!,
                    tone = tone,
                    isBackground = this.isBackground,
                    chromaMultiplier = this.chromaMultiplier,
                    background = this.background,
                    secondBackground = this.secondBackground,
                    contrastCurve = this.contrastCurve,
                    toneDeltaPair = this.toneDeltaPair,
                    opacity = this.opacity,
                )
            }

            private fun validateExtendedColor(
                specVersion: ColorSpec.SpecVersion?,
                extendedColor: DynamicColor,
            ) {
                require(this.name == extendedColor.name) {
                    (
                        "Attempting to extend color " +
                            this.name +
                            " with color " +
                            extendedColor.name +
                            " of different name for spec version " +
                            specVersion +
                            "."
                    )
                }
                require(this.isBackground == extendedColor.isBackground) {
                    (
                        "Attempting to extend color " +
                            this.name +
                            " as a " +
                            (if (this.isBackground) "background" else "foreground") +
                            " with color " +
                            extendedColor.name +
                            " as a " +
                            (if (extendedColor.isBackground) "background" else "foreground") +
                            " for spec version " +
                            specVersion +
                            "."
                    )
                }
            }
        }
    }
}
