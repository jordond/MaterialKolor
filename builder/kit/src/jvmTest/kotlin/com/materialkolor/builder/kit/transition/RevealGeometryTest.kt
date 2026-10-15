package com.materialkolor.builder.kit.transition

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RevealGeometryTest {
    private val host = Size(width = 300f, height = 400f)

    @Test
    fun revealCenter_withAnUnspecifiedOrigin_isTheMiddleOfTheHost() {
        revealCenter(Offset.Unspecified, host) shouldBe Offset(150f, 200f)
    }

    @Test
    fun revealCenter_withAnOrigin_isThatOrigin() {
        revealCenter(Offset(20f, 30f), host) shouldBe Offset(20f, 30f)
    }

    @Test
    fun farthestCorner_fromACorner_isTheDiagonal() {
        farthestCorner(Offset.Zero, host) shouldBe 500f
        farthestCorner(Offset(300f, 400f), host) shouldBe 500f
    }

    @Test
    fun farthestCorner_fromTheMiddle_isHalfTheDiagonal() {
        farthestCorner(Offset(150f, 200f), host) shouldBe 250f
    }

    @Test
    fun farthestCorner_fromAnOffCentrePoint_reachesTheOppositeCorner() {
        farthestCorner(Offset(100f, 100f), host) shouldBe (360.555f plusOrMinus 0.001f)
    }

    @Test
    fun circlePath_reusedAcrossFrames_holdsOnlyTheLatestCircle() {
        val path = Path()

        circlePath(path, Offset(50f, 50f), radius = 40f)
        circlePath(path, Offset(100f, 120f), radius = 10f)

        path.getBounds() shouldBe Rect(center = Offset(100f, 120f), radius = 10f)
    }
}
