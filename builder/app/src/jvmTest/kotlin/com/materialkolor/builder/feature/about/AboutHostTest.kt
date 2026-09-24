package com.materialkolor.builder.feature.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuildKonfig
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.link.shareLink
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FAKE_BROWSER
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import com.materialkolor.builder.feature.poster.InfoTopic
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.ShippedFont
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

// At 1280 the top bar runs out of room before More options, which gets no width at all.
// TODO(b-231) Back to 1280 wide once B-231 fixes the top bar at 1280 dp.
private const val WIDTH = 1600
private const val HEIGHT = 800

// b-314a

/** The first line of both OFL texts, under the copyright lines. */
private const val OFL_HEADING = "SIL OPEN FONT LICENSE Version 1.1"

/** The line About opens with. */
private val BUILDER_VERSION_LINE = "Builder ${BuildKonfig.BUILDER_VERSION}"

/**
 * The kit's switch for the web's keyboard habits. It is internal to the kit and off on the JVM, so
 * this reaches it by name to turn it on for the whole builder.
 */
@Suppress("UNCHECKED_CAST")
private val LocalWebKeyboardOfTheKit: ProvidableCompositionLocal<Boolean> =
    Class
        .forName("com.materialkolor.builder.kit.a11y.WebMirrorKt")
        .getMethod("getLocalWebKeyboard")
        .invoke(null) as ProvidableCompositionLocal<Boolean>

// b-314
@OptIn(ExperimentalTestApi::class)
class AboutHostTest {
    private val platform = FakePlatform()
    private val opened = mutableListOf<String>()
    private var reducedMotion: Boolean? = null
    private lateinit var workspace: WorkspaceModel

    @Test
    fun about_opened_namesBothVersions() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()

            openFromMore("About")

            onNodeWithText("Builder ${BuildKonfig.BUILDER_VERSION}").assertExists()
            onNodeWithText("MaterialKolor ${BuildKonfig.MATERIAL_KOLOR_VERSION}").assertExists()
        }

    // b-314a
    @Test
    fun about_withTheWebKeyboard_listIsATabStop() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot(webKeyboard = true)

            openFromMore("About")

            val aboutList = hasAnyChild(hasScrollAction() and hasAnyDescendant(hasText(BUILDER_VERSION_LINE)))
            onNode(aboutList).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
        }

    @Test
    fun fontLicense_opened_showsTheFullText() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            openFromMore("About")

            onNodeWithText("Bricolage Grotesque").performScrollTo().performClick()
            waitUntil { onAllNodesWithText(OFL_HEADING, substring = true).fetchSemanticsNodes().isNotEmpty() }

            onNodeWithText("Copyright 2022 The Bricolage Grotesque Project Authors", substring = true).assertExists()
        }

    @Test
    fun fontLicense_whenTheReadFails_saysSoWithTheLicenseName() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val document = ThemeDocument.Default
            setContent {
                BuilderTheme(
                    skin = Skin(library = Library.Material3, expressive = false),
                    result = ThemeResolver().resolve(document),
                    isDark = false,
                    reducedMotion = true,
                ) {
                    ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                        FontLicense(ShippedFont.JetBrainsMono, read = { error("The fetch failed") })
                    }
                }
            }

            onNodeWithText("JetBrains Mono").performClick()
            waitForIdle()

            onNodeWithText("Could not load the license text of the SIL Open Font License 1.1.").assertExists()
            onNodeWithText(OFL_HEADING, substring = true).assertDoesNotExist()
        }

    @Test
    fun about_closedWithEsc_handsFocusBackToMore() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            openFromMore("About")

            escapeFromTheOverlay()

            workspace.state.value.panel shouldBe null
            onNodeWithContentDescription("More options").assertIsFocused()
        }

    @Test
    fun help_opened_listsEveryTopicAndNoDocsButton() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()

            openFromMore("Help")

            val questions = runBlocking { InfoTopic.entries.map { topic -> getString(topic.question) } }
            questions shouldHaveSize 8
            questions.forEach { question -> onNodeWithText(question).assertExists() }
            onNodeWithText("Open the help pages").assertDoesNotExist()
            opened shouldBe emptyList()
        }

    @Test
    fun help_closedWithEsc_handsFocusBackToMore() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            openFromMore("Help")

            escapeFromTheOverlay()

            workspace.state.value.panel shouldBe null
            onNodeWithContentDescription("More options").assertIsFocused()
        }

    @Test
    fun motion_eachChoice_setsReducedMotionAndSystemFollowsTheBrowserLive() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            openFromMore("About")

            reducedMotion shouldBe false
            chooseMotion("Reduce", MotionOverride.Reduce, reduced = true)

            platform.environment.reducedMotion.value = true
            chooseMotion("Full", MotionOverride.Full, reduced = false)

            chooseMotion("System", MotionOverride.System, reduced = true)
            platform.environment.reducedMotion.value = false
            waitUntil { reducedMotion == false }
            platform.environment.reducedMotion.value = true
            waitUntil { reducedMotion == true }
        }

    @Test
    fun copyDetails_whenTheClipboardRefuses_opensTheManualCopyDialog() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.clipboard.failure = IllegalStateException("No user activation")
            showRoot()
            openFromMore("About")

            onNodeWithText("Copy details").performScrollTo().performClick()
            waitForIdle()

            onNodeWithText("Copy it yourself").assertExists()
            onNodeWithText(FAKE_BROWSER, substring = true).assertExists()
            platform.clipboard.texts shouldBe emptyList()
        }

    @Test
    fun copyDetails_copiesTheVersionsTheBrowserAndTheThemeLinkWithoutTheProjectName() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()
            openFromMore("About")

            onNodeWithText("Copy details").performScrollTo().performClick()
            waitForIdle()

            val link = shareLink(graph.session.document.value, projectName = "")
            platform.clipboard.text shouldBe ReportDetails(
                builderVersion = BuildKonfig.BUILDER_VERSION,
                materialKolorVersion = BuildKonfig.MATERIAL_KOLOR_VERSION,
                browser = FAKE_BROWSER,
                themeLink = link,
            ).text()
        }

    @Test
    fun reportAProblem_opensANewIssueWithTheDetails() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            openFromMore("About")

            onNodeWithText("Report a problem").performScrollTo().performClick()
            waitForIdle()

            opened shouldHaveSize 1
            opened.single() shouldStartWith "$ISSUES_URL/new?title=&body="
            opened.single() shouldContain percentEncode(FAKE_BROWSER)
        }

    private fun ComposeUiTest.openFromMore(item: String) {
        onNodeWithContentDescription("More options").performClick()
        waitForIdle()
        onNodeWithText(item).performClick()
        waitForIdle()
    }

    /** Choose [label] in About's motion row, and wait until the chrome draws [reduced] motion. */
    private fun ComposeUiTest.chooseMotion(
        label: String,
        motion: MotionOverride,
        reduced: Boolean,
    ) {
        onNodeWithText(label).performScrollTo().performClick()
        waitUntil { workspace.state.value.preferences.motion == motion && reducedMotion == reduced }
        waitForIdle()
        reducedMotion shouldBe reduced
    }

    private fun ComposeUiTest.escapeFromTheOverlay() {
        onAllNodes(isFocused()).onLast().performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
    }

    /**
     * The whole builder on fakes, booted, noting the reduced motion it draws with and every link it
     * opens. With [webKeyboard] it keeps to the web's keyboard habits, the way it does in a browser.
     */
    private fun ComposeUiTest.showRoot(webKeyboard: Boolean = false): AppGraph {
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                LocalUriHandler provides uriHandler,
                LocalWebKeyboardOfTheKit provides webKeyboard, // b-314a
            ) {
                workspace = metroViewModel()
                BuilderRoot(graph, workspaceModel = workspace, probe = { _ -> NoteReducedMotion() })
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }

    @Composable
    private fun NoteReducedMotion() {
        reducedMotion = LocalReducedMotion.current
    }
}
