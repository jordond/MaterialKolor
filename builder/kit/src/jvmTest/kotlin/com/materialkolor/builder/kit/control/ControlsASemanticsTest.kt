package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
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
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
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
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Skins = listOf(
    Skin(Library.Material3, expressive = false),
    Skin(Library.Material3, expressive = true),
    Skin(Library.Unstyled, expressive = false),
    Skin(Library.Custom, expressive = false),
    Skin(Library.Fluent, expressive = false),
)

private val Desktop = LayoutInfo.of(widthDp = 1280.dp, heightDp = 800.dp)
private val Phone = LayoutInfo.of(widthDp = 400.dp, heightDp = 800.dp)

private val PreviewModes = listOf("Light", "Split", "Dark")

private fun hasRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

private val HasNoRole = SemanticsMatcher.keyNotDefined(SemanticsProperties.Role)

@OptIn(ExperimentalTestApi::class)
class ControlsASemanticsTest {
    @Test
    fun button_everySkin_isANamedButtonThatReportsDisabled() =
        runComposeUiTest {
            var presses by mutableIntStateOf(0)
            eachSkin(
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
            eachSkin(
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
            eachSkin(
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
            eachSkin(
                content = {
                    BuilderSegmented(PreviewModes, mode, onSelect = { mode = it }, label = "Preview mode") { it }
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
    fun segmented_arrowKeys_moveTheChoiceAndTheFocus() =
        runComposeUiTest {
            var mode by mutableStateOf("Light")
            eachSkin(
                content = {
                    BuilderSegmented(PreviewModes, mode, onSelect = { mode = it }, label = "Preview mode") { it }
                },
            ) {
                mode = "Light"
                waitForIdle()
                onNodeWithText("Light").requestFocus()
                onNodeWithText("Light").assertIsFocused()

                onNodeWithText("Light").performKeyInput { pressKey(Key.DirectionRight) }
                waitForIdle()
                mode shouldBe "Split"
                onNodeWithText("Split").assertIsSelected().assertIsFocused()

                onNodeWithText("Split").performKeyInput { pressKey(Key.DirectionLeft) }
                waitForIdle()
                mode shouldBe "Light"

                onNodeWithText("Light").performKeyInput { pressKey(Key.DirectionLeft) }
                waitForIdle()
                mode shouldBe "Dark"
                onNodeWithText("Dark").assertIsSelected().assertIsFocused()

                onNodeWithText("Dark").performKeyInput { pressKey(Key.MoveHome) }
                waitForIdle()
                mode shouldBe "Light"
            }
        }

    @Test
    fun filterChip_everySkin_isACheckboxThatReportsSelection() =
        runComposeUiTest {
            var pinned by mutableStateOf(false)
            eachSkin(
                content = {
                    BuilderFilterChip(selected = pinned, onSelectedChange = { pinned = it }, label = "Pinned")
                    BuilderFilterChip(selected = true, onSelectedChange = {}, label = "Locked", enabled = false)
                },
            ) {
                pinned = false
                waitForIdle()
                onNodeWithText("Pinned").assert(hasRole(Role.Checkbox)).assertIsNotSelected().performClick()
                pinned shouldBe true
                onNodeWithText("Pinned").assertIsSelected()
                onNodeWithText("Locked").assert(hasRole(Role.Checkbox)).assertIsSelected().assertIsNotEnabled()
            }
        }

    @Test
    fun badge_everySkin_readsAsOneNodeNamedByItsLabel() =
        runComposeUiTest {
            eachSkin(
                content = {
                    BuilderBadge("AA", Modifier.testTag("badge"), status = BadgeStatus.Success, icon = IconId.Check)
                },
            ) {
                onNodeWithTag("badge").assert(hasText("AA")).assert(HasNoRole)
            }
        }

    @Test
    fun card_everySkin_pressableCardIsOneButtonAndPlainCardIsAGroup() =
        runComposeUiTest {
            var presses by mutableIntStateOf(0)
            eachSkin(
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
            eachSkin(content = { BuilderDivider(Modifier.testTag("divider")) }) {
                onNodeWithTag("divider")
                    .assert(HasNoRole)
                    .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                    .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            }
        }

    @Test
    fun progress_everySkin_reportsItsLabelAndAmount() =
        runComposeUiTest {
            eachSkin(
                content = {
                    BuilderProgress("Exporting", progress = 0.4f)
                    BuilderProgress("Generating candidates")
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
            }
        }

    @Test
    fun listRow_everySkin_isAButtonThatReportsTheCurrentRow() =
        runComposeUiTest {
            var opened by mutableStateOf<String?>(null)
            eachSkin(
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
                onNodeWithText("Ocean").assert(hasRole(Role.Button)).assert(hasText("Edited today")).assertIsSelected()
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
            eachSkin(
                layout = Phone,
                content = {
                    Footprint("button") { BuilderButton(onClick = {}, label = "Share") }
                    Footprint("icon") { BuilderIconButton({}, IconId.Undo, contentDescription = "Undo") }
                    Footprint("toggle") { BuilderToggleButton(true, {}, label = "Inspect") }
                    Footprint("chip") { BuilderFilterChip(false, {}, label = "Pinned") }
                    Footprint("row") { BuilderListRow("Ocean", onClick = {}) }
                    Footprint("segmented") {
                        BuilderSegmented(PreviewModes, "Light", onSelect = {}, label = "Preview mode") { it }
                    }
                },
            ) {
                for (tag in listOf("button", "icon", "toggle", "chip", "row", "segmented")) {
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
 * the skin it happened in.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.eachSkin(
    layout: LayoutInfo = Desktop,
    content: @Composable () -> Unit,
    check: ComposeUiTest.(Skin) -> Unit,
) {
    var skin by mutableStateOf(Skins.first())
    setContent {
        val result = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))) }
        CompositionLocalProvider(LocalMotionFrozen provides true, LocalLayout provides layout) {
            BuilderTheme(skin, result, isDark = false, reducedMotion = false) {
                Column(Modifier.background(LocalBuilderTokens.current.panel)) { content() }
            }
        }
    }
    for (next in Skins) {
        skin = next
        waitForIdle()
        try {
            check(next)
        } catch (failure: AssertionError) {
            throw AssertionError("$next: ${failure.message}", failure)
        }
    }
}
