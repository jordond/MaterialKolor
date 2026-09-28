package com.materialkolor.builder.domain.capability

import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style

/**
 * The spec a style actually runs, which is not always the one the document asks for.
 *
 * Only TonalSpot, Neutral, Vibrant and Expressive exist in the 2025 spec. Every other classic style
 * falls back to 2021, and Cmf only exists in 2026. This mirrors the fallback the color engine does
 * on its own, so the control can show the spec the scheme is really built with.
 *
 * 2026 can be asked for on every style, since the engine falls back from it the same way. It runs
 * as 2025 on a style with a 2025 form and as 2021 on the rest.
 *
 * The document keeps the spec it asked for regardless. Moving to a 2021 only style and back again
 * brings a requested 2025 back.
 */
public object EffectiveSpec {
    /**
     * The spec [style] is built with when [requested] is asked for.
     *
     * A style that has a 2025 form gives 2021 or 2025 as asked, and a request for 2026 lands on
     * 2025 the same way the engine handles it.
     */
    public fun of(
        style: Style,
        requested: SpecVersion,
    ): SpecVersion =
        when (availability(style)) {
            Availability.Classic -> {
                SpecVersion.Spec2021
            }
            Availability.Revised -> {
                when (requested) {
                    SpecVersion.Spec2021 -> SpecVersion.Spec2021
                    SpecVersion.Spec2025 -> SpecVersion.Spec2025
                    SpecVersion.Spec2026 -> SpecVersion.Spec2025
                }
            }
            Availability.CmfOnly -> {
                SpecVersion.Spec2026
            }
        }

    /**
     * The specs the control lets someone pick for [style].
     *
     * The control shows all three specs for every style and turns off the ones missing here. Every
     * style offers 2026 on top of the specs it has a form in, because the engine falls back from
     * 2026 on its own. So a 2021 only style offers 2021 and 2026, and Cmf offers just 2026.
     */
    public fun offered(style: Style): Set<SpecVersion> = forms(style) + SpecVersion.Spec2026

    /**
     * The specs [style] has a form in, the ones a scheme for it can really be built with.
     *
     * A 2021 only style has just 2021, a style with a 2025 form has 2021 and 2025, and Cmf has just
     * 2026.
     */
    public fun forms(style: Style): Set<SpecVersion> =
        when (availability(style)) {
            Availability.Classic -> setOf(SpecVersion.Spec2021)
            Availability.Revised -> setOf(SpecVersion.Spec2021, SpecVersion.Spec2025)
            Availability.CmfOnly -> setOf(SpecVersion.Spec2026)
        }

    private fun availability(style: Style): Availability =
        when (style) {
            Style.TonalSpot,
            Style.Neutral,
            Style.Vibrant,
            Style.Expressive,
            -> Availability.Revised
            Style.Rainbow,
            Style.FruitSalad,
            Style.Monochrome,
            Style.Fidelity,
            Style.Content,
            -> Availability.Classic
            Style.Cmf -> Availability.CmfOnly
        }

    /**
     * Which specs a style has a form in.
     */
    private enum class Availability {
        /**
         * Only the 2021 spec.
         */
        Classic,

        /**
         * The 2021 spec and its 2025 revision.
         */
        Revised,

        /**
         * Only the 2026 spec.
         */
        CmfOnly,
    }
}
