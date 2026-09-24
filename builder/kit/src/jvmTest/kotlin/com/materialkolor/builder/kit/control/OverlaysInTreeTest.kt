package com.materialkolor.builder.kit.control

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.DropdownPositionProvider
import com.materialkolor.builder.kit.headless.HeadlessDropdown
import com.materialkolor.builder.kit.headless.LocalOverlayHost
import com.materialkolor.builder.kit.headless.OverlayHostState
import com.materialkolor.builder.kit.headless.OverlayKind
import com.materialkolor.builder.kit.headless.TooltipPositionProvider
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.shell.PosterSurface
import com.materialkolor.builder.kit.shell.ShellPosterColors
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

private val MenuItems: List<BuilderMenuItem> = listOf(
    BuilderMenuItem("Duplicate", {}, IconId.Copy),
    BuilderMenuItem("Delete", {}, IconId.Trash, Emphasis.Danger),
    BuilderMenuItem("Archive", {}, enabled = false),
)

private val PanelCycle: List<SemanticsMatcher> = listOf(
    hasContentDescription("Close") and hasRole(Role.Button),
    hasTestTag("first"),
    hasTestTag("second"),
)

private val DialogCycle: List<SemanticsMatcher> = listOf(hasTestTag("cancel"), hasTestTag("confirm"))

@OptIn(ExperimentalTestApi::class)
class OverlaysInTreeTest {
    @Test
    fun dialog_eachWay_trapsTabClosesOnEscAndReturnsFocus() =
        hostEachWay { skin, inTree ->
            hostCheckModal(skin, inTree, "Delete project", DialogCycle) { open, close, trigger ->
                BuilderDialog(
                    visible = open,
                    onDismissRequest = close,
                    title = "Delete project",
                    returnFocusTo = trigger,
                    actions = {
                        OverlayTestButton("cancel")
                        OverlayTestButton("confirm")
                    },
                ) { BuilderText("This removes the project from this browser.") }
            }
        }

    @Test
    fun sheet_eachWay_trapsTabClosesOnEscAndReturnsFocus() =
        hostEachWay { skin, inTree ->
            hostCheckModal(skin, inTree, "Export", PanelCycle) { open, close, trigger ->
                BuilderSheet(open, close, "Export", SheetPresentation.EndPanel, returnFocusTo = trigger) {
                    OverlayTestButton("first")
                    OverlayTestButton("second")
                }
            }
        }

    @Test
    fun sidePanel_eachWay_trapsTabClosesOnEscAndReturnsFocus() =
        hostEachWay { skin, inTree ->
            hostCheckModal(skin, inTree, "Projects", PanelCycle) { open, close, trigger ->
                BuilderSidePanel(open, close, "Projects", returnFocusTo = trigger) {
                    OverlayTestButton("first")
                    OverlayTestButton("second")
                }
            }
        }

    @Test
    fun dialogAndSheet_inTree_closeOnAClickOnTheVeil() =
        forEachSkin { _, skin ->
            var dialog by mutableStateOf(true)
            var sheet by mutableStateOf(false)
            setContent {
                HostOverlays(skin, inTree = true) {
                    BuilderDialog(dialog, { dialog = false }, "Rename") { BuilderText("Sunset") }
                    BuilderSheet(sheet, { sheet = false }, "Export", SheetPresentation.EndPanel) {
                        BuilderText("Kotlin")
                    }
                }
            }
            waitForIdle()
            onRoot().performTouchInput { click(Offset(2f, 2f)) }
            waitForIdle()
            dialog shouldBe false
            sheet = true
            waitForIdle()
            onRoot().performTouchInput { click(Offset(2f, 2f)) }
            waitForIdle()
            sheet shouldBe false
            onNodeWithText("Kotlin").assertDoesNotExist()
        }

    @Test
    fun menu_eachWay_opensOnTheFirstRowAndChoosingOneCloses() =
        hostEachWay { skin, inTree ->
            // Material3 in windows keeps its own DropdownMenu, which leaves focus on the popup itself.
            val firstRowFocused = inTree || skin.library != Library.Material3
            var open by mutableStateOf(true)
            var chosen: String? = null
            setContent {
                HostOverlays(skin, inTree) {
                    val items = listOf(BuilderMenuItem("Duplicate", { chosen = "Duplicate" }))
                    BuilderMenu(open, { open = false }, items) { BuilderText("Project") }
                }
            }
            waitForIdle()
            val row = onNode(hasText("Duplicate") and hasRole(Role.Button))
            if (firstRowFocused) row.assertIsFocused()
            row.performClick()
            waitForIdle()
            chosen shouldBe "Duplicate"
            open shouldBe false
            onNodeWithText("Duplicate").assertDoesNotExist()
        }

    @Test
    fun menu_inTree_trapsTabClosesOnEscAndOutsidePressAndReturnsFocus() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(false)
            setContent {
                HostOverlays(skin, inTree = true) {
                    Column {
                        Box(Modifier.testTag("page").size(40.dp).focusable())
                        BuilderMenu(open, { open = false }, MenuItems) {
                            Box(Modifier.testTag("trigger").size(40.dp).focusable())
                        }
                    }
                }
            }
            val duplicate = onNode(hasText("Duplicate") and hasRole(Role.Button))
            val delete = onNode(hasText("Delete") and hasRole(Role.Button))
            onNodeWithTag("trigger").requestFocus()
            open = true
            waitForIdle()
            onNodeWithTag("page").assertExists()
            duplicate.assertIsFocused()
            duplicate.performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            delete.assertIsFocused()
            delete.performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            duplicate.assertIsFocused()
            duplicate.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            open shouldBe false
            onNodeWithTag("trigger").assertIsFocused()

            open = true
            waitForIdle()
            onRoot().performTouchInput { click(Offset(width - 2f, height - 2f)) }
            waitForIdle()
            open shouldBe false
            onNodeWithText("Duplicate").assertDoesNotExist()
            onNodeWithTag("trigger").assertIsFocused()
        }

    @Test
    fun select_inTree_opensOnTheChosenOptionClosesOnEscAndReturnsFocus() =
        forEachSkin { _, skin ->
            var style by mutableStateOf("Vibrant")
            setContent {
                HostOverlays(skin, inTree = true) {
                    BuilderSelect("Style", listOf("Tonal spot", "Vibrant", "Expressive"), style, { style = it })
                }
            }
            val field = onNode(hasRole(Role.DropdownList))
            val chosen = onNode(hasText("Vibrant") and hasRole(Role.RadioButton))
            field.performClick()
            waitForIdle()
            chosen.assertIsFocused()
            chosen.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            chosen.assertDoesNotExist()
            field.assertIsFocused()
            field.performClick()
            waitForIdle()
            onNode(hasText("Expressive") and hasRole(Role.RadioButton)).performClick()
            waitForIdle()
            style shouldBe "Expressive"
            onNode(hasRole(Role.DropdownList) and hasStateDescription("Expressive")).assertIsFocused()
        }

    @Test
    fun tooltip_inTree_drawsInTheHostOutOfTheSemanticsTree() =
        forEachSkin { _, skin ->
            lateinit var host: OverlayHostState
            setContent {
                HostOverlays(skin, inTree = true) {
                    host = checkNotNull(LocalOverlayHost.current)
                    Column {
                        BuilderTooltip("Undo last edit") { Box(Modifier.testTag("anchor").size(40.dp).focusable()) }
                        Box(Modifier.testTag("other").size(40.dp).focusable())
                    }
                }
            }
            onNodeWithTag("anchor", useUnmergedTree = true).requestFocus()
            waitForIdle()
            host.layers.map { it.kind } shouldBe listOf(OverlayKind.Passive)
            onNodeWithText("Undo last edit").assertDoesNotExist()
            onNodeWithTag("other").requestFocus()
            waitForIdle()
            host.layers.size shouldBe 0
        }

    @Test
    fun menuInsideDialog_inTree_stacksAboveItAndEscClosesOnlyTheMenu() =
        forEachSkin { _, skin ->
            lateinit var host: OverlayHostState
            var menu by mutableStateOf(false)
            var chosen: String? = null
            setContent {
                HostOverlays(skin, inTree = true) {
                    host = checkNotNull(LocalOverlayHost.current)
                    BuilderDialog(visible = true, onDismissRequest = {}, title = "Theme") {
                        val items = listOf(BuilderMenuItem("Rename", { chosen = "Rename" }))
                        BuilderMenu(menu, { menu = false }, items) { OverlayTestButton("more") }
                    }
                }
            }
            menu = true
            waitForIdle()
            host.layers.map { it.kind } shouldBe listOf(OverlayKind.Modal, OverlayKind.Popover)
            val rename = onNode(hasText("Rename") and hasRole(Role.Button))
            rename.assertIsFocused()
            rename.performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            menu shouldBe false
            onNode(hasOverlayPaneTitle("Theme")).assertExists()
            onNodeWithTag("more").assertIsFocused()
            menu = true
            waitForIdle()
            rename.performClick()
            waitForIdle()
            chosen shouldBe "Rename"
            host.layers.map { it.kind } shouldBe listOf(OverlayKind.Modal)
        }

    @Test
    fun overlay_inTree_keepsTheTokensDensityAndLayoutDirectionOfWhereItOpened() =
        forEachSkin { _, skin ->
            var changed: HostLocals? = null
            var poster: HostLocals? = null
            val seen = mutableMapOf<String, HostLocals>()
            setContent {
                HostOverlays(skin, inTree = true) {
                    val style = overlayStyle(skin.library)
                    CompositionLocalProvider(
                        LocalBuilderTokens provides LocalBuilderTokens.current.copy(canvas = Color.Magenta),
                        LocalLayoutDirection provides LayoutDirection.Rtl,
                        LocalDensity provides Density(density = 3f, fontScale = 1.5f),
                    ) {
                        changed = hostLocals()
                        BuilderDialog(visible = true, onDismissRequest = {}, title = "Theme") {
                            seen["dialog"] = hostLocals()
                        }
                        BuilderSheet(true, {}, "Export", SheetPresentation.FullScreen) {
                            seen["sheet"] = hostLocals()
                        }
                        Box { HeadlessDropdown(true, {}, style) { _ -> seen["menu"] = hostLocals() } }
                    }
                    PosterSurface(ShellPosterColors) {
                        poster = hostLocals()
                        Box { HeadlessDropdown(true, {}, style) { _ -> seen["poster"] = hostLocals() } }
                    }
                }
            }
            waitForIdle()
            onNode(hasOverlayPaneTitle("Export")).assertExists()
            val expected = checkNotNull(changed)
            seen shouldBe mapOf(
                "dialog" to expected,
                "sheet" to expected,
                "menu" to expected,
                "poster" to checkNotNull(poster),
            )
        }

    @Test
    fun dialogWithoutActions_inTree_holdsFocusThroughTabAndClosesOnEsc() =
        forEachSkin { _, skin ->
            hostCheckHeldFocus(skin) { open, close, focus, trigger ->
                trigger()
                BuilderDialog(open, close, "Rename", returnFocusTo = focus) { BuilderText("Sunset") }
            }
        }

    @Test
    fun menuOfDisabledRows_inTree_holdsFocusThroughTabAndClosesOnEsc() =
        forEachSkin { _, skin ->
            hostCheckHeldFocus(skin) { open, close, _, trigger ->
                BuilderMenu(open, close, listOf(BuilderMenuItem("Archive", {}, enabled = false))) { trigger() }
            }
        }

    @Test
    fun menuRowThatFocusesAPageField_inTree_leavesFocusOnTheField() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(false)
            val name = FocusRequester()
            setContent {
                HostOverlays(skin, inTree = true) {
                    Column {
                        Box(
                            Modifier
                                .testTag("name")
                                .size(40.dp)
                                .focusRequester(name)
                                .focusable(),
                        )
                        val items = listOf(BuilderMenuItem("Rename", { name.requestFocus() }))
                        BuilderMenu(open, { open = false }, items) {
                            Box(Modifier.testTag("trigger").size(40.dp).focusable())
                        }
                    }
                }
            }
            onNodeWithTag("trigger").requestFocus()
            open = true
            waitForIdle()
            onNode(hasText("Rename") and hasRole(Role.Button)).performClick()
            waitForIdle()
            open shouldBe false
            onNodeWithText("Rename").assertDoesNotExist()
            onNodeWithTag("name").assertIsFocused()
        }

    @Test
    fun dialog_inTree_callerLeavingMidExit_emptiesTheHostAndBringsThePageBack() =
        forEachSkin { _, skin ->
            lateinit var host: OverlayHostState
            var caller by mutableStateOf(true)
            var open by mutableStateOf(true)
            setContent {
                HostOverlays(skin, inTree = true) {
                    host = checkNotNull(LocalOverlayHost.current)
                    Column {
                        Box(Modifier.testTag("page").size(40.dp).focusable())
                        if (caller) BuilderDialog(open, { open = false }, "Rename") { BuilderText("Sunset") }
                    }
                }
            }
            waitForIdle()
            onNodeWithTag("page").assertDoesNotExist()
            mainClock.autoAdvance = false
            open = false
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            host.layers.size shouldBe 1
            caller = false
            mainClock.advanceTimeByFrame()
            host.layers.size shouldBe 0
            onNodeWithTag("page").assertExists()
        }

    @Test
    fun toast_inTree_drawsOverAnOpenDialogInItsOwnRegion() =
        forEachSkin { _, skin ->
            lateinit var host: OverlayHostState
            val toasts = BuilderToastHostState()
            setContent {
                HostOverlays(skin, inTree = true) {
                    host = checkNotNull(LocalOverlayHost.current)
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.align(Alignment.BottomStart).size(width = 320.dp, height = 240.dp)) {
                            BuilderToastHost(toasts)
                        }
                        BuilderDialog(visible = true, onDismissRequest = {}, title = "Export") {
                            BuilderText("Kotlin")
                        }
                    }
                }
            }
            waitForIdle()
            toasts.show("Copied", duration = ToastDuration.Indefinite)
            waitForIdle()
            host.top.size shouldBe 1
            host.layers.map { it.kind } shouldBe listOf(OverlayKind.Modal)
            val root = onRoot().getBoundsInRoot()
            val toast = onNodeWithText("Copied").assertExists().getBoundsInRoot()
            (toast.right <= root.left + 320.dp) shouldBe true
            (toast.top >= root.bottom - 240.dp) shouldBe true
        }

    @Test
    fun textField_rightClick_opensNoPopupWhereOverlaysRenderInThePage() =
        hostEachWay { skin, inTree ->
            setContent { HostOverlays(skin, inTree) { BuilderTextField("Ocean", {}, "Project name") } }
            onNode(hasSetTextAction()).performMouseInput { rightClick() }
            waitForIdle()
            val popups = onAllNodes(isPopup()).fetchSemanticsNodes().size
            // In windows foundation's own context menu opens, which shows the click lands.
            if (inTree) popups shouldBe 0 else (popups > 0) shouldBe true
        }

    @Test
    fun overlays_switchOnWithoutAHost_composeWhileClosed() =
        forEachSkin { _, skin ->
            val saved = BuilderToastHostState().apply { show("Saved", duration = ToastDuration.Indefinite) }
            setContent {
                HostOverlays(skin, inTree = true) {
                    CompositionLocalProvider(LocalOverlayHost provides null) {
                        Column {
                            BuilderMenu(false, {}, MenuItems) { BuilderText("Project") }
                            BuilderSelect("Style", listOf("Vibrant", "Expressive"), "Vibrant", {})
                            BuilderTooltip("Undo last edit") { Box(Modifier.size(40.dp)) }
                            BuilderDialog(false, {}, "Rename") { BuilderText("Sunset") }
                            BuilderToastHost(saved, Modifier.size(200.dp))
                        }
                    }
                }
            }
            waitForIdle()
            onNodeWithText("Project").assertExists()
            onNodeWithText("Saved").assertExists()
        }

    @Test
    fun overlay_switchOnWithoutAHost_failsAsItOpens() =
        runComposeUiTest {
            val failure = shouldThrow<IllegalStateException> {
                setContent {
                    HostOverlays(Skin(Library.Unstyled, expressive = false), inTree = true) {
                        CompositionLocalProvider(LocalOverlayHost provides null) {
                            BuilderDialog(true, {}, "Rename") { BuilderText("Sunset") }
                        }
                    }
                }
            }
            failure.message shouldContain "OverlayHost"
        }

    @Test
    fun menu_inTree_flipsAboveAtTheBottomEdgeAndLinesUpWithTheEndInRtl() =
        forEachSkin { _, skin ->
            var direction by mutableStateOf(LayoutDirection.Ltr)
            setContent {
                HostOverlays(skin, inTree = true) {
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        Box(Modifier.fillMaxSize()) {
                            val ltr = direction == LayoutDirection.Ltr
                            val corner = Modifier.align(if (ltr) Alignment.BottomStart else Alignment.TopStart)
                            BuilderMenu(true, {}, listOf(BuilderMenuItem("Rename", {})), corner) {
                                Box(Modifier.testTag("anchor").size(40.dp))
                            }
                        }
                    }
                }
            }
            waitForIdle()
            val row = { onNode(hasText("Rename") and hasRole(Role.Button)).getBoundsInRoot() }
            val anchor = { onNodeWithTag("anchor").getBoundsInRoot() }
            (row().bottom <= anchor().top) shouldBe true
            (row().left >= anchor().left) shouldBe true
            direction = LayoutDirection.Rtl
            waitForIdle()
            (row().top >= anchor().bottom) shouldBe true
            (row().right <= anchor().right) shouldBe true
            (row().right >= anchor().right - 32.dp) shouldBe true
        }

    @Test
    fun positionProviders_atTheEdges_flipAndFollowTheLayoutDirection() {
        val window = IntSize(400, 600)
        val list = IntSize(120, 200)
        val dropdown = DropdownPositionProvider(gap = 4)
        val ltr = LayoutDirection.Ltr
        val rtl = LayoutDirection.Rtl
        dropdown.calculatePosition(IntRect(200, 0, 240, 40), window, ltr, list) shouldBe IntOffset(200, 44)
        dropdown.calculatePosition(IntRect(200, 0, 240, 40), window, rtl, list) shouldBe IntOffset(120, 44)
        dropdown.calculatePosition(IntRect(200, 560, 240, 600), window, ltr, list) shouldBe IntOffset(200, 356)
        dropdown.calculatePosition(IntRect(360, 0, 400, 40), window, ltr, list) shouldBe IntOffset(280, 44)
        val tooltip = TooltipPositionProvider(gap = 4)
        val bubble = IntSize(80, 30)
        tooltip.calculatePosition(IntRect(100, 200, 140, 240), window, ltr, bubble) shouldBe IntOffset(80, 166)
        tooltip.calculatePosition(IntRect(100, 0, 140, 40), window, ltr, bubble) shouldBe IntOffset(80, 44)
        tooltip.calculatePosition(IntRect(0, 200, 40, 240), window, rtl, bubble) shouldBe IntOffset(0, 166)
    }
}

/**
 * Opens [modal] from a trigger, walks Tab both ways around [cycle] without reaching the page,
 * closes it on Esc and checks focus is back on the trigger. In the page the rest of the page also
 * has to be gone from the semantics tree while the modal is open, and back once it closes.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.hostCheckModal(
    skin: Skin,
    inTree: Boolean,
    title: String,
    cycle: List<SemanticsMatcher>,
    modal: @Composable (open: Boolean, close: () -> Unit, trigger: FocusRequester) -> Unit,
) {
    var open by mutableStateOf(false)
    var pageTookFocus = false
    val trigger = FocusRequester()
    setContent {
        HostOverlays(skin, inTree) {
            Column {
                Box(
                    Modifier
                        .testTag("trigger")
                        .size(40.dp)
                        .focusRequester(trigger)
                        .focusable(),
                )
                Box(
                    Modifier
                        .testTag("page")
                        .size(40.dp)
                        .onFocusChanged { state -> if (state.isFocused) pageTookFocus = true }
                        .focusable(),
                )
                modal(open, { open = false }, trigger)
            }
        }
    }
    onNodeWithTag("trigger").requestFocus()
    open = true
    waitForIdle()
    onNode(hasOverlayPaneTitle(title)).assertExists()
    if (inTree) onNodeWithTag("page").assertDoesNotExist()
    onNode(cycle.first()).assertIsFocused()
    for (step in cycle.indices + cycle.indices) {
        onNode(cycle[step]).performKeyInput { pressKey(Key.Tab) }
        waitForIdle()
        onNode(cycle[(step + 1) % cycle.size]).assertIsFocused()
    }
    onNode(cycle.first()).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
    waitForIdle()
    onNode(cycle.last()).assertIsFocused()
    pageTookFocus shouldBe false
    onNode(cycle.last()).performKeyInput { pressKey(Key.Escape) }
    waitForIdle()
    open shouldBe false
    onNode(hasOverlayPaneTitle(title)).assertDoesNotExist()
    onNodeWithTag("page").assertExists()
    onNodeWithTag("trigger").assertIsFocused()
}

/**
 * Opens [overlay], which holds nothing that takes focus, from a trigger. Focus has to leave the
 * trigger for the overlay, Tab both ways must not reach the page, and Esc closes it with focus back
 * on the trigger.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.hostCheckHeldFocus(
    skin: Skin,
    overlay: @Composable (
        open: Boolean,
        close: () -> Unit,
        focus: FocusRequester,
        trigger: @Composable () -> Unit,
    ) -> Unit,
) {
    var open by mutableStateOf(false)
    var pageTookFocus = false
    var triggerFocused = false
    val focus = FocusRequester()
    setContent {
        HostOverlays(skin, inTree = true) {
            Column {
                Box(
                    Modifier
                        .size(40.dp)
                        .onFocusChanged { state -> if (state.isFocused) pageTookFocus = true }
                        .focusable(),
                )
                overlay(open, { open = false }, focus) {
                    Box(
                        Modifier
                            .testTag("trigger")
                            .size(40.dp)
                            .focusRequester(focus)
                            .onFocusChanged { state -> triggerFocused = state.isFocused }
                            .focusable(),
                    )
                }
            }
        }
    }
    onNodeWithTag("trigger").requestFocus()
    waitForIdle()
    triggerFocused shouldBe true
    open = true
    waitForIdle()
    triggerFocused shouldBe false
    onRoot().performKeyInput { pressKey(Key.Tab) }
    waitForIdle()
    onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
    waitForIdle()
    pageTookFocus shouldBe false
    triggerFocused shouldBe false
    onRoot().performKeyInput { pressKey(Key.Escape) }
    waitForIdle()
    open shouldBe false
    triggerFocused shouldBe true
}

/** The locals an overlay has to carry from where it opened. */
private data class HostLocals(
    val tokens: BuilderTokens,
    val direction: LayoutDirection,
    val density: Density,
)

@Composable
private fun hostLocals(): HostLocals =
    HostLocals(LocalBuilderTokens.current, LocalLayoutDirection.current, LocalDensity.current)
