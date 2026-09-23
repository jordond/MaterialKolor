package com.materialkolor.builder.kit.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class WindowClassTest {
    @Test
    fun windowClass_acrossTheBreakpoints_followsTheSpecTable() {
        WindowClass.of(360.dp) shouldBe WindowClass.Compact
        WindowClass.of(390.dp) shouldBe WindowClass.Compact
        WindowClass.of(600.dp) shouldBe WindowClass.Medium
        WindowClass.of(839.dp) shouldBe WindowClass.Medium
        WindowClass.of(840.dp) shouldBe WindowClass.Medium
        WindowClass.of(1199.dp) shouldBe WindowClass.Medium
        WindowClass.of(1200.dp) shouldBe WindowClass.Expanded
        WindowClass.of(1600.dp) shouldBe WindowClass.Expanded
        WindowClass.of(1920.dp) shouldBe WindowClass.Expanded
    }

    @Test
    fun posterMode_acrossTheBreakpoints_followsTheSpecTable() {
        posterModeAt(360.dp) shouldBe PosterMode.Sheet
        posterModeAt(390.dp) shouldBe PosterMode.Sheet
        posterModeAt(600.dp) shouldBe PosterMode.Rail72
        posterModeAt(839.dp) shouldBe PosterMode.Rail72
        posterModeAt(840.dp) shouldBe PosterMode.Docked320
        posterModeAt(1199.dp) shouldBe PosterMode.Docked320
        posterModeAt(1200.dp) shouldBe PosterMode.Docked400
        posterModeAt(1600.dp) shouldBe PosterMode.Docked400
        posterModeAt(1920.dp) shouldBe PosterMode.Docked400
    }

    @Test
    fun minTouchTarget_perClassAndPointer_followsTheSpecTable() {
        layoutAt(390.dp, coarsePointer = true).minTouchTarget shouldBe 44.dp
        layoutAt(390.dp, coarsePointer = false).minTouchTarget shouldBe 44.dp
        layoutAt(840.dp, coarsePointer = true).minTouchTarget shouldBe 44.dp
        layoutAt(840.dp, coarsePointer = false).minTouchTarget shouldBe 32.dp
        layoutAt(1440.dp, coarsePointer = true).minTouchTarget shouldBe 44.dp
        layoutAt(1440.dp, coarsePointer = false).minTouchTarget shouldBe 24.dp
    }

    @Test
    fun primaryTouchTarget_onCompact_growsToFortyEight() {
        layoutAt(390.dp, coarsePointer = false).primaryTouchTarget shouldBe 48.dp
        layoutAt(390.dp, coarsePointer = true).primaryTouchTarget shouldBe 48.dp
        layoutAt(840.dp, coarsePointer = false).primaryTouchTarget shouldBe 32.dp
        layoutAt(1440.dp, coarsePointer = false).primaryTouchTarget shouldBe 24.dp
    }

    @Test
    fun canvasMaxWidth_pastSixteenHundred_capsAtFourteenHundred() {
        layoutAt(1199.dp).canvasMaxWidth shouldBe Dp.Infinity
        layoutAt(1200.dp).canvasMaxWidth shouldBe Dp.Infinity
        layoutAt(1599.dp).canvasMaxWidth shouldBe Dp.Infinity
        layoutAt(1600.dp).canvasMaxWidth shouldBe CanvasContentCap
        layoutAt(1920.dp).canvasMaxWidth shouldBe CanvasContentCap
    }

    @Test
    fun layoutInfo_builtFromASize_derivesItsClassAndPosterMode() {
        val layout = LayoutInfo.of(widthDp = 1280.dp, heightDp = 800.dp)

        layout.windowClass shouldBe WindowClass.Expanded
        layout.posterMode shouldBe PosterMode.Docked400
        layout.widthDp shouldBe 1280.dp
        layout.heightDp shouldBe 800.dp
        layout.coarsePointer shouldBe false
    }

    @Test
    fun layoutInfo_copiedToANewWidth_derivesTheNewClassAndPosterMode() {
        val phone = LayoutInfo.of(widthDp = 390.dp, heightDp = 844.dp)

        val desktop = phone.copy(widthDp = 1280.dp)

        desktop.windowClass shouldBe WindowClass.Expanded
        desktop.posterMode shouldBe PosterMode.Docked400
    }

    private fun posterModeAt(widthDp: Dp): PosterMode = layoutAt(widthDp).posterMode

    private fun layoutAt(
        widthDp: Dp,
        coarsePointer: Boolean = false,
    ): LayoutInfo = LayoutInfo.of(widthDp = widthDp, heightDp = 900.dp, coarsePointer = coarsePointer)
}
