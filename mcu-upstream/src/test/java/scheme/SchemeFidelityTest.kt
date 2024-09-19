package scheme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeFidelity
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeFidelityTest {

    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeFidelity(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeFidelity(actualHct, false, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeFidelity(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeFidelity(actualHct, true, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeFidelity(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeFidelity(actualHct, false, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeFidelity(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeFidelity(actualHct, true, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeFidelity(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeFidelity(actualHct, false, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeFidelity(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeFidelity(actualHct, true, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeFidelity(expectedHct, false, ContrastLevels.High)
        val actual = SchemeFidelity(actualHct, false, ContrastLevels.High)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeFidelity(expectedHct, true, ContrastLevels.High)
        val actual = SchemeFidelity(actualHct, true, ContrastLevels.High)
        expected shouldMatch actual
    }
}