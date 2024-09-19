package scheme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeContent
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeContentTest {

    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeContent(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeContent(actualHct, false, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeContent(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeContent(actualHct, true, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeContent(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeContent(actualHct, false, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeContent(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeContent(actualHct, true, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeContent(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeContent(actualHct, false, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeContent(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeContent(actualHct, true, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeContent(expectedHct, false, ContrastLevels.High)
        val actual = SchemeContent(actualHct, false, ContrastLevels.High)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeContent(expectedHct, true, ContrastLevels.High)
        val actual = SchemeContent(actualHct, true, ContrastLevels.High)
        expected shouldMatch actual
    }
}