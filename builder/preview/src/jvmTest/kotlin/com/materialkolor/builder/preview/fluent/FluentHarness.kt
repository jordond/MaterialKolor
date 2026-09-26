package com.materialkolor.builder.preview.fluent

import com.materialkolor.builder.preview.ShellChrome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.unit.IntSize
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.trips.TripsDestination

/**
 * The frame the dock shows each device in, the kit's screen widths at the height of a first screen.
 */
internal val FluentFrames: Map<DeviceWidth, IntSize> = mapOf(
    DeviceWidth.Phone to IntSize(412, 900),
    DeviceWidth.Tablet to IntSize(840, 900),
    DeviceWidth.Desktop to IntSize(1280, 800),
)

/**
 * A blue document that targets Fluent, so the contrast audit rates Fluent's own pairs.
 */
internal val FluentResult: ThemeResult =
    ThemeResolver().resolve(ThemeDocument(seed = Argb(0x1E88E5), library = Library.Fluent))

internal val FluentLightSpec: PaneSpec = PaneSpec(FluentResult, isDark = false, label = "Light")

internal val FluentDarkSpec: PaneSpec = PaneSpec(FluentResult, isDark = true, label = "Dark")

/**
 * Anything under a layer that took over its semantics, the library control a [FluentOverlaid]
 * covers. The unmerged tree still lists it, though nothing that reads the tree ever reaches it.
 */
internal val UnderAnOverlay: SemanticsMatcher =
    hasAnyAncestor(SemanticsMatcher("clears the semantics under it") { node -> node.config.isClearingSemantics })

/**
 * The layer compose-fluent lays under a compact or open navigation menu, which swallows a click so
 * it never reaches the page. It is no control of its own and declares nothing, the destinations on it do.
 * The app keeps it out of the Tab order, so it never takes focus, though compose-fluent 0.1.0 still
 * leaves its click action in the tree.
 */
internal val NavigationShield: SemanticsMatcher =
    hasClickAction() and hasAnyDescendant(hasContentDescription(TripsDestination.Explore.label))

/**
 * The arrows of the scrollbar compose-fluent puts beside its navigation menu, which do nothing while
 * the menu fits. They have no role and no name, and Trips has no disabled control.
 */
internal val ScrollbarArrow: SemanticsMatcher =
    hasClickAction() and
        isNotEnabled() and
        SemanticsMatcher.keyNotDefined(SemanticsProperties.Role) and
        SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription)

/**
 * The Trips app in a Fluent pane of [spec], under the shell chrome, with motion frozen.
 */
@Composable
internal fun FluentHarness(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(ShellChrome) {
            PreviewPane(spec, modifier) { FluentAppEntry(spec, state, width) }
        }
    }
}
