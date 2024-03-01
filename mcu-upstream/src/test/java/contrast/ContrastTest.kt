package contrast

import com.materialkolor.contrast.Contrast
import com.materialkolor.hct.Hct
import com.materialkolor.utils.ColorUtils
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.equals.shouldBeEqual
import kotlin.test.Test

class ContrastTest {

    @Test
    fun ratioOfYs() {
        val expectedFirstXyz = utils.ColorUtils.xyzFromArgb(COLOR1).toList()
        val expectedSecondXyz = utils.ColorUtils.xyzFromArgb(COLOR2).toList()
        val expected = contrast.Contrast.ratioOfYs(expectedFirstXyz[1], expectedSecondXyz[1])

        val actualFirstXyz = ColorUtils.xyzFromArgb(COLOR1).toList()
        val actualSecondXyz = ColorUtils.xyzFromArgb(COLOR2).toList()
        val actual = Contrast.ratioOfYs(actualFirstXyz[1], actualSecondXyz[1])

        expectedFirstXyz shouldContainExactly actualFirstXyz
        expectedSecondXyz shouldContainExactly actualSecondXyz
        expected shouldBeExactly  actual
    }

    @Test
    fun ratioOfTones() {
        val expectedFirstHct = hct.Hct.fromInt(COLOR1)
        val expectedSecondHct = hct.Hct.fromInt(COLOR2)
        val expected = contrast.Contrast.ratioOfTones(expectedFirstHct.tone, expectedSecondHct.tone)

        val actualFirstHct = Hct.fromInt(COLOR1)
        val actualSecondHct = Hct.fromInt(COLOR2)
        val actual = Contrast.ratioOfTones(actualFirstHct.tone, actualSecondHct.tone)

        expectedFirstHct.toInt() shouldBeEqual actualFirstHct.toInt()
        expectedSecondHct.toInt() shouldBeEqual actualSecondHct.toInt()
        expected shouldBeExactly actual
    }

    @Test
    fun lighter() {
        val expectedHct = hct.Hct.fromInt(COLOR1)
        val expected = contrast.Contrast.lighter(expectedHct.tone, 1.0)

        val actualHct = Hct.fromInt(COLOR1)
        val actual = Contrast.lighter(actualHct.tone, 1.0)

        listOf(expected, actual) shouldNotContain -1
        expected shouldBeExactly actual
    }

    @Test
    fun lighterUnsafe() {
        val expectedHct = hct.Hct.fromInt(COLOR1)
        val expected = contrast.Contrast.lighterUnsafe(expectedHct.tone, 1.0)

        val actualHct = Hct.fromInt(COLOR1)
        val actual = Contrast.lighterUnsafe(actualHct.tone, 1.0)

        expected shouldBeExactly actual
    }

    @Test
    fun lighterUnsafe100() {
        val expectedHct = hct.Hct.fromInt(COLOR1)
        val expected = contrast.Contrast.lighterUnsafe(expectedHct.tone, 99.0)

        val actualHct = Hct.fromInt(COLOR1)
        val actual = Contrast.lighterUnsafe(actualHct.tone, 99.0)

        expected shouldBeExactly 100.0
        actual shouldBeExactly 100.0
    }

    @Test
    fun darker() {
        val expectedHct = hct.Hct.fromInt(COLOR1)
        val expected = contrast.Contrast.darker(expectedHct.tone, 1.0)

        val actualHct = Hct.fromInt(COLOR1)
        val actual = Contrast.darker(actualHct.tone, 1.0)

        listOf(expected, actual) shouldNotContain -1
        expected shouldBeExactly actual
    }

    @Test
    fun darkerUnsafe() {
        val expectedHct = hct.Hct.fromInt(COLOR1)
        val expected = contrast.Contrast.lighterUnsafe(expectedHct.tone, 1.1)

        val actualHct = Hct.fromInt(COLOR1)
        val actual = Contrast.lighterUnsafe(actualHct.tone, 1.1)

        expected shouldBeExactly actual
    }

    @Test
    fun darkerUnsafe100() {
        val expectedHct = hct.Hct.fromInt(COLOR1)
        val expected = contrast.Contrast.darkerUnsafe(expectedHct.tone, 99.0)

        val actualHct = Hct.fromInt(COLOR1)
        val actual = Contrast.darkerUnsafe(actualHct.tone, 99.0)

        expected shouldBeExactly 0.0
        actual shouldBeExactly 0.0
    }

    companion object {

        private const val COLOR1 = 0x6C7059
        private const val COLOR2 = 0xB44C43
    }
}