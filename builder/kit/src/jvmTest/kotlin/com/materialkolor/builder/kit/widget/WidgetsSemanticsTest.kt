package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
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
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.audit.ContrastBadge
import com.materialkolor.builder.engine.export.ExportResolver
import com.materialkolor.builder.engine.resolve.RampSet
import com.materialkolor.builder.engine.resolve.RampStep
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.control.ControlSkins
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.hasStateDescription
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinTestTheme
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test

/**
 * The seed every widget test resolves.
 */
internal val WidgetDocument: ThemeDocument = ThemeDocument(seed = Argb(0x6750A4))

/**
 * Every skin the widgets are drawn in, each named so a failure says which one.
 */
internal val WidgetSkins: List<Pair<String, Skin>> = ControlSkins

/**
 * Runs [block] once per skin in a fresh test, with the skin's name as the clue.
 */
@OptIn(ExperimentalTestApi::class)
internal fun forEachWidgetSkin(block: suspend ComposeUiTest.(name: String, skin: Skin) -> Unit) {
    for ((name, skin) in WidgetSkins) {
        withClue(name) { runComposeUiTest { block(name, skin) } }
    }
}

/**
 * A skin over [WidgetDocument], a measured layout and frozen motion.
 */
@Composable
internal fun WidgetHarness(
    skin: Skin,
    isDark: Boolean = false,
    coarsePointer: Boolean = false,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(WidgetDocument) }
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        SkinTestTheme(skin, result, isDark, reducedMotion = false) {
            ProvideBuilderLayout(coarsePointer, Modifier.fillMaxSize()) { content() }
        }
    }
}

/**
 * The frozen Material3 `Color.kt` codegen writes for [WidgetDocument], a real generated file.
 */
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
            composeMaterial3 = "1.12.0-alpha03",
            androidxMaterial3 = "1.5.0-alpha28",
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

            onNodeWithContentDescription("Tonal spot, radio, selected")
                .assert(widgetHasRole(Role.RadioButton))
                .assertIsSelected()
            onNodeWithContentDescription("Vibrant, radio, not selected")
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

    @Test
    fun deviceWidth_screenWidths_eachFallInADifferentWindowClass() {
        DeviceWidth.entries.associateWith { width -> WindowClass.of(width.screenWidth) } shouldBe mapOf(
            DeviceWidth.Phone to WindowClass.Compact,
            DeviceWidth.Tablet to WindowClass.Medium,
            DeviceWidth.Desktop to WindowClass.Expanded,
        )
    }

    @Test
    fun oneDecimal_ratioJustUnderATenth_floorsSoItNeverReadsAboveItsBadge() {
        oneDecimal(4.49) shouldBe "4.4"
        textBadge(4.49) shouldBe ContrastBadge.AaLarge
        oneDecimal(6.96) shouldBe "6.9"
        textBadge(6.96) shouldBe ContrastBadge.Aa
        oneDecimal(4.5) shouldBe "4.5"
        oneDecimal(7.0) shouldBe "7.0"
        oneDecimal(21.0) shouldBe "21.0"
    }

    @Test
    fun swatchTile_ratioJustUnderAA_showsTheFlooredRatioBesideAaLarge() =
        runComposeUiTest {
            setContent { WidgetHarness(WidgetSkins.first().second) { WidgetSwatch(contrast = 4.49) } }

            onNodeWithText("4.4:1", useUnmergedTree = true).assertExists()
            onNodeWithText("AA large", useUnmergedTree = true).assertExists()
        }

    @Test
    fun swatchTile_noOnPair_hidesTheContrastLine() =
        runComposeUiTest {
            setContent { WidgetHarness(WidgetSkins.first().second) { WidgetSwatch(contrast = null) } }

            onNodeWithContentDescription(SwatchName).assert(widgetHasRole(Role.Button))
            onAllNodesWithText(":1", substring = true, useUnmergedTree = true).assertCountEquals(0)
            for (badge in listOf("AAA", "AA", "AA large", "Fail")) {
                onAllNodesWithText(badge, useUnmergedTree = true).assertCountEquals(0)
            }
        }

    @Test
    fun rampStrip_compactCoarsePointerAt320_keepsEveryStopAtTheMinimumTarget() =
        runComposeUiTest {
            val ramp = ThemeResolver().resolve(WidgetDocument).ramps[KeyColor.Primary, false]
            val target = LayoutInfo.of(320.dp, 640.dp, coarsePointer = true).minTouchTarget
            setContent {
                Box(Modifier.size(320.dp, 640.dp)) {
                    WidgetHarness(WidgetSkins.first().second, coarsePointer = true) {
                        RampStrip(ramp, onCopyTone = {}, Modifier.fillMaxWidth())
                    }
                }
            }

            val stops = onAllNodes(widgetHasRole(Role.Button))
            stops.assertCountEquals(RampSet.Tones.size)
            repeat(RampSet.Tones.size) { index ->
                stops[index].assertWidthIsAtLeast(target).assertHeightIsAtLeast(target)
            }
        }

    @Test
    fun swatchTile_pinned_drawsThePinAndReadsOutPinned() =
        forEachWidgetSkin { _, skin ->
            var pinned by mutableStateOf(false)
            var folds by mutableStateOf(false)
            setContent {
                WidgetHarness(skin) {
                    CompositionLocalProvider(LocalFoldsStateIntoName provides folds) { WidgetSwatch(pinned = pinned) }
                }
            }
            val noState = SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription)
            onNodeWithTag(SwatchPinTag, useUnmergedTree = true).assertDoesNotExist()
            onNodeWithContentDescription(SwatchName).assert(noState)

            pinned = true
            waitForIdle()
            val badge = onNodeWithTag(SwatchPinTag, useUnmergedTree = true).captureToImage().toPixelMap()
            val inked = (0 until badge.height).sumOf { y ->
                (0 until badge.width).count { x -> widgetMatches(badge[x, y], SwatchOnColor) }
            }
            withClue("pin pixels in the on color") { inked shouldBeGreaterThan 0 }
            onNodeWithContentDescription(SwatchName).assert(hasStateDescription("pinned"))

            folds = true
            waitForIdle()
            onNodeWithContentDescription("$SwatchName, 6.4:1, AA, pinned").assert(widgetHasRole(Role.Button))
        }

    @Test
    fun swatchTile_flagOn_foldsTheRatioAndBadgeIntoTheNameOnlyWhenShown() =
        forEachWidgetSkin { _, skin ->
            var contrast by mutableStateOf<Double?>(4.49)
            setContent {
                WidgetHarness(skin) {
                    CompositionLocalProvider(LocalFoldsStateIntoName provides true) {
                        WidgetSwatch(contrast = contrast)
                    }
                }
            }
            onNodeWithContentDescription("$SwatchName, 4.4:1, AA large").assert(widgetHasRole(Role.Button))

            contrast = null
            waitForIdle()
            onNodeWithContentDescription(SwatchName).assert(widgetHasRole(Role.Button))
        }

    @Test
    fun codeView_scrollArea_takesFocusAndScrollsOnPageDown() =
        runComposeUiTest {
            val file = widgetGoldenColorFile()
            setContent {
                WidgetHarness(WidgetSkins.first().second) {
                    CodeView(file.lines, onCopy = {}, Modifier.size(480.dp, 200.dp))
                }
            }
            val scrollArea = onNodeWithTag(CodeScrollTag, useUnmergedTree = true)
            val scrollRange = SemanticsProperties.VerticalScrollAxisRange
            val lines = onNode(SemanticsMatcher.keyIsDefined(scrollRange), useUnmergedTree = true)

            fun offset(): Float = lines.fetchSemanticsNode().config[scrollRange].value()

            scrollArea.requestFocus()
            scrollArea.assertIsFocused()
            offset() shouldBe 0f
            scrollArea.performKeyInput { pressKey(Key.PageDown) }
            waitForIdle()
            offset() shouldBeGreaterThan 0f
        }
}

/**
 * Whether [actual] is [expected] to within rounding.
 */
internal fun widgetMatches(
    actual: Color,
    expected: Color,
): Boolean =
    listOf(
        actual.red to expected.red,
        actual.green to expected.green,
        actual.blue to expected.blue,
    ).all { (a, b) -> abs(a - b) <= WidgetColorTolerance }

/**
 * Fails unless [actual] is [expected] to within rounding.
 */
internal fun widgetShouldMatch(
    actual: Color,
    expected: Color,
) {
    withClue("$actual should be $expected") { widgetMatches(actual, expected) shouldBe true }
}

private const val WidgetColorTolerance = 0.02f

@Composable
private fun WidgetSwatch(
    onCopy: () -> Unit = {},
    onClick: () -> Unit = {},
    contrast: Double? = 6.4,
    pinned: Boolean = false,
) {
    Box(Modifier.fillMaxSize()) {
        SwatchTile(
            name = "primary",
            color = SwatchColor,
            onColor = SwatchOnColor,
            tone = 40.0,
            contrast = contrast,
            onCopy = onCopy,
            onClick = onClick,
            modifier = Modifier.testTag(SwatchTag).width(200.dp),
            pinned = pinned,
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
