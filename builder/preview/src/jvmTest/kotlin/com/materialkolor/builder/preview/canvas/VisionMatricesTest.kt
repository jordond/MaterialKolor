package com.materialkolor.builder.preview.canvas

import androidx.compose.ui.graphics.ColorMatrix
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class VisionMatricesTest {
    @Test
    fun everyMatrix_onWhite_keepsItWhite() {
        val matrices = with(VisionMatrices) { listOf(Protanopia, Deuteranopia, Tritanopia, Achromatopsia) }
        for (matrix in matrices) {
            for (row in matrix) row.sum() shouldBe (1f plusOrMinus 0.0001f)
        }
    }

    @Test
    fun achromatopsia_onPureGreen_leavesItsLuminanceInEveryChannel() {
        val matrix = VisionMatrices.colorMatrix(VisionMatrices.Achromatopsia)

        val (red, green, blue, alpha) = matrix.transform(listOf(0f, 1f, 0f, 1f))

        red shouldBe (0.7152f plusOrMinus 0.0001f)
        green shouldBe (0.7152f plusOrMinus 0.0001f)
        blue shouldBe (0.7152f plusOrMinus 0.0001f)
        alpha shouldBe 1f
    }

    /**
     * The red, green, blue and alpha of [channels] drawn through this matrix. The offset column is
     * left out, every vision matrix keeps it at zero.
     */
    private fun ColorMatrix.transform(channels: List<Float>): List<Float> =
        (0 until 4).map { row ->
            (0 until 4).sumOf { column -> (values[row * 5 + column] * channels[column]).toDouble() }.toFloat()
        }
}
