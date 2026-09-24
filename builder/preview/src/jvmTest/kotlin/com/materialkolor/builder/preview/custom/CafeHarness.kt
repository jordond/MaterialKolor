package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntSize
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import java.io.File

// The frames, documents and harness CafeAppTest checks the cafe with.

/**
 * Where B-213's recording job writes the baselines. Nothing is written unless a Roborazzi task
 * turns capture on.
 */
internal const val CafeScreenshotDir = "src/jvmTest/screenshots/cafe"

/** The frame the dock shows each device in, the kit's screen widths at the height of a first screen. */
internal val CafeFrames: Map<DeviceWidth, IntSize> = mapOf(
    DeviceWidth.Phone to IntSize(412, 900),
    DeviceWidth.Tablet to IntSize(840, 900),
    DeviceWidth.Desktop to IntSize(1280, 800),
)

/** The chrome the cafe sits in, the Custom skin coloured from the red chrome document. */
private val CafeSkin: Skin = Skin(Library.Custom, expressive = false)

/** The eight seeds of the sample's `AppThemeSeeds` as accents, in the sample's order (D25). */
private val CafeSampleAccents: List<Accent> = listOf(
    Accent("Love", Argb(0xE05263)),
    Accent("Cold", Argb(0x3E92CC)),
    Accent("Warm", Argb(0xF0A202)),
    Accent("Coffee", Argb(0x6F4E37)),
    Accent("Matcha", Argb(0x8DB600)),
    Accent("Iced", Argb(0x7EC8E3)),
    Accent("Tea", Argb(0xC9A227)),
    Accent("Chocolate", Argb(0x3F2212)),
)

/** The blue preview document with the sample's eight accents. */
private val AccentResult: ThemeResult =
    ThemeResolver().resolve(ThemeDocument(seed = Argb(0x1E88E5), accents = CafeSampleAccents))

internal val AccentLightSpec: PaneSpec = PaneSpec(AccentResult, isDark = false, label = "Light")

internal val AccentDarkSpec: PaneSpec = PaneSpec(AccentResult, isDark = true, label = "Dark")

/** How many accents the pane's document has. */
internal val PaneSpec.accentCount: Int
    get() = result.accents.families.size

/** The four families F-20 wants on every first screen, any slot of each. */
internal val CafeFamilies: Map<String, Set<CustomSlot>> = mapOf(
    "primary" to setOf(
        CustomSlot.Primary,
        CustomSlot.OnPrimary,
        CustomSlot.PrimaryContainer,
        CustomSlot.OnPrimaryContainer,
        CustomSlot.PrimaryPressed,
        CustomSlot.PrimaryRaised,
    ),
    "secondary" to setOf(
        CustomSlot.Secondary,
        CustomSlot.OnSecondary,
        CustomSlot.SecondaryContainer,
        CustomSlot.OnSecondaryContainer,
    ),
    "tertiary" to setOf(
        CustomSlot.Tertiary,
        CustomSlot.OnTertiary,
        CustomSlot.TertiaryContainer,
        CustomSlot.OnTertiaryContainer,
    ),
    "error" to setOf(CustomSlot.Error, CustomSlot.OnError, CustomSlot.ErrorContainer, CustomSlot.OnErrorContainer),
)

/** Every surface level of the Custom slots, all of which F-20 wants on every first screen. */
internal val CafeSurfaces: List<CustomSlot> =
    listOf(CustomSlot.Surface, CustomSlot.SurfaceRaised, CustomSlot.SurfaceSunken, CustomSlot.SurfaceInverse)

/** The border slots, one of which F-20 wants on every first screen. */
internal val CafeBorders: Set<CustomSlot> =
    setOf(CustomSlot.BorderFaint, CustomSlot.BorderSoft, CustomSlot.BorderStrong)

/** The accent parts that fill an area, rather than ink text or a glyph on one. */
internal val CafeFills: Set<AccentPart> = setOf(AccentPart.Color, AccentPart.Container)

/** Imports that open a popup, a window or an overlay, which on the web take the mirror over (D40). */
internal val CafePopupImports: List<String> = listOf(
    "com.materialkolor.builder.kit.control.BuilderMenu",
    "com.materialkolor.builder.kit.control.BuilderTooltip",
    "com.materialkolor.builder.kit.control.BuilderDialog",
    "com.materialkolor.builder.kit.control.BuilderSheet",
    "com.materialkolor.builder.kit.control.BuilderSidePanel",
    "com.materialkolor.builder.kit.control.BuilderBottomSheet",
    "com.materialkolor.builder.kit.control.BuilderToastHost",
    "com.materialkolor.builder.kit.control.BuilderSelect",
)

/**
 * A call that loops for ever, an infinite transition or an infinite repeat (F-20). The names are
 * split by a wildcard so the builder's architecture scan does not read this pattern as a call.
 */
internal val CafeEndlessMotion: Regex = Regex("""\b(rememberInfinite\w*Transition|infinite\w*Repeatable)\b""")

/** The sources the cafe is drawn from, its entry and every Cafe file. */
internal val CafeSources: List<File>
    get() {
        val folder = File("src/commonMain/kotlin/com/materialkolor/builder/preview/custom")
        val cafe = folder.listFiles().orEmpty().filter { file -> file.name.startsWith("Cafe") }
        return cafe.sortedBy { file -> file.name } + File(folder, "AppEntry.kt")
    }

/** The slot groups a kit control of several options declares once for all of them. */
private val CafeGroupRefs: Set<List<ColorRef>> = setOf(CustomComponent.Chip.refs)

/** A state with the order open on a phone and the last order's confirmation showing. */
internal fun cafeOrderOpen(): DemoAppState =
    DemoAppState().apply {
        setOn(PhoneOrderKey, true)
        setOn(PlacedKey, true)
    }

/**
 * Whether the node declares its slots itself, or is an option of a kit chip group that declares
 * them once for every option.
 */
internal fun SemanticsNode.cafeDeclaresSlots(): Boolean {
    if (PreviewRoles in config) return true
    val holder = generateSequence(parent) { node -> node.parent }.firstOrNull { node -> PreviewRoles in node.config }
    return holder != null && holder.config[PreviewRoles] in CafeGroupRefs
}

/** Every slot and accent declared by a node that shows at least partly on screen. */
internal fun SemanticsNodeInteractionsProvider.cafeRefsOnScreen(): List<ColorRef> {
    val screen = onRoot().fetchSemanticsNode().boundsInRoot
    return onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { node -> node.boundsInRoot.overlaps(screen) }
        .flatMap { node -> node.config[PreviewRoles] }
}

/** Every distinct color the node is drawn in. */
internal fun SemanticsNodeInteraction.captureToImagePixels(): Set<Int> =
    captureToImage().toPixelMap().let { map ->
        buildSet { for (x in 0 until map.width) for (y in 0 until map.height) add(map[x, y].toArgb()) }
    }

/** The cafe in a pane of [spec], under a red Custom chrome, with motion frozen. */
@Composable
internal fun CafeHarness(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(CafeSkin) {
            ProvideBuilderLayout(modifier = modifier) {
                PreviewPane(spec, Modifier.fillMaxSize()) { CustomAppEntry(spec, state, width) }
            }
        }
    }
}

/** The cafe on a tablet in a light and dark split, under a red Custom chrome, with motion frozen. */
@Composable
internal fun CafeSplitHarness(
    state: DemoAppState,
    split: SplitState,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(CafeSkin) {
            ProvideBuilderLayout(modifier = modifier) {
                SplitPreview(LightSpec, DarkSpec, split, Modifier.fillMaxSize()) { spec ->
                    CustomAppEntry(spec, state, DeviceWidth.Tablet)
                }
            }
        }
    }
}
