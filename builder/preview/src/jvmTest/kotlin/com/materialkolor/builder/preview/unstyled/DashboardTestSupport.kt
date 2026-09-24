package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.split.PaneSpec
import androidx.compose.ui.semantics.Role as SemanticsRole

/** The frame the dock shows each device in, the kit's screen widths at the height of a first screen. */
internal val DashboardFrames: Map<DeviceWidth, IntSize> = mapOf(
    DeviceWidth.Phone to IntSize(412, 900),
    DeviceWidth.Tablet to IntSize(840, 900),
    DeviceWidth.Desktop to IntSize(1280, 800),
)

/** The order status button, which is the only dropdown on the page. */
internal val StatusButton: SemanticsMatcher =
    hasClickAction() and SemanticsMatcher.expectValue(SemanticsProperties.Role, SemanticsRole.DropdownList)

/** The pick of the status menu that keeps [filter], in any copy. */
internal fun menuItem(filter: OrderFilter): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, SemanticsRole.RadioButton) and
        hasAnyDescendant(hasText(filter.label))

/** The icon button named [label], which off the web goes by its label alone. */
internal fun iconButton(label: String): SemanticsMatcher = hasClickAction() and hasContentDescription(label)

/** Every panel open, so the menu, a phone's navigation and, when [drawer], the token panel are on screen too. */
internal fun DemoAppState.openEverything(drawer: Boolean = true) {
    setOn(DashboardDrawerSwitch, drawer)
    setOn(DashboardMenuSwitch, true)
    setOn(DashboardNavSwitch, true)
}

/**
 * The dashboard for [width] with every panel open, the token panel only when [drawer], in a frame
 * tall enough that every lazy item composes.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.tallDashboard(
    width: DeviceWidth,
    frame: IntSize,
    drawer: Boolean = true,
) {
    val state = DemoAppState()
    state.openEverything(drawer)
    setContent {
        DashboardHarness(
            spec = LightSpec,
            state = state,
            width = width,
            // Wider than the window on desktop, and tall enough for the whole page.
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(frame.width.dp, 2400.dp),
        )
    }
}

/**
 * The dashboard in an Unstyled pane of [spec], under the Unstyled chrome, with motion frozen. With
 * [folds] the kit's fold modifiers fold state into names as they do on the web.
 */
@OptIn(KitTestApi::class)
@Composable
internal fun DashboardHarness(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
    folds: Boolean = false,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(Skin(Library.Unstyled, expressive = false)) {
            if (folds) {
                ProvideWebFoldsForTest { PreviewPane(spec, modifier) { UnstyledAppEntry(spec, state, width) } }
            } else {
                PreviewPane(spec, modifier) { UnstyledAppEntry(spec, state, width) }
            }
        }
    }
}
