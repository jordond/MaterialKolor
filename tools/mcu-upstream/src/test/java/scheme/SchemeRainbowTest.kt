package scheme

import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeRainbow
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeRainbowTest {
    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeRainbow(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeRainbow(actualHct, false, ContrastLevels.Default, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeRainbow(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeRainbow(actualHct, true, ContrastLevels.Default, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeRainbow(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeRainbow(actualHct, false, ContrastLevels.Reduced, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeRainbow(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeRainbow(actualHct, true, ContrastLevels.Reduced, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeRainbow(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeRainbow(actualHct, false, ContrastLevels.Medium, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeRainbow(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeRainbow(actualHct, true, ContrastLevels.Medium, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeRainbow(expectedHct, false, ContrastLevels.High)
        val actual = SchemeRainbow(actualHct, false, ContrastLevels.High, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeRainbow(expectedHct, true, ContrastLevels.High)
        val actual = SchemeRainbow(actualHct, true, ContrastLevels.High, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }
}
