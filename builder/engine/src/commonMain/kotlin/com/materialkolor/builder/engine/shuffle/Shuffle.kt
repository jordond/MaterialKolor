package com.materialkolor.builder.engine.shuffle

import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.dislike.DislikeAnalyzer
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.contrastRatio
import kotlin.random.Random

/**
 * Picks what the Shuffle button and the Space key move to.
 *
 * Seeds are drawn from curated HCT bands, chroma 36 to 84 and tone 40 to 70, and a disliked color
 * is fixed on the way out. A draw is thrown back when it repeats the current seed, is still
 * disliked, or leaves primary on onPrimary under 4.5 to 1 at standard contrast in either mode.
 * After [MAX_DRAWS] misses a short fixed list of seeds takes over, so a shuffle always lands.
 * When no seed on that list reads either, which a primary override can cause, the first one that
 * is new and not disliked is taken anyway, so this last resort can land under 4.5 to 1.
 *
 * With the style unlocked a style is picked too, from those the document's target offers.
 * Monochrome is never picked, since it hides the new seed.
 * Nothing else ever moves. Contrast, spec, target, key colors, pins and accents stay as they are.
 */
public object Shuffle {
    /**
     * The next shuffle of [document] with [locks] applied, drawing from [random].
     *
     * The same [random] state and document always give the same result.
     *
     * @param[resolver] Generates the schemes a candidate seed is checked against. Pass the app's
     * own so the winning seed's schemes are cached for later. Candidates are checked at standard
     * contrast, so the cached schemes only match the app's while the document sits there.
     */
    public fun next(
        random: Random,
        document: ThemeDocument,
        locks: ShuffleLocks,
        resolver: ThemeResolver = ThemeResolver(),
    ): ShuffleResult {
        val style = if (locks.style) null else pickStyle(random, document)
        if (locks.seed) {
            return if (style == null) {
                ShuffleResult.NothingToShuffle
            } else {
                ShuffleResult.Shuffled(seed = document.seed, style = style)
            }
        }
        val seed = pickSeed(
            random = random,
            document = document,
            hueLock = locks.hue,
            style = style ?: document.style,
            resolver = resolver,
        )
        return ShuffleResult.Shuffled(seed = seed, style = style)
    }

    /**
     * The styles [document]'s target lets someone pick, leaving out the one it already has and
     * Monochrome, which would hide the new seed.
     */
    internal fun pickableStyles(document: ThemeDocument): List<Style> =
        Style.entries.filter { style ->
            style != document.style && style != Style.Monochrome && isPickable(document, style)
        }

    private fun pickStyle(
        random: Random,
        document: ThemeDocument,
    ): Style? = pickableStyles(document).takeIf { styles -> styles.isNotEmpty() }?.random(random)

    private fun isPickable(
        document: ThemeDocument,
        style: Style,
    ): Boolean {
        val capabilities = Capabilities.of(
            library = document.library,
            expressive = document.expressive,
            style = style,
            effectiveSpec = EffectiveSpec.of(style, document.spec),
        )
        return when (capabilities[Control.Style]) {
            is ControlState.Enabled -> true
            is ControlState.Hidden -> false
            is ControlState.Disabled -> false
        }
    }

    private fun pickSeed(
        random: Random,
        document: ThemeDocument,
        hueLock: Boolean,
        style: Style,
        resolver: ThemeResolver,
    ): Argb {
        val current = document.seed
        val lockedHue = if (hueLock) Hct.fromInt(current.value).hue else null
        val inputs = SchemeInputs.from(document.copy(style = style, contrast = ContrastLevel.Standard))
        repeat(MAX_DRAWS) {
            val candidate = draw(random, lockedHue)
            if (isGoodSeed(candidate, current, inputs, resolver)) return candidate
        }
        val fallbacks = fallbackSeeds(current, lockedHue)
        return fallbacks.firstOrNull { candidate -> isGoodSeed(candidate, current, inputs, resolver) }
            ?: fallbacks.first { candidate -> candidate != current && !isDisliked(candidate) }
    }

    private fun draw(
        random: Random,
        lockedHue: Double?,
    ): Argb {
        val hue = lockedHue ?: random.nextDouble(FULL_TURN)
        val chroma = random.nextDouble(MIN_CHROMA, MAX_CHROMA)
        val tone = random.nextDouble(MIN_TONE, MAX_TONE)
        return seedOf(hue, chroma, tone)
    }

    /**
     * The seeds tried once every draw has missed, opposite the current hue or on the locked one.
     *
     * The tones differ, so the list always holds a seed other than the current one, and tones 65
     * and 70 are never disliked.
     */
    private fun fallbackSeeds(
        current: Argb,
        lockedHue: Double?,
    ): List<Argb> {
        val hue = lockedHue ?: ((Hct.fromInt(current.value).hue + FULL_TURN / 2) % FULL_TURN)
        return FALLBACK_TONES.map { tone -> seedOf(hue, FALLBACK_CHROMA, tone) }
    }

    private fun seedOf(
        hue: Double,
        chroma: Double,
        tone: Double,
    ): Argb = Argb(DislikeAnalyzer.fixIfDisliked(Hct.from(hue, chroma, tone)).toInt())

    private fun isGoodSeed(
        candidate: Argb,
        current: Argb,
        inputs: SchemeInputs,
        resolver: ThemeResolver,
    ): Boolean =
        candidate != current &&
            !isDisliked(candidate) &&
            primaryReads(inputs.copy(seed = candidate), resolver)

    /** The color as it will be stored is checked, since rounding to ARGB can nudge the hue. */
    private fun isDisliked(seed: Argb): Boolean = DislikeAnalyzer.isDisliked(Hct.fromInt(seed.value))

    /**
     * Whether onPrimary reads on primary in both modes. Pins are the document's own and are
     * left to the contrast audit, so this reads the generated roles.
     */
    private fun primaryReads(
        inputs: SchemeInputs,
        resolver: ThemeResolver,
    ): Boolean {
        val colors = MaterialDynamicColors()
        return listOf(false, true).all { isDark ->
            val scheme = resolver.scheme(inputs, isDark)
            val primary = Color(colors.primary.getArgb(scheme))
            val onPrimary = Color(colors.onPrimary.getArgb(scheme))
            primary.contrastRatio(onPrimary) >= MIN_PRIMARY_CONTRAST
        }
    }

    /** How many random draws a shuffle makes before the fallback seeds take over. */
    public const val MAX_DRAWS: Int = 32

    /** The lowest chroma a drawn seed asks for. */
    public const val MIN_CHROMA: Double = 36.0

    /** The highest chroma a drawn seed asks for. Hues that cannot reach it get their most. */
    public const val MAX_CHROMA: Double = 84.0

    /** The darkest tone a drawn seed has. */
    public const val MIN_TONE: Double = 40.0

    /** The lightest tone a drawn seed has. */
    public const val MAX_TONE: Double = 70.0

    /** What primary on onPrimary has to reach at standard contrast. */
    public const val MIN_PRIMARY_CONTRAST: Double = 4.5

    private const val FULL_TURN: Double = 360.0

    private const val FALLBACK_CHROMA: Double = 48.0

    private val FALLBACK_TONES: List<Double> = listOf(40.0, 50.0, 60.0, 65.0, 70.0)
}
