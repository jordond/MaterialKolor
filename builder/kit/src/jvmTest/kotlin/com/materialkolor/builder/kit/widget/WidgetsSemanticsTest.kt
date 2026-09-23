package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.export.ExportResolver
import com.materialkolor.builder.engine.resolve.RampSet
import com.materialkolor.builder.engine.resolve.RampStep
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt
import kotlin.test.Test

/** The seed every widget test resolves. */
internal val WidgetDocument: ThemeDocument = ThemeDocument(seed = Argb(0x6750A4))

/** Every skin the widgets are drawn in, named for the screenshots. */
internal val WidgetSkins: List<Pair<String, Skin>> = listOf(
    "material3" to Skin(Library.Material3, expressive = false),
    "expressive" to Skin(Library.Material3, expressive = true),
    "unstyled" to Skin(Library.Unstyled, expressive = false),
    "custom" to Skin(Library.Custom, expressive = false),
    "fluent" to Skin(Library.Fluent, expressive = false),
)

/** Runs [block] once per skin in a fresh test, with the skin's name as the clue. */
@OptIn(ExperimentalTestApi::class)
internal fun forEachWidgetSkin(block: suspend ComposeUiTest.(name: String, skin: Skin) -> Unit) {
    for ((name, skin) in WidgetSkins) {
        withClue(name) { runComposeUiTest { block(name, skin) } }
    }
}

/** A skin over [WidgetDocument], a measured layout and frozen motion. */
@Composable
internal fun WidgetHarness(
    skin: Skin,
    isDark: Boolean = false,
    coarsePointer: Boolean = false,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(WidgetDocument) }
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        BuilderTheme(skin, result, isDark, reducedMotion = false) {
            ProvideBuilderLayout(coarsePointer, Modifier.fillMaxSize()) { content() }
        }
    }
}

/** The frozen Material3 `Color.kt` codegen writes for [WidgetDocument], a real generated file. */
internal fun widgetGoldenColorFile(): GeneratedFile {
    val target = ExportTarget.Material3
    val prefs = ExportPrefs(mode = ExportMode.Frozen)
    val input = ExportInput(
        document = WidgetDocument,
        prefs = prefs,
        resolved = ExportResolver().resolve(WidgetDocument.forTarget(target), prefs),
        versions = ExportVersions(
            builder = "2.0.0",
            materialKolor = "6.0.0",
            fluent = "0.1.0",
            composeUnstyled = "1.0.0",
        ),
        shareUrl = "https://materialkolor.com/?seed=6750A4",
    )
    return generate(input).first { file -> file.path.endsWith("/Color.kt") }
}

internal fun widgetHasRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

private const val SwatchName = "primary, #6750A4, tone 40"
private const val SwatchTag = "swatch"
private val SwatchColor = Color(0xFF6750A4)
private val SwatchOnColor = Color(0xFFFFFFFF)

@OptIn(ExperimentalTestApi::class)
class WidgetsSemanticsTest {
    @Test
    fun swatchTile_everySkin_readsAsAButtonNamedByRoleHexAndTone() =
        forEachWidgetSkin { _, skin ->
            var clicks = 0
            setContent { WidgetHarness(skin) { WidgetSwatch(onClick = { clicks++ }) } }

            onNodeWithContentDescription(SwatchName).assert(widgetHasRole(Role.Button)).performClick()
            clicks shouldBe 1
        }

    @Test
    fun swatchTile_finePointer_showsCopyOnHoverAndOnFocus() =
        forEachWidgetSkin { _, skin ->
            var copies = 0
            setContent { WidgetHarness(skin) { WidgetSwatch(onCopy = { copies++ }) } }

            onNodeWithContentDescription("Copy").assertDoesNotExist()
            onNodeWithTag(SwatchTag).performMouseInput { moveTo(center) }
            waitForIdle()
            onNodeWithContentDescription("Copy").assertExists()
            onNodeWithTag(SwatchTag).performMouseInput { exit() }
            waitForIdle()
            onNodeWithContentDescription("Copy").assertDoesNotExist()

            onNodeWithContentDescription(SwatchName).requestFocus()
            waitForIdle()
            onNodeWithContentDescription("Copy").assert(widgetHasRole(Role.Button)).performClick()
            copies shouldBe 1
        }

    @Test
    fun swatchTile_coarsePointer_alwaysShowsCopy() =
        forEachWidgetSkin { _, skin ->
            setContent { WidgetHarness(skin, coarsePointer = true) { WidgetSwatch() } }

            onNodeWithContentDescription("Copy").assert(widgetHasRole(Role.Button))
        }

    @Test
    fun schemeChip_flagOff_isARadioNamedByItsLabel() =
        forEachWidgetSkin { _, skin ->
            var picked = 0
            setContent { WidgetHarness(skin) { WidgetChips(onPick = { picked++ }) } }

            onNodeWithContentDescription("Tonal spot").assert(widgetHasRole(Role.RadioButton)).assertIsSelected()
            onNodeWithContentDescription("Vibrant")
                .assert(widgetHasRole(Role.RadioButton))
                .assertIsNotSelected()
                .performClick()
            picked shouldBe 1
        }

    @Test
    fun schemeChip_flagOn_foldsTheSelectedStateIntoTheName() =
        forEachWidgetSkin { _, skin ->
            setContent {
                WidgetHarness(skin) {
                    CompositionLocalProvider(LocalFoldsStateIntoName provides true) { WidgetChips(onPick = {}) }
                }
            }

            onNodeWithContentDescription("Tonal spot, selected")
                .assert(widgetHasRole(Role.RadioButton))
                .assertIsSelected()
            onNodeWithContentDescription("Vibrant, not selected")
                .assert(widgetHasRole(Role.RadioButton))
                .assertIsNotSelected()
        }

    @Test
    fun rampStrip_everySkin_copiesAStopAndNamesItsMarkers() =
        forEachWidgetSkin { _, skin ->
            val ramp = ThemeResolver().resolve(WidgetDocument).ramps[KeyColor.Primary, false]
            var copied: RampStep? = null
            setContent {
                WidgetHarness(skin) { RampStrip(ramp, onCopyTone = { step -> copied = step }, Modifier.width(720.dp)) }
            }

            onAllNodes(widgetHasRole(Role.Button)).assertCountEquals(RampSet.Tones.size)
            val forty = ramp.steps.first { step -> step.tone == 40 }
            onNodeWithContentDescription("tone 40, ${forty.argb.toHex()}").performClick()
            copied shouldBe forty
            for (marker in ramp.markers) {
                val name = marker.role.name.replaceFirstChar { char -> char.lowercaseChar() }
                onNodeWithContentDescription("$name, tone ${marker.tone.roundToInt()}").assertExists()
            }
            onNodeWithContentDescription("key color, tone ${ramp.keyTone.roundToInt()}").assertExists()
        }

    @Test
    fun codeView_goldenColorFile_drawsOneSwatchPerColorLiteralAndCopies() =
        forEachWidgetSkin { _, skin ->
            val file = widgetGoldenColorFile()
            val literals = file.lines.flatten().count { token ->
                token.kind == TokenKind.ColorLiteral && token.color != null
            }
            var copies = 0
            setContent {
                WidgetHarness(skin) {
                    CodeView(
                        lines = file.lines,
                        onCopy = { copies++ },
                        modifier = Modifier
                            .width(900.dp)
                            .wrapContentHeight(Alignment.Top, unbounded = true)
                            .height(48.dp * file.lines.size),
                    )
                }
            }

            literals shouldBeGreaterThan 0
            onAllNodesWithTag(CodeSwatchTag, useUnmergedTree = true).assertCountEquals(literals)
            onNodeWithContentDescription("Copy").performClick()
            copies shouldBe 1
        }

    @Test
    fun deviceFrame_phone_laysTheScreenOutAtThePhoneWidthWithOrWithoutTheBezel() =
        runComposeUiTest {
            setContent {
                WidgetHarness(WidgetSkins.first().second) {
                    Column {
                        DeviceFrame(DeviceWidth.Phone) { Box(Modifier.testTag("bare").fillMaxWidth().height(40.dp)) }
                        DeviceFrame(DeviceWidth.Phone, enabled = true, modifier = Modifier.testTag("framed")) {
                            Box(Modifier.testTag("screen").fillMaxWidth().height(40.dp))
                        }
                    }
                }
            }

            onNodeWithTag("bare").assertWidthIsEqualTo(DeviceWidth.Phone.screenWidth)
            onNodeWithTag("screen").assertWidthIsEqualTo(DeviceWidth.Phone.screenWidth)
            onNodeWithTag("framed").getUnclippedBoundsInRoot().width shouldBeGreaterThan DeviceWidth.Phone.screenWidth
        }
}

@Composable
private fun WidgetSwatch(
    onCopy: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        SwatchTile(
            name = "primary",
            color = SwatchColor,
            onColor = SwatchOnColor,
            tone = 40.0,
            contrast = 6.4,
            onCopy = onCopy,
            onClick = onClick,
            modifier = Modifier.testTag(SwatchTag).width(200.dp),
        )
    }
}

@Composable
private fun WidgetChips(onPick: () -> Unit) {
    Row(Modifier.selectableGroup()) {
        SchemeChip(SwatchColor, SwatchOnColor, SwatchColor, selected = true, onClick = {}, label = "Tonal spot")
        SchemeChip(SwatchColor, SwatchOnColor, SwatchColor, selected = false, onClick = onPick, label = "Vibrant")
    }
}
