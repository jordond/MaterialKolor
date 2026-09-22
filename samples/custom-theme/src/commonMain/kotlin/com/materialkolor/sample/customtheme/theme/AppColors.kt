package com.materialkolor.sample.customtheme.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * There are seven accent families instead of three, the primary accent carries two interaction tones, surfaces come
 * in three steps, and the drink colors are decorative and belong to no family at all.
 *
 * @see appColors for how the slots are derived.
 */
@Immutable
public data class AppColors(
    public val isLight: Boolean,
    public val primary: Color,
    public val onPrimary: Color,
    public val primaryContainer: Color,
    public val onPrimaryContainer: Color,
    public val primaryPressed: Color,
    public val primaryRaised: Color,
    public val secondary: Color,
    public val onSecondary: Color,
    public val secondaryContainer: Color,
    public val onSecondaryContainer: Color,
    public val tertiary: Color,
    public val onTertiary: Color,
    public val tertiaryContainer: Color,
    public val onTertiaryContainer: Color,
    public val error: Color,
    public val onError: Color,
    public val errorContainer: Color,
    public val onErrorContainer: Color,
    public val love: Color,
    public val onLove: Color,
    public val loveContainer: Color,
    public val onLoveContainer: Color,
    public val cold: Color,
    public val onCold: Color,
    public val coldContainer: Color,
    public val onColdContainer: Color,
    public val warm: Color,
    public val onWarm: Color,
    public val warmContainer: Color,
    public val onWarmContainer: Color,
    public val surface: Color,
    public val surfaceRaised: Color,
    public val surfaceSunken: Color,
    public val surfaceInverse: Color,
    public val onSurface: Color,
    public val onSurfaceInverse: Color,
    public val textStrong: Color,
    public val textMuted: Color,
    public val borderFaint: Color,
    public val borderSoft: Color,
    public val borderStrong: Color,
    public val drinkCoffee: Color,
    public val drinkMatcha: Color,
    public val drinkIced: Color,
    public val drinkTea: Color,
    public val drinkChoc: Color,
    public val scrim: Color,
    public val focusRing: Color,
    public val shadow: Color,
) {
    /**
     * Every color slot by name, so a test can walk the whole theme without listing it again.
     */
    public val slots: Map<String, Color>
        get() = mapOf(
            "primary" to primary,
            "onPrimary" to onPrimary,
            "primaryContainer" to primaryContainer,
            "onPrimaryContainer" to onPrimaryContainer,
            "primaryPressed" to primaryPressed,
            "primaryRaised" to primaryRaised,
            "secondary" to secondary,
            "onSecondary" to onSecondary,
            "secondaryContainer" to secondaryContainer,
            "onSecondaryContainer" to onSecondaryContainer,
            "tertiary" to tertiary,
            "onTertiary" to onTertiary,
            "tertiaryContainer" to tertiaryContainer,
            "onTertiaryContainer" to onTertiaryContainer,
            "error" to error,
            "onError" to onError,
            "errorContainer" to errorContainer,
            "onErrorContainer" to onErrorContainer,
            "love" to love,
            "onLove" to onLove,
            "loveContainer" to loveContainer,
            "onLoveContainer" to onLoveContainer,
            "cold" to cold,
            "onCold" to onCold,
            "coldContainer" to coldContainer,
            "onColdContainer" to onColdContainer,
            "warm" to warm,
            "onWarm" to onWarm,
            "warmContainer" to warmContainer,
            "onWarmContainer" to onWarmContainer,
            "surface" to surface,
            "surfaceRaised" to surfaceRaised,
            "surfaceSunken" to surfaceSunken,
            "surfaceInverse" to surfaceInverse,
            "onSurface" to onSurface,
            "onSurfaceInverse" to onSurfaceInverse,
            "textStrong" to textStrong,
            "textMuted" to textMuted,
            "borderFaint" to borderFaint,
            "borderSoft" to borderSoft,
            "borderStrong" to borderStrong,
            "drinkCoffee" to drinkCoffee,
            "drinkMatcha" to drinkMatcha,
            "drinkIced" to drinkIced,
            "drinkTea" to drinkTea,
            "drinkChoc" to drinkChoc,
            "scrim" to scrim,
            "focusRing" to focusRing,
            "shadow" to shadow,
        )

    /**
     * The foreground and background slots that get painted on top of each other.
     */
    public val contrastPairs: List<ContrastPair>
        get() = listOf(
            ContrastPair("primary", foreground = onPrimary, background = primary),
            ContrastPair("primaryContainer", foreground = onPrimaryContainer, background = primaryContainer),
            ContrastPair("primaryPressed", foreground = onPrimary, background = primaryPressed),
            ContrastPair("primaryRaised", foreground = onPrimary, background = primaryRaised),
            ContrastPair("secondary", foreground = onSecondary, background = secondary),
            ContrastPair("secondaryContainer", foreground = onSecondaryContainer, background = secondaryContainer),
            ContrastPair("tertiary", foreground = onTertiary, background = tertiary),
            ContrastPair("tertiaryContainer", foreground = onTertiaryContainer, background = tertiaryContainer),
            ContrastPair("error", foreground = onError, background = error),
            ContrastPair("errorContainer", foreground = onErrorContainer, background = errorContainer),
            ContrastPair("love", foreground = onLove, background = love),
            ContrastPair("loveContainer", foreground = onLoveContainer, background = loveContainer),
            ContrastPair("cold", foreground = onCold, background = cold),
            ContrastPair("coldContainer", foreground = onColdContainer, background = coldContainer),
            ContrastPair("warm", foreground = onWarm, background = warm),
            ContrastPair("warmContainer", foreground = onWarmContainer, background = warmContainer),
            ContrastPair("surface", foreground = onSurface, background = surface),
            ContrastPair("surfaceRaised", foreground = onSurface, background = surfaceRaised),
            ContrastPair("surfaceSunken", foreground = onSurface, background = surfaceSunken),
            ContrastPair("surfaceInverse", foreground = onSurfaceInverse, background = surfaceInverse),
            ContrastPair("textStrong on surface", foreground = textStrong, background = surface),
            ContrastPair("textMuted on surface", foreground = textMuted, background = surface),
        )

    public companion object {
        /**
         * Slots that nothing is ever written on top of, so no contrast rule applies to them.
         */
        public val DecorativeSlots: Set<String> = setOf(
            "borderFaint",
            "borderSoft",
            "borderStrong",
            "drinkCoffee",
            "drinkMatcha",
            "drinkIced",
            "drinkTea",
            "drinkChoc",
            "scrim",
            "focusRing",
            "shadow",
        )

        /**
         * Slots that are the same color in light and dark.
         *
         * The drinks are brand colors for a fixed set of categories, the focus ring is one accent
         * tone that reads on both, and the scrim and shadow are ink regardless of mode.
         */
        public val ModeIndependentSlots: Set<String> = setOf(
            "drinkCoffee",
            "drinkMatcha",
            "drinkIced",
            "drinkTea",
            "drinkChoc",
            "focusRing",
            "scrim",
            "shadow",
        )
    }
}

/**
 * A foreground color and the background it is painted on.
 *
 * @property[name] The slot the background comes from, used to name a failing assertion.
 * @property[foreground] The content color.
 * @property[background] The color behind it.
 */
@Immutable
public data class ContrastPair(
    public val name: String,
    public val foreground: Color,
    public val background: Color,
)
