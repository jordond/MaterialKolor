package com.materialkolor.builder.preview.inspect

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.preview.split.PaneSide
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** A layout a thousand pixels wide, its handle at three tenths from the start edge. */
private const val WIDTH = 1000

private const val FRACTION = 0.3f

/** Where the handle sits in right to left, counted from the left edge. */
private const val RTL_HANDLE = 700f

private val Room = IntSize(1000, 800)

private val Card = IntSize(200, 100)

private const val GAP = 8

class InspectGeometryTest {
    @Test
    fun paneSideAt_oneCopyInRtl_isTheStartCopyEverywhere() {
        for (shown in listOf(PreviewMode.Light, PreviewMode.Dark)) {
            for (x in listOf(0f, RTL_HANDLE - 1f, RTL_HANDLE, WIDTH - 1f)) {
                withClue("$shown at $x") { rtl(x, shown) shouldBe PaneSide.Start }
            }
        }
    }

    @Test
    fun paneSideAt_splitInRtl_putsTheEndCopyLeftOfTheHandle() {
        rtl(0f, PreviewMode.Split) shouldBe PaneSide.End
        rtl(RTL_HANDLE - 1f, PreviewMode.Split) shouldBe PaneSide.End
        rtl(RTL_HANDLE + 1f, PreviewMode.Split) shouldBe PaneSide.Start
        rtl(WIDTH - 1f, PreviewMode.Split) shouldBe PaneSide.Start
    }

    @Test
    fun paneSideAt_rightOnTheHandlesEdge_goesToTheCopyTheClipKeepsThere() {
        // The end copy's clip keeps [0, 700) in right to left and [300, 1000) in left to right.
        rtl(RTL_HANDLE, PreviewMode.Split) shouldBe PaneSide.Start
        rtl(RTL_HANDLE - 0.01f, PreviewMode.Split) shouldBe PaneSide.End
        paneSideAt(WIDTH * FRACTION, WIDTH, PreviewMode.Split, FRACTION, LayoutDirection.Ltr) shouldBe PaneSide.End
        paneSideAt(WIDTH * FRACTION - 0.01f, WIDTH, PreviewMode.Split, FRACTION, LayoutDirection.Ltr) shouldBe
            PaneSide.Start
    }

    @Test
    fun cardSpot_withMoreRoomAfter_goesAfterLevelWithTheTop() {
        cardSpot(Rect(100f, 100f, 200f, 150f), Card, Room, GAP) shouldBe IntOffset(208, 100)
    }

    @Test
    fun cardSpot_tooNarrowAfter_goesBefore() {
        cardSpot(Rect(700f, 100f, 900f, 150f), Card, Room, GAP) shouldBe IntOffset(492, 100)
    }

    @Test
    fun cardSpot_fittingBothSides_takesTheSideWithMoreRoom() {
        cardSpot(Rect(500f, 100f, 560f, 150f), Card, Room, GAP) shouldBe IntOffset(292, 100)
    }

    @Test
    fun cardSpot_noRoomEitherSide_goesBelow() {
        cardSpot(Rect(50f, 100f, 950f, 150f), Card, Room, GAP) shouldBe IntOffset(50, 158)
    }

    @Test
    fun cardSpot_tooShortBelow_goesAbove() {
        cardSpot(Rect(50f, 700f, 950f, 750f), Card, Room, GAP) shouldBe IntOffset(50, 592)
    }

    @Test
    fun cardSpot_pastTheRoom_isHeldInsideIt() {
        val wide = IntSize(950, 100)
        cardSpot(Rect(900f, 100f, 1000f, 150f), wide, Room, GAP) shouldBe IntOffset(50, 158)
        val tall = IntSize(200, 900)
        cardSpot(Rect(100f, 100f, 200f, 150f), tall, Room, GAP) shouldBe IntOffset(208, 0)
    }

    private fun rtl(
        x: Float,
        shown: PreviewMode,
    ): PaneSide = paneSideAt(x, WIDTH, shown, FRACTION, LayoutDirection.Rtl)
}
