package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.AppTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * Where a recording job would write the screenshots. Nothing is written unless a Roborazzi task
 * turns capture on, and the preview keeps no baselines.
 */
private const val PhotoScreenshotDir = "src/jvmTest/screenshots/photos"

private const val PhotoSourceDir = "src/commonMain/kotlin/com/materialkolor/builder/preview/material"

/**
 * What the photo app's sources never name, anything that opens outside the layout, a field or an endless clock.
 */
private val PhotoBannedWords: List<String> = listOf(
    "androidx.compose.ui.window",
    "Popup",
    "Dialog",
    "DropdownMenu",
    "Tooltip",
    "ModalWideNavigationRail",
    "TextField",
    // Stems, so the architecture scan does not read this list as an endless animation.
    "rememberInfinite",
    "infiniteRepeat",
)

/**
 * The feed, the one list of the app that scrolls down.
 */
private val PhotoFeedNode: SemanticsMatcher =
    hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

/**
 * The button that opens the create menu, as the tree shows it off the web.
 */
private val CreateButton: SemanticsMatcher = isToggleable() and hasContentDescription(PhotoCopy.Create)

@OptIn(ExperimentalTestApi::class)
class PhotoAppTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnRoles() {
        for ((width, frame) in PhotoFrames) {
            withClue(width) {
                runComposeUiTest {
                    // The create menu open, so its items are there to check too.
                    val state = DemoAppState().apply { setOn(PhotoCreateSwitch, true) }
                    setContent {
                        PhotoHarness(
                            spec = LightSpec,
                            state = state,
                            width = width,
                            // Tall enough that every lazy item composes, wider than the window on desktop.
                            modifier = Modifier
                                .wrapContentSize(Alignment.TopStart, unbounded = true)
                                .requiredSize(frame.width.dp, 2400.dp),
                        )
                    }

                    onAllNodesWithText(PhotoCreate.Camera.label).fetchSemanticsNodes().shouldNotBeEmpty()
                    // Unmerged, since a merged node also carries the roles its children declared.
                    val controls = onAllNodes(hasClickAction() or hasSetTextAction(), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                    controls.shouldNotBeEmpty()
                    controls
                        .filter { node -> PreviewRoles !in node.config }
                        .map { node -> node.config.toString() }
                        .shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun roles_everyDeviceWidthFirstScreen_showTheSchemeF20Asks() {
        for ((width, frame) in PhotoFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    setContent { PhotoHarness(LightSpec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    val screen = onRoot().fetchSemanticsNode().boundsInRoot
                    val used = onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .filter { node -> node.boundsInRoot.overlaps(screen) }
                        .flatMap { node -> node.config[PreviewRoles] }
                        .filterIsInstance<ColorRef.OfRole>()
                        .map { ref -> ref.role }
                        .toSet()
                    PhotoFamilies.filterValues { family -> family.none { role -> role in used } }.keys.shouldBeEmpty()
                    (PhotoContainerLevels intersect used).size shouldBeGreaterThanOrEqual 4
                    used shouldContain Role.Outline
                }
            }
        }
    }

    @Test
    fun screens_everyDeviceWidthBothModes_layOutForTheWidthAndRender() {
        for ((width, frame) in PhotoFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    var spec by mutableStateOf(LightSpec)
                    setContent { PhotoHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    for (mode in listOf(LightSpec, DarkSpec)) {
                        spec = mode
                        waitForIdle()
                        val albums = onNode(hasClickAction() and hasContentDescription(PhotoDestination.Albums.label))
                            .fetchSemanticsNode()
                            .boundsInRoot
                        val memories = onNodeWithText(PhotoCopy.Memories).fetchSemanticsNode().boundsInRoot
                        if (width == DeviceWidth.Phone) {
                            albums.top shouldBeGreaterThan memories.bottom
                        } else {
                            memories.left shouldBeGreaterThan albums.right
                        }
                        onRoot().captureRoboImage("$PhotoScreenshotDir/${width.name}-${mode.label}.png")
                    }
                }
            }
        }
    }

    @Test
    fun split_scrollPickMoveAndMenuInOneCopy_showTheSameInBoth() =
        runDesktopComposeUiTest(412, 900) {
            val state = DemoAppState()
            // With the handle at the start edge the dark copy shows everywhere, and only the light
            // copy is in the merged tree, so every action below lands on that one copy.
            val split = SplitState(0f)
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Chrome(PhotoSkin) {
                        SplitPreview(LightSpec, DarkSpec, split, Modifier.fillMaxSize()) { spec ->
                            MaterialAppEntry(spec, state, DeviceWidth.Phone, expressive = true)
                        }
                    }
                }
            }
            waitForIdle()

            val market = PhotoMemories[2]
            onNode(hasClickAction() and hasText(market.title)).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.choice(PhotoMemoryChoice, PhotoMemories.size) shouldBe 2
            boundsInBoth(hasClickAction() and hasAnyDescendant(hasText(market.title)))

            val favourites = PhotoView.Favourites.label
            onNode(isToggleable() and hasText(favourites)).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.choice(PhotoViewChoice, PhotoView.entries.size) shouldBe PhotoView.Favourites.ordinal
            onAllNodes(isToggleable() and hasAnyDescendant(hasText(favourites)), useUnmergedTree = true)
                .assertCountEquals(2)
                .assertAll(isOn())

            onNode(CreateButton).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(PhotoCreateSwitch) shouldBe true
            onAllNodes(CreateButton, useUnmergedTree = true).assertCountEquals(2).assertAll(isOn())
            boundsInBoth(hasText(PhotoCreate.Camera.label)).height shouldBeGreaterThan 0f

            val before = boundsInBoth(PhotoFeedNode)
            onNode(PhotoFeedNode).performScrollToIndex(1)
            waitForIdle()
            // The feed starts where the bar ends, so a collapsed bar starts it higher, in both copies alike.
            boundsInBoth(PhotoFeedNode).top shouldBeLessThan before.top
        }

    @Test
    fun webNames_phoneAndDesktop_carryTheStateOfEveryChoiceDestinationAndTheMenu() {
        // The phone's destinations are in its bottom bar, the desktop's in its rail.
        for (width in listOf(DeviceWidth.Phone, DeviceWidth.Desktop)) {
            withClue(width) {
                runComposeUiTest {
                    val state = DemoAppState()
                    val frame = PhotoFrames.getValue(width)
                    setContent {
                        PhotoHarness(
                            spec = LightSpec,
                            state = state,
                            width = width,
                            modifier = Modifier.size(frame.width.dp, frame.height.dp),
                            webFolds = true,
                        )
                    }

                    for (name in listOf(
                        "${PhotoView.Recent.label}, radio, selected",
                        "${PhotoView.Shared.label}, radio, not selected",
                        "${PhotoMemories[0].title}, selected",
                        "${PhotoMemories[1].title}, not selected",
                        "${PhotoDestination.Photos.label}, selected",
                        "${PhotoDestination.Albums.label}, not selected",
                        "${PhotoCopy.Create}, collapsed",
                    )) {
                        withClue(name) { onNode(hasContentDescription(name)).assertExists() }
                    }

                    onNode(hasContentDescription("${PhotoCopy.Create}, collapsed"))
                        .performSemanticsAction(SemanticsActions.OnClick)
                    waitForIdle()
                    state.isOn(PhotoCreateSwitch) shouldBe true
                    onNode(hasContentDescription("${PhotoCopy.Create}, expanded")).assertExists()
                }
            }
        }
    }

    @Test
    fun memoryTitles_everyDeviceWidth_sitWholeInsideTheVisiblePartOfTheirMemory() {
        for ((width, frame) in PhotoFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    setContent { PhotoHarness(LightSpec, DemoAppState(), width, Modifier.fillMaxSize()) }
                    waitForIdle()

                    val masks = memoryMasks()
                    masks.size shouldBeGreaterThanOrEqual 3
                    val inset = with(density) { SectionGap.toPx() }
                    val whole = masks.filter { (index, mask) ->
                        val title = onNode(hasText(PhotoMemories[index].title), useUnmergedTree = true)
                            .fetchSemanticsNode()
                            .unclippedBoundsInRoot()
                        val inside = title.left >= mask.left && title.right <= mask.right
                        // A title that would stick out of its memory is not drawn at all.
                        val hidden = memoryTitleAlpha(mask.width, 0f, mask.width, reach = title.width + inset) == 0f
                        withClue("${PhotoMemories[index].title} at $title in $mask") {
                            (inside || hidden) shouldBe true
                            if (inside) title.left shouldBe (mask.left + inset plusOrMinus 1f)
                        }
                        inside
                    }
                    // The featured memory and the one beside it, at least, show their titles whole.
                    whole.size shouldBeGreaterThanOrEqual 2
                }
            }
        }
    }

    @Test
    fun memoryTitleAlpha_fadesAsTheMemoryShrinksAndHidesATitleThatWouldNotFit() {
        memoryTitleAlpha(size = 200f, minSize = 10f, maxSize = 200f, reach = 120f) shouldBe 1f
        memoryTitleAlpha(size = 105f, minSize = 10f, maxSize = 200f, reach = 100f) shouldBe 0.5f
        memoryTitleAlpha(size = 56f, minSize = 10f, maxSize = 200f, reach = 120f) shouldBe 0f
        memoryTitleAlpha(size = 56f, minSize = 56f, maxSize = 56f, reach = 40f) shouldBe 1f
    }

    @Test
    fun appTab_expressiveOrNot_showsThePhotoAppOrTrips() {
        for (expressive in listOf(true, false)) {
            withClue("expressive $expressive") {
                runComposeUiTest {
                    setContent {
                        CompositionLocalProvider(LocalMotionFrozen provides true) {
                            Chrome(Skin(Library.Material3, expressive)) {
                                PreviewPane(LightSpec, Modifier.size(840.dp, 900.dp)) {
                                    AppTab(LightSpec, DemoAppState(), DeviceWidth.Tablet)
                                }
                            }
                        }
                    }

                    onAllNodesWithText(PhotoCopy.Memories).fetchSemanticsNodes().isNotEmpty() shouldBe expressive
                    onAllNodesWithText("Lisbon, Portugal").fetchSemanticsNodes().isNotEmpty() shouldBe !expressive
                }
            }
        }
    }

    @Test
    fun inspect_clickOnTheCreateToggle_pinsItsRolesAndNeverReachesTheApp() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Inspecting(shown = PreviewMode.Light, split = remember { SplitState() }, skin = PhotoSkin) {
                        PreviewPane(LightSpec, Modifier.fillMaxSize()) {
                            MaterialAppEntry(LightSpec, state, DeviceWidth.Phone, expressive = true)
                        }
                    }
                }
            }
            waitForIdle()
            val before = state.photoSnapshot()

            onNode(CreateButton).performClick()
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
            onNode(OnCard and hasText("primaryContainer", substring = true)).assertExists()
            onNode(OnCard and hasText("onPrimaryContainer", substring = true)).assertExists()
            state.photoSnapshot() shouldBe before
        }

    @Test
    fun photoSources_openNothingOutsideTheLayoutAndNeverLoop() {
        val sources = File(PhotoSourceDir)
            .listFiles()
            .orEmpty()
            .filter { file -> file.name.startsWith("Photo") || file.name == "ExpressiveRoles.kt" }
        sources.size shouldBeGreaterThanOrEqual 4
        for (source in sources) {
            withClue(source.name) {
                val text = source.readText()
                PhotoBannedWords.filter { word -> word in text }.shouldBeEmpty()
                // A loading indicator only with its progress, a wavy one only holding its wave still.
                text
                    .callArguments("""(Contained)?LoadingIndicator""")
                    .filterNot { arguments -> arguments.trim().startsWith("Role.") }
                    .filterNot { arguments -> "progress" in arguments }
                    .shouldBeEmpty()
                text
                    .callArguments("""(Linear|Circular)WavyProgressIndicator""")
                    .filterNot { arguments -> arguments.trim().startsWith("Role.") }
                    .filterNot { arguments -> "waveSpeed = 0.dp" in arguments }
                    .shouldBeEmpty()
                text
                    .lines()
                    .map { line -> line.trim() }
                    .filter { line -> line.startsWith("import com.materialkolor.builder.kit.") }
                    .map { line -> line.removePrefix("import ") }
                    .filterNot { imported -> imported.startsWith("com.materialkolor.builder.kit.motion.") }
                    .filterNot { imported -> imported.startsWith("com.materialkolor.builder.kit.control.folded") }
                    .shouldBeEmpty()
            }
        }
    }
}

/**
 * The part of each memory on screen that its mask leaves visible, by index, with the carousel at
 * its start.
 *
 * The carousel lays every memory out at the featured size and masks it down around its middle, and
 * it pins the masks edge to edge a gap apart, from the start of its padding to its far edge. So each
 * mask spans from the end of the one before it, a gap on, to as far past the memory's middle.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.memoryMasks(): Map<Int, Rect> {
    val feed = onNode(PhotoFeedNode).fetchSemanticsNode().boundsInRoot
    val (padding, gap) = with(density) { SectionGap.toPx() to Gap.toPx() }
    val masks = mutableMapOf<Int, Rect>()
    var left = feed.left + padding
    for (index in PhotoMemories.indices) {
        if (left >= feed.right - 1f) break
        val memory = onNode(hasClickAction() and hasText(PhotoMemories[index].title))
            .fetchSemanticsNode()
            .unclippedBoundsInRoot()
        val right = 2 * memory.center.x - left
        masks[index] = Rect(left, memory.top, right, memory.bottom)
        left = right + gap
    }
    // The masks run out at the carousel's far edge, the last one a little past it at most, or the
    // reading of them is off.
    withClue("$masks in $feed") {
        val past = masks.values.maxOf { mask -> mask.right } - feed.right
        (past >= -1f && past <= gap) shouldBe true
    }
    return masks
}

/**
 * Where the node's layout sits in the root, moved by every layer above it but clipped by none.
 */
private fun SemanticsNode.unclippedBoundsInRoot(): Rect {
    val coordinates = layoutInfo.coordinates
    return Rect(coordinates.localToRoot(Offset.Zero), coordinates.size.toSize())
}

/**
 * The bounds of the node [matcher] finds in each copy of a split, which have to be the same.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.boundsInBoth(matcher: SemanticsMatcher): Rect {
    val bounds = onAllNodes(matcher, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node -> node.boundsInRoot }
    bounds.size shouldBe 2
    bounds[0] shouldBe bounds[1]
    return bounds[0]
}

/**
 * What is passed to every call of a function whose name matches [name], each up to its closing parenthesis.
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
