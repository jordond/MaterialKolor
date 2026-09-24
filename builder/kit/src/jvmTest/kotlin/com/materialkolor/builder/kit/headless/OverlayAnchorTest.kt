package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.HostOverlays
import com.materialkolor.builder.kit.control.forEachSkin
import com.materialkolor.builder.kit.control.hasRole
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OverlayAnchorTest {
    @Test
    fun openMenu_inTree_followsItsAnchorAsThePageScrolls() =
        forEachSkin { _, skin ->
            val scroll = ScrollState(0)
            setContent {
                HostOverlays(skin, inTree = true) {
                    Column(Modifier.size(320.dp).verticalScroll(scroll)) {
                        Box(Modifier.height(120.dp))
                        BuilderMenu(true, {}, listOf(BuilderMenuItem("Rename", {}))) {
                            Box(Modifier.testTag("anchor").size(40.dp))
                        }
                        Box(Modifier.height(640.dp))
                    }
                }
            }
            waitForIdle()
            val row = { onNode(hasText("Rename") and hasRole(Role.Button)).getBoundsInRoot() }
            val anchor = { onNodeWithTag("anchor").getBoundsInRoot() }
            val before = anchor()
            val offset = row().top - before.top
            for (step in 1..3) {
                scroll.scrollTo(step * 30)
                waitForIdle()
                (row().top - anchor().top) shouldBe offset
            }
            anchor().top shouldNotBe before.top
        }

    @Test
    fun closedAnchor_inTree_writesNoStateAsThePageScrollsUntilAnOverlayFollowsIt() =
        runComposeUiTest {
            val scroll = ScrollState(0)
            lateinit var host: OverlayHostState
            lateinit var anchor: OverlayAnchor
            setContent {
                CompositionLocalProvider(LocalOverlaysInTree provides true) {
                    OverlayHost {
                        host = checkNotNull(LocalOverlayHost.current)
                        Column(Modifier.size(320.dp).verticalScroll(scroll)) {
                            Box(Modifier.height(120.dp))
                            Box(Modifier.testTag("anchor").size(40.dp)) { anchor = rememberOverlayAnchor() }
                            Box(Modifier.height(640.dp))
                        }
                    }
                }
            }
            waitForIdle()
            val closed = hostCountBoundsWrites(anchor) {
                for (step in 1..3) {
                    scroll.scrollTo(step * 20)
                    waitForIdle()
                }
            }
            closed shouldBe 0
            anchor.bounds shouldBe IntRect.Zero

            anchor.follow(host)
            val followed = hostCountBoundsWrites(anchor) {
                for (step in 4..5) {
                    scroll.scrollTo(step * 20)
                    waitForIdle()
                }
            }
            followed shouldBeGreaterThan 0
            val top = with(density) { onNodeWithTag("anchor").getBoundsInRoot().top.roundToPx() }
            anchor.bounds.top shouldBe top
        }
}

/** Counts the writes to [anchor]'s bounds while [block] runs. */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.hostCountBoundsWrites(
    anchor: OverlayAnchor,
    block: suspend ComposeUiTest.() -> Unit,
): Int {
    var bounds: Any? = null
    Snapshot.observe(readObserver = { state -> bounds = state }) { anchor.bounds }
    val watched = checkNotNull(bounds)
    var writes = 0
    val observer = Snapshot.registerGlobalWriteObserver { state -> if (state === watched) writes++ }
    try {
        block()
    } finally {
        observer.dispose()
    }
    return writes
}
