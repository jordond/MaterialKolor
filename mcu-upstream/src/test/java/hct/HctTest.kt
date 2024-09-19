package hct;

import com.materialkolor.hct.Hct
import com.materialkolor.hct.ViewingConditions
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.ints.shouldBeExactly
import org.junit.jupiter.api.Test
import utils.shouldBeExactly

class HctTest {

    @Test
    fun from() {
        val expected = hct.Hct.from(180.0, 50.0, 50.0)
        val actual = Hct.from(180.0, 50.0, 50.0)
        expected shouldBeExactly actual
    }

    @Test
    fun fromInt() {
        val expected = hct.Hct.fromInt(0x00FF00)
        val actual = Hct.fromInt(0x00FF00)
        expected shouldBeExactly actual
    }

    @Test
    fun getHue() {
        expectedHct1.hue shouldBeExactly actualHct1.hue
    }

    @Test
    fun getChroma() {
        expectedHct1.chroma shouldBeExactly actualHct1.chroma
    }

    @Test
    fun getTone() {
        expectedHct1.tone shouldBeExactly actualHct1.tone
    }

    @Test
    fun toInt() {
        expectedHct1.toInt() shouldBeExactly actualHct1.toInt()
    }

    @Test
    fun inViewingConditions() {
        val expectedViewingConditions = hct.ViewingConditions.defaultWithBackgroundLstar(50.0)
        val expected = expectedHct1.inViewingConditions(expectedViewingConditions)

        val actualViewingConditions = ViewingConditions.defaultWithBackgroundLstar(50.0)
        val actual = actualHct1.inViewingConditions(actualViewingConditions)

        expected shouldBeExactly actual
    }

    companion object {

        private const val COLOR1 = 0xFFA420
        private const val COLOR2 = 0xC7B446

        private val expectedHct1 = hct.Hct.fromInt(COLOR1)
        private val expectedHct2 = hct.Hct.fromInt(COLOR2)
        private val actualHct1 = Hct.fromInt(COLOR1)
        private val actualHct2 = Hct.fromInt(COLOR2)
    }
}