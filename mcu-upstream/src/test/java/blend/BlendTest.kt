package blend

import com.materialkolor.blend.Blend
import io.kotest.matchers.ints.shouldBeExactly
import kotlin.test.Test

class BlendTest {

    @Test
    fun harmonize() {
        val expected = blend.Blend.harmonize(COLOR1, COLOR2)
        val actual = Blend.harmonize(COLOR1, COLOR2)

        expected shouldBeExactly actual
    }

    @Test
    fun hctHue() {
        val expected = blend.Blend.hctHue(COLOR1, COLOR2, 0.5)
        val actual = Blend.hctHue(COLOR1, COLOR2, 0.5)

        expected shouldBeExactly actual
    }

    @Test
    fun cam16Ucs() {
        val expected = blend.Blend.cam16Ucs(COLOR1, COLOR2, 0.5)
        val actual = Blend.cam16Ucs(COLOR1, COLOR2, 0.5)

        expected shouldBeExactly actual
    }

    companion object {

        private const val COLOR1 = 0x1E2460
        private const val COLOR2 = 0x9E9764
    }
}