package com.materialkolor.builder.kit.skin.material

import com.materialkolor.builder.kit.checkEach
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.a11y.foldsValueIntoName
import com.materialkolor.builder.kit.a11y.progressRoleWord
import com.materialkolor.builder.kit.a11y.valueNodeName
import com.materialkolor.builder.kit.control.BuilderProgress
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.ControlsHarness
import com.materialkolor.builder.kit.control.RingPixelsNeeded
import com.materialkolor.builder.kit.control.foldedSwitchName
import com.materialkolor.builder.kit.control.hasContentDescriptionExactly
import com.materialkolor.builder.kit.control.hasRole
import com.materialkolor.builder.kit.control.shouldCoverEverySide
import com.materialkolor.builder.kit.control.shouldRingAllTheWayRound
import com.materialkolor.builder.kit.control.shouldShowRing
import com.materialkolor.builder.kit.control.tabOntoRing
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalTabVisible
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.materialkolor.builder.kit.moduleSource
import kotlin.test.Test

/**
 * The Material3 skin in both of its flavours, the flat one first.
 */
private val Flavours: List<Skin> = listOf(
    Skin(SkinLibrary.Material3, expressive = false),
    Skin(SkinLibrary.Material3, expressive = true),
)

/**
 * The library switcher's own options, the longest row of the segmented control's real callers.
 */
private val Libraries: List<String> = listOf("Material 3", "Expressive", "Unstyled", "Fluent", "Custom")

/**
 * The preview modes, a row of three like most of the segmented control's callers.
 */
private val Modes: List<String> = listOf("Light", "Split", "Dark")

/**
 * Widths the segmented control's callers give it, from a narrow panel to an unbounded top bar.
 */
private val CallerWidths: List<Dp?> = listOf(160.dp, 240.dp, 360.dp, 480.dp, 720.dp, null)

/**
 * The files the expressive chrome is drawn in, which may never start an endless clock.
 */
private val ChromeSources: List<String> = listOf(
    "commonMain/kotlin/com/materialkolor/builder/kit/skin/material/MaterialSegmented.kt",
    "commonMain/kotlin/com/materialkolor/builder/kit/skin/material/MaterialProgress.kt",
    "commonMain/kotlin/com/materialkolor/builder/kit/control/BuilderSegmented.kt",
    "commonMain/kotlin/com/materialkolor/builder/kit/control/FoldedToggleName.kt",
    "commonMain/kotlin/com/materialkolor/builder/kit/a11y/ValueNodeName.kt",
)

/**
 * Stems, so the architecture scan does not read this list as an endless animation.
 */
private val EndlessClockWords: List<String> = listOf("rememberInfinite", "infiniteRepeat")

private const val RowTag = "row"

private const val BarTag = "bar"

private const val SwitchTag = "switch"

private const val ChromeTag = "chrome"

/**
 * An option of the segmented row tagged [RowTag].
 */
private val RowOption: SemanticsMatcher = hasRole(Role.RadioButton) and hasAnyAncestor(hasTestTag(RowTag))

/**
 * Whether a node reports a toggle state, which only the expressive toggle buttons do.
 */
private val HasToggleState: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

/**
 * The expressive chrome of the Material3 skin, each part checked in both flavours, so the flat one
 * keeps what it had and the expressive one swaps only what it should.
 */
@OptIn(ExperimentalTestApi::class)
class MaterialExpressiveChromeTest {
    @Test
    fun segmented_bothFlavours_readsAsARadioGroupAndOnlyTheExpressiveRowIsToggleButtons() =
        forEachFlavour { skin ->
            setContent { ControlsHarness(skin) { ModeRow() } }
            waitForIdle()

            onAllNodes(RowOption).assertCountEquals(Modes.size)
            onNode(RowOption and hasText("Split")).assertIsSelected()
            onAllNodes(hasRole(Role.Checkbox)).assertCountEquals(0)
            onAllNodes(RowOption and HasToggleState).assertCountEquals(if (skin.expressive) Modes.size else 0)

            onNode(RowOption and hasText("Dark")).performClick()
            waitForIdle()
            onNode(RowOption and hasText("Dark")).assertIsSelected()
            onNode(RowOption and hasText("Split")).assertIsNotSelected()
        }

    @OptIn(KitTestApi::class)
    @Test
    fun segmented_webFolds_bothFlavours_foldsTheRadioWordAndStateIntoEachName() =
        forEachFlavour { skin ->
            setContent { ControlsHarness(skin) { ProvideWebFoldsForTest { ModeRow() } } }
            waitForIdle()

            val names = onAllNodes(RowOption).fetchSemanticsNodes().map { node ->
                node.config[SemanticsProperties.ContentDescription].single()
            }
            names shouldContainExactlyInAnyOrder listOf(
                "Light, radio, not selected",
                "Split, radio, selected",
                "Dark, radio, not selected",
            )
            onNodeWithTag(RowTag)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Text)
                ?.joinToString { text -> text.text } shouldBe "Preview mode"
        }

    @Test
    fun segmented_everyCallerWidth_bothFlavours_opensNoPopupAndKeepsEveryOption() =
        forEachFlavour { skin ->
            var width by mutableStateOf<Dp?>(null)
            var options by mutableStateOf(Libraries)
            setContent {
                ControlsHarness(skin) {
                    val sized =
                        width?.let { given -> Modifier.width(given) } ?: Modifier.wrapContentWidth(unbounded = true)
                    Box(sized) {
                        BuilderSegmented(
                            options = options,
                            selected = options[1],
                            onSelect = {},
                            label = "Library",
                            modifier = Modifier.testTag(RowTag),
                        ) { option -> option }
                    }
                }
            }
            for (row in listOf(Libraries, Modes)) {
                for (given in CallerWidths) {
                    withClue("${row.size} options at ${given ?: "any width"}") {
                        options = row
                        width = given
                        waitForIdle()
                        onAllNodes(isPopup()).assertCountEquals(0)
                        onAllNodes(RowOption).assertCountEquals(row.size)
                    }
                }
            }
        }

    @Test
    fun segmented_expressive_tabOntoTheChosenOption_ringsItAndShowsMaterialsOwnFocusLayer() {
        for (chosen in Modes) {
            withClue(chosen) {
                runComposeUiTest {
                    val capture = tabOntoRing(Skin(SkinLibrary.Material3, expressive = true)) {
                        BuilderSegmented(
                            options = Modes,
                            selected = chosen,
                            onSelect = {},
                            label = "Preview mode",
                        ) { option -> option }
                    }
                    capture.shouldShowRing(RingPixelsNeeded)
                    capture.shouldCoverEverySide()
                    capture.shouldRingAllTheWayRound()
                    // Material lays its own focus layer over the toggle button, inside the kit's ring.
                    val inside = capture.focused.deflate(4f)
                    var layered = 0
                    for (y in inside.top.toInt() until inside.bottom.toInt()) {
                        for (x in inside.left.toInt() until inside.right.toInt()) {
                            if (capture.before[x, y] != capture.after[x, y]) layered++
                        }
                    }
                    withClue("pixels Material's focus layer changed") { layered shouldBeGreaterThan 0 }
                }
            }
        }
    }

    @Test
    fun progress_bothFlavours_reportsTheAmountOrThatNobodyCanTell() =
        forEachFlavour { skin ->
            var amount by mutableStateOf<Float?>(0.4f)
            setContent { ControlsHarness(skin) { BuilderProgress("Exporting", Modifier.testTag(BarTag), amount) } }
            waitForIdle()
            val known = onNodeWithTag(BarTag).fetchSemanticsNode()
            known.config[SemanticsProperties.ProgressBarRangeInfo] shouldBe ProgressBarRangeInfo(0.4f, 0f..1f)
            known.config[SemanticsProperties.ContentDescription] shouldBe listOf("Exporting")
            known.config[SemanticsProperties.StateDescription] shouldBe "40%"

            amount = null
            waitForIdle()
            onNodeWithTag(BarTag).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo] shouldBe
                ProgressBarRangeInfo.Indeterminate
        }

    @OptIn(KitTestApi::class)
    @Test
    fun progress_webFolds_bothFlavours_foldsTheRoleWordAndAmountIntoTheText() =
        forEachFlavour { skin ->
            var amount by mutableStateOf<Float?>(0.4f)
            setContent {
                ControlsHarness(skin) {
                    ProvideWebFoldsForTest { BuilderProgress("Exporting", Modifier.testTag(BarTag), amount) }
                }
            }
            waitForIdle()
            barText() shouldBe "Exporting, progress bar, 40%"
            amount = null
            waitForIdle()
            barText() shouldBe "Exporting, progress bar"
        }

    @Test
    fun progress_expressiveIndeterminate_movesOnTheKitClockAndHoldsWhenFrozenOrHidden() {
        for ((frozen, visible) in listOf(false to true, true to true, false to false)) {
            withClue("frozen $frozen, tab visible $visible") {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    setContent {
                        ControlsHarness(Skin(SkinLibrary.Material3, expressive = true)) {
                            CompositionLocalProvider(
                                LocalMotionFrozen provides frozen,
                                LocalTabVisible provides visible,
                            ) {
                                BuilderProgress("Exporting", Modifier.testTag(BarTag))
                            }
                        }
                    }
                    mainClock.advanceTimeByFrame()
                    val first = onNodeWithTag(BarTag).captureToImage().toPixelMap()
                    mainClock.advanceTimeBy(350)
                    val later = onNodeWithTag(BarTag).captureToImage().toPixelMap()
                    val changed = (0 until first.height).sumOf { y ->
                        (0 until first.width).count { x -> first[x, y] != later[x, y] }
                    }
                    if (frozen || !visible) changed shouldBe 0 else changed shouldBeGreaterThan 0
                }
            }
        }
    }

    @Test
    fun chromeSources_neverStartAnEndlessClock() {
        for (path in ChromeSources) {
            withClue(path) {
                val text = moduleSource(path).readText()
                EndlessClockWords.filter { word -> word in text }.shouldBeEmpty()
                // A loading indicator only with its progress, never the one that runs a clock of its own.
                text
                    .callArguments("LoadingIndicator")
                    .filterNot { arguments ->
                        "progress" in arguments
                    }.shouldBeEmpty()
                // A wavy bar only with its wave held still, since a moving wave runs Material's own clock.
                text
                    .callArguments("LinearWavyProgressIndicator")
                    .filterNot { arguments ->
                        "waveSpeed = 0.dp" in arguments
                    }.shouldBeEmpty()
            }
        }
    }

    @OptIn(KitTestApi::class)
    @Test
    fun foldedSwitchName_foldOn_readsTheSwitchWordAndStateAfterTheName() =
        runComposeUiTest {
            setContent { ControlsHarness(Flavours.first()) { ProvideWebFoldsForTest { WifiSwitch() } } }
            val wifi = onNodeWithTag(SwitchTag)
            wifi.assert(hasContentDescriptionExactly("Wi-Fi, switch, off"))

            wifi.performClick()
            waitForIdle()
            wifi.assert(hasContentDescriptionExactly("Wi-Fi, switch, on"))
        }

    @Test
    fun foldedSwitchName_foldOff_keepsTheNameAndLeavesTheStateToTheSwitch() =
        runComposeUiTest {
            setContent { ControlsHarness(Flavours.first()) { WifiSwitch() } }
            val wifi = onNodeWithTag(SwitchTag)
            wifi.assert(hasContentDescriptionExactly("Wi-Fi")).assert(hasRole(Role.Switch)).assertIsOff()

            wifi.performClick()
            waitForIdle()
            wifi.assert(hasContentDescriptionExactly("Wi-Fi")).assertIsOn()
        }

    @Test
    fun progressRoleWord_foldOnAndOff_readsBetweenTheNameAndTheValueOnlyOnTheWeb() {
        for (folds in listOf(true, false)) {
            withClue("folds $folds") {
                runComposeUiTest {
                    setContent {
                        ControlsHarness(Flavours.first()) {
                            val scope = if (folds) FoldScope.On else FoldScope.Off
                            scope.Provide {
                                val fold = foldsValueIntoName
                                val word = progressRoleWord
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .testTag(BarTag)
                                        .semantics { valueNodeName("Upload", "40%", fold, roleWord = word) },
                                )
                            }
                        }
                    }
                    val config = onNodeWithTag(BarTag).fetchSemanticsNode().config
                    config.getOrNull(SemanticsProperties.Text)?.joinToString { text -> text.text } shouldBe
                        if (folds) "Upload, progress bar, 40%" else null
                    config.getOrNull(SemanticsProperties.ContentDescription) shouldBe
                        if (folds) null else listOf("Upload")
                }
            }
        }
    }

    @Test
    fun expressiveFlag_changesWhatTheSegmentedRowAndTheBarDraw() {
        val images = Flavours.map { skin ->
            var pixels: List<Int> = emptyList()
            runComposeUiTest {
                setContent {
                    ControlsHarness(skin) {
                        Column(Modifier.testTag(ChromeTag)) {
                            ModeRow()
                            BuilderProgress("Exporting", progress = 0.4f)
                        }
                    }
                }
                val map = onNodeWithTag(ChromeTag).captureToImage().toPixelMap()
                pixels = (0 until map.height).flatMap { y -> (0 until map.width).map { x -> map[x, y].hashCode() } }
            }
            pixels
        }
        images.first() shouldNotBe images.last()
    }
}

/**
 * Whether a test turns the web's folds on around what it shows.
 */
private enum class FoldScope {
    On,
    Off,
    ;

    @OptIn(KitTestApi::class)
    @Composable
    fun Provide(content: @Composable () -> Unit) {
        if (this == On) ProvideWebFoldsForTest(content) else content()
    }
}

/**
 * Runs [block] in a fresh test for each flavour of the Material3 skin, then fails with every
 * flavour that failed, each by name.
 */
@OptIn(ExperimentalTestApi::class)
private fun forEachFlavour(block: suspend ComposeUiTest.(skin: Skin) -> Unit) =
    checkEach(Flavours, name = { skin -> if (skin.expressive) "expressive" else "material3" }) { skin ->
        runComposeUiTest { block(skin) }
    }

/**
 * The preview modes with Split chosen, tagged [RowTag].
 */
@Composable
private fun ModeRow() {
    var mode by remember { mutableStateOf("Split") }
    BuilderSegmented(
        options = Modes,
        selected = mode,
        onSelect = { next -> mode = next },
        label = "Preview mode",
        modifier = Modifier.testTag(RowTag),
    ) { option -> option }
}

/**
 * A Material switch an app lays out for itself, the row taking the switch and naming it.
 */
@Composable
private fun WifiSwitch() {
    var on by remember { mutableStateOf(false) }
    Row(
        Modifier
            .testTag(SwitchTag)
            .toggleable(value = on, role = Role.Switch) { next -> on = next }
            .foldedSwitchName("Wi-Fi", on),
    ) {
        Switch(checked = on, onCheckedChange = null)
    }
}

/**
 * The text of the bar tagged [BarTag], as the web reads it.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.barText(): String? =
    onNodeWithTag(BarTag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.joinToString { text ->
        text.text
    }

/**
 * What each call of [name] passes, the text between its parentheses.
 */
private fun String.callArguments(name: String): List<String> =
    Regex("""\b$name\(""")
        .findAll(this)
        .map { call ->
            val start = call.range.last + 1
            var depth = 1
            var end = start
            while (depth > 0 && end < length) {
                when (this[end]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                end++
            }
            substring(start, end - 1)
        }.toList()
