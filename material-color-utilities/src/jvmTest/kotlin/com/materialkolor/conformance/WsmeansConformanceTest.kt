package com.materialkolor.conformance

import com.materialkolor.quantize.QuantizerWsmeans
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private typealias Quantizer = (IntArray, IntArray, Int) -> Map<Int, Int>

class WsmeansConformanceTest {
    @Test
    fun wsmeansWithValidInitialClustersMatchesOrderedEntries() {
        val pixels = intArrayOf(RED, RED, GREEN, BLUE, RED, GREEN)
        for (clusters in listOf(intArrayOf(RED), intArrayOf(RED, BLUE), intArrayOf(RED, GREEN, BLUE))) {
            val expected = QuantizerWsmeans.quantize(pixels, clusters, clusters.size).toList()
            eachImplementation { label, quantize ->
                val actual = quantize(pixels, clusters, clusters.size)
                assertEquals(expected, actual.toList(), label)
                assertEquals(pixels.size, actual.values.sum(), label)
            }
        }
        eachImplementation { label, quantize ->
            assertEquals(emptyMap(), quantize(intArrayOf(), intArrayOf(), 8), label)
        }
    }

    @Test
    fun upstreamNonemptyInputWithEmptyClustersStillFailsDespiteItsDocumentation() {
        // Pinned Java and Kotlin leave random-centroid initialization empty. Do not repair the
        // algorithm in a portability transform. An upstream update must revisit this regression.
        eachImplementationFailsWith<NullPointerException>(intArrayOf(RED, GREEN, BLUE), intArrayOf(), 3)
    }

    @Test
    fun oversizedInitialClustersAndZeroLimitRetainUpstreamRejection() {
        val pixels = intArrayOf(RED, GREEN)
        eachImplementationFailsWith<IndexOutOfBoundsException>(pixels, intArrayOf(RED, GREEN), 1)
        eachImplementationFailsWith<IllegalArgumentException>(pixels, intArrayOf(), 0)
    }

    private fun eachImplementation(assertions: (label: String, quantize: Quantizer) -> Unit) {
        IMPLEMENTATIONS.forEach { (label, quantize) -> assertions(label, quantize) }
    }

    private inline fun <reified T : Throwable> eachImplementationFailsWith(
        pixels: IntArray,
        clusters: IntArray,
        maxColors: Int,
    ) = eachImplementation { label, quantize ->
        assertFailsWith<T>(label) { quantize(pixels, clusters, maxColors) }
    }

    companion object {
        private val RED = 0xffff0000.toInt()
        private val GREEN = 0xff00ff00.toInt()
        private val BLUE = 0xff0000ff.toInt()

        /**
         * The generated library alongside both independent references. Every claim in this file is
         * a conformance claim, so it has to hold for all three.
         */
        private val IMPLEMENTATIONS: Map<String, Quantizer> = mapOf(
            "library" to QuantizerWsmeans::quantize,
            "upstream Kotlin" to upstream.kotlin.quantize.QuantizerWsmeans::quantize,
            "upstream Java" to quantize.QuantizerWsmeans::quantize,
        )
    }
}
