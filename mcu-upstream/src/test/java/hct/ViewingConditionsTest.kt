package hct;

import com.materialkolor.hct.ViewingConditions
import io.kotest.matchers.doubles.shouldBeExactly
import utils.shouldBeExactly
import kotlin.test.Test

class ViewingConditionsTest {

    @Test
    fun testMake() {
        val whitePoint = doubleArrayOf(0.95047, 1.0, 1.08883)
        val adaptingLuminance = 11.72
        val backgroundLstar = 50.0
        val surround = 2.0
        val discountingIlluminant = false

        val expected = hct.ViewingConditions.make(
            whitePoint, adaptingLuminance, backgroundLstar, surround, discountingIlluminant
        )
        val actual = ViewingConditions.make(
            whitePoint, adaptingLuminance, backgroundLstar, surround, discountingIlluminant
        )

        expected shouldBeEqualTo actual
    }

    @Test
    fun testMakeDiscountingLuminance() {
        val whitePoint = doubleArrayOf(0.95047, 1.0, 1.08883)
        val adaptingLuminance = 11.72
        val backgroundLstar = 50.0
        val surround = 2.0
        val discountingIlluminant = true

        val expected = hct.ViewingConditions.make(
            whitePoint, adaptingLuminance, backgroundLstar, surround, discountingIlluminant
        )
        val actual = ViewingConditions.make(
            whitePoint, adaptingLuminance, backgroundLstar, surround, discountingIlluminant
        )

        expected shouldBeEqualTo actual
    }

    @Test
    fun testMakeDiscountingSurround() {
        val whitePoint = doubleArrayOf(0.95047, 1.0, 1.08883)
        val adaptingLuminance = 11.72
        val backgroundLstar = 50.0
        val surround = -5.0
        val discountingIlluminant = false

        val expected = hct.ViewingConditions.make(
            whitePoint, adaptingLuminance, backgroundLstar, surround, discountingIlluminant
        )
        val actual = ViewingConditions.make(
            whitePoint, adaptingLuminance, backgroundLstar, surround, discountingIlluminant
        )

        expected shouldBeEqualTo actual
    }

    @Test
    fun testDefaultWithBackgroundLstar() {
        val lstar = 60.0
        val expected = hct.ViewingConditions.defaultWithBackgroundLstar(lstar)
        val actual = ViewingConditions.defaultWithBackgroundLstar(lstar)
        expected shouldBeEqualTo actual
    }

    @Test
    fun testDefaultViewingConditions() {
        val expected = hct.ViewingConditions.DEFAULT
        val actual = ViewingConditions.DEFAULT
        expected shouldBeEqualTo actual
    }

    private infix fun hct.ViewingConditions.shouldBeEqualTo(actual: ViewingConditions) {
        n shouldBeExactly actual.n
        aw shouldBeExactly actual.aw
        nbb shouldBeExactly actual.nbb
        ncb shouldBeExactly actual.ncb
        c shouldBeExactly actual.c
        nc shouldBeExactly actual.nc
        rgbD shouldBeExactly actual.rgbD
        fl shouldBeExactly actual.fl
        flRoot shouldBeExactly actual.flRoot
        z shouldBeExactly actual.z
    }
}