package com.materialkolor.builder.preview.canvas

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
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
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
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

    @Test
    fun grid_widthSweptLikeTheRail_recomposesTheCardsOnlyWhenTheColumnsChange() =
        runDesktopComposeUiTest(1400, 900) {
            val composed = mutableListOf<String>()
            var wide by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalCompositionProbe provides { where -> composed += where }) {
                    // The canvas grows by the 328 dp between the open poster and the rail, from two
                    // columns to three.
                    val width = animateDpAsState(
                        targetValue = if (wide) GallerySweepStart + 328.dp else GallerySweepStart,
                        animationSpec = tween(GallerySweepMillis, easing = LinearEasing),
                    )
                    Box(Modifier.layoutWidth { width.value }) { GalleryStandInGrid(cardHeight = 40.dp) }
                }
            }
            waitForIdle()
            val first = composed.count { where -> where.startsWith(GALLERY_CARD) }
            first shouldBe GalleryStandIns.size
            composed.clear()

            mainClock.autoAdvance = false
            wide = true
            val frames = GallerySweepMillis / 16 + 2
            repeat(frames) { mainClock.advanceTimeByFrame() }
            mainClock.autoAdvance = true
            waitForIdle()

            // Before B-217c the grid sat in a BoxWithConstraints whose content composed 19 times over
            // this sweep. The cards composed 12 times then as now, only those that change rows.
            composed.count { where -> where == GALLERY_GRID } shouldBe 0
            composed.count { where -> where.startsWith(GALLERY_CARD) } shouldBeLessThanOrEqual GalleryStandIns.size
            val row = (5 until 8).map { index -> onNodeWithTag("Card $index").fetchSemanticsNode().boundsInRoot.top }
            row.distinct().size shouldBe 1
        }
}

/** Where the swept grid starts, two columns of cards. */
private val GallerySweepStart = 800.dp

/** How long the sweep takes, about the rail's panel motion. */
private const val GallerySweepMillis = 300

/** Sizes to [width] at layout time, so the width moving recomposes nothing here. */
private fun Modifier.layoutWidth(width: () -> Dp): Modifier =
    layout { measurable, constraints ->
        val px = width().roundToPx()
        val placeable = measurable.measure(constraints.copy(minWidth = px, maxWidth = px))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
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
