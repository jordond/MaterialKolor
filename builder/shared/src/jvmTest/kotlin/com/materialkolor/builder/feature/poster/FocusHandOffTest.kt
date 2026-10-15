package com.materialkolor.builder.feature.poster

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Seed = Argb(0x6750A4)

private val Teal = Argb(0x00897B)

private val Plain = ThemeDocument(seed = Seed)

private val TwoPins = mapOf(
    Role.OnPrimary to RolePin(light = Argb(0xFFFFFF), dark = Argb(0x222222)),
    Role.Surface to RolePin(light = Argb(0xFAFAFA)),
)

/**
 * A button that removes itself hands a keyboard user's focus to the control named in the poster's
 * a11y rules before it goes, and leaves a pointer's focus alone.
 */
@OptIn(ExperimentalTestApi::class)
class FocusHandOffTest {
    @Test
    fun keyColorClear_handsFocusToThatRowsPick() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(keyColors = KeyColors(secondary = Teal)))
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            pressWithKeyboard(onNodeWithContentDescription("Clear Secondary"))

            harness.document.keyColors shouldBe KeyColors()
            onNodeWithContentDescription("Pick Secondary").assertIsFocused()
        }

    @Test
    fun keyColorClear_byPointer_leavesTheFocusAlone() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(keyColors = KeyColors(secondary = Teal)))
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            onNodeWithContentDescription("Clear Secondary").performClick()
            waitForIdle()

            harness.document.keyColors shouldBe KeyColors()
            onNodeWithContentDescription("Pick Secondary").assertIsNotFocused()
        }

    @Test
    fun useAsSeed_handsFocusToPrimarysPick() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(keyColors = KeyColors(primary = Teal)))
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            pressWithKeyboard(onNodeWithText("Use as seed"))

            harness.document.seed shouldBe Teal
            onNodeWithContentDescription("Pick Primary").assertIsFocused()
        }

    @Test
    fun resetAll_handsFocusToPrimarysPick() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(keyColors = KeyColors(tertiary = Teal)))
            showSection(harness) { context, dispatcher -> KeyColorRows(context, dispatcher) }

            pressWithKeyboard(onNodeWithText("Reset all"))

            harness.document.keyColors shouldBe KeyColors()
            onNodeWithContentDescription("Pick Primary").assertIsFocused()
        }

    @Test
    fun pinClear_handsFocusToTheNextPinsClear() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(pins = TwoPins))
            showSection(harness) { context, dispatcher -> PinnedRoles(context, dispatcher) }

            pressWithKeyboard(onNodeWithContentDescription("Clear onPrimary, Light"))

            harness.document.pins[Role.OnPrimary] shouldBe RolePin(dark = Argb(0x222222))
            onNodeWithContentDescription("Clear onPrimary, Dark").assertIsFocused()
        }

    @Test
    fun pinClear_lastInTheList_handsFocusToThePreviousPinsClear() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(pins = TwoPins))
            showSection(harness) { context, dispatcher -> PinnedRoles(context, dispatcher) }

            pressWithKeyboard(onNodeWithContentDescription("Clear surface, Light"))

            harness.document.pins.keys shouldBe setOf(Role.OnPrimary)
            onNodeWithContentDescription("Clear onPrimary, Dark").assertIsFocused()
        }

    @Test
    fun pinClear_theLastPin_handsFocusToTheLastKeyColorsPick() =
        runComposeUiTest {
            val pins = mapOf(Role.OnPrimary to RolePin(light = Argb(0xFFFFFF)))
            val harness = PosterHarness(Plain.copy(pins = pins))
            showSection(harness) { context, dispatcher -> FineTuneContent(context, dispatcher) }

            pressWithKeyboard(onNodeWithContentDescription("Clear onPrimary, Light"))

            harness.document.pins shouldBe emptyMap()
            onNodeWithContentDescription("Pick Neutral variant").assertIsFocused()
        }

    @Test
    fun pinsClearAll_handsFocusToTheLastKeyColorsPick() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(pins = TwoPins))
            showSection(harness) { context, dispatcher -> FineTuneContent(context, dispatcher) }

            pressWithKeyboard(onNodeWithText("Clear all"))

            harness.document.pins shouldBe emptyMap()
            onNodeWithContentDescription("Pick Neutral variant").assertIsFocused()
        }

    @Test
    fun cmfDeriveFromSeed_handsFocusToTheCmfField() =
        runComposeUiTest {
            val harness = PosterHarness(Plain.copy(style = Style.Cmf, cmfTertiarySeed = Teal))
            showSection(harness) { context, dispatcher -> StyleChipsSection(context, dispatcher) }

            pressWithKeyboard(onNodeWithText("Derive from seed"))

            harness.document.cmfTertiarySeed shouldBe null
            onNode(hasSetTextAction()).assertIsFocused()
        }

    @Test
    fun toneReset_handsFocusToItsLightSlider() =
        runComposeUiTest {
            val tones = mapOf(CustomSlot.BorderStrong to CustomTone(light = 30))
            val harness = PosterHarness(Plain.copy(library = Library.Custom, customTones = tones))
            showSection(harness) { context, dispatcher -> CustomToneTable(context, dispatcher) }

            pressWithKeyboard(onNodeWithText("Reset borderStrong"))

            harness.document.customTones shouldBe emptyMap()
            onNodeWithContentDescription("borderStrong light tone").assertIsFocused()
        }
}

/**
 * Focuses [node] and presses Enter on it, the way a keyboard user does.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pressWithKeyboard(node: SemanticsNodeInteraction) {
    node.requestFocus()
    node.performKeyInput { pressKey(Key.Enter) }
    waitForIdle()
}
