package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.ToastDuration
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val ToastHeight = 900
private val ToastMargin = 12.dp

/** A window, how the poster shows in it, and whether the shell is in fullscreen. */
private class ToastWindow(
    val width: Int,
    val mode: PosterMode,
    val collapsed: Boolean = false,
    val fullscreen: Boolean = false,
) {
    override fun toString(): String = "$width dp, $mode, collapsed $collapsed, fullscreen $fullscreen"
}

private val ToastWindows = listOf(
    ToastWindow(1280, PosterMode.Docked400),
    ToastWindow(840, PosterMode.Docked320, collapsed = true),
    ToastWindow(600, PosterMode.Rail72, collapsed = false),
    ToastWindow(1280, PosterMode.Docked400, fullscreen = true),
    ToastWindow(390, PosterMode.Sheet),
)

@OptIn(ExperimentalTestApi::class)
class ShellToastTest {
    @Test
    fun toasts_everyLayoutEachWayAndDirection_startPastThePosterAndEndAboveTheDock() {
        for (inTree in listOf(false, true)) {
            for (direction in LayoutDirection.entries) {
                for (window in ToastWindows) {
                    withClue("$window, $direction, in tree $inTree") {
                        toastTest(window, direction, inTree)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun toastTest(
    window: ToastWindow,
    direction: LayoutDirection,
    inTree: Boolean,
) = runSkikoComposeUiTest(size = Size(window.width.toFloat(), ToastHeight.toFloat())) {
    val toasts = BuilderToastHostState()
    toasts.show("Theme saved", duration = ToastDuration.Indefinite)
    var mode: PosterMode? = null
    setContent {
        CompositionLocalProvider(
            LocalOverlaysInTree provides inTree,
            LocalLayoutDirection provides direction,
        ) {
            ShellHarness(Skin(Library.Material3, expressive = false)) {
                mode = LocalLayout.current.posterMode
                WorkspaceShell(
                    posterColors = ShellPosterColors,
                    posterCollapsed = window.collapsed,
                    poster = { Box(Modifier.fillMaxSize()) },
                    topBar = { TopBarRegion {} },
                    canvas = { ShellSlot(ShellCanvasTag) },
                    dock = { Box(Modifier.size(width = 240.dp, height = 56.dp).testTag(ShellDockTag)) },
                    fullscreen = window.fullscreen,
                    overlays = { ToastRegion(toasts) },
                )
            }
        }
    }
    waitForIdle()

    mode shouldBe window.mode
    val width = window.width.dp
    val stack = toastStack()
    val dock = shellBounds(ShellDockTag)
    stack.bottom shouldBeLessThanOrEqualTo dock.top
    when {
        window.fullscreen -> {
            stack.left shouldBeGreaterThanOrEqualTo ToastMargin
            stack.right shouldBeLessThanOrEqualTo width - ToastMargin
        }
        window.mode == PosterMode.Sheet -> {
            val peek = onNode(toastPane()).getBoundsInRoot()
            stack.bottom shouldBeLessThanOrEqualTo ToastHeight.dp - 344.dp - 16.dp - 64.dp
            stack.bottom shouldBeLessThanOrEqualTo peek.top
        }
        else -> {
            val poster = onNode(toastPane()).getBoundsInRoot()
            stack.pastPoster(poster, direction)
            if (direction == LayoutDirection.Ltr) {
                stack.right shouldBeLessThanOrEqualTo width - ToastMargin
            } else {
                stack.left shouldBeGreaterThanOrEqualTo ToastMargin
            }
        }
    }
}

/** The toast stack, the one polite live region the host keeps. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.toastStack(): DpRect =
    onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true).getBoundsInRoot()

/** The poster's pane, docked, as the rail, opened over the canvas or as the sheet. */
private fun toastPane(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, ShellPosterLabel)

/** Clear of [poster] by the margin on its inner side, whichever way the page reads. */
private fun DpRect.pastPoster(
    poster: DpRect,
    direction: LayoutDirection,
) {
    val gap: Dp = if (direction == LayoutDirection.Ltr) left - poster.right else poster.left - right
    gap shouldBeGreaterThanOrEqualTo ToastMargin
}
