package com.materialkolor.builder.engine.mapping

import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CoreMappingTest {
    @Test
    fun toPaletteStyle_cmf_carriesTheTertiarySeed() {
        val seed = Argb(0x00897B)

        val seeded = assertIs<PaletteStyle.Cmf>(Style.Cmf.toPaletteStyle(seed))
        val unseeded = assertIs<PaletteStyle.Cmf>(Style.Cmf.toPaletteStyle(null))

        assertEquals(seed.value, seeded.tertiarySeedColor?.toArgb())
        assertNull(unseeded.tertiarySeedColor)
    }

    @Test
    fun toPaletteStyle_classicStyle_ignoresTheTertiarySeed() {
        assertEquals(PaletteStyle.TonalSpot, Style.TonalSpot.toPaletteStyle(Argb(0x00897B)))
    }

    @Test
    fun toCore_everySpec_roundTrips() {
        for (spec in SpecVersion.entries) {
            assertEquals(spec, spec.toCore().toDomain())
        }
        assertEquals(ColorSpec.SpecVersion.entries.toSet(), SpecVersion.entries.map { spec -> spec.toCore() }.toSet())
    }

    @Test
    fun toCore_everyPlatform_matchesByName() {
        for (platform in SchemePlatform.entries) {
            assertEquals(DynamicScheme.Platform.valueOf(platform.name.uppercase()), platform.toCore())
        }
    }
}
