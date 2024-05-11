package hct;

import com.materialkolor.hct.Cam16
import com.materialkolor.hct.ViewingConditions
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.ints.shouldBeExactly
import org.junit.jupiter.api.Test
import utils.shouldBeExactly

class Cam16Test {

    @Test
    fun distance() {
        val expected = expectedCam1.distance(expectedCam2)
        val actual = actualCam1.distance(actualCam2)
        expected shouldBeExactly actual
    }

    @Test
    fun getHue() {
        val expected = expectedCam1.hue
        val actual = actualCam1.hue
        expected shouldBeExactly actual
    }

    @Test
    fun getChroma() {
        val expected = expectedCam1.chroma
        val actual = actualCam1.chroma
        expected shouldBeExactly actual
    }

    @Test
    fun getJ() {
        val expected = expectedCam1.j
        val actual = actualCam1.j
        expected shouldBeExactly actual
    }

    @Test
    fun getQ() {
        val expected = expectedCam1.q
        val actual = actualCam1.q
        expected shouldBeExactly actual
    }

    @Test
    fun getM() {
        val expected = expectedCam1.m
        val actual = actualCam1.m
        expected shouldBeExactly actual
    }

    @Test
    fun getS() {
        val expected = expectedCam1.s
        val actual = actualCam1.s
        expected shouldBeExactly actual
    }

    @Test
    fun getJstar() {
        val expected = expectedCam1.jstar
        val actual = actualCam1.jstar
        expected shouldBeExactly actual
    }

    @Test
    fun getAstar() {
        val expected = expectedCam1.astar
        val actual = actualCam1.astar
        expected shouldBeExactly actual
    }

    @Test
    fun getBstar() {
        val expected = expectedCam1.bstar
        val actual = actualCam1.bstar
        expected shouldBeExactly actual
    }

    @Test
    fun fromInt() {
        val expected = hct.Cam16.fromInt(COLOR1)
        val actual = Cam16.fromInt(COLOR1)
        expected shouldBeExactly actual
    }

    @Test
    fun fromIntInViewingConditions() {
        val expected = hct.Cam16.fromIntInViewingConditions(COLOR1, hct.ViewingConditions.DEFAULT)
        val actual = Cam16.fromIntInViewingConditions(COLOR1, ViewingConditions.DEFAULT)
        expected shouldBeExactly actual
    }

    @Test
    fun fromXyzInViewingConditions() {
        val expected = hct.Cam16
            .fromXyzInViewingConditions(1.0, 2.0, 3.0, hct.ViewingConditions.DEFAULT)
        val actual = Cam16
            .fromXyzInViewingConditions(1.0, 2.0, 3.0, ViewingConditions.DEFAULT)
        expected shouldBeExactly actual
    }

    @Test
    fun fromJch() {
        val expected = hct.Cam16.fromJch(1.0, 2.0, 3.0)
        val actual = Cam16.fromJch(1.0, 2.0, 3.0)
        expected shouldBeExactly actual
    }

    @Test
    fun fromUcs() {
        val expected = hct.Cam16.fromUcs(1.0, 2.0, 3.0)
        val actual = Cam16.fromUcs(1.0, 2.0, 3.0)
        expected shouldBeExactly actual
    }

    @Test
    fun fromUcsInViewingConditions() {
        val expected = hct
            .Cam16.fromUcsInViewingConditions(1.0, 2.0, 3.0, hct.ViewingConditions.DEFAULT)
        val actual = Cam16
            .fromUcsInViewingConditions(1.0, 2.0, 3.0, ViewingConditions.DEFAULT)
        expected shouldBeExactly actual
    }

    @Test
    fun toInt() {
        val expected = expectedCam1.toInt()
        val actual = actualCam1.toInt()
        expected shouldBeExactly actual
    }

    @Test
    fun viewed() {
        val expected = expectedCam1.viewed(hct.ViewingConditions.DEFAULT)
        val actual = actualCam1.viewed(ViewingConditions.DEFAULT)
        expected shouldBeExactly actual
    }

    @Test
    fun xyzInViewingConditionsNull() {
        val expected = expectedCam1.xyzInViewingConditions(hct.ViewingConditions.DEFAULT, null)
        val actual = actualCam1.xyzInViewingConditions(ViewingConditions.DEFAULT, null)
        expected.toList() shouldContainExactly actual.toList()
    }

    @Test
    fun xyzInViewingConditions() {
        val expectedArray = doubleArrayOf(1.0, 2.0, 3.0)
        val actualArray = expectedArray.copyOf()
        val expected = expectedCam1.xyzInViewingConditions(hct.ViewingConditions.DEFAULT, expectedArray)
        val actual = actualCam1.xyzInViewingConditions(ViewingConditions.DEFAULT, actualArray)
        expected.toList() shouldContainExactly actual.toList()
    }

    companion object {

        private const val COLOR1 = 0xFFA420
        private const val COLOR2 = 0xC7B446

        private val expectedCam1 = hct.Cam16.fromInt(COLOR1)
        private val expectedCam2 = hct.Cam16.fromInt(COLOR2)
        private val actualCam1 = Cam16.fromInt(COLOR1)
        private val actualCam2 = Cam16.fromInt(COLOR2)
    }
}