package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.FineTuneRow
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.test.Test

/**
 * What the Trips app says once the Past filter is on, there being no past trips.
 */
private const val NO_PAST_TRIPS = "No past trips yet"

/**
 * What the Trips app's floating action button reads as.
 */
private const val NEW_TRIP = "New trip"

/**
 * The first action of a pinned card, so its presence means a card is pinned.
 */
private const val PIN_ROLE = "Pin this role"

/**
 * Anything drawn on the Inspect card.
 */
private val OnCard: SemanticsMatcher = hasAnyAncestor(hasTestTag(INSPECT_CARD_TAG))

@OptIn(ExperimentalTestApi::class)
class InspectLayerTest {
    @Test
    fun hover_overTheTripsFab_namesItsRolesInTheModeOfTheSideUnderThePointer() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = inspecting()
            val result = ThemeResolver().resolve(host.state.document)
            setContent { Canvas(host) }
            waitForIdle()

            onNode(SplitHandle).performSemanticsAction(SemanticsActions.SetProgress) { move -> move(1f) }
            waitForIdle()
            onNodeWithContentDescription(NEW_TRIP).performMouseInput { moveTo(center) }
            waitForIdle()
            assertFabCard(result, isDark = false)

            onNode(SplitHandle).performSemanticsAction(SemanticsActions.SetProgress) { move -> move(0f) }
            waitForIdle()
            onNodeWithContentDescription(NEW_TRIP).performMouseInput { moveTo(center + Offset(1f, 0f)) }
            waitForIdle()
            assertFabCard(result, isDark = true)
        }

    @Test
    fun click_withInspectOn_pinsTheCardAndLeavesTheAppAlone() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = inspecting()
            setContent { Canvas(host) }
            waitForIdle()

            onNodeWithText("Past").performClick()
            waitForIdle()
            onNodeWithText(NO_PAST_TRIPS).assertDoesNotExist()
            onNode(OnCard and hasText(PIN_ROLE)).assertExists()
            onNode(OnCard and hasText("outlineVariant")).assertExists()

            onNodeWithText("Explore").performClick()
            waitForIdle()
            onNode(OnCard and hasText("secondary")).assertExists()
            onNode(OnCard and hasText("outlineVariant")).assertDoesNotExist()
            host.state.inspect shouldBe true
        }

    @Test
    fun pinnedCard_actionsOnTheFab_pinTheRoleJumpToKeyColorsAndShowTheRamp() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = inspecting()
            val result = ThemeResolver().resolve(host.state.document)
            setContent { Canvas(host) }
            waitForIdle()
            onNodeWithContentDescription(NEW_TRIP).performClick()
            waitForIdle()

            onNode(OnCard and hasText(PIN_ROLE)).assertIsEnabled().performClick()
            waitForIdle()
            val argb = result.roles[Role.PrimaryContainer, false].argb
            host.actions.last() shouldBe
                WorkspaceAction.Edit(
                    DocumentChange.SetPin(Role.PrimaryContainer, PinMode.Light, argb),
                    EditPhase.Discrete,
                )

            onNode(OnCard and hasText("Jump to key color")).performClick()
            waitForIdle()
            host.actions.takeLast(2) shouldBe listOf(
                WorkspaceAction.SetPosterCollapsed(collapsed = false),
                WorkspaceAction.SetFineTuneRowOpen(FineTuneRow.CoreColors, open = true),
            )

            onNode(OnCard and hasText("Show on ramp")).performClick()
            waitForIdle()
            host.actions.last() shouldBe
                WorkspaceAction.ShowOnRamp(RampTarget.OfRole(Role.PrimaryContainer, isDark = false))
            host.state.inspect shouldBe true
        }

    @Test
    fun pinnedCard_underFluent_hasPinThisRoleDisabled() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = inspecting()
            val fluent = host.state.document.copy(library = Library.Fluent)
            host.state = host.state.copy(capabilities = capabilitiesOf(fluent))
            setContent { Canvas(host) }
            waitForIdle()

            onNodeWithContentDescription(NEW_TRIP).performClick()
            waitForIdle()

            onNode(OnCard and hasText(PIN_ROLE)).assertIsNotEnabled()
            onNode(OnCard and hasText("Show on ramp")).assertIsEnabled()
        }

    @Test
    fun splitHandle_withInspectOn_stillDragsAndPinsNothing() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = inspecting()
            setContent { Canvas(host) }
            waitForIdle()

            val handle = onNode(SplitHandle)
            handle.performTouchInput { down(center) }
            repeat(DRAG_STEPS) {
                handle.performTouchInput { moveBy(Offset(-40f, 0f)) }
                mainClock.advanceTimeByFrame()
            }
            handle.performTouchInput { up() }
            waitForIdle()

            val fraction = handleFraction()
            fraction shouldBeLessThan 0.45f
            host.savedFractions shouldBe listOf(fraction)
            onNodeWithTag(INSPECT_CARD_TAG).assertDoesNotExist()
        }

    @Test
    fun keyboardFocus_withInspectOn_showsTheCardAndEscUnpinsThenLeaves() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = inspecting()
            setContent { Canvas(host) }
            waitForIdle()

            onNodeWithText("App").requestFocus()
            val fab = hasContentDescription(NEW_TRIP) and isFocused()
            var presses = 0
            while (onAllNodes(fab).fetchSemanticsNodes().isEmpty()) {
                check(presses++ < MAX_TABS) { "Tab never reached the Trips app's floating action button" }
                onRoot().performKeyInput { pressKey(Key.Tab) }
                waitForIdle()
            }
            onNode(OnCard and hasText("primaryContainer")).assertExists()
            onNode(OnCard and hasText(PIN_ROLE)).assertDoesNotExist()

            onNodeWithContentDescription(NEW_TRIP).performClick()
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertExists()

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertDoesNotExist()
            host.state.inspect shouldBe true

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            host.actions shouldContain WorkspaceAction.SetInspect(on = false)
            host.state.inspect shouldBe false
            onNodeWithTag(INSPECT_CARD_TAG).assertDoesNotExist()
        }

    @Test
    fun esc_afterACardActionTookFocus_unpinsThenLeaves() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = inspecting()
            setContent { Canvas(host) }
            waitForIdle()
            onNodeWithContentDescription(NEW_TRIP).performMouseInput { click() }
            waitForIdle()

            onNode(OnCard and hasText(PIN_ROLE)).performMouseInput { click() }
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE) and isFocused()).assertExists()

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onNode(OnCard and hasText(PIN_ROLE)).assertDoesNotExist()
            host.state.inspect shouldBe true

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            host.actions.last() shouldBe WorkspaceAction.SetInspect(on = false)
            host.state.inspect shouldBe false
        }

    /**
     * A canvas host at the default view with Inspect on.
     */
    private fun inspecting(): CanvasHost =
        CanvasHost(view = ProjectViewState()).also { host -> host.state = host.state.copy(inspect = true) }

    /**
     * The card shows the Fab's two roles in the given mode, their contrast and the mode's name.
     */
    private fun ComposeUiTest.assertFabCard(
        result: ThemeResult,
        isDark: Boolean,
    ) {
        for (role in listOf(Role.PrimaryContainer, Role.OnPrimaryContainer)) {
            val entry = result.roles[role, isDark]
            val name = role.name.replaceFirstChar { char -> char.lowercaseChar() }
            onNode(
                OnCard and hasText(name) and hasText(entry.argb.toHex()) and hasText("tone ${entry.tone.roundToInt()}"),
            ).assertExists()
        }
        val pair =
            ContrastPair(
                ColorRef.OfRole(Role.OnPrimaryContainer),
                ColorRef.OfRole(Role.PrimaryContainer),
                PairKind.Text,
            )
        val row = result.audit.rows.single { row -> row.pair == pair && row.isDark == isDark }
        val tenths = floor(row.ratio * 10).toInt()
        onNode(OnCard and hasText("${tenths / 10}.${tenths % 10} to 1")).assertExists()
        onNode(OnCard and hasText(if (isDark) "Dark mode" else "Light mode")).assertExists()
    }
}

/**
 * How many frames a stepped drag of the handle takes.
 */
private const val DRAG_STEPS = 6

/**
 * How many Tab presses it may take to reach the Trips app from the canvas tabs.
 */
private const val MAX_TABS = 12
