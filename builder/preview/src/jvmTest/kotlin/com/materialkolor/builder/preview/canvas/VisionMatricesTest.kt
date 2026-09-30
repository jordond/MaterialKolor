package com.materialkolor.builder.preview.canvas

import androidx.compose.ui.graphics.ColorMatrix
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class VisionMatricesTest {
    @Test
    fun dichromacies_atFullSeverity_matchMachado2009() {
        // Machado, Oliveira and Fernandes 2009, table of simulation matrices, severity 1.0.
        VisionMatrices.Protanopia shouldBe listOf(
            listOf(0.152286f, 1.052583f, -0.204868f),
            listOf(0.114503f, 0.786281f, 0.099216f),
            listOf(-0.003882f, -0.048116f, 1.051998f),
        )
        VisionMatrices.Deuteranopia shouldBe listOf(
            listOf(0.367322f, 0.860646f, -0.227968f),
            listOf(0.280085f, 0.672501f, 0.047413f),
            listOf(-0.011820f, 0.042940f, 0.968881f),
        )
        VisionMatrices.Tritanopia shouldBe listOf(
            listOf(1.255528f, -0.076749f, -0.178779f),
            listOf(-0.078411f, 0.930809f, 0.147602f),
            listOf(0.004733f, 0.691367f, 0.303900f),
        )
    }

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
