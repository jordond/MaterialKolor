package com.materialkolor.builder.kit.headless

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-511
class PopoverPlacementTest {
    private val window = IntSize(400, 600)
    private val popover = IntSize(160, 200)

    /** 8 px under the anchor, measured from the edge it draws, which sits 8 px inside its bounds. */
    private val provider = DropdownPositionProvider(gap = 8, alignEnd = true, inset = 8)

    @Test
    fun popover_hangsUnderItsAnchor_withItsEndOnTheAnchorsEnd() {
        val anchor = IntRect(200, 20, 240, 60)

        provider.calculatePosition(anchor, window, LayoutDirection.Ltr, popover) shouldBe IntOffset(88, 60)
        provider.calculatePosition(anchor, window, LayoutDirection.Rtl, popover) shouldBe IntOffset(192, 60)
    }

    @Test
    fun popover_atTheWindowsEdges_staysInsideAndFlipsAbove() {
        provider.calculatePosition(IntRect(0, 20, 40, 60), window, LayoutDirection.Ltr, popover) shouldBe
            IntOffset(0, 60)
        provider.calculatePosition(IntRect(200, 540, 240, 580), window, LayoutDirection.Ltr, popover) shouldBe
            IntOffset(88, 340)
    }
}
