package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.InvalidReason
import com.materialkolor.builder.domain.color.ParseNote
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** The five skin variants every input is checked in. */
internal enum class SkinVariant(
    val skin: Skin,
) {
    Material3(Skin(Library.Material3, expressive = false)),
    Expressive(Skin(Library.Material3, expressive = true)),
    Unstyled(Skin(Library.Unstyled, expressive = false)),
    Custom(Skin(Library.Custom, expressive = false)),
    Fluent(Skin(Library.Fluent, expressive = false)),
}

private val Document = ThemeDocument(seed = Argb(0x6750A4))

/** A desktop window with a mouse, where the touch target is at its smallest. */
private val Desktop = LayoutInfo.of(1280.dp, 800.dp)

/** Draws [content] on a panel of [variant], with motion frozen and a desktop layout. */
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

/** A panel of [variant] with motion frozen and a desktop layout. */
@Composable
internal fun SkinnedPanel(
    variant: SkinVariant,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(Document) }
    CompositionLocalProvider(LocalMotionFrozen provides true, LocalLayout provides Desktop) {
        BuilderTheme(variant.skin, result, isDark, reducedMotion = false) {
            Box(Modifier.background(LocalBuilderTokens.current.panel).padding(16.dp)) {
                content()
            }
        }
    }
}

/** Runs [block] in a fresh composition for each skin variant, naming the variant on failure. */
@OptIn(ExperimentalTestApi::class)
internal fun forEverySkin(block: ComposeUiTest.(SkinVariant) -> Unit) {
    for (variant in SkinVariant.entries) {
        runComposeUiTest {
            withClue(variant.name) { block(variant) }
        }
    }
}

internal fun hasInputRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

/** What the seed field says under itself in these tests, standing in for the app's own copy. */
internal fun inputErrorMessage(reason: InvalidReason): String =
    when (reason) {
        InvalidReason.Empty -> "Type a color"
        InvalidReason.BadHex -> "Hex takes 3, 6 or 8 digits"
        InvalidReason.BadArguments -> "Check the values inside the brackets"
        InvalidReason.UnknownFunction -> "Try rgb(), hsl() or oklch()"
        InvalidReason.UnknownName -> "Not a CSS color name"
        InvalidReason.Unrecognized -> "Not a color"
    }

internal fun inputNoteMessage(notes: Set<ParseNote>): String =
    notes.sorted().joinToString(". ") { note ->
        when (note) {
            ParseNote.AlphaDropped -> "Alpha dropped, the seed is always opaque"
            ParseNote.Clamped -> "Clamped to sRGB"
        }
    }

/** The hex field with the test copy for its messages. */
@Composable
internal fun InputHexField(
    value: Argb,
    onCommit: (Argb, Set<ParseNote>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    enabled: Boolean = true,
) {
    BuilderHexField(
        value = value,
        onCommit = onCommit,
        label = label,
        errorMessage = ::inputErrorMessage,
        noteMessage = ::inputNoteMessage,
        modifier = modifier,
        large = large,
        enabled = enabled,
    )
}

internal fun hasStateDescription(state: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)

private const val On = "on"
private const val Off = "off"

@OptIn(ExperimentalTestApi::class)
class ControlsBSemanticsTest {
    @Test
    fun switch_everySkin_isOneSwitchTargetLabelIncluded() =
        forEverySkin { variant ->
            var checked by mutableStateOf(false)
            var disabledFlipped by mutableStateOf(false)
            setSkinnedContent(variant) {
                Column {
                    BuilderSwitch(checked, { checked = it }, "Dark mode", Modifier.testTag(On))
                    BuilderSwitch(
                        checked = false,
                        onCheckedChange = { disabledFlipped = true },
                        label = "AMOLED black",
                        modifier = Modifier.testTag(Off),
                        enabled = false,
                    )
                }
            }

            onNodeWithTag(On)
                .assert(hasInputRole(Role.Switch))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
                .assert(hasStateDescription("Off"))
                .assertIsEnabled()
            onNodeWithText("Dark mode", useUnmergedTree = true).performClick()
            checked shouldBe true
            onNodeWithTag(On).assert(hasStateDescription("On"))

            onNodeWithTag(Off)
                .assert(hasInputRole(Role.Switch))
                .assert(hasStateDescription("Off, disabled"))
                .assertIsNotEnabled()
            onNodeWithTag(Off).performClick()
            disabledFlipped shouldBe false
        }

    @Test
    fun checkbox_everySkin_isOneCheckboxTargetLabelIncluded() =
        forEverySkin { variant ->
            var checked by mutableStateOf(false)
            setSkinnedContent(variant) {
                Column {
                    BuilderCheckbox(checked, { checked = it }, "Show pins", Modifier.testTag(On))
                    BuilderCheckbox(true, {}, "Locked", Modifier.testTag(Off), enabled = false)
                }
            }

            onNodeWithTag(On)
                .assert(hasInputRole(Role.Checkbox))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
                .assert(hasStateDescription("Not checked"))
            onNodeWithText("Show pins", useUnmergedTree = true).performClick()
            checked shouldBe true
            onNodeWithTag(On).assert(hasStateDescription("Checked"))

            onNodeWithTag(Off).assert(hasStateDescription("Checked, disabled")).assertIsNotEnabled()
        }

    @Test
    fun slider_everySkin_setsProgressNameAndSpokenValue() =
        forEverySkin { variant ->
            setSkinnedContent(variant) {
                Column {
                    BuilderSlider(0.5f, {}, "Contrast", Modifier.testTag(On).width(320.dp), valueRange = -1f..1f)
                    BuilderSlider(
                        value = 0.25f,
                        onValueChange = {},
                        label = "Chroma",
                        modifier = Modifier.testTag(Off).width(320.dp),
                        stateDescription = "Quarter",
                        enabled = false,
                    )
                }
            }

            onNodeWithTag(On)
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Contrast")))
                .assert(hasStateDescription("0.50"))
                .assertIsEnabled()
            onNodeWithTag(Off).assert(hasStateDescription("Quarter, disabled")).assertIsNotEnabled()
        }

    @Test
    fun tabs_everySkin_haveTheTabRoleAndSelection() =
        forEverySkin { variant ->
            var selected by mutableStateOf("App")
            setSkinnedContent(variant) {
                BuilderTabs(listOf("App", "Components", "Roles"), selected, { selected = it }, { it })
            }

            tab("App").assertIsSelected()
            tab("Roles").assertIsNotSelected().performClick()
            selected shouldBe "Roles"
            tab("Roles").assertIsSelected()
            tab("App").assertIsNotSelected()
        }

    @Test
    fun disclosure_everySkin_isAButtonThatSpeaksAndActsOnItsState() =
        forEverySkin { variant ->
            var expanded by mutableStateOf(false)
            setSkinnedContent(variant) {
                BuilderDisclosure(expanded, { expanded = it }, "Core colors and pins", summary = "Primary and pins") {
                    BuilderText("Inside the row")
                }
            }

            val row = onNode(hasText("Core colors and pins") and hasClickAction())
            row
                .assert(hasInputRole(Role.Button))
                .assert(hasStateDescription("Collapsed"))
                .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            onNodeWithText("Inside the row").assertDoesNotExist()
            row.performClick()
            expanded shouldBe true
            row.assert(hasStateDescription("Expanded")).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            onNodeWithText("Inside the row").assertExists()
        }

    @Test
    fun disclosure_everySkin_disabledOffersNoExpandOrCollapse() =
        forEverySkin { variant ->
            var changes = 0
            setSkinnedContent(variant) {
                Column {
                    BuilderDisclosure(false, { changes++ }, "Locked closed", enabled = false) {
                        BuilderText("Closed inside")
                    }
                    BuilderDisclosure(true, { changes++ }, "Locked open", enabled = false) {
                        BuilderText("Open inside")
                    }
                }
            }

            for (title in listOf("Locked closed", "Locked open")) {
                withClue(title) {
                    onNode(hasText(title) and hasInputRole(Role.Button))
                        .assertIsNotEnabled()
                        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
                        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
                        .performClick()
                }
            }
            changes shouldBe 0
        }

    @Test
    fun textFields_everySkin_areEditableAndReportErrorsAndDisabled() =
        forEverySkin { variant ->
            setSkinnedContent(variant) {
                Column {
                    BuilderTextField("Ocean", {}, "Project name", Modifier.testTag(On), error = { text ->
                        if (text.isBlank()) "A project needs a name" else null
                    })
                    InputHexField(Argb(0x6750A4), { _, _ -> }, "Seed", Modifier.testTag(Off), enabled = false)
                }
            }

            onNodeWithTag(On).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)).assertIsEnabled()
            onNodeWithTag(On).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
            onNodeWithTag(On).requestFocus()
            onNodeWithTag(On).performTextReplacement(" ")
            onNodeWithTag(On).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            onNodeWithText("A project needs a name", useUnmergedTree = true).assertExists()

            onNodeWithTag(Off).assertIsNotEnabled()
            editableText(Off) shouldBe "#6750A4"
        }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.tab(name: String): SemanticsNodeInteraction =
    onNode(hasText(name) and hasInputRole(Role.Tab))

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.editableText(tag: String): String =
    onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text
