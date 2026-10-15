package scheme

import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.hct.Hct
import io.kotest.assertions.withClue
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemePaletteParityTest {
    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    private val contrastLevels = listOf(
        ContrastLevels.Reduced,
        ContrastLevels.Default,
        ContrastLevels.Medium,
        ContrastLevels.High,
    )

    private val variants = listOf(
        Variant(
            name = "Content",
            upstream = { isDark, contrast -> SchemeContent(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeContent(actualHct, isDark, contrast, specVersion = SpecVersion.SPEC_2021)
            },
        ),
        Variant(
            name = "Expressive",
            upstream = { isDark, contrast -> SchemeExpressive(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeExpressive(
                    actualHct,
                    isDark,
                    contrast,
                    specVersion = SpecVersion.SPEC_2021,
                )
            },
        ),
        Variant(
            name = "Fidelity",
            upstream = { isDark, contrast -> SchemeFidelity(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeFidelity(
                    actualHct,
                    isDark,
                    contrast,
                    specVersion = SpecVersion.SPEC_2021,
                )
            },
        ),
        Variant(
            name = "FruitSalad",
            upstream = { isDark, contrast -> SchemeFruitSalad(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeFruitSalad(
                    actualHct,
                    isDark,
                    contrast,
                    specVersion = SpecVersion.SPEC_2021,
                )
            },
        ),
        Variant(
            name = "Monochrome",
            upstream = { isDark, contrast -> SchemeMonochrome(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeMonochrome(
                    actualHct,
                    isDark,
                    contrast,
                    specVersion = SpecVersion.SPEC_2021,
                )
            },
        ),
        Variant(
            name = "Neutral",
            upstream = { isDark, contrast -> SchemeNeutral(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeNeutral(actualHct, isDark, contrast, specVersion = SpecVersion.SPEC_2021)
            },
        ),
        Variant(
            name = "Rainbow",
            upstream = { isDark, contrast -> SchemeRainbow(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeRainbow(actualHct, isDark, contrast, specVersion = SpecVersion.SPEC_2021)
            },
        ),
        Variant(
            name = "TonalSpot",
            upstream = { isDark, contrast -> SchemeTonalSpot(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeTonalSpot(
                    actualHct,
                    isDark,
                    contrast,
                    specVersion = SpecVersion.SPEC_2021,
                )
            },
        ),
        Variant(
            name = "Vibrant",
            upstream = { isDark, contrast -> SchemeVibrant(expectedHct, isDark, contrast) },
            kolor = { isDark, contrast ->
                com.materialkolor.scheme.SchemeVibrant(actualHct, isDark, contrast, specVersion = SpecVersion.SPEC_2021)
            },
        ),
    )

    @Test
    fun everyVariantBuildsTheSamePalettesAsUpstreamAtEveryContrast() {
        for (variant in variants) {
            for (isDark in listOf(false, true)) {
                for (contrast in contrastLevels) {
                    val expected = variant.upstream(isDark, contrast)
                    val actual = variant.kolor(isDark, contrast)
                    withClue("$variant isDark=$isDark contrast=$contrast") {
                        expected shouldMatch actual
                    }
                }
            }
        }
    }

    private class Variant(
        val name: String,
        val upstream: (isDark: Boolean, contrast: Double) -> dynamiccolor.DynamicScheme,
        val kolor: (isDark: Boolean, contrast: Double) -> DynamicScheme,
    ) {
        override fun toString(): String = name
    }
}
