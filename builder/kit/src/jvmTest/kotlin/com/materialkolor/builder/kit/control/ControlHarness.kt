package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.headless.HeadlessBottomSheet
import com.materialkolor.builder.kit.shell.posterDetentNames
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.SkinTestTheme
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.withClue

/**
 * Every skin the controls dispatch to, each named so a failure says which one.
 */
internal val ControlSkins: List<Pair<String, Skin>> = listOf(
    "material3" to Skin(SkinLibrary.Material3, expressive = false),
    "expressive" to Skin(SkinLibrary.Material3, expressive = true),
    "custom" to Skin(SkinLibrary.Custom, expressive = false),
)

/**
 * Runs [block] once per skin in a fresh test, with the skin's name as the clue.
 */
@OptIn(ExperimentalTestApi::class)
internal fun forEachSkin(block: suspend ComposeUiTest.(name: String, skin: Skin) -> Unit) {
    for ((name, skin) in ControlSkins) {
        withClue(name) { runComposeUiTest { block(name, skin) } }
    }
}

/**
 * A skin over a resolved document, a measured layout and frozen motion.
 */
@Composable
internal fun ControlsHarness(
    skin: Skin,
    isDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))) }
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        SkinTestTheme(skin, result, isDark, reducedMotion = false) {
            ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}

@Composable
internal fun OverlayTestButton(tag: String) {
    Box(Modifier.testTag(tag).size(40.dp).clickable(interactionSource = null, indication = null) {})
}

/**
 * [ControlsHarness] with the overlays rendering in the page or in windows of their own.
 */
@Composable
internal fun HostOverlays(
    skin: Skin,
    inTree: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalOverlaysInTree provides inTree) { ControlsHarness(skin, content = content) }
}

/**
 * Runs [block] for every skin, first with overlays in windows of their own and then in the page.
 */
@OptIn(ExperimentalTestApi::class)
internal fun hostEachWay(block: suspend ComposeUiTest.(skin: Skin, inTree: Boolean) -> Unit) {
    for (inTree in listOf(false, true)) {
        withClue(if (inTree) "in tree" else "in windows") { forEachSkin { _, skin -> block(skin, inTree) } }
    }
}

/**
 * The five skin variants every input is checked in.
 */
internal enum class SkinVariant(
    val skin: Skin,
) {
    Material3(Skin(SkinLibrary.Material3, expressive = false)),
    Expressive(Skin(SkinLibrary.Material3, expressive = true)),
    Custom(Skin(SkinLibrary.Custom, expressive = false)),
}

private val Document = ThemeDocument(seed = Argb(0x6750A4))

/**
 * A desktop window with a mouse, where the touch target is at its smallest.
 */
private val Desktop = LayoutInfo.of(1280.dp, 800.dp)

/**
 * Draws [content] on a panel of [variant], with motion frozen and a desktop layout.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.setSkinnedContent(
    variant: SkinVariant,
    isDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    setContent {
        SkinnedPanel(variant, isDark, content)
    }
}

/**
 * A panel of [variant] with motion frozen and a desktop layout.
 */
@Composable
internal fun SkinnedPanel(
    variant: SkinVariant,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(Document) }
    CompositionLocalProvider(LocalMotionFrozen provides true, LocalLayout provides Desktop) {
        SkinTestTheme(variant.skin, result, isDark, reducedMotion = false) {
            Box(Modifier.background(LocalBuilderTokens.current.panel).padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Runs [block] in a fresh composition for each skin variant, naming the variant on failure.
 */
@OptIn(ExperimentalTestApi::class)
internal fun forEverySkin(block: ComposeUiTest.(SkinVariant) -> Unit) {
    for (variant in SkinVariant.entries) {
        runComposeUiTest {
            withClue(variant.name) { block(variant) }
        }
    }
}

/**
 * The headless bottom sheet in the surrounding skin's overlay style, named by the kit's detent
 * names, the way the shell draws the poster's sheet.
 */
@Composable
internal fun TestBottomSheet(
    state: BottomSheetState,
    label: String,
    modifier: Modifier = Modifier,
    detentLabel: (BottomSheetDetent) -> String = posterDetentNames(),
    peekHeight: Dp = OverlayMetrics.sheetPeekHeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    HeadlessBottomSheet(
        state = state,
        label = label,
        detentLabel = detentLabel,
        peekHeight = peekHeight,
        style = overlayStyle(LocalSkin.current.library),
        modifier = modifier,
        content = content,
    )
}
