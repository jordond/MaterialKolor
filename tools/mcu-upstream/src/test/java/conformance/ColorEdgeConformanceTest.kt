package conformance

import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.SchemeTonalSpot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import scheme.SchemeTonalSpot as JavaSchemeTonalSpot

class ColorEdgeConformanceTest {
    @Test
    fun hueBoundariesAndFractionalOrOutOfRangeTonesMatchBothReferencesExactly() {
        for (hue in listOf(
            -360.0,
            -0.001,
            0.0,
            0.001,
            89.999,
            90.0,
            179.999,
            180.0,
            269.999,
            270.0,
            359.999,
            360.0,
            720.0,
        )) {
            for (chroma in listOf(0.0, 0.001, 4.0, 48.0, 120.0)) {
                val palette = TonalPalette.fromHueAndChroma(hue, chroma)
                val rawPalette = upstream.kotlin.palettes.TonalPalette
                    .fromHueAndChroma(hue, chroma)
                val javaPalette = palettes.TonalPalette.fromHueAndChroma(hue, chroma)
                for (tone in listOf(-1.0, 0.0, 0.001, 0.5, 1.0, 49.5, 50.0, 98.5, 99.0, 99.5, 100.0, 101.0)) {
                    val label = "hue=$hue chroma=$chroma tone=$tone"
                    val actual = Hct.from(hue, chroma, tone).toInt()
                    assertEquals(
                        upstream.kotlin.hct.Hct
                            .from(hue, chroma, tone)
                            .toInt(),
                        actual,
                        "$label HCT Kotlin",
                    )
                    assertEquals(hct.Hct.from(hue, chroma, tone).toInt(), actual, "$label HCT Java")
                    assertEquals(rawPalette.getHct(tone).toInt(), palette.getHct(tone).toInt(), "$label palette Kotlin")
                    assertEquals(javaPalette.getHct(tone).toInt(), palette.getHct(tone).toInt(), "$label palette Java")
                }
            }
        }
    }

    @Test
    fun customPalettesAndOpacityClampAndRoundExactlyLikeBothReferences() {
        val spec = com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.SPEC_2021
        val scheme = SchemeTonalSpot(Hct.fromInt(SEED), false, 0.0, spec)
        val rawScheme = upstream.kotlin.scheme.SchemeTonalSpot(
            upstream.kotlin.hct.Hct
                .fromInt(SEED),
            false,
            0.0,
            upstream.kotlin.dynamiccolor.ColorSpec.SpecVersion.SPEC_2021,
        )
        val javaScheme = JavaSchemeTonalSpot(
            hct.Hct.fromInt(SEED),
            false,
            0.0,
            dynamiccolor.ColorSpec.SpecVersion.SPEC_2021,
            dynamiccolor.DynamicScheme.Platform.PHONE,
        )
        for (opacity in listOf(null, -0.1, 0.0, 0.5 / 255.0, 0.5, 254.5 / 255.0, 1.0, 1.1)) {
            for (tone in listOf(0.0, 12.5, 50.0, 99.0, 100.0)) {
                val color = DynamicColor(
                    name = "custom",
                    palette = { TonalPalette.fromHueAndChroma(359.9, 78.0) },
                    tone = { tone },
                    opacity = { opacity },
                )
                val rawColor = upstream.kotlin.dynamiccolor.DynamicColor(
                    name = "custom",
                    palette = {
                        upstream.kotlin.palettes.TonalPalette
                            .fromHueAndChroma(359.9, 78.0)
                    },
                    tone = { tone },
                    opacity = { opacity },
                )
                val javaColor = dynamiccolor.DynamicColor(
                    "custom",
                    { palettes.TonalPalette.fromHueAndChroma(359.9, 78.0) },
                    { tone },
                    false,
                    null,
                    null,
                    null,
                    null,
                    { opacity },
                )
                assertEquals(
                    rawColor.getArgb(rawScheme),
                    color.getArgb(scheme),
                    "opacity=$opacity tone=$tone raw Kotlin",
                )
                assertEquals(
                    javaColor.getArgb(javaScheme),
                    color.getArgb(scheme),
                    "opacity=$opacity tone=$tone raw Java",
                )
            }
        }
    }

    @Test
    fun nanPalettesPreserveKotlinRejectionRatherThanLegacyJavaRounding() {
        // Kotlin's roundToInt rejects NaN. Java Math.round and the retired local port accepted it.
        // This is an intentional raw-language difference, not a portability transformation.
        assertFailsWith<IllegalArgumentException> { TonalPalette.fromHueAndChroma(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> {
            upstream.kotlin.palettes.TonalPalette
                .fromHueAndChroma(Double.NaN, 0.0)
        }
        assertTrue(
            palettes.TonalPalette
                .fromHueAndChroma(Double.NaN, 0.0)
                .hue
                .isNaN(),
        )
    }

    companion object {
        private const val SEED = -0x98af5c
    }
}
