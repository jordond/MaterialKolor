package palettes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Legacy Java factories remain a reference only. CorePalettes is not their replacement.
 */
class CorePaletteTest {
    @Test
    fun legacyFactoriesAreRetiredFromThePublishedNamespace() {
        assertFailsWith<ClassNotFoundException> {
            Class.forName("com.materialkolor.palettes.CorePalette")
        }
    }

    @Test
    fun legacyAndContentFactoriesKeepDistinctChromaPolicies() {
        // Hct.fromInt goes through Math.pow, whose last bits differ between JVMs and CPUs. The
        // source pin allows a little slack and everything below derives from it exactly.
        val source = hct.Hct.fromInt(BLUE)
        assertEquals(282.78817956187277, source.hue, SOURCE_TOLERANCE, "source hue")
        assertEquals(87.23069368032539, source.chroma, SOURCE_TOLERANCE, "source chroma")
        val hue = source.hue
        val chroma = source.chroma

        // Both factories reuse the source hue and rotate a3 by 60 degrees, and both pin the error
        // palette. They differ in chroma. `of` uses fixed accent and neutral chroma, `contentOf`
        // derives accent chroma from the source and caps the neutrals.
        val hues = listOf(hue, hue, hue + 60.0, hue, hue, 25.0)
        assertEquals(hues, CorePalette.of(BLUE).hues(), "legacy hues")
        assertEquals(hues, CorePalette.contentOf(BLUE).hues(), "content hues")
        assertEquals(
            listOf(chroma, 16.0, 24.0, 4.0, 8.0, 84.0),
            CorePalette.of(BLUE).chromas(),
            "legacy chromas",
        )
        assertEquals(
            listOf(chroma, chroma / 3.0, chroma / 2.0, 4.0, 8.0, 84.0),
            CorePalette.contentOf(BLUE).chromas(),
            "content chromas",
        )
    }

    private fun CorePalette.palettes(): List<TonalPalette> = listOf(a1, a2, a3, n1, n2, error)

    private fun CorePalette.hues(): List<Double> = palettes().map { it.hue }

    private fun CorePalette.chromas(): List<Double> = palettes().map { it.chroma }

    companion object {
        private val BLUE = 0xff0000ff.toInt()
        private const val SOURCE_TOLERANCE = 1e-9
    }
}
