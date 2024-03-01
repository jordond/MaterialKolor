package hct;

import com.materialkolor.hct.HctSolver
import com.materialkolor.utils.ColorUtils
import io.kotest.matchers.ints.shouldBeExactly
import utils.shouldBeExactly
import kotlin.test.Test

class HctSolverTest {

    @Test
    fun solveToInt() {
        val expectedLstar = utils.ColorUtils.lstarFromArgb(0xC7B446)
        val expected = hct.HctSolver.solveToInt(90.0, 50.0, expectedLstar)
        val actualLstar = ColorUtils.lstarFromArgb(0xC7B446)
        val actual = HctSolver.solveToInt(90.0, 50.0, actualLstar)
        actual shouldBeExactly expected
    }

    @Test
    fun solveToIntFromLstarChroma() {
        val expectedLstar = utils.ColorUtils.lstarFromArgb(0xC7B446)
        val expected = hct.HctSolver.solveToInt(90.0, 0.00009, expectedLstar)
        val actualLstar = ColorUtils.lstarFromArgb(0xC7B446)
        val actual = HctSolver.solveToInt(90.0, 0.00009, actualLstar)
        actual shouldBeExactly expected
    }

    @Test
    fun testSolveToIntWithSmallChromaAndLstar() {
        val hueDegrees = 90.0
        val chroma = 0.00009
        val lstar = 0.00009
        val expected = hct.HctSolver.solveToInt(hueDegrees, chroma, lstar)
        val actual = HctSolver.solveToInt(hueDegrees, chroma, lstar)
        expected shouldBeExactly actual
    }

    @Test
    fun testSolveToIntWithLstarOutOfRange() {
        val hueDegrees = 90.0
        val chroma = 50.0
        val lstar = 100.0
        val expected = hct.HctSolver.solveToInt(hueDegrees, chroma, lstar)
        val actual = HctSolver.solveToInt(hueDegrees, chroma, lstar)
        expected shouldBeExactly actual
    }

    @Test
    fun testSolveToIntWithExactAnswer() {
        val hueDegrees = 90.0
        val chroma = 50.0
        val lstar = ColorUtils.lstarFromArgb(0xC7B446)
        val expected = hct.HctSolver.solveToInt(hueDegrees, chroma, lstar)
        val actual = HctSolver.solveToInt(hueDegrees, chroma, lstar)
        expected shouldBeExactly actual
    }

    @Test
    fun testSolveToIntWithoutExactAnswer() {
        val hueDegrees = 90.0
        val chroma = 50.0
        val lstar = 50.0
        val expected = hct.HctSolver.solveToInt(hueDegrees, chroma, lstar)
        val actual = HctSolver.solveToInt(hueDegrees, chroma, lstar)
        expected shouldBeExactly actual
    }

    @Test
    fun solveToCam() {
        val expectedLstar = utils.ColorUtils.lstarFromArgb(0xC7B446)
        val expected = hct.HctSolver.solveToCam(90.0, 50.0, expectedLstar)
        val actualLstar = ColorUtils.lstarFromArgb(0xC7B446)
        val actual = HctSolver.solveToCam(90.0, 50.0, actualLstar)
        actual shouldBeExactly expected
    }
}