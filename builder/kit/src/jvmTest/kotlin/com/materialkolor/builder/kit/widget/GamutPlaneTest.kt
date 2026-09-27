package com.materialkolor.builder.kit.widget

import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GamutPlaneTest {
    // Near white the search found more chroma at tone 99 than at 98, and the edge doubled back under the corner (B-551a).
    @Test
    fun smoothEdge_everyHue_onlyClimbsToItsWidestAndOnlyFallsAfter() {
        for (hue in 0 until 360 step 5) {
            val edges = DoubleArray(101) { tone -> GamutLimit.maxChroma(hue.toDouble(), tone.toDouble()) }
            val drawn = smoothEdge(edges)
            val widest = drawn.indices.maxBy { tone -> drawn[tone] }
            withClue("hue $hue") {
                drawn[0] shouldBe 0.0
                drawn[100] shouldBe 0.0
                for (tone in 1..widest) drawn[tone - 1] shouldBeLessThanOrEqual drawn[tone]
                for (tone in widest until 100) drawn[tone + 1] shouldBeLessThanOrEqual drawn[tone]
                for (tone in drawn.indices) drawn[tone] shouldBeLessThanOrEqual edges[tone]
            }
        }
    }
}
