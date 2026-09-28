package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.InThePage
import com.materialkolor.builder.kit.control.PageOverlays
import com.materialkolor.builder.kit.control.RingReach
import com.materialkolor.builder.kit.control.shouldCoverEverySide
import com.materialkolor.builder.kit.control.shouldRingAllTheWayRound
import com.materialkolor.builder.kit.control.shouldShowABubbleAbove
import com.materialkolor.builder.kit.control.shouldShowRing
import com.materialkolor.builder.kit.control.tabOntoRing
import io.kotest.assertions.withClue
import kotlin.test.Test

/**
 * The widgets whose rings once sat under a tooltip bubble, with the bubble showing in the page the
 * way the web draws it. The ring covers at least half the middle of every side at 3 to 1 and stands
 * 3 to 1 from what it covers all the way round.
 */
@OptIn(ExperimentalTestApi::class)
class WidgetRingEdgesTest {
    @Test
    fun swatchTile_copyButtonUnderItsTooltip_everySkin_ringsAllTheWayRound() =
        forEachWidgetSkin { _, skin ->
            val overlays = PageOverlays()
            val whole = tabOntoRing(skin, presses = 2) {
                InThePage(overlays) {
                    SwatchTile(
                        name = "primary",
                        color = Color(0xFF6750A4),
                        onColor = Color(0xFFFFFFFF),
                        tone = 40.0,
                        contrast = 6.4,
                        onCopy = {},
                        onClick = {},
                        modifier = Modifier.width(200.dp),
                    )
                }
            }
            onNodeWithContentDescription("Copy").assertIsFocused()
            whole.shouldShowABubbleAbove(overlays)
            val copy = whole.nearFocused(RingReach)
            copy.shouldShowRing()
            copy.shouldCoverEverySide()
            copy.shouldRingAllTheWayRound()
        }

    @Test
    fun codeView_copyButtonUnderItsTooltip_everySkin_ringsAllTheWayRound() {
        val file = widgetGoldenColorFile()
        forEachWidgetSkin { _, skin ->
            val overlays = PageOverlays()
            val whole = tabOntoRing(skin, presses = 2) {
                InThePage(overlays) { CodeView(file.lines, onCopy = {}, Modifier.size(480.dp, 200.dp)) }
            }
            onNodeWithContentDescription("Copy").assertIsFocused()
            whole.shouldShowABubbleAbove(overlays)
            val copy = whole.nearFocused(RingReach)
            copy.shouldShowRing()
            copy.shouldCoverEverySide()
            copy.shouldRingAllTheWayRound()
        }
    }

    /**
     * The chip is small and round, so it is drawn at two pixels per dp like its other ring test.
     */
    @Test
    fun schemeChip_chosenOrNot_underItsTooltip_everySkin_ringsAllTheWayRound() {
        for (selected in listOf(false, true)) {
            withClue("selected $selected") {
                forEachWidgetSkin { _, skin ->
                    val overlays = PageOverlays()
                    val whole = tabOntoRing(skin, density = 2f) {
                        InThePage(overlays) {
                            SchemeChip(
                                primary = Color(0xFF6750A4),
                                secondaryContainer = Color(0xFFE8DEF8),
                                tertiaryContainer = Color(0xFFFFD8E4),
                                selected = selected,
                                onClick = {},
                                label = "Tonal spot",
                            )
                        }
                    }
                    whole.shouldShowABubbleAbove(overlays)
                    val chip = whole.nearFocused(RingReach)
                    chip.shouldShowRing()
                    chip.shouldCoverEverySide()
                    chip.shouldRingAllTheWayRound()
                }
            }
        }
    }
}
