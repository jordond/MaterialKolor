package scheme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeVibrant
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeVibrantTest {

    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeVibrant(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeVibrant(actualHct, false, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeVibrant(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeVibrant(actualHct, true, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeVibrant(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeVibrant(actualHct, false, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeVibrant(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeVibrant(actualHct, true, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeVibrant(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeVibrant(actualHct, false, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeVibrant(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeVibrant(actualHct, true, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeVibrant(expectedHct, false, ContrastLevels.High)
        val actual = SchemeVibrant(actualHct, false, ContrastLevels.High)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeVibrant(expectedHct, true, ContrastLevels.High)
        val actual = SchemeVibrant(actualHct, true, ContrastLevels.High)
        expected shouldMatch actual
    }
}