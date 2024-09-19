package scheme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeExpressive
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeExpressiveTest {

    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeExpressive(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeExpressive(actualHct, false, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeExpressive(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeExpressive(actualHct, true, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeExpressive(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeExpressive(actualHct, false, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeExpressive(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeExpressive(actualHct, true, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeExpressive(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeExpressive(actualHct, false, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeExpressive(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeExpressive(actualHct, true, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeExpressive(expectedHct, false, ContrastLevels.High)
        val actual = SchemeExpressive(actualHct, false, ContrastLevels.High)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeExpressive(expectedHct, true, ContrastLevels.High)
        val actual = SchemeExpressive(actualHct, true, ContrastLevels.High)
        expected shouldMatch actual
    }
}