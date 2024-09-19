package score

import kotlin.test.Test
import kotlin.test.assertEquals
import com.materialkolor.score.Score as KotlinScore
import score.Score as JavaScore

class ScoreTest {

    @Test
    fun testScoreComparison() {
        val testCases = listOf(
            mapOf(0xff000000.toInt() to 1, 0xffffffff.toInt() to 1, 0xff0000ff.toInt() to 1),
            mapOf(0xffff0000.toInt() to 1, 0xff00ff00.toInt() to 1, 0xff0000ff.toInt() to 1),
            mapOf(0xff000000.toInt() to 1),
            mapOf(0xff008772.toInt() to 1, 0xff318477.toInt() to 1),
            mapOf(0xff008772.toInt() to 1, 0xff008587.toInt() to 1, 0xff007ebc.toInt() to 1)
        )

        for ((index, colorsToPopulation) in testCases.withIndex()) {
            val actual = KotlinScore.score(colorsToPopulation, 4)
            val expected = JavaScore.score(colorsToPopulation, 4)

            assertEquals(actual, expected, "Test case $index failed")
        }
    }

    @Test
    fun testGeneratedScenarios() {
        val scenarios = listOf(
            Triple(mapOf(0xff7ea16d.toInt() to 67, 0xffd8ccae.toInt() to 67, 0xff835c0d.toInt() to 49), 3, 0xff8d3819.toInt()),
            Triple(mapOf(0xffd33881.toInt() to 14, 0xff3205cc.toInt() to 77, 0xff0b48cf.toInt() to 36, 0xffa08f5d.toInt() to 81), 4, 0xff7d772b.toInt()),
            Triple(mapOf(0xffbe94a6.toInt() to 23, 0xffc33fd7.toInt() to 42, 0xff899f36.toInt() to 90, 0xff94c574.toInt() to 82), 3, 0xffaa79a4.toInt()),
            Triple(mapOf(0xffdf241c.toInt() to 85, 0xff685859.toInt() to 44, 0xffd06d5f.toInt() to 34, 0xff561c54.toInt() to 27, 0xff713090.toInt() to 88), 5, 0xff58c19c.toInt()),
            Triple(mapOf(0xffbe66f8.toInt() to 41, 0xff4bbda9.toInt() to 88, 0xff80f6f9.toInt() to 44, 0xffab8017.toInt() to 43, 0xffe89307.toInt() to 65), 3, 0xff916691.toInt())
        )

        for ((index, scenario) in scenarios.withIndex()) {
            val (colorsToPopulation, desired, fallbackColor) = scenario
            val actual = KotlinScore.score(colorsToPopulation, desired, fallbackColor, true)
            val expected = JavaScore.score(colorsToPopulation, desired, fallbackColor, true)

            assertEquals(actual, expected, "Generated scenario $index failed")
        }
    }
}