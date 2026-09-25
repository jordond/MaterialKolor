package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CafeAppTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnSlots() {
        for ((width, frame) in CafeFrames) {
            for ((screen, state) in listOf("menu" to DemoAppState(), "order" to cafeOrderOpen())) {
                withClue("$width $screen") {
                    runComposeUiTest {
                        setContent {
                            // Tall enough that every lazy item composes, wider than the window on desktop.
                            CafeHarness(
                                spec = LightSpec,
                                state = state,
                                width = width,
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                                    .requiredSize(frame.width.dp, 2400.dp),
                            )
                        }

                        // Unmerged, since a merged node also carries the slots its children declared.
                        val controls = onAllNodes(hasClickAction() or hasSetTextAction(), useUnmergedTree = true)
                            .fetchSemanticsNodes()
                        controls.shouldNotBeEmpty()
                        controls
                            .filterNot { node -> node.cafeDeclaresSlots() }
                            .map { node -> node.config.toString() }
                            .shouldBeEmpty()
                    }
                }
            }
        }
    }

    @Test
    fun slots_everyDeviceWidthFirstScreen_showWhatF20AsksWithOrWithoutAccents() {
        for ((width, frame) in CafeFrames) {
            for (spec in listOf(LightSpec, AccentLightSpec)) {
                withClue("$width ${spec.accentCount} accents") {
                    runDesktopComposeUiTest(frame.width, frame.height) {
                        setContent { CafeHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                        val refs = cafeRefsOnScreen()
                        val slots = refs.filterIsInstance<ColorRef.OfSlot>().map { ref -> ref.slot }.toSet()
                        CafeFamilies
                            .filterValues { family -> family.none { slot -> slot in slots } }
                            .keys
                            .shouldBeEmpty()
                        CafeSurfaces.filterNot { slot -> slot in slots }.shouldBeEmpty()
                        (CafeBorders intersect slots).size shouldBeGreaterThanOrEqual 1
                        if (spec.accentCount == 0) {
                            refs.filterIsInstance<ColorRef.OfAccent>().shouldBeEmpty()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun accents_eightInTheDocument_eachPaintsItsPlaceInTheColorItDeclares() {
        for (spec in listOf(AccentLightSpec, AccentDarkSpec)) {
            withClue(spec.label) {
                runDesktopComposeUiTest(1280, 800) {
                    setContent { CafeHarness(spec, DemoAppState(), DeviceWidth.Desktop, Modifier.fillMaxSize()) }

                    val accents = cafeRefsOnScreen().filterIsInstance<ColorRef.OfAccent>().map { ref -> ref.slot }
                    accents.map { slot -> slot.index }.toSet() shouldBe CafeAccent.entries.indices.toSet()
                    val pixels = onRoot().captureToImagePixels()
                    // The fills, since text and glyph edges blend into what they sit on.
                    for (slot in accents.filter { slot -> slot.part in CafeFills }.toSet()) {
                        withClue(slot) {
                            val painted = spec.result.accents[slot, spec.isDark].toColor()
                            pixels shouldContain painted.toArgb()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun screens_everyDeviceWidthBothModes_layOutForTheWidthAndRender() {
        for ((width, frame) in CafeFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    var spec by mutableStateOf(LightSpec)
                    setContent { CafeHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    for (mode in listOf(LightSpec, DarkSpec)) {
                        spec = mode
                        waitForIdle()
                        // The flat white is in the order too, after the menu.
                        val first = onAllNodesWithText("Flat white").fetchSemanticsNodes().first().boundsInRoot
                        val second = onNodeWithText("Cortado").fetchSemanticsNode().boundsInRoot
                        val banner = onNodeWithText(CafeCategory.Coffee.blurb).fetchSemanticsNode().boundsInRoot
                        val order = onAllNodesWithText(CafeCopy.YourOrder)
                            .fetchSemanticsNodes()
                            .map { node -> node.boundsInRoot }
                        when (width) {
                            DeviceWidth.Phone -> {
                                second.top shouldBeGreaterThan first.bottom
                                order.shouldBeEmpty()
                                onNodeWithText(CafeCopy.ViewOrder).assertExists()
                            }
                            DeviceWidth.Tablet -> {
                                second.top shouldBeGreaterThan first.bottom
                                order.single().left shouldBeGreaterThan banner.right
                            }
                            DeviceWidth.Desktop -> {
                                second.left shouldBeGreaterThan first.right
                                order.single().left shouldBeGreaterThan banner.right
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun category_pickedInEitherCopyOfASplit_showsInBoth() =
        runComposeUiTest {
            val state = DemoAppState()
            // With the handle at the start edge the dark copy shows everywhere, so it takes the click.
            val split = SplitState(0f)
            setContent { CafeSplitHarness(state, split, Modifier.size(840.dp, 760.dp)) }
            val bothBanners = { category: CafeCategory ->
                onAllNodes(hasText(category.blurb), useUnmergedTree = true)
            }

            onNode(hasClickAction() and hasText(CafeCategory.Matcha.label)).performClick()
            waitForIdle()
            state.category shouldBe CafeCategory.Matcha
            bothBanners(CafeCategory.Matcha).assertCountEquals(2)

            split.fraction = 1f
            waitForIdle()
            onNode(hasClickAction() and hasText(CafeCategory.Tea.label)).performClick()
            waitForIdle()
            state.category shouldBe CafeCategory.Tea
            bothBanners(CafeCategory.Tea).assertCountEquals(2)
            bothBanners(CafeCategory.Matcha).assertCountEquals(0)
        }

    @Test
    fun order_addRemoveFavouritePlaceAndDismiss_changeTheSharedState() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { CafeHarness(LightSpec, state, DeviceWidth.Tablet, Modifier.size(840.dp, 900.dp)) }
            val coldBrew = CafeMenu.first { item -> item.id == "coldBrew" }
            val flatWhite = CafeMenu.first { item -> item.id == "flatWhite" }

            // The Coffee drinks that can be ordered, in menu order, the cortado being sold out.
            onAllNodes(hasClickAction() and hasText(CafeCopy.Add) and isEnabled())[1].performClick()
            waitForIdle()
            state.quantity(coldBrew) shouldBe 1
            onAllNodesWithText(coldBrew.name).assertCountEquals(2)

            onNode(hasContentDescription(CafeCopy.removeOne(coldBrew.name))).performClick()
            waitForIdle()
            state.quantity(coldBrew) shouldBe 0
            onAllNodesWithText(coldBrew.name).assertCountEquals(1)

            onNode(isToggleable() and hasContentDescription(CafeCopy.favourite(flatWhite.name))).performClick()
            waitForIdle()
            state.isFavourite(flatWhite) shouldBe false

            onNode(hasClickAction() and hasText(CafeCopy.PlaceOrder)).performClick()
            waitForIdle()
            state.isOn(PlacedKey) shouldBe true
            state.orderLines().shouldBeEmpty()
            onNodeWithText(CafeCopy.Placed).assertExists()
            onNodeWithText(CafeCopy.EmptyOrder).assertExists()

            onNode(hasContentDescription(CafeCopy.Dismiss)).performClick()
            waitForIdle()
            onAllNodesWithText(CafeCopy.Placed).assertCountEquals(0)
        }

    @Test
    fun placeOrderByKey_inTheStartCopyOfASplit_focusesItsDismissThenTheOrderType() =
        runComposeUiTest {
            val state = DemoAppState().apply { pick(OrderType.DineIn) }
            // Only the start copy is in the semantics tree, and the end copy shows its own note too.
            setContent { CafeSplitHarness(state, SplitState(1f), Modifier.size(840.dp, 760.dp)) }

            onNode(hasClickAction() and hasText(CafeCopy.PlaceOrder)).requestFocus().performKeyInput {
                pressKey(Key.Enter)
            }
            waitForIdle()
            state.isOn(PlacedKey) shouldBe true
            val dismiss = onNode(hasClickAction() and hasContentDescription(CafeCopy.Dismiss))
            dismiss.assertIsFocused()

            dismiss.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            state.isOn(PlacedKey) shouldBe false
            onNode(hasClickAction() and hasText(OrderType.DineIn.label)).assertIsFocused()
        }

    @Test
    fun placeOrder_clickedInTheEndCopyOfASplit_leavesTheStartCopyUnfocused() =
        runComposeUiTest {
            val state = DemoAppState()
            // With the handle at the start edge the end copy shows everywhere, so it takes the click.
            setContent { CafeSplitHarness(state, SplitState(0f), Modifier.size(840.dp, 760.dp)) }

            onNode(hasClickAction() and hasText(CafeCopy.PlaceOrder)).performClick()
            waitForIdle()
            state.isOn(PlacedKey) shouldBe true
            onAllNodes(isFocused()).assertCountEquals(0)
        }

    @Test
    fun clearByKey_emptiesTheOrderAndFocusesTheOrderType() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { CafeHarness(LightSpec, state, DeviceWidth.Tablet, Modifier.size(840.dp, 900.dp)) }
            state.orderLines().shouldNotBeEmpty()

            onNode(hasClickAction() and hasText(CafeCopy.Clear)).requestFocus().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            state.orderLines().shouldBeEmpty()
            onNode(hasClickAction() and hasText(OrderType.PickUp.label)).assertIsFocused()
        }

    @Test
    fun phone_orderBar_opensTheOrderInPlaceAndBack() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { CafeHarness(LightSpec, state, DeviceWidth.Phone, Modifier.size(412.dp, 900.dp)) }

            onNode(hasClickAction() and hasText(CafeCopy.ViewOrder)).performClick()
            waitForIdle()
            state.isOn(PhoneOrderKey) shouldBe true
            onNodeWithText(CafeCopy.YourOrder).assertExists()
            onAllNodesWithText(CafeCategory.Coffee.blurb).assertCountEquals(0)

            onNode(hasClickAction() and hasText(CafeCopy.BackToMenu)).performClick()
            waitForIdle()
            state.isOn(PhoneOrderKey) shouldBe false
            onNodeWithText(CafeCategory.Coffee.blurb).assertExists()
        }

    @Test
    fun cafeSources_importNothingThatOpensAPopupOrLoops() {
        val sources = CafeSources
        sources.size shouldBeGreaterThanOrEqual 5
        for (source in sources) {
            withClue(source.name) {
                val lines = source.readLines().map { line -> line.trim() }
                lines
                    .filter { line -> line.startsWith("import ") }
                    .map { line -> line.removePrefix("import ").substringBefore(" as ") }
                    .filter { imported ->
                        imported in CafePopupImports || imported.startsWith("androidx.compose.ui.window.")
                    }.shouldBeEmpty()
                lines.filter { line -> CafeEndlessMotion.containsMatchIn(line) }.shouldBeEmpty()
            }
        }
    }
}
