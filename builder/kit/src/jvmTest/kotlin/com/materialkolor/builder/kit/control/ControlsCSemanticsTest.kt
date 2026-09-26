package com.materialkolor.builder.kit.control

import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.SkinTestTheme
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.PanelEdge
import com.materialkolor.builder.kit.skin.headless.panelEnter
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ControlsCSemanticsTest {
    @Test
    fun dialog_openedFromTrigger_trapsFocusClosesOnEscAndReturnsFocus() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(false)
            val trigger = FocusRequester()
            setContent {
                ControlsHarness(skin) {
                    Column {
                        Box(
                            Modifier
                                .testTag("trigger")
                                .size(40.dp)
                                .focusRequester(trigger)
                                .focusable(),
                        )
                        Box(Modifier.testTag("after").size(40.dp).focusable())
                        BuilderDialog(
                            visible = open,
                            onDismissRequest = { open = false },
                            title = "Delete project",
                            returnFocusTo = trigger,
                            actions = {
                                OverlayTestButton("cancel")
                                OverlayTestButton("confirm")
                            },
                        ) { BuilderText("This removes the project from this browser.") }
                    }
                }
            }
            onNodeWithTag("trigger").requestFocus()
            open = true
            waitForIdle()

            onNode(hasOverlayPaneTitle("Delete project")).assertExists()
            onNodeWithTag("cancel").assertIsFocused()
            onNodeWithTag("cancel").performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            onNodeWithTag("confirm").assertIsFocused()
            onNodeWithTag("confirm").performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            onNodeWithTag("cancel").assertIsFocused()
            onNodeWithTag("after").assertIsNotFocused()

            onNodeWithTag("cancel").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            open shouldBe false
            onNode(hasOverlayPaneTitle("Delete project")).assertDoesNotExist()
            onNodeWithTag("trigger").assertIsFocused()
        }

    @Test
    fun sidePanel_open_isNamedStartsOnCloseAndClosesOnEsc() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(true)
            setContent {
                ControlsHarness(skin) {
                    BuilderSidePanel(visible = open, onDismissRequest = { open = false }, title = "Projects") {
                        BuilderText("Sunset")
                    }
                }
            }
            waitForIdle()
            onNode(hasOverlayPaneTitle("Projects")).assertExists()
            val close = onNode(hasContentDescription("Close") and hasRole(Role.Button))
            close.assertIsFocused()
            close.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            open shouldBe false
            onNode(hasOverlayPaneTitle("Projects")).assertDoesNotExist()
        }

    @Test
    fun sheet_closeButton_asksToClose() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(true)
            setContent {
                ControlsHarness(skin) {
                    BuilderSheet(open, { open = false }, "Export", SheetPresentation.EndPanel) { BuilderText("Kotlin") }
                }
            }
            waitForIdle()
            onNode(hasOverlayPaneTitle("Export")).assertExists()
            onNode(hasContentDescription("Close") and hasRole(Role.Button)).performClick()
            waitForIdle()
            open shouldBe false
            onNodeWithText("Kotlin").assertDoesNotExist()
        }

    @Test
    fun overlayMotion_reducedMotion_keepsOnlyTheFade() =
        runComposeUiTest {
            val onlyFades = mutableMapOf<String, Boolean>()
            setContent {
                val result = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4))) }
                for (reduced in listOf(false, true)) {
                    SkinTestTheme(
                        Skin(SkinLibrary.Custom, expressive = false),
                        result,
                        false,
                        reducedMotion = reduced,
                    ) {
                        val motion = LocalBuilderMotion.current
                        onlyFades["popover reduced=$reduced"] = popoverEnter() == fadeIn(motion.popover())
                        onlyFades["panel reduced=$reduced"] = panelEnter(PanelEdge.End) == fadeIn(motion.panelEnter())
                    }
                }
            }
            waitForIdle()
            onlyFades shouldBe mapOf(
                "popover reduced=false" to false,
                "panel reduced=false" to false,
                "popover reduced=true" to true,
                "panel reduced=true" to true,
            )
        }

    @Test
    fun sheetPresentation_width_isAnEndPanelFrom840() {
        SheetPresentation.of(LayoutInfo(839.dp, 900.dp)) shouldBe SheetPresentation.FullScreen
        SheetPresentation.of(LayoutInfo(840.dp, 900.dp)) shouldBe SheetPresentation.EndPanel
        SheetPresentation.of(LayoutInfo(1600.dp, 900.dp)) shouldBe SheetPresentation.EndPanel
    }

    @Test
    fun bottomSheet_keyboardAndDrag_snapToEveryDetent() =
        forEachSkin { _, skin ->
            lateinit var state: BottomSheetState
            setContent {
                ControlsHarness(skin) {
                    state = rememberBottomSheetState()
                    BuilderBottomSheet(state, label = "Poster") { BuilderText("#6750A4") }
                }
            }
            val handle = onNode(hasContentDescription("Poster") and hasRole(Role.Button))
            state.detent shouldBe BottomSheetDetent.Peek
            handle.assert(hasStateDescription("Peek"))

            handle.requestFocus()
            for ((key, expected) in KeySteps) {
                handle.performKeyInput { pressKey(key) }
                waitForIdle()
                state.detent shouldBe expected
                handle.assert(hasStateDescription(expected.name))
            }

            val anchors = state.draggable.anchors
            for ((from, to) in DragSteps) {
                state.detent shouldBe from
                val distance = anchors.positionOf(to) - anchors.positionOf(from)
                handle.performTouchInput {
                    down(center)
                    moveBy(Offset(0f, distance))
                    advanceEventTime(200)
                    up()
                }
                waitForIdle()
                state.detent shouldBe to
            }
        }

    @Test
    fun toastHost_fiveToasts_keepsTheNewestThreeInOnePoliteRegion() =
        forEachSkin { _, skin ->
            val toasts = BuilderToastHostState()
            val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
            setContent { ControlsHarness(skin) { BuilderToastHost(toasts) } }
            waitForIdle()
            onAllNodes(polite).assertCountEquals(1)
            repeat(5) { index -> toasts.show("Saved $index", duration = ToastDuration.Indefinite) }
            waitForIdle()
            toasts.toasts shouldHaveSize BuilderToastHostState.MaxToasts
            onAllNodes(polite).assertCountEquals(1)
            onNodeWithText("Saved 1").assertDoesNotExist()
            for (index in 2..4) onNode(polite and hasAnyDescendant(hasText("Saved $index"))).assertExists()
        }

    @Test
    fun toastHost_undoAction_runsAndClosesTheToast() =
        forEachSkin { _, skin ->
            val toasts = BuilderToastHostState()
            var undone = false
            setContent { ControlsHarness(skin) { BuilderToastHost(toasts) } }
            toasts.show("Pin removed", actionLabel = "Undo", duration = ToastDuration.Indefinite) { undone = true }
            waitForIdle()
            onNode(hasText("Undo") and hasRole(Role.Button)).performClick()
            waitForIdle()
            undone shouldBe true
            toasts.toasts shouldHaveSize 0
            onNodeWithText("Pin removed").assertDoesNotExist()
        }

    @Test
    fun toastHost_shortToast_goesAfterItsTimeout() =
        runComposeUiTest {
            val toasts = BuilderToastHostState()
            setContent { ControlsHarness(Skin(SkinLibrary.Material3, expressive = false)) { BuilderToastHost(toasts) } }
            waitForIdle()
            mainClock.autoAdvance = false
            toasts.show("Saved")
            mainClock.advanceTimeBy(1_000)
            onNodeWithText("Saved").assertExists()
            mainClock.advanceTimeBy(checkNotNull(ToastDuration.Short.millis))
            onNodeWithText("Saved").assertDoesNotExist()
        }

    @Test
    fun tooltip_focusAndHover_showTheLabel() =
        forEachSkin { _, skin ->
            setContent {
                ControlsHarness(skin) {
                    Column {
                        BuilderTooltip("Undo last edit") { Box(Modifier.testTag("anchor").size(40.dp).focusable()) }
                        Box(Modifier.testTag("other").size(40.dp).focusable())
                    }
                }
            }
            onNodeWithText("Undo last edit").assertDoesNotExist()
            onNodeWithTag("anchor", useUnmergedTree = true).requestFocus()
            waitForIdle()
            onNodeWithText("Undo last edit").assertExists()
            onNodeWithTag("other").requestFocus()
            waitForIdle()
            onNodeWithText("Undo last edit").assertDoesNotExist()
            onNodeWithTag("anchor", useUnmergedTree = true).performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(OverlayMetrics.tooltipDelayMillis * 2)
            waitForIdle()
            onNodeWithText("Undo last edit").assertExists()
        }

    @Test
    fun menu_open_rowsAreButtonsAndChoosingOneCloses() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(true)
            var chosen: String? = null
            setContent {
                ControlsHarness(skin) {
                    BuilderMenu(
                        expanded = open,
                        onDismissRequest = { open = false },
                        items = listOf(
                            BuilderMenuItem("Duplicate", { chosen = "Duplicate" }, IconId.Copy),
                            BuilderMenuItem("Delete", { chosen = "Delete" }, IconId.Trash, Emphasis.Danger),
                            BuilderMenuItem("Archive", { chosen = "Archive" }, enabled = false),
                        ),
                    ) { BuilderText("Project") }
                }
            }
            waitForIdle()
            onNode(hasText("Duplicate") and hasRole(Role.Button)).assertIsEnabled()
            onNode(hasText("Delete") and hasRole(Role.Button)).assertIsEnabled()
            onNode(hasText("Archive") and hasRole(Role.Button)).assertIsNotEnabled()
            onNode(hasText("Delete") and hasRole(Role.Button)).performClick()
            waitForIdle()
            chosen shouldBe "Delete"
            open shouldBe false
            onNodeWithText("Duplicate").assertDoesNotExist()
        }

    @Test
    fun select_choosingAnOption_readsAsSelectedAndReportsIt() =
        forEachSkin { _, skin ->
            var style by mutableStateOf("Tonal spot")
            setContent {
                ControlsHarness(skin) {
                    Column {
                        BuilderSelect("Style", listOf("Tonal spot", "Vibrant", "Expressive"), style, { style = it })
                        BuilderSelect("Spec", listOf("2021"), "2021", {}, enabled = false)
                    }
                }
            }
            val field = onNode(hasRole(Role.DropdownList) and hasStateDescription("Tonal spot"))
            field.assertIsEnabled()
            onNode(hasRole(Role.DropdownList) and hasStateDescription("2021")).assertIsNotEnabled()

            field.performClick()
            waitForIdle()
            onNode(hasText("Tonal spot") and hasRole(Role.RadioButton)).assertIsSelected()
            onNode(hasText("Vibrant") and hasRole(Role.RadioButton)).assertIsNotSelected()
            onNode(hasText("Vibrant") and hasRole(Role.RadioButton)).performClick()
            waitForIdle()
            style shouldBe "Vibrant"
            onAllNodesWithText("Expressive").assertCountEquals(0)
            onNode(hasRole(Role.DropdownList) and hasStateDescription("Vibrant")).assertExists()
        }

    @Test
    fun scrollArea_longContent_scrollsToTheEnd() =
        forEachSkin { _, skin ->
            val scroll = ScrollState(0)
            setContent {
                ControlsHarness(skin) {
                    BuilderScrollArea(Modifier.height(160.dp), state = scroll) {
                        repeat(40) { index -> BuilderText("Row $index") }
                    }
                }
            }
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
                .performScrollToNode(hasText("Row 39"))
            waitForIdle()
            (scroll.value > 0) shouldBe true
            onNodeWithText("Row 39").assertExists()
        }
}

private val KeySteps: List<Pair<Key, BottomSheetDetent>> = listOf(
    Key.DirectionUp to BottomSheetDetent.Half,
    Key.DirectionUp to BottomSheetDetent.Full,
    Key.DirectionDown to BottomSheetDetent.Half,
    Key.MoveEnd to BottomSheetDetent.Peek,
    Key.MoveHome to BottomSheetDetent.Full,
    Key.PageDown to BottomSheetDetent.Half,
    Key.PageDown to BottomSheetDetent.Peek,
)

private val DragSteps: List<Pair<BottomSheetDetent, BottomSheetDetent>> = listOf(
    BottomSheetDetent.Peek to BottomSheetDetent.Half,
    BottomSheetDetent.Half to BottomSheetDetent.Full,
    BottomSheetDetent.Full to BottomSheetDetent.Peek,
)
