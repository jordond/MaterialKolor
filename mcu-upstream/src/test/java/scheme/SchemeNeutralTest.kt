package scheme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeNeutral
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeNeutralTest {

    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeNeutral(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeNeutral(actualHct, false, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeNeutral(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeNeutral(actualHct, true, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeNeutral(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeNeutral(actualHct, false, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeNeutral(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeNeutral(actualHct, true, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeNeutral(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeNeutral(actualHct, false, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeNeutral(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeNeutral(actualHct, true, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeNeutral(expectedHct, false, ContrastLevels.High)
        val actual = SchemeNeutral(actualHct, false, ContrastLevels.High)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeNeutral(expectedHct, true, ContrastLevels.High)
        val actual = SchemeNeutral(actualHct, true, ContrastLevels.High)
        expected shouldMatch actual
    }
}