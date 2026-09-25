package com.materialkolor.builder.domain.capability

import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.SpecVersion.Spec2021
import com.materialkolor.builder.domain.model.SpecVersion.Spec2025
import com.materialkolor.builder.domain.model.SpecVersion.Spec2026
import com.materialkolor.builder.domain.model.Style
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectiveSpecTest {
    /**
     * The S1 table, the effective spec for each style when 2021, 2025 or 2026 is requested.
     *
     * A 2026 request on a style with a 2025 form lands on 2025, the same fallback the color
     * engine does.
     */
    private val effective: Map<Style, List<SpecVersion>> = mapOf(
        Style.TonalSpot to listOf(Spec2021, Spec2025, Spec2025),
        Style.Neutral to listOf(Spec2021, Spec2025, Spec2025),
        Style.Vibrant to listOf(Spec2021, Spec2025, Spec2025),
        Style.Expressive to listOf(Spec2021, Spec2025, Spec2025),
        Style.Rainbow to listOf(Spec2021, Spec2021, Spec2021),
        Style.FruitSalad to listOf(Spec2021, Spec2021, Spec2021),
        Style.Monochrome to listOf(Spec2021, Spec2021, Spec2021),
        Style.Fidelity to listOf(Spec2021, Spec2021, Spec2021),
        Style.Content to listOf(Spec2021, Spec2021, Spec2021),
        Style.Cmf to listOf(Spec2026, Spec2026, Spec2026),
    )

    /**
     * What the spec control lets someone pick, per style.
     */
    private val offered: Map<Style, Set<SpecVersion>> = mapOf(
        Style.TonalSpot to setOf(Spec2021, Spec2025),
        Style.Neutral to setOf(Spec2021, Spec2025),
        Style.Vibrant to setOf(Spec2021, Spec2025),
        Style.Expressive to setOf(Spec2021, Spec2025),
        Style.Rainbow to setOf(Spec2021),
        Style.FruitSalad to setOf(Spec2021),
        Style.Monochrome to setOf(Spec2021),
        Style.Fidelity to setOf(Spec2021),
        Style.Content to setOf(Spec2021),
        Style.Cmf to setOf(Spec2026),
    )

    private val requests = listOf(Spec2021, Spec2025, Spec2026)

    @Test
    fun tables_everyStyle_hasARow() {
        assertEquals(Style.entries.toSet(), effective.keys)
        assertEquals(Style.entries.toSet(), offered.keys)
        assertEquals(SpecVersion.entries, requests)
    }

    @Test
    fun of_everyStyleAndRequest_matchesTheS1Table() {
        for ((style, row) in effective) {
            requests.forEachIndexed { index, requested ->
                assertEquals(row[index], EffectiveSpec.of(style, requested), "$style when $requested is requested")
            }
        }
    }

    @Test
    fun offered_everyStyle_matchesTheS1Table() {
        for ((style, specs) in offered) {
            assertEquals(specs, EffectiveSpec.offered(style), style.name)
        }
    }

    @Test
    fun of_anyRequest_landsOnASpecTheStyleOffers() {
        for (style in Style.entries) {
            for (requested in SpecVersion.entries) {
                val spec = EffectiveSpec.of(style, requested)
                assertTrue(spec in EffectiveSpec.offered(style), "$style ran $spec, which it does not offer")
            }
        }
    }
}
