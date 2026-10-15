package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.GalleryFrames
import com.materialkolor.builder.preview.GalleryHarness
import com.materialkolor.builder.preview.GalleryInteractive
import com.materialkolor.builder.preview.GalleryWhole
import com.materialkolor.builder.preview.InspectingPane
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.PaneKitImports
import com.materialkolor.builder.preview.ShellExpressive
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.checkCardsFillEveryGroup
import com.materialkolor.builder.preview.checkCardsShowEnabledAndDisabled
import com.materialkolor.builder.preview.checkGalleryControlsDeclareRoles
import com.materialkolor.builder.preview.checkInspectCardSwallowsPress
import com.materialkolor.builder.preview.checkInspectPins
import com.materialkolor.builder.preview.checkNameFlips
import com.materialkolor.builder.preview.checkNamesAreTheLabelsAlone
import com.materialkolor.builder.preview.checkSourcesOpenNothingAndNeverLoop
import com.materialkolor.builder.preview.checkWebNames
import com.materialkolor.builder.preview.galleryCardDeclaresRoles
import com.materialkolor.builder.preview.galleryDeclaresRoles
import com.materialkolor.builder.preview.galleryDescendants
import com.materialkolor.builder.preview.galleryFrame
import com.materialkolor.builder.preview.galleryInteractive
import com.materialkolor.builder.preview.galleryNamed
import com.materialkolor.builder.preview.gallerySnapshot
import com.materialkolor.builder.preview.importedNames
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.isKitImportBeyond
import com.materialkolor.builder.preview.moduleSource
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.opensAWindow
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitState
import com.materialkolor.builder.preview.sweepEveryControl
import com.materialkolor.builder.preview.sweepWholeGallery
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The blue preview document drawn in Unstyled, light and dark.
 */
private val UnstyledLightSpec: PaneSpec = LightSpec.on(Library.Unstyled)

private val UnstyledDarkSpec: PaneSpec = DarkSpec.on(Library.Unstyled)

/**
 * The cards with nothing to press, or whose only control has no disabled look.
 */
private val GalleryNoDisabled: Set<String> = setOf("Separators", "Scroll area", "Progress", "Tooltip", "Badges")

/**
 * The sources every Unstyled gallery card is drawn from.
 */
private val GallerySources: List<String> = listOf(
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/GalleryEntry.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/GalleryInputs.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/GalleryPanels.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/GalleryRoles.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledGallery.kt",
)

/**
 * Words in the names of what opens a popup, a window or a portal, which on the web take the mirror over.
 */
private val UnstyledPopupWords: List<String> =
    listOf("Popup", "Dialog", "Modal", "BottomSheet", "DropdownMenu", "Tooltip", "Portal")

/**
 * What the gallery may take from the kit, what Trips may and the value node names.
 */
private val GalleryKitImports: List<String> = PaneKitImports + listOf(
    "com.materialkolor.builder.kit.a11y.foldsValueIntoName",
    "com.materialkolor.builder.kit.a11y.valueNodeName",
    "com.materialkolor.builder.kit.a11y.sliderRoleWord",
    "com.materialkolor.builder.kit.a11y.progressRoleWord",
)

/**
 * The switches the gallery keeps in [DemoAppState].
 */
private val GallerySwitches: List<String> = listOf(
    GalleryKeys.Favourite,
    GalleryKeys.Starred,
    GalleryKeys.Wifi,
    GalleryKeys.SortShut,
    GalleryKeys.MenuShut,
    GalleryKeys.Details,
)

/**
 * The single choices the gallery keeps in [DemoAppState], each a switch per option.
 */
private val GalleryChoices: List<String> =
    listOf(GalleryKeys.Volume, GalleryKeys.Plan, GalleryKeys.Sort, GalleryKeys.Tab, GalleryKeys.Destination)

@OptIn(ExperimentalTestApi::class)
class UnstyledGalleryTest {
    @Test
    fun cards_everyGroup_holdUniquelyNamedCards() = checkCardsFillEveryGroup(UnstyledCards)

    @Test
    fun controls_bothFramesBothModes_declareTheirOwnRoles() {
        for (frame in GalleryFrames) {
            for (spec in listOf(UnstyledLightSpec, UnstyledDarkSpec)) {
                withClue("${frame.width} ${spec.label}") {
                    runComposeUiTest {
                        val composed = mutableSetOf<String>()
                        // As wide as the frame, and tall enough that every card composes.
                        val whole = Modifier
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(frame.width.dp, 9000.dp)
                        setContent { UnstyledGalleryHarness(spec, DemoAppState(), whole, composed) }
                        waitForIdle()

                        checkGalleryControlsDeclareRoles(UnstyledCards, composed)
                    }
                }
            }
        }
    }

    @Test
    fun cards_everyControlWithADisabledLook_showItEnabledAndDisabled() =
        runComposeUiTest {
            setContent { UnstyledGalleryHarness(UnstyledLightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            checkCardsShowEnabledAndDisabled(UnstyledCards, GalleryNoDisabled)
        }

    @Test
    fun gallery_everyControlPressedHoveredFocusedRightClickedAndLongPressed_opensNoPopupOrWindow() =
        sweepWholeGallery { UnstyledGalleryHarness(UnstyledLightSpec, DemoAppState(), GalleryWhole) }

    @Test
    fun gallerySources_openNoPopupWindowOrPortalAndNeverLoop() =
        checkSourcesOpenNothingAndNeverLoop(GallerySources.map { path -> moduleSource(path) }) { imported ->
            imported.isBannedInUnstyled(GalleryKitImports)
        }

    @Test
    fun names_onTheWeb_foldTheStateOfEveryKindOfControl() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { UnstyledGalleryHarness(UnstyledLightSpec, state, GalleryWhole, webFolds = true) }
            waitForIdle()

            checkWebNames(WebNames)
            for (name in WebValueNames) {
                withClue(name) {
                    onAllNodes(hasText(name), useUnmergedTree = true).fetchSemanticsNodes().shouldNotBeEmpty()
                }
            }

            checkNameFlips("Wi-Fi, switch, off", "Wi-Fi, switch, on")
            state.isOn(GalleryKeys.Wifi) shouldBe true

            checkNameFlips("Shipping details, collapsed", "Shipping details, expanded")
            state.isOn(GalleryKeys.Details) shouldBe true
        }

    @Test
    fun names_offTheWeb_areTheLabelsAlone() =
        runComposeUiTest {
            setContent { UnstyledGalleryHarness(UnstyledLightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            checkNamesAreTheLabelsAlone(
                names = listOf("Overview", "Free", "Rename", "Newest", "Options", "Shipping details", "Favourite"),
                allowed = setOf(listOf("Sort by, Newest"), listOf("Group by, Folder")),
            )
        }

    @Test
    fun inspect_clickOnAToggle_pinsItsTokensAndNeverReachesTheGallery() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                InspectingPane {
                    PreviewPane(UnstyledLightSpec, Modifier.fillMaxSize()) { ComponentsTab(UnstyledLightSpec, state) }
                }
            }
            waitForIdle()
            onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Toggle button"))
            waitForIdle()
            val snapshot = { state.gallerySnapshot(GalleryChoices, GallerySwitches, listOf(GalleryKeys.Newsletter)) }

            checkInspectPins(
                target = onNode(galleryNamed("Favourite") and isEnabled(), useUnmergedTree = true),
                tokens = listOf(
                    hasText(UnstyledToken.Outline.token.name),
                    hasText(UnstyledToken.OnSurfaceVariant.token.name),
                ),
                snapshot = snapshot,
            )
            checkInspectCardSwallowsPress(snapshot)
        }
}

/**
 * What the web mirror hears from one control of each kind, with the gallery as it first shows.
 */
private val WebNames: List<String> = listOf(
    "Overview, tab, selected",
    "Activity, tab, not selected",
    "Settings, tab, not selected, disabled",
    "Free, radio, selected",
    "Enterprise, radio, not selected, disabled",
    "Rename, menu item",
    "Archive, menu item, disabled",
    "Options, expanded",
    "Options, collapsed, disabled",
    "Sort by, Newest, expanded",
    "Newest, option, selected",
    "Size, option, not selected, disabled",
    "Favourite, checkbox, not checked",
    "Email me the newsletter, checkbox, not checked",
    "Keep a copy on this device, checkbox, checked, disabled",
    "Wi-Fi, switch, off",
    "Airplane mode, switch, on, disabled",
    "Home, selected",
    "Team, not selected, disabled",
    "Shipping details, collapsed",
)

/**
 * What the web mirror hears from the slider and the progress bars, which play no role of their own
 * and so read their name, role word and value as text.
 */
private val WebValueNames: List<String> = listOf(
    "Volume, slider, 6",
    "Uploading, progress bar, 40%",
    "Exporting, progress bar, 75%",
)

/**
 * Whether this import opens a popup, a window or a portal, is Compose Unstyled's own field, which
 * keeps its inner text to itself, or comes from the kit and none of [kitImports].
 */
internal fun String.isBannedInUnstyled(kitImports: List<String>): Boolean {
    val name = substringAfterLast('.')
    return opensAWindow() ||
        UnstyledPopupWords.any { word -> word in name } ||
        (startsWith("com.composeunstyled.") && "TextField" in name) ||
        isKitImportBeyond(kitImports)
}

/**
 * The Unstyled gallery in a pane of [spec], under the shell chrome, with motion frozen. With
 * [webFolds] the kit's fold modifiers fold state into names as they do on the web.
 */
@Composable
private fun UnstyledGalleryHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
    webFolds: Boolean = false,
) = GalleryHarness(spec, state, modifier, composed, expressive = ShellExpressive, webFolds = webFolds)
