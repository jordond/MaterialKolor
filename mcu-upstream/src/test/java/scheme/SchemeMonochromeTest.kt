package scheme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeMonochrome
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeMonochromeTest {

    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeMonochrome(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeMonochrome(actualHct, false, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeMonochrome(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeMonochrome(actualHct, true, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeMonochrome(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeMonochrome(actualHct, false, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeMonochrome(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeMonochrome(actualHct, true, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeMonochrome(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeMonochrome(actualHct, false, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeMonochrome(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeMonochrome(actualHct, true, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeMonochrome(expectedHct, false, ContrastLevels.High)
        val actual = SchemeMonochrome(actualHct, false, ContrastLevels.High)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeMonochrome(expectedHct, true, ContrastLevels.High)
        val actual = SchemeMonochrome(actualHct, true, ContrastLevels.High)
        expected shouldMatch actual
    }
}