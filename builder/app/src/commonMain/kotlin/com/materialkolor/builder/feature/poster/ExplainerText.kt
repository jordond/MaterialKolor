package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.mapping.toDomain
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.ktx.onTone
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The seed next to what one mode of a scheme made of it, in HCT. Every explainer sentence is built
 * from these numbers and nothing else.
 *
 * @property[style] The style the scheme was built with.
 * @property[spec] The spec the scheme really ran, after any fallback.
 * @property[isDark] Whether this is the dark scheme.
 * @property[seed] The seed.
 * @property[paletteHue] The hue the style built the primary palette around.
 * @property[paletteChroma] The chroma the style asked of the primary palette.
 * @property[primary] Primary as the scheme generated it, before any pin.
 */
@Immutable
internal data class PrimaryFacts(
    val style: Style,
    val spec: SpecVersion,
    val isDark: Boolean,
    val seed: HctReadout,
    val paletteHue: Double,
    val paletteChroma: Double,
    val primary: HctReadout,
) {
    companion object {
        /** What [scheme] made of [seed] with [style]. */
        fun of(
            seed: Argb,
            style: Style,
            scheme: DynamicScheme,
        ): PrimaryFacts =
            PrimaryFacts(
                style = style,
                spec = scheme.specVersion.toDomain(),
                isDark = scheme.isDark,
                seed = HctReadout.of(seed),
                paletteHue = scheme.primaryPalette.hue,
                paletteChroma = scheme.primaryPalette.chroma,
                primary = HctReadout.of(Argb(scheme.primary)),
            )

        /** What [result] made of its seed in the mode [isDark] picks. */
        fun of(
            result: ThemeResult,
            isDark: Boolean,
        ): PrimaryFacts = of(result.document.seed, result.document.style, result.scheme(isDark))
    }
}

/** How primary reads next to the seed, the word the poster's line leads with. */
internal enum class PrimaryTake {
    /** Less chroma than the seed. */
    Calmer,

    /** More chroma than the seed. */
    Bolder,

    /** A lower tone than the seed. */
    Deeper,

    /** A higher tone than the seed. */
    Lighter,

    /** Another hue than the seed. */
    Turned,
}

/**
 * The sentences the explainer can say. Each one takes the style's name first when [namesStyle] is
 * true, then the whole numbers of its [ExplainerSentence].
 */
internal enum class ExplainerKey(
    val namesStyle: Boolean,
) {
    /** The style holds primary to less chroma than the seed has. */
    ChromaCapped(namesStyle = true),

    /** The style asks primary for more chroma than the seed has. */
    ChromaLifted(namesStyle = true),

    /** The style keeps the seed's chroma. */
    ChromaKept(namesStyle = true),

    /** The style draws primary without chroma. */
    ChromaNone(namesStyle = true),

    /** Primary's hue and tone cannot hold all the chroma the style asked for. */
    ChromaGamut(namesStyle = false),

    /** The style moves primary to another hue. */
    HueTurned(namesStyle = true),

    /** Where the spec puts primary in light mode. */
    ToneLight(namesStyle = false),

    /** Where the spec puts primary in dark mode. */
    ToneDark(namesStyle = false),

    /** What Keep chroma gives, read from the Fidelity scheme. */
    KeepChroma(namesStyle = false),

    /** What Use as primary override gives, read from the scheme with the override. */
    PrimaryOverride(namesStyle = false),
}

/**
 * One sentence of the explainer, a string key and the numbers it is filled with.
 *
 * @property[key] Which sentence it is.
 * @property[values] Its numbers, rounded for the screen, in the order the sentence reads them.
 */
@Immutable
internal data class ExplainerSentence(
    val key: ExplainerKey,
    val values: List<Int>,
)

/**
 * What Match exactly would do, pin primary to the seed and onPrimary to the tone of the primary
 * ramp that reads on it.
 *
 * @property[before] Primary on onPrimary in light mode now.
 * @property[after] The seed on the new light onPrimary.
 * @property[onPrimaryLight] The light onPrimary it pins.
 * @property[onPrimaryDark] The dark onPrimary it pins, or null when dark mode is left alone.
 */
@Immutable
internal data class MatchExactly(
    val seed: Argb,
    val before: Double,
    val after: Double,
    val onPrimaryLight: Argb,
    val onPrimaryDark: Argb?,
) {
    /** Whether small text on the matched primary misses 4.5 to 1. */
    val warns: Boolean
        get() = after < MATCH_TEXT_RATIO

    /** The pins it writes, light first. */
    val pins: List<DocumentChange.SetPin>
        get() = buildList {
            add(DocumentChange.SetPin(Role.Primary, PinMode.Light, seed))
            add(DocumentChange.SetPin(Role.OnPrimary, PinMode.Light, onPrimaryLight))
            if (onPrimaryDark != null) {
                add(DocumentChange.SetPin(Role.Primary, PinMode.Dark, seed))
                add(DocumentChange.SetPin(Role.OnPrimary, PinMode.Dark, onPrimaryDark))
            }
        }

    /**
     * [document] with every pin written. No single change sets several pins, so the explainer
     * lands this through `DocumentChange.Replace` as one undo entry.
     */
    fun applyTo(document: ThemeDocument): ThemeDocument = pins.fold(document) { pinned, pin -> pin.apply(pinned) }

    companion object {
        /**
         * Match exactly for [result], the dark pins too when [pinDark] is true. The seed is pinned as
         * it was typed, and onPrimary comes from each mode's primary ramp at the seed's tone.
         */
        fun of(
            result: ThemeResult,
            seed: Argb,
            pinDark: Boolean,
        ): MatchExactly {
            val tone = HctReadout.of(seed).tone.roundToInt()
            val onLight = Argb(
                result.light.primaryPalette
                    .onTone(tone)
                    .toArgb(),
            )
            val onDark = if (pinDark) {
                Argb(
                    result.dark.primaryPalette
                        .onTone(tone)
                        .toArgb(),
                )
            } else {
                null
            }
            val primary = result.roles[Role.Primary, false].argb
            val onPrimary = result.roles[Role.OnPrimary, false].argb
            return MatchExactly(
                seed = seed,
                before = primary.toColor().contrastRatio(onPrimary.toColor()),
                after = seed.toColor().contrastRatio(onLight.toColor()),
                onPrimaryLight = onLight,
                onPrimaryDark = onDark,
            )
        }
    }
}

/**
 * Builds the explainer's words from HCT values, as string keys and numbers so a test can check
 * them and a translation can reorder them (F-16).
 */
internal object ExplainerText {
    /**
     * How [primary] reads next to [seed], or null when no axis moved more than 2.
     *
     * The axis that moved furthest names the take. Hue only counts while both colors carry enough
     * chroma for a hue to show.
     */
    fun take(
        seed: HctReadout,
        primary: HctReadout,
    ): PrimaryTake? {
        val chroma = primary.chroma - seed.chroma
        val tone = primary.tone - seed.tone
        val hue = if (min(seed.chroma, primary.chroma) < HUE_CHROMA_FLOOR) 0.0 else hueDistance(seed.hue, primary.hue)
        val furthest = maxOf(abs(chroma), abs(tone), abs(hue))
        return when {
            furthest <= NOTICEABLE -> null
            furthest == abs(chroma) -> if (chroma < 0) PrimaryTake.Calmer else PrimaryTake.Bolder
            furthest == abs(tone) -> if (tone < 0) PrimaryTake.Deeper else PrimaryTake.Lighter
            else -> PrimaryTake.Turned
        }
    }

    /**
     * Why primary in [facts] is what it is, the chroma first, then the hue and the tone. There is
     * always a chroma and a tone sentence.
     */
    fun sentences(facts: PrimaryFacts): List<ExplainerSentence> =
        buildList {
            add(chroma(facts))
            gamut(facts)?.let(::add)
            hue(facts)?.let(::add)
            add(tone(facts))
        }

    /** What Keep chroma would give, from the [fidelity] scheme built from the same seed. */
    fun keepChroma(
        seed: Argb,
        fidelity: DynamicScheme,
    ): ExplainerSentence {
        val container = HctReadout.of(Argb(fidelity.primaryContainer))
        return ExplainerSentence(
            ExplainerKey.KeepChroma,
            listOf(
                fidelity.primaryPalette.chroma.roundToInt(),
                container.tone.roundToInt(),
                HctReadout.of(seed).tone.roundToInt(),
            ),
        )
    }

    /** What Use as primary override would give, from the [overridden] scheme. */
    fun primaryOverride(overridden: DynamicScheme): ExplainerSentence =
        ExplainerSentence(
            ExplainerKey.PrimaryOverride,
            listOf(
                overridden.specVersion.toDomain().year,
                HctReadout.of(Argb(overridden.primary)).tone.roundToInt(),
            ),
        )

    private fun chroma(facts: PrimaryFacts): ExplainerSentence {
        val palette = facts.paletteChroma.roundToInt()
        val seed = facts.seed.chroma.roundToInt()
        return when {
            palette == 0 -> {
                ExplainerSentence(ExplainerKey.ChromaNone, listOf(seed))
            }
            facts.seed.chroma - facts.paletteChroma > NOTICEABLE -> {
                ExplainerSentence(ExplainerKey.ChromaCapped, listOf(palette, seed))
            }
            facts.paletteChroma - facts.seed.chroma > NOTICEABLE -> {
                ExplainerSentence(ExplainerKey.ChromaLifted, listOf(palette, seed))
            }
            else -> {
                ExplainerSentence(ExplainerKey.ChromaKept, listOf(seed))
            }
        }
    }

    /** The chroma primary really got when its hue and tone hold less than the style asked for. */
    private fun gamut(facts: PrimaryFacts): ExplainerSentence? {
        if (facts.paletteChroma - facts.primary.chroma <= NOTICEABLE) return null
        return ExplainerSentence(
            ExplainerKey.ChromaGamut,
            listOf(facts.primary.tone.roundToInt(), facts.primary.chroma.roundToInt()),
        )
    }

    private fun hue(facts: PrimaryFacts): ExplainerSentence? {
        if (facts.seed.chroma < HUE_CHROMA_FLOOR || facts.paletteChroma < HUE_CHROMA_FLOOR) return null
        val turn = hueDistance(facts.seed.hue, facts.paletteHue)
        if (abs(turn) <= NOTICEABLE) return null
        return ExplainerSentence(
            ExplainerKey.HueTurned,
            listOf(abs(turn).roundToInt(), wholeHue(facts.seed.hue), wholeHue(facts.paletteHue)),
        )
    }

    private fun tone(facts: PrimaryFacts): ExplainerSentence =
        ExplainerSentence(
            if (facts.isDark) ExplainerKey.ToneDark else ExplainerKey.ToneLight,
            listOf(facts.spec.year, facts.primary.tone.roundToInt(), facts.seed.tone.roundToInt()),
        )

    /** How far [to] sits from [from] around the hue circle, signed, within half a turn. */
    private fun hueDistance(
        from: Double,
        to: Double,
    ): Double = ((to - from) % FULL_TURN + FULL_TURN + HALF_TURN) % FULL_TURN - HALF_TURN

    private fun wholeHue(hue: Double): Int = hue.roundToInt() % FULL_TURN.toInt()
}

/** The year a spec is named after, as the explainer says it. */
internal val SpecVersion.year: Int
    get() = when (this) {
        SpecVersion.Spec2021 -> 2021
        SpecVersion.Spec2025 -> 2025
        SpecVersion.Spec2026 -> 2026
    }

/** How far apart in hue, chroma or tone two colors sit before the poster calls them different. */
private const val NOTICEABLE = 2.0

/** Below this chroma a hue barely shows, so a change of hue says nothing. */
private const val HUE_CHROMA_FLOOR = 5.0

/** The ratio small text needs for AA, below which Match exactly warns. */
private const val MATCH_TEXT_RATIO = 4.5

private const val FULL_TURN = 360.0
private const val HALF_TURN = 180.0
