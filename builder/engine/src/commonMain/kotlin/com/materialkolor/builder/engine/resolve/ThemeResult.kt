package com.materialkolor.builder.engine.resolve

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.audit.ContrastAudit
import com.materialkolor.builder.engine.mapping.toDomain
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.dynamiccolor.DynamicScheme

/**
 * Everything the builder shows for one document, generated once and read many times.
 *
 * Both modes are always built, so flipping between light and dark never generates anything. The
 * role tables and ramps are worked out the first time someone reads them.
 *
 * @property[document] The document this was resolved from.
 * @property[light] The light scheme, generated with the document's own contrast.
 * @property[dark] The dark scheme, generated with the document's own contrast.
 */
@Immutable
public class ThemeResult internal constructor(
    public val document: ThemeDocument,
    public val light: DynamicScheme,
    public val dark: DynamicScheme,
    private val chromeLight: DynamicScheme,
    private val chromeDark: DynamicScheme,
) {
    /**
     * The spec the schemes were really built with.
     *
     * A style that has no form in the requested spec falls back, so this can differ from the
     * document's spec. It always agrees with `EffectiveSpec.of` for the same style and request.
     */
    public val effectiveSpec: SpecVersion
        get() = light.specVersion.toDomain()

    /**
     * Every role in both modes, with AMOLED and pins applied.
     */
    public val roles: RoleTables by lazy { RoleTables.from(light, dark, document) }

    /**
     * The six tonal palettes in both modes, with the tones their roles picked.
     */
    public val ramps: RampSet by lazy { RampSet.from(light, dark) }

    /**
     * Every accent of the document as a family, in the document's order.
     */
    public val accents: AccentFamilies by lazy { AccentFamilies.from(document) }

    /**
     * Every slot of the Custom target in both modes, with the document's custom tones moved in.
     */
    public val customSlots: CustomSlotColors by lazy { CustomSlotColors.from(document, light, dark, roles) }

    /**
     * Every slot of the Custom target cut from the [chrome] schemes, for the builder's own Custom skin.
     *
     * Pins, AMOLED and custom tones stay out, the same as they stay out of [chrome], so nothing a
     * document does to its own slots can make the builder hard to read.
     */
    public val chromeCustomSlots: CustomSlotColors by lazy {
        val plain = document.copy(amoled = false, pins = emptyMap(), customTones = emptyMap())
        val chromeRoles = RoleTables.from(chromeLight, chromeDark, plain)
        CustomSlotColors.from(plain, chromeLight, chromeDark, chromeRoles)
    }

    /**
     * The colors of the seed poster.
     */
    public val poster: PosterColors by lazy { PosterColors.of(document.seed) }

    /**
     * Every contrast pair of the document's target, rated in both modes.
     */
    public val audit: ContrastAudit by lazy { ContrastAudit.from(this) }

    /**
     * The document scheme for the mode [isDark] picks.
     */
    public fun scheme(isDark: Boolean): DynamicScheme = if (isDark) dark else light

    /**
     * The scheme the builder's own chrome is drawn with in the mode [isDark] picks.
     *
     * It is the document scheme with contrast floored at the standard level, so a theme tuned for
     * reduced contrast does not make the builder itself hard to read. At standard contrast or above
     * it is the very same scheme object as [scheme].
     */
    public fun chrome(isDark: Boolean): DynamicScheme = if (isDark) chromeDark else chromeLight
}
