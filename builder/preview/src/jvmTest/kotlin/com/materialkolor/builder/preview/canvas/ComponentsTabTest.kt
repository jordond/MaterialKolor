package com.materialkolor.builder.preview.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Pane widths and how many columns of cards each fits. */
private val GalleryColumns: Map<Int, Int> = mapOf(412 to 1, 840 to 2, 1280 to 4)

/** Twenty stand-in cards over two groups. */
private val GalleryStandIns: List<GalleryCard> = List(20) { index ->
    GalleryCard("Card $index", if (index < 5) GalleryGroup.Actions else GalleryGroup.Feedback) {}
}

@OptIn(ExperimentalTestApi::class)
class ComponentsTabTest {
    @Test
    fun choice_untilPicked_isTheDefaultThenOnlyThePickedOption() {
        val state = DemoAppState()
        state.choice("seat", count = 3, default = 1) shouldBe 1

        state.choose("seat", count = 3, option = 2)
        state.choice("seat", count = 3, default = 1) shouldBe 2

        state.choose("seat", count = 3, option = 0)
        state.choice("seat", count = 3) shouldBe 0
        state.isOn("seat.2") shouldBe false
    }

    @Test
    fun choose_anOptionOutsideTheSet_isRefused() {
        shouldThrow<IllegalArgumentException> { DemoAppState().choose("seat", count = 3, option = 3) }
    }

    @Test
    fun grid_everyPaneWidth_fitsAsManyColumnsAsStayAtLeast280Wide() {
        for ((width, columns) in GalleryColumns) {
            withClue(width) {
                runDesktopComposeUiTest(width, 900) {
                    setContent { GalleryStandInGrid(cardHeight = 40.dp) }

                    val density = onRoot()
                        .fetchSemanticsNode()
                        .layoutInfo.density.density
                    val first = onNodeWithTag("Card 5").fetchSemanticsNode().boundsInRoot
                    val row = (5 until 5 + columns).map { index ->
                        onNodeWithTag("Card $index").fetchSemanticsNode().boundsInRoot
                    }
                    row.map { bounds -> bounds.top }.distinct() shouldBe listOf(first.top)
                    for (bounds in row) bounds.width / density shouldBeGreaterThanOrEqual 280f
                    onNodeWithTag("Card ${5 + columns}").fetchSemanticsNode().boundsInRoot.top shouldBe
                        first.bottom + 16 * density
                }
            }
        }
    }

    @Test
    fun grid_tallerThanThePane_composesOnlyTheCardsInView() =
        runDesktopComposeUiTest(412, 900) {
            val composed = mutableSetOf<String>()
            setContent {
                CompositionLocalProvider(LocalCompositionProbe provides { where -> composed += where }) {
                    GalleryStandInGrid(cardHeight = 200.dp)
                }
            }
            waitForIdle()

            composed shouldContain GALLERY_CARD + "Card 0"
            composed shouldNotContain GALLERY_CARD + "Card 19"
            composed.size shouldBeLessThan GalleryStandIns.size / 2
        }
}

/** The stand-in cards, each [cardHeight] tall and tagged with its title. */
@Composable
private fun GalleryStandInGrid(cardHeight: Dp) {
    GalleryGrid(
        cards = GalleryStandIns,
        listState = rememberLazyListState(),
        gap = 16.dp,
        header = { group -> BasicText(group.name) },
        card = { card, modifier -> Box(modifier.height(cardHeight).testTag(card.title)) { BasicText(card.title) } },
    )
}
