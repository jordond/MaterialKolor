package com.materialkolor.builder.engine.resolve

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument

/**
 * The part of a document that changes the generated schemes, and nothing more.
 *
 * This is the key the scheme cache is looked up by. Library, export, AMOLED, pins, accents, motion,
 * theme name and custom tones are all applied after generation or never reach it, so they are left
 * out and switching any of them reuses the schemes already built.
 *
 * A style chip or a project thumbnail builds one of these with [copy] and asks
 * [ThemeResolver.scheme] for it, which lands on the same cache the open theme uses.
 *
 * @property[seed] The color the scheme is generated from.
 * @property[keyColors] Palettes set by hand, passed to the library exactly as the document holds
 * them. A primary override pins only the primary palette, the seed still drives the rest.
 * @property[style] The palette style.
 * @property[cmfTertiarySeed] The second seed [Style.Cmf] reads for its tertiary palette.
 * @property[contrast] The contrast level.
 * @property[spec] The spec the document asks for, which may not be the one the style runs.
 * @property[platform] The device the scheme is tuned for.
 */
@Immutable
public data class SchemeInputs(
    public val seed: Argb,
    public val keyColors: KeyColors = KeyColors(),
    public val style: Style = Style.TonalSpot,
    public val cmfTertiarySeed: Argb? = null,
    public val contrast: ContrastLevel = ContrastLevel.Standard,
    public val spec: SpecVersion = SpecVersion.Spec2021,
    public val platform: SchemePlatform = SchemePlatform.Phone,
) {
    /**
     * These inputs with contrast raised to at least the standard level, which is what the builder's
     * own chrome is drawn with. Inputs already at or above standard come back unchanged.
     */
    public fun forChrome(): SchemeInputs =
        if (contrast.hundredths >= ContrastLevel.Standard.hundredths) this else copy(contrast = ContrastLevel.Standard)

    public companion object {
        /**
         * The generation inputs of [document].
         *
         * A tertiary seed only means something to [Style.Cmf], so it is dropped for every other
         * style. Two documents that differ only in a seed nobody reads then share their schemes.
         */
        public fun from(document: ThemeDocument): SchemeInputs =
            SchemeInputs(
                seed = document.seed,
                keyColors = document.keyColors,
                style = document.style,
                cmfTertiarySeed = document.cmfTertiarySeed.takeIf { document.style == Style.Cmf },
                contrast = document.contrast,
                spec = document.spec,
                platform = document.platform,
            )
    }
}
