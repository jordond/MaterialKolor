package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.preview.split.PaneSide
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-315b

/**
 * How many rows the scrolling list has, far more than fit.
 */
private const val ROWS = 200

/**
 * How tall each row is, and how far each step scrolls.
 */
private val RowHeight = 40.dp

/**
 * How many one-row steps the list scrolls, two and a half screens. A row leaving at one step hands
 * its node to the row arriving at the next, so the rows on screen at the end sit on nodes that held
 * earlier rows.
 */
private const val STEPS = 25

@OptIn(ExperimentalTestApi::class)
class InspectReuseTest {
    @Test
    fun lazyListReusingItsNodes_neverBringsBackAnOldKey_andRecordsEveryRowOnScreen() =
        runComposeUiTest {
            val registry = InspectRegistry()
            val list = LazyListState()
            setContent {
                CompositionLocalProvider(LocalInspectRegistry provides registry) {
                    LazyColumn(Modifier.size(200.dp, 400.dp), state = list) {
                        items(ROWS) { row ->
                            Box(Modifier.size(RowHeight).testTag(rowTag(row)).previewRoles(*PrimaryPair))
                        }
                    }
                }
            }
            waitForIdle()
            val first = shownRows(list).map { bounds ->
                registry.ownerAt(PaneSide.Start, bounds.center).shouldNotBeNull()
            }
            first.shouldNotBeEmpty()

            repeat(STEPS) {
                runOnIdle { list.dispatchRawDelta(with(density) { RowHeight.toPx() }) }
                waitForIdle()
            }

            // A key that came back would carry an outline or a pin over to whatever row reused the node.
            for (owner in first) registry.entryOf(owner).shouldBeNull()
            val shown = shownRows(list)
            for (bounds in shown) registry.hit(PaneSide.Start, bounds.center).shouldNotBeNull()
            // A row the list keeps composed off screen, placed nowhere, holds no entry either.
            registry.size shouldBe shown.size
        }

    /**
     * Where each row [list] shows sits in the window.
     */
    private fun ComposeUiTest.shownRows(list: LazyListState): List<Rect> {
        val rows = runOnIdle { list.layoutInfo.visibleItemsInfo.map { item -> item.index } }
        return rows.map { row -> onNodeWithTag(rowTag(row)).fetchSemanticsNode().boundsInWindow }
    }

    private fun rowTag(row: Int): String = "row $row"
}
