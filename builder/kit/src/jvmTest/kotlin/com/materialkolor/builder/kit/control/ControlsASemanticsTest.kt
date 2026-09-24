package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Every skin the action controls are checked in. */
internal val ActionSkins: List<Skin> = listOf(
    Skin(Library.Material3, expressive = false),
    Skin(Library.Material3, expressive = true),
    Skin(Library.Unstyled, expressive = false),
    Skin(Library.Custom, expressive = false),
    Skin(Library.Fluent, expressive = false),
)

private val Desktop = LayoutInfo.of(widthDp = 1280.dp, heightDp = 800.dp)
private val Phone = LayoutInfo.of(widthDp = 400.dp, heightDp = 800.dp)

internal val ActionPreviewModes: List<String> = listOf("Light", "Split", "Dark")

internal val ActionPaletteStyles: List<String> = listOf("Tonal spot", "Vibrant", "Expressive", "Fidelity", "Content")

private val HasNoRole = SemanticsMatcher.keyNotDefined(SemanticsProperties.Role)

@OptIn(ExperimentalTestApi::class)
class ControlsASemanticsTest {
    @Test
    fun button_everySkin_isANamedButtonThatReportsDisabled() =
        runComposeUiTest {
            var presses by mutableIntStateOf(0)
            eachActionSkin(
                content = {
                    for (emphasis in Emphasis.entries) {
                        BuilderButton(onClick = { presses++ }, label = emphasis.name, emphasis = emphasis)
                    }
                    BuilderButton(onClick = {}, label = "Delete", emphasis = Emphasis.Danger, enabled = false)
                },
            ) {
                presses = 0
                for (emphasis in Emphasis.entries) {
                    onNodeWithText(emphasis.name).assert(hasRole(Role.Button)).assertIsEnabled().performClick()
                }
                presses shouldBe Emphasis.entries.size
                onNodeWithText("Delete").assert(hasRole(Role.Button)).assertIsNotEnabled()
            }
        }

    @Test
    fun iconButton_everySkin_isAButtonNamedByItsDescription() =
        runComposeUiTest {
            eachActionSkin(
                content = {
                    BuilderIconButton(onClick = {}, icon = IconId.Undo, contentDescription = "Undo")
                    BuilderIconButton(onClick = {}, icon = IconId.Redo, contentDescription = "Redo", enabled = false)
                },
            ) {
                onNodeWithContentDescription("Undo").assert(hasRole(Role.Button)).assertIsEnabled()
                onNodeWithContentDescription("Redo").assert(hasRole(Role.Button)).assertIsNotEnabled()
            }
        }

    @Test
    fun toggleButton_everySkin_isACheckboxThatReportsOnAndOff() =
        runComposeUiTest {
            var inspect by mutableStateOf(false)
            eachActionSkin(
                content = {
                    BuilderToggleButton(checked = inspect, onCheckedChange = { inspect = it }, label = "Inspect")
                    BuilderToggleButton(checked = true, onCheckedChange = {}, label = "Vision", enabled = false)
                },
            ) {
                inspect = false
                waitForIdle()
                onNodeWithText("Inspect").assert(hasRole(Role.Checkbox)).assertIsOff().performClick()
                inspect shouldBe true
                onNodeWithText("Inspect").assertIsOn()
                onNodeWithText("Vision").assert(hasRole(Role.Checkbox)).assertIsOn().assertIsNotEnabled()
            }
        }

    @Test
    fun segmented_everySkin_isARadioGroupWithTheChoiceSelected() =
        runComposeUiTest {
            var mode by mutableStateOf("Light")
            eachActionSkin(
                content = {
                    BuilderSegmented(ActionPreviewModes, mode, onSelect = { mode = it }, label = "Preview mode") { it }
                    BuilderSegmented(
                        listOf("Hex", "HCT"),
                        "Hex",
                        onSelect = {},
                        label = "Input",
                        enabled = false,
                    ) { it }
                },
            ) {
                mode = "Light"
                waitForIdle()
                onNodeWithContentDescription("Preview mode").assertExists()
                onNodeWithText("Light").assert(hasRole(Role.RadioButton)).assertIsSelected()
                onNodeWithText("Split").assert(hasRole(Role.RadioButton)).assertIsNotSelected().performClick()
                mode shouldBe "Split"
                onNodeWithText("Split").assertIsSelected()
                onNodeWithText("Light").assertIsNotSelected()
                onNodeWithText("HCT").assert(hasRole(Role.RadioButton)).assertIsNotSelected().assertIsNotEnabled()
            }
        }

    @Test
    fun segmented_choiceOutsideTheOptions_leavesNothingChosen() =
        runComposeUiTest {
            eachActionSkin(
                content = {
                    BuilderSegmented(ActionPreviewModes, "Auto", onSelect = {}, label = "Preview mode") { it }
                },
            ) {
                for (mode in ActionPreviewModes) onNodeWithText(mode).assertIsNotSelected()
            }
        }

    @Test
    fun segmented_stretchedGroup_sharesTheWidthAmongTheOptions() =
        runComposeUiTest {
            eachActionSkin(
                content = {
                    Box(Modifier.width(600.dp)) {
                        BuilderSegmented(
                            ActionPreviewModes,
                            "Light",
                            onSelect = {},
                            label = "Preview mode",
                            modifier = Modifier.fillMaxWidth(),
                        ) { it }
                    }
                },
            ) {
                for (mode in ActionPreviewModes) onNodeWithText(mode).assertWidthIsAtLeast(180.dp)
            }
        }

    @Test
    fun filterChip_everySkin_isACheckboxThatReportsSelection() =
        runComposeUiTest {
            var pinned by mutableStateOf(false)
            eachActionSkin(
                content = {
                    BuilderFilterChip(selected = pinned, onSelectedChange = { pinned = it }, label = "Pinned")
                    BuilderFilterChip(selected = true, onSelectedChange = {}, label = "Locked", enabled = false)
                },
            ) {
                pinned = false
                waitForIdle()
                onNodeWithText("Pinned")
                    .assert(hasRole(Role.Checkbox))
                    .assertIsNotSelected()
                    .assertIsOff()
                    .performClick()
                pinned shouldBe true
                onNodeWithText("Pinned").assertIsSelected().assertIsOn()
                onNodeWithText("Locked")
                    .assert(hasRole(Role.Checkbox))
                    .assertIsSelected()
                    .assertIsOn()
                    .assertIsNotEnabled()
            }
        }

    @Test
    fun choiceChips_everySkin_isARadioGroupWithTheChoiceSelected() =
        runComposeUiTest {
            var style by mutableStateOf("Tonal spot")
            eachActionSkin(
                content = {
                    BuilderChoiceChips(ActionPaletteStyles, style, onSelect = { style = it }, label = "Style") { it }
                    BuilderChoiceChips(
                        listOf("Standard", "Medium"),
                        "Standard",
                        onSelect = {},
                        label = "Contrast",
                        enabled = false,
                    ) { it }
                },
            ) {
                style = "Tonal spot"
                waitForIdle()
                onNodeWithContentDescription("Style").assertExists()
                onNodeWithText("Tonal spot").assert(hasRole(Role.RadioButton)).assertIsSelected()
                onNodeWithText("Vibrant").assert(hasRole(Role.RadioButton)).assertIsNotSelected().performClick()
                style shouldBe "Vibrant"
                onNodeWithText("Vibrant").assertIsSelected()
                onNodeWithText("Tonal spot").assertIsNotSelected()
                onNodeWithText("Medium")
                    .assert(hasRole(Role.RadioButton))
                    .assertIsNotSelected()
                    .assertIsNotEnabled()
            }
        }

    @Test
    fun choiceChips_narrowWidth_wrapsOntoAnotherLine() =
        runComposeUiTest {
            eachActionSkin(
                content = {
                    Box(Modifier.width(240.dp)) {
                        BuilderChoiceChips(ActionPaletteStyles, "Vibrant", onSelect = {}, label = "Style") { it }
                    }
                },
            ) {
                val first = onNodeWithText(ActionPaletteStyles.first()).getUnclippedBoundsInRoot()
                val last = onNodeWithText(ActionPaletteStyles.last()).getUnclippedBoundsInRoot()
                last.top shouldBeGreaterThan first.bottom
            }
        }

    @Test
    fun badge_everySkin_readsAsOneNodeNamedByItsLabel() =
        runComposeUiTest {
            eachActionSkin(
                content = {
                    BuilderBadge("AA", Modifier.testTag("badge"), status = BadgeStatus.Success, icon = IconId.Check)
                },
            ) {
                onNodeWithTag("badge").assert(hasText("AA")).assert(HasNoRole)
            }
        }

    // b-225
    @Test
    fun badge_blankLabel_drawsNothingInsteadOfFailing() =
        runComposeUiTest {
            eachActionSkin(
                content = {
                    BuilderBadge("", Modifier.testTag("blank"), status = BadgeStatus.Success, icon = IconId.Check)
                },
            ) {
                onNodeWithTag("blank").assertDoesNotExist()
            }
        }

    @Test
    fun card_everySkin_pressableCardIsOneButtonAndPlainCardIsAGroup() =
        runComposeUiTest {
            var presses by mutableIntStateOf(0)
            eachActionSkin(
                content = {
                    BuilderCard(Modifier.testTag("plain")) { BuilderText("Kotlin") }
                    BuilderCard(Modifier.testTag("pressable"), onClick = { presses++ }) { BuilderText("Swift") }
                    BuilderCard(Modifier.testTag("disabled"), onClick = {}, enabled = false) { BuilderText("Dart") }
                },
            ) {
                presses = 0
                onNodeWithTag("pressable").assert(hasRole(Role.Button)).assert(hasText("Swift")).performClick()
                presses shouldBe 1
                onNodeWithTag("disabled").assert(hasRole(Role.Button)).assertIsNotEnabled()
                onNodeWithTag("plain").assert(HasNoRole)
                onNodeWithText("Kotlin").assertExists()
            }
        }

    @Test
    fun divider_everySkin_staysOutOfTheAccessibilityTree() =
        runComposeUiTest {
            eachActionSkin(content = { BuilderDivider(Modifier.testTag("divider")) }) {
                onNodeWithTag("divider")
                    .assert(HasNoRole)
                    .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                    .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            }
        }

    @Test
    fun progress_everySkin_reportsItsLabelAndAmount() =
        runComposeUiTest {
            eachActionSkin(
                content = {
                    BuilderProgress("Exporting", progress = 0.4f)
                    BuilderProgress("Generating candidates")
                    BuilderProgress("Broken", progress = Float.NaN)
                },
            ) {
                onNodeWithContentDescription("Exporting")
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ProgressBarRangeInfo,
                            ProgressBarRangeInfo(0.4f, 0f..1f),
                        ),
                    ).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "40%"))
                onNodeWithContentDescription("Generating candidates")
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ProgressBarRangeInfo,
                            ProgressBarRangeInfo.Indeterminate,
                        ),
                    )
                onNodeWithContentDescription("Broken")
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ProgressBarRangeInfo,
                            ProgressBarRangeInfo(0f, 0f..1f),
                        ),
                    ).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0%"))
            }
        }

    @Test
    fun listRow_everySkin_isAButtonThatReportsTheCurrentRow() =
        runComposeUiTest {
            var opened by mutableStateOf<String?>(null)
            eachActionSkin(
                content = {
                    BuilderListRow(
                        "Ocean",
                        supporting = "Edited today",
                        onClick = { opened = "Ocean" },
                        selected = true,
                    )
                    BuilderListRow("Forest", onClick = { opened = "Forest" }, selected = false)
                    BuilderListRow("Archived", onClick = {}, enabled = false)
                    BuilderListRow("Read only", Modifier.testTag("info"), supporting = "Nothing to press")
                },
            ) {
                opened = null
                onNodeWithText("Ocean")
                    .assert(hasRole(Role.Button))
                    .assert(hasText("Edited today"))
                    .assertIsSelected()
                onNodeWithText("Forest").assert(hasRole(Role.Button)).assertIsNotSelected().performClick()
                opened shouldBe "Forest"
                onNodeWithText("Archived")
                    .assert(hasRole(Role.Button))
                    .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
                    .assertIsNotEnabled()
                onNodeWithTag("info").assert(HasNoRole).assert(hasText("Nothing to press"))
            }
        }

    @Test
    fun touchTarget_compactLayout_growsEveryPressableControlToFortyEightDp() =
        runComposeUiTest {
            eachActionSkin(
                layout = Phone,
                content = {
                    Footprint("button") { BuilderButton(onClick = {}, label = "Share") }
                    Footprint("icon") { BuilderIconButton({}, IconId.Undo, contentDescription = "Undo") }
                    Footprint("toggle") { BuilderToggleButton(true, {}, label = "Inspect") }
                    Footprint("chip") { BuilderFilterChip(false, {}, label = "Pinned") }
                    Footprint("row") { BuilderListRow("Ocean", onClick = {}) }
                    Footprint("segmented") {
                        BuilderSegmented(ActionPreviewModes, "Light", onSelect = {}, label = "Preview mode") { it }
                    }
                    Footprint("choice") {
                        BuilderChoiceChips(
                            ActionPaletteStyles.take(2),
                            "Vibrant",
                            onSelect = {},
                            label = "Style",
                        ) { it }
                    }
                },
            ) {
                for (tag in listOf("button", "icon", "toggle", "chip", "row", "segmented", "choice")) {
                    onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
                }
                onNodeWithTag("icon").assertWidthIsAtLeast(48.dp)
            }
        }
}

/**
 * The space [content] takes in its parent, tagged [tag].
 *
 * Material grows the space a component takes rather than the component's own node, so the only
 * size every skin agrees on is the footprint.
 */
@Composable
private fun Footprint(
    tag: String,
    content: @Composable () -> Unit,
) {
    Box(Modifier.testTag(tag)) { content() }
}

/**
 * Draws [content] once and walks it through every skin, running [check] in each.
 *
 * Motion is frozen so a press or a sweep never leaves the tree mid animation, and a failure names
 * the skin it happened in. [direction] sets the reading direction for the right to left runs.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.eachActionSkin(
    layout: LayoutInfo = Desktop,
    direction: LayoutDirection = LayoutDirection.Ltr,
    content: @Composable () -> Unit,
    check: ComposeUiTest.(Skin) -> Unit,
) {
    var skin by mutableStateOf(ActionSkins.first())
    setContent {
        val result = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))) }
        CompositionLocalProvider(
            LocalMotionFrozen provides true,
            LocalLayout provides layout,
            LocalLayoutDirection provides direction,
        ) {
            BuilderTheme(skin, result, isDark = false, reducedMotion = false) {
                Column(Modifier.background(LocalBuilderTokens.current.panel)) { content() }
            }
        }
    }
    for (next in ActionSkins) {
        skin = next
        waitForIdle()
        try {
            check(next)
        } catch (failure: AssertionError) {
            throw AssertionError("$next: ${failure.message}", failure)
        }
    }
}
