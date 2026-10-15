package conformance

import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantizerConformanceTest {
    @Test
    fun celebiPreservesOrderedEntriesPopulationsAndScores() {
        val random = java.util.Random(987654321L)
        val inputs = buildList {
            add(intArrayOf())
            add(intArrayOf(RED, RED, GREEN, BLUE, RED, GREEN))
            add(intArrayOf(0x00ff0000, 0x00ff0000, 0x8000ff00.toInt(), BLUE, BLUE))
            add(intArrayOf(BLUE, GREEN, RED))
            repeat(30) { sample ->
                add(IntArray(30 + sample * 17) { 0xff000000.toInt() or random.nextInt(0x1000000) })
            }
        }
        for ((index, pixels) in inputs.withIndex()) {
            for (count in listOf(1, 2, 4, 16, 32)) {
                val label = "case=$index maxColors=$count pixels=${pixels.size}"
                val raw = upstream.kotlin.quantize.QuantizerCelebi
                    .quantize(pixels, count)
                val actual = QuantizerCelebi.quantize(pixels, count)
                val java = quantize.QuantizerCelebi.quantize(pixels, count)
                assertEquals(raw.toList(), actual.toList(), "$label ordered Kotlin entries")
                assertEquals(java.toList(), actual.toList(), "$label ordered Java entries")
                assertEquals(pixels.size, actual.values.sum(), "$label population")
                assertTrue(actual.size <= count, "$label cluster limit")
                assertEquals(
                    upstream.kotlin.score.Score
                        .score(raw),
                    Score.score(actual),
                    "$label Kotlin score",
                )
                assertEquals(score.Score.score(java), Score.score(actual), "$label Java score")
            }
        }
    }

    @Test
    fun scoresPreserveTieOrderingFilteringAndFallbacks() {
        for (populations in listOf(
            emptyMap(),
            linkedMapOf(RED to 4, GREEN to 4, BLUE to 4),
            linkedMapOf(BLUE to 4, GREEN to 4, RED to 4),
            linkedMapOf(0xff000000.toInt() to 3, 0xffffffff.toInt() to 3),
            linkedMapOf(0x00ff0000 to 5, RED to 5, GREEN to 0),
        )) {
            for (desired in listOf(1, 2, 4)) {
                for (filter in listOf(false, true)) {
                    val actual = Score.score(populations, desired, BLUE, filter)
                    assertEquals(
                        upstream.kotlin.score.Score
                            .score(populations, desired, BLUE, filter),
                        actual,
                    )
                    assertEquals(score.Score.score(populations, desired, BLUE, filter), actual)
                }
            }
        }
    }

    companion object {
        private const val RED = -0x10000
        private const val GREEN = -0xff0100
        private const val BLUE = -0xffff01
    }
}
