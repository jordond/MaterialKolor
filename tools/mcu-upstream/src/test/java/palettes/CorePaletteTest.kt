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
        val source = hct.Hct.fromInt(BLUE)
        assertEquals(282.78817956187277, source.hue, "source hue")
        assertEquals(87.23069368032539, source.chroma, "source chroma")

        // Both factories reuse the source hue and rotate a3 by 60 degrees, and both pin the error
        // palette. They differ in chroma. `of` uses fixed accent and neutral chroma, `contentOf`
        // derives accent chroma from the source and caps the neutrals.
        assertEquals(HUES, CorePalette.of(BLUE).hues(), "legacy hues")
        assertEquals(HUES, CorePalette.contentOf(BLUE).hues(), "content hues")
        assertEquals(
            listOf(87.23069368032539, 16.0, 24.0, 4.0, 8.0, 84.0),
            CorePalette.of(BLUE).chromas(),
            "legacy chromas",
        )
        assertEquals(
            listOf(87.23069368032539, 29.076897893441796, 43.615346840162694, 4.0, 8.0, 84.0),
            CorePalette.contentOf(BLUE).chromas(),
            "content chromas",
        )
    }

    private fun CorePalette.palettes(): List<TonalPalette> = listOf(a1, a2, a3, n1, n2, error)

    private fun CorePalette.hues(): List<Double> = palettes().map { it.hue }

    private fun CorePalette.chromas(): List<Double> = palettes().map { it.chroma }

    companion object {
        private val BLUE = 0xff0000ff.toInt()
        private val HUES = listOf(
            282.78817956187277,
            282.78817956187277,
            342.78817956187277,
            282.78817956187277,
            282.78817956187277,
            25.0,
        )
    }
}
