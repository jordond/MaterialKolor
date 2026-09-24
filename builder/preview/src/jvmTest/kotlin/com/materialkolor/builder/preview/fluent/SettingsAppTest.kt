package com.materialkolor.builder.preview.fluent

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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.inspect.firstRated
import com.materialkolor.builder.preview.split.SplitState
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * Where a recording job would write the screenshots. Nothing is written unless a Roborazzi task
 * turns capture on, and the preview keeps no baselines.
 */
private const val FluentScreenshotDir = "src/jvmTest/screenshots/fluent"

private const val FluentSourceDir = "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent"

/** The files of the Settings app, which the gallery's sources are not. */
private val SettingsSources: Set<String> =
    setOf("SettingsApp.kt", "SettingsData.kt", "ShadeMapping.kt", "FluentRoles.kt", "AppEntry.kt")

/** What the Settings app's sources never name, anything that opens outside the layout or an endless clock. */
private val SettingsBannedWords: List<String> = listOf(
    "androidx.compose.ui.window",
    "Popup",
    "Dialog",
    // Only the theme, never the one that adds a host and a backdrop round the screen.
    "FluentTheme(",
    // Stems, so the architecture scan does not read this list as an endless animation.
    "rememberInfinite",
    "infiniteRepeat",
)

/** What the web mirror hears from one control of each kind, with the app as it first shows on a tablet. */
private val SettingsWebNames: List<String> = listOf(
    "Transparency effects, switch, on",
    "Show badges on taskbar apps, switch, on",
    "Accent color, collapsed",
    "Taskbar behaviors, collapsed",
    "Personalization, selected",
    "Home, not selected",
    "Accent shades, checkbox, not checked",
)

/** The switch of a setting that starts on. */
private val TransparencySwitch: SemanticsMatcher =
    isToggleable() and hasContentDescription(FluentSetting.Transparency.title)

/** The header of the accent color group. */
private val AccentGroupHeader: SemanticsMatcher =
    hasClickAction() and hasContentDescription(FluentGroup.AccentColor.title)

@OptIn(ExperimentalTestApi::class)
class SettingsAppTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnRoles() {
        for ((width, frame) in FluentFrames) {
            withClue(width) {
                runComposeUiTest {
                    setContent {
                        FluentHarness(
                            spec = FluentLightSpec,
                            state = everythingOpen(),
                            width = width,
                            // Tall enough that every lazy item composes, wider than the window on desktop.
                            modifier = Modifier
                                .wrapContentSize(Alignment.TopStart, unbounded = true)
                                .requiredSize(frame.width.dp, 2400.dp),
                        )
                    }

                    onNode(hasContentDescription(FluentSetting.Flashing.title), useUnmergedTree = true).assertExists()
                    // Unmerged, since a merged node also carries the roles its children declared. The
                    // navigation layer of compose-fluent 0.1.0 keeps its click action. It wraps the nav
                    // items, so nothing outside it can clear its semantics and keep theirs. The app
                    // keeps it out of focus, which the Tab test below checks.
                    val controls = onAllNodes(hasClickAction() or hasSetTextAction(), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .filterNot { node -> (NavigationShield or ScrollbarArrow or UnderAnOverlay).matches(node) }
                    controls.shouldNotBeEmpty()
                    controls
                        .filter { node -> PreviewRoles !in node.config }
                        .map { node -> "${node.boundsInRoot} ${node.config}" }
                        .shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun shades_everyDeviceWidthFirstScreen_showTheAccentFillAndAllSevenWithTheLegend() {
        for ((width, frame) in FluentFrames) {
            for (spec in listOf(FluentLightSpec, FluentDarkSpec)) {
                withClue("$width ${spec.label}") {
                    runDesktopComposeUiTest(frame.width, frame.height) {
                        val state = DemoAppState()
                        setContent { FluentHarness(spec, state, width, Modifier.fillMaxSize()) }

                        // The real controls paint the accent fill, and no other shade is theirs to declare.
                        shadesOnScreen(spec.isDark) shouldBe setOf(fillAccentShade(spec.isDark))

                        state.setOn(FluentShadesSwitch, true)
                        waitForIdle()
                        shadesOnScreen(spec.isDark) shouldBe FluentShade.entries.toSet()
                    }
                }
            }
        }
    }

    @Test
    fun accentControls_switchOnAndGroupHeader_resolveToARowOfTheAudit() =
        runComposeUiTest {
            setContent {
                FluentHarness(
                    FluentLightSpec,
                    DemoAppState(),
                    DeviceWidth.Desktop,
                    Modifier.size(1280.dp, 800.dp),
                )
            }

            for (control in listOf(TransparencySwitch and isOn(), AccentGroupHeader)) {
                val refs = onNode(control, useUnmergedTree = true).fetchSemanticsNode().config[PreviewRoles]
                refs shouldBe FluentAccentRefs
                for (isDark in listOf(false, true)) {
                    withClue("$refs dark $isDark") {
                        FluentLightSpec.result.audit
                            .firstRated(refs, isDark)
                            .shouldNotBeNull()
                    }
                }
            }
        }

    @Test
    fun screens_everyDeviceWidthBothModes_layOutForTheWidthAndRender() {
        for ((width, frame) in FluentFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    var spec by mutableStateOf(FluentLightSpec)
                    setContent { FluentHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    for (mode in listOf(FluentLightSpec, FluentDarkSpec)) {
                        spec = mode
                        waitForIdle()
                        val title = onNodeWithText(FluentStartPage.label).fetchSemanticsNode().boundsInRoot
                        val pages = onAllNodes(hasContentDescription(FluentPage.Home.label), useUnmergedTree = true)
                            .fetchSemanticsNodes()
                        if (width == DeviceWidth.Phone) {
                            pages.shouldBeEmpty()
                        } else {
                            title.left shouldBeGreaterThan pages.single().boundsInRoot.right
                        }
                        onRoot().captureRoboImage("$FluentScreenshotDir/app-${width.name}-${mode.label}.png")
                    }
                }
            }
        }
    }

    @Test
    fun tab_backThroughTheTabletRailAndThePhoneMenu_stopsOnEveryPageButNeverTheLibraryLayer() {
        for (width in listOf(DeviceWidth.Tablet, DeviceWidth.Phone)) {
            withClue(width) {
                runComposeUiTest {
                    val frame = FluentFrames.getValue(width)
                    val state = DemoAppState().apply { setOn(FluentMenuSwitch, true) }
                    lateinit var focus: FocusManager
                    setContent {
                        focus = LocalFocusManager.current
                        FluentHarness(FluentLightSpec, state, width, Modifier.size(frame.width.dp, frame.height.dp))
                    }
                    waitForIdle()

                    // The rail and the menu come last, so going back from the end reaches them first.
                    val stops = List(FluentPage.entries.size + 4) {
                        runOnIdle { focus.moveFocus(FocusDirection.Previous) }
                        waitForIdle()
                        onAllNodes(isFocused(), useUnmergedTree = true).fetchSemanticsNodes().single()
                    }

                    stops.filter { node -> NavigationShield.matches(node) }.map { node -> "${node.config}" }.shouldBeEmpty()
                    stops
                        .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.ContentDescription) }
                        .flatten() shouldContainAll FluentPage.entries.map { page -> page.label }
                }
            }
        }
    }

    @Test
    fun phoneMenu_openPickAndClose_changeTheSharedState() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { FluentHarness(FluentLightSpec, state, DeviceWidth.Phone, Modifier.size(412.dp, 900.dp)) }

            onNode(hasClickAction() and hasContentDescription(FluentCopy.Menu)).performClick()
            waitForIdle()
            state.isOn(FluentMenuSwitch) shouldBe true

            onNode(hasClickAction() and hasContentDescription(FluentPage.Apps.label)).performClick()
            waitForIdle()
            state.fluentPage() shouldBe FluentPage.Apps
            state.isOn(FluentMenuSwitch) shouldBe false
            onNodeWithText(FluentPage.Apps.label).assertExists()
        }

    @Test
    fun settings_switchAndGroup_flipTheSharedState() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { FluentHarness(FluentLightSpec, state, DeviceWidth.Tablet, Modifier.size(840.dp, 900.dp)) }
            state.isOn(FluentSetting.Transparency) shouldBe true

            onNode(TransparencySwitch).performClick()
            waitForIdle()
            state.isOn(FluentSetting.Transparency) shouldBe false

            onNode(AccentGroupHeader).performClick()
            waitForIdle()
            state.isOn(FluentGroup.AccentColor.key) shouldBe true
            onNode(isToggleable() and hasContentDescription(FluentSetting.AccentOnStart.title)).performClick()
            waitForIdle()
            state.isOn(FluentSetting.AccentOnStart) shouldBe true
        }

    @Test
    fun inspect_clickOnASwitch_pinsItsColorsAndNeverReachesTheApp() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Inspecting(shown = PreviewMode.Light, split = remember { SplitState() }, skin = FluentSkin) {
                        PreviewPane(FluentLightSpec, Modifier.fillMaxSize()) {
                            FluentAppEntry(FluentLightSpec, state, DeviceWidth.Phone)
                        }
                    }
                }
            }
            waitForIdle()
            val before = state.fluentSnapshot()

            onNode(TransparencySwitch).performClick()
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
            onNode(OnCard and hasText("onAccentPrimary", substring = true)).assertExists()
            onNode(OnCard and hasText("primary", substring = true)).assertExists()
            state.fluentSnapshot() shouldBe before
        }

    @OptIn(KitTestApi::class)
    @Test
    fun names_onTheWeb_foldTheStateOfEveryKindOfControl() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Chrome(FluentSkin) {
                        ProvideWebFoldsForTest {
                            PreviewPane(FluentLightSpec, Modifier.size(840.dp, 900.dp)) {
                                FluentAppEntry(FluentLightSpec, state, DeviceWidth.Tablet)
                            }
                        }
                    }
                }
            }
            waitForIdle()

            for (name in SettingsWebNames) withClue(name) { onNode(hasContentDescription(name)).assertExists() }

            onNode(hasContentDescription("Accent color, collapsed")).performSemanticsAction(SemanticsActions.OnClick)
            onNode(hasContentDescription("Transparency effects, switch, on"))
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            for (name in listOf(
                "Accent color, expanded",
                "Show accent color on Start and taskbar, switch, off",
                "Transparency effects, switch, off",
            )) {
                withClue(name) { onNode(hasContentDescription(name)).assertExists() }
            }
        }

    @Test
    fun settingsSources_openNothingOutsideTheLayoutAndNeverLoop() {
        val sources = File(FluentSourceDir)
            .listFiles()
            .orEmpty()
            .filter { file -> file.name in SettingsSources }
        sources.size shouldBe SettingsSources.size
        for (source in sources) {
            withClue(source.name) {
                val text = source.readText()
                SettingsBannedWords.filter { word -> word in text }.shouldBeEmpty()
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
        sources.sumOf { source -> source.readLines().size } shouldBeGreaterThanOrEqual 300
    }
}

/** Every shade the elements declared on the first screen paint, in the mode [isDark] picks. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.shadesOnScreen(isDark: Boolean): Set<FluentShade> {
    val screen = onRoot().fetchSemanticsNode().boundsInRoot
    return onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { node -> node.boundsInRoot.overlaps(screen) }
        .flatMap { node -> node.config[PreviewRoles] }
        .mapNotNull { ref: ColorRef -> ref.paintedShade(isDark) }
        .toSet()
}
