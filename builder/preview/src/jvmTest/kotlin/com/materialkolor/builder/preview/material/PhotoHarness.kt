package com.materialkolor.builder.preview.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.split.PaneSpec

/** The Material 3 skin in its Expressive flavour, the one that shows the photo app. */
internal val PhotoSkin: Skin = Skin(Library.Material3, expressive = true)

/** The frame the dock shows each device in, the kit's screen widths at the height of a first screen. */
internal val PhotoFrames: Map<DeviceWidth, IntSize> = mapOf(
    DeviceWidth.Phone to IntSize(412, 900),
    DeviceWidth.Tablet to IntSize(840, 900),
    DeviceWidth.Desktop to IntSize(1280, 800),
)

/** The four families F-20 wants on every first screen. */
internal val PhotoFamilies: Map<String, Set<Role>> = mapOf(
    "primary" to setOf(Role.Primary, Role.OnPrimary, Role.PrimaryContainer, Role.OnPrimaryContainer),
    "secondary" to setOf(Role.Secondary, Role.OnSecondary, Role.SecondaryContainer, Role.OnSecondaryContainer),
    "tertiary" to setOf(Role.Tertiary, Role.OnTertiary, Role.TertiaryContainer, Role.OnTertiaryContainer),
    "error" to setOf(Role.Error, Role.OnError, Role.ErrorContainer, Role.OnErrorContainer),
)

/** The five container levels, four of which F-20 wants on every first screen. */
internal val PhotoContainerLevels: Set<Role> = setOf(
    Role.SurfaceContainerLowest,
    Role.SurfaceContainerLow,
    Role.SurfaceContainer,
    Role.SurfaceContainerHigh,
    Role.SurfaceContainerHighest,
)

/**
 * The photo app in an expressive Material 3 pane of [spec], under the chrome, with motion frozen,
 * and with the web fold on when [webFolds] asks for it.
 */
@OptIn(KitTestApi::class)
@Composable
internal fun PhotoHarness(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
    webFolds: Boolean = false,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(PhotoSkin) {
            if (webFolds) {
                ProvideWebFoldsForTest { PhotoPane(spec, state, width, modifier) }
            } else {
                PhotoPane(spec, state, width, modifier)
            }
        }
    }
}

@Composable
private fun PhotoPane(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
) {
    PreviewPane(spec, modifier) { MaterialAppEntry(spec, state, width, expressive = true) }
}

/** Everything the photo app keeps in [DemoAppState], to tell whether anything changed. */
internal fun DemoAppState.photoSnapshot(): List<Any> {
    val picks = listOf(
        PhotoMemoryChoice to PhotoMemories.size,
        PhotoViewChoice to PhotoView.entries.size,
    ).flatMap { (group, count) -> (0 until count).map { option -> isOn("$group.$option") } }
    return picks + listOf(isOn(PhotoCreateSwitch), selectedItem, tabIndex, text)
}
