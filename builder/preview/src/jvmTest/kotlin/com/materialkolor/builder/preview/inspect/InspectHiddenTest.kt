package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.preview.split.PaneSide
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A declared element near the start of a row wider than the preview.
 */
private const val ELEMENT = "element"

/**
 * The first action of a pinned card, so its presence means a card is pinned.
 */
private const val PIN_ROLE = "Pin this role"

@OptIn(ExperimentalTestApi::class)
class InspectHiddenTest {
    @Test
    fun pinnedElement_scrolledOffAHorizontalScroll_showsNoCardOrOutline_untilItScrollsBack() =
        runComposeUiTest {
            val scroll = ScrollState(0)
            setContent {
                Inspecting(PreviewMode.Light, remember { SplitState() }) {
                    Row(Modifier.fillMaxSize().background(Color.White).horizontalScroll(scroll)) {
                        Box(
                            Modifier
                                .padding(start = 40.dp, top = 40.dp)
                                .size(40.dp)
                                .testTag(ELEMENT)
                                .previewRoles(*PrimaryPair),
                        )
                        Spacer(Modifier.width(2000.dp))
                    }
                }
            }
            waitForIdle()
            onNodeWithTag(ELEMENT).performClick()
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertIsDisplayed()

            val away = with(density) { 1000.dp.toPx() }
            runOnIdle { scroll.dispatchRawDelta(away) }
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertIsNotDisplayed()
            // Empty bounds sit at the corner, where an outline around them would show.
            onNodeWithTag(PREVIEW_TAG).captureToImage().toPixelMap()[0, 0] shouldBe Color.White

            runOnIdle { scroll.dispatchRawDelta(-away) }
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertIsDisplayed()
        }

    @Test
    fun handleMoving_withThePinnedElementOutOfSight_leavesThePinAlone() {
        val state = InspectOverlayState()
        val owner = Any()
        val roles = PrimaryPair.toList()
        state.registry.record(owner, InspectEntry(PaneSide.Start, roles, Rect.Zero))
        state.width = 600
        val pin = InspectTarget(owner, PaneSide.Start, roles, isDark = false)
        state.pinned = pin

        state.followHandle(PreviewMode.Split, fraction = 0f, LayoutDirection.Ltr, FocusRequester())

        state.pinned shouldBe pin
    }
}
