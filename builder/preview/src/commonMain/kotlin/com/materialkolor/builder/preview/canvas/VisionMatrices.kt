package com.materialkolor.builder.preview.canvas

import androidx.compose.ui.graphics.ColorMatrix

/**
 * The color matrices the canvas draws through to simulate color vision deficiencies (F-25).
 *
 * The three dichromacies are Machado, Oliveira and Fernandes 2009 at severity 1.0, and
 * achromatopsia weighs every color with the Rec. 709 luminance weights. Each is three rows of red,
 * green and blue weights and leaves alpha alone. [colorMatrix] builds a fresh [ColorMatrix] every
 * call, since a `PaneSpec` compares filters by their array and one shared matrix edited in place
 * would look unchanged.
 *
 * Both are a gamma space approximation. Machado's matrices are made for linear RGB and the Rec. 709
 * weights give luminance only on linear values, but a [ColorMatrix] runs on the gamma encoded sRGB
 * the canvas draws, so the dichromacies come out a little off and achromatopsia shows luma. Close
 * enough for a preview, so nothing is linearised first.
 */
public object VisionMatrices {
    /**
     * Machado 2009 protanopia, no working long wavelength cones.
     */
    public val Protanopia: List<List<Float>> = listOf(
        listOf(0.152286f, 1.052583f, -0.204868f),
        listOf(0.114503f, 0.786281f, 0.099216f),
        listOf(-0.003882f, -0.048116f, 1.051998f),
    )

    /**
     * Machado 2009 deuteranopia, no working medium wavelength cones.
     */
    public val Deuteranopia: List<List<Float>> = listOf(
        listOf(0.367322f, 0.860646f, -0.227968f),
        listOf(0.280085f, 0.672501f, 0.047413f),
        listOf(-0.011820f, 0.042940f, 0.968881f),
    )

    /**
     * Machado 2009 tritanopia, no working short wavelength cones.
     */
    public val Tritanopia: List<List<Float>> = listOf(
        listOf(1.255528f, -0.076749f, -0.178779f),
        listOf(-0.078411f, 0.930809f, 0.147602f),
        listOf(0.004733f, 0.691367f, 0.303900f),
    )

    /**
     * Every channel set to the color's Rec. 709 luma, so only lightness is left.
     */
    public val Achromatopsia: List<List<Float>> = List(CHANNELS) { listOf(0.2126f, 0.7152f, 0.0722f) }

    /**
     * A fresh color matrix that weighs red, green and blue by [rgb] and keeps alpha as it is.
     *
     * @param[rgb] Three rows of three weights, the red output first, such as [Protanopia].
     */
    public fun colorMatrix(rgb: List<List<Float>>): ColorMatrix {
        require(rgb.size == CHANNELS && rgb.all { row -> row.size == CHANNELS }) {
            "A color vision matrix is 3 by 3, got $rgb"
        }
        val values = FloatArray(MATRIX_SIZE)
        for ((row, weights) in rgb.withIndex()) {
            for ((column, weight) in weights.withIndex()) values[row * ROW_SIZE + column] = weight
        }
        values[ALPHA_INDEX] = 1f
        return ColorMatrix(values)
    }

    private const val CHANNELS = 3

    /**
     * A Compose color matrix is four rows of five, the last column an offset.
     */
    private const val ROW_SIZE = 5
    private const val MATRIX_SIZE = 20

    /**
     * Where alpha's own weight sits, row four column four.
     */
    private const val ALPHA_INDEX = 18
}
