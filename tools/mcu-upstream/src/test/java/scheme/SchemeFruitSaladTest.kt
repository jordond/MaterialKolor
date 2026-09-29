package scheme

import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeFruitSalad
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeFruitSaladTest {
    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeFruitSalad(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeFruitSalad(actualHct, false, ContrastLevels.Default, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeFruitSalad(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeFruitSalad(actualHct, true, ContrastLevels.Default, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeFruitSalad(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeFruitSalad(actualHct, false, ContrastLevels.Reduced, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeFruitSalad(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeFruitSalad(actualHct, true, ContrastLevels.Reduced, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeFruitSalad(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeFruitSalad(actualHct, false, ContrastLevels.Medium, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeFruitSalad(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeFruitSalad(actualHct, true, ContrastLevels.Medium, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeFruitSalad(expectedHct, false, ContrastLevels.High)
        val actual = SchemeFruitSalad(actualHct, false, ContrastLevels.High, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeFruitSalad(expectedHct, true, ContrastLevels.High)
        val actual = SchemeFruitSalad(actualHct, true, ContrastLevels.High, specVersion = SpecVersion.SPEC_2021)
        expected shouldMatch actual
    }
}
