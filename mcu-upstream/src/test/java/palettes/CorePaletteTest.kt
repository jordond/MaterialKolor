package palettes

import com.materialkolor.palettes.CorePalette
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CorePaletteTest {

    @Test
    fun of() {
        val expectedPalette = palettes.CorePalette.of(COLOR)
        val actualPalette = CorePalette.of(COLOR)

        assertPalettesEqual(expectedPalette, actualPalette)
    }

    @Test
    fun ofInt() {
        val expectedPalette = palettes.CorePalette.of(COLOR)
        val actualPalette = CorePalette.of(COLOR)

        assertPalettesEqual(expectedPalette, actualPalette)
    }

    @Test
    fun contentOf() {
        val expectedPalette = palettes.CorePalette.contentOf(COLOR)
        val actualPalette = CorePalette.contentOf(COLOR)

        assertPalettesEqual(expectedPalette, actualPalette)
    }

    private fun assertPalettesEqual(expected: palettes.CorePalette, actual: CorePalette) {
        expected.a1 shouldBeExactly actual.a1
        expected.a2 shouldBeExactly actual.a2
        expected.a3 shouldBeExactly actual.a3
        expected.n1 shouldBeExactly actual.n1
        expected.n2 shouldBeExactly actual.n2
        expected.error shouldBeExactly actual.error
    }

    private infix fun TonalPalette.shouldBeExactly(actual: com.materialkolor.palettes.TonalPalette) {
        this.hue shouldBe actual.hue
        this.chroma shouldBe actual.chroma
        for (tone in 0..100 step 10) {
            this.tone(tone) shouldBe actual.tone(tone)
        }
    }

    companion object {
        private const val COLOR = 0x0000FF
    }
}