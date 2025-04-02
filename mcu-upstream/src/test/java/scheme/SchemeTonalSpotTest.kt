package scheme

import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot
import utils.ContrastLevels
import utils.shouldMatch
import kotlin.test.Test

class SchemeTonalSpotTest {
    private val expectedHct = hct.Hct.from(180.0, 50.0, 50.0)
    private val actualHct = Hct.from(180.0, 50.0, 50.0)

    @Test
    fun default() {
        val expected = SchemeTonalSpot(expectedHct, false, ContrastLevels.Default)
        val actual = SchemeTonalSpot(actualHct, false, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun defaultDark() {
        val expected = SchemeTonalSpot(expectedHct, true, ContrastLevels.Default)
        val actual = SchemeTonalSpot(actualHct, true, ContrastLevels.Default)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrast() {
        val expected = SchemeTonalSpot(expectedHct, false, ContrastLevels.Reduced)
        val actual = SchemeTonalSpot(actualHct, false, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun reducedContrastDark() {
        val expected = SchemeTonalSpot(expectedHct, true, ContrastLevels.Reduced)
        val actual = SchemeTonalSpot(actualHct, true, ContrastLevels.Reduced)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrast() {
        val expected = SchemeTonalSpot(expectedHct, false, ContrastLevels.Medium)
        val actual = SchemeTonalSpot(actualHct, false, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun mediumContrastDark() {
        val expected = SchemeTonalSpot(expectedHct, true, ContrastLevels.Medium)
        val actual = SchemeTonalSpot(actualHct, true, ContrastLevels.Medium)
        expected shouldMatch actual
    }

    @Test
    fun highContrast() {
        val expected = SchemeTonalSpot(expectedHct, false, ContrastLevels.High)
        val actual = SchemeTonalSpot(actualHct, false, ContrastLevels.High)
        expected shouldMatch actual
    }

    @Test
    fun highContrastDark() {
        val expected = SchemeTonalSpot(expectedHct, true, ContrastLevels.High)
        val actual = SchemeTonalSpot(actualHct, true, ContrastLevels.High)
        expected shouldMatch actual
    }
}
