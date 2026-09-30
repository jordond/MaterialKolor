package com.materialkolor.builder.feature.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.AppHarness
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.domain.link.RoutePath
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.fakes.FakeRouter
import com.materialkolor.builder.feature.projects.ProjectsModel
import com.materialkolor.builder.kit.a11y.Announcer
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

private const val CONFLICT = "This project changed in another tab"
private const val INVALID_LINK = "That link didn’t work, so your last theme is open instead."
private const val INVALID_LINK_DEFAULTS = "That link didn’t work, so you’re starting from the defaults."
private const val UNKNOWN_PATH = "There’s no page at that address, so here’s the builder."
private const val NEWER_VERSION = "Made with a newer version. Reload to update."
private const val STORAGE_FULL =
    "This browser’s storage is full, so your latest changes aren’t saved. Delete a project you don’t need, " +
        "or get a link to keep this theme."
private const val STORAGE_UNAVAILABLE =
    "This browser isn’t keeping anything, so your work won’t be saved here. Share a link to keep it."
private const val UNSAVED_THEME = "This theme isn’t in your projects yet"
private const val NEWER_DATA =
    "A newer version of the builder saved some of your work here. Reload the page to pick it up."

/**
 * One banner, what it says, and what each of its buttons asks for, found by label or, for a button
 * that only has an icon, by its description.
 */
private data class BannerCase(
    val banner: WorkspaceBanner,
    val message: String,
    val buttons: List<Pair<String, BannerAction>>,
    val absent: List<String> = emptyList(),
)

private val CASES = listOf(
    BannerCase(
        WorkspaceBanner.Conflict,
        CONFLICT,
        listOf(
            "Load latest" to BannerAction.ResolveConflict(keepMine = false),
            "Keep mine" to BannerAction.ResolveConflict(keepMine = true),
        ),
    ),
    BannerCase(
        WorkspaceBanner.InvalidLink,
        INVALID_LINK,
        listOf("Start from defaults" to BannerAction.StartFromDefaults, "Dismiss" to BannerAction.DismissBootNotice),
    ),
    BannerCase(
        WorkspaceBanner.InvalidLinkDefaults,
        INVALID_LINK_DEFAULTS,
        listOf("Dismiss" to BannerAction.DismissBootNotice),
        absent = listOf("Start from defaults"),
    ),
    BannerCase(
        WorkspaceBanner.UnknownPath,
        UNKNOWN_PATH,
        listOf("Dismiss" to BannerAction.DismissBootNotice),
    ),
    BannerCase(
        WorkspaceBanner.NewerVersion,
        NEWER_VERSION,
        listOf("Reload" to BannerAction.ReloadLink, "Dismiss" to BannerAction.DismissBootNotice),
    ),
    BannerCase(
        WorkspaceBanner.StorageFull,
        STORAGE_FULL,
        listOf("Projects" to BannerAction.OpenProjects, "Get a link" to BannerAction.GetLink),
    ),
    BannerCase(
        WorkspaceBanner.StorageUnavailable,
        STORAGE_UNAVAILABLE,
        listOf("Get a link" to BannerAction.GetLink, "Close" to BannerAction.CloseStorageUnavailable),
    ),
    BannerCase(
        WorkspaceBanner.UnsavedTheme,
        UNSAVED_THEME,
        listOf("Save to my projects" to BannerAction.SaveTheme),
    ),
    BannerCase(
        WorkspaceBanner.NewerData,
        NEWER_DATA,
        listOf("Reload" to BannerAction.ReloadHome),
        absent = listOf("Dismiss"),
    ),
)

/**
 * The banners that ask for a reload, where a reload does nothing.
 */
private val NO_RELOAD_CASES = listOf(
    BannerCase(
        WorkspaceBanner.NewerVersion,
        NEWER_VERSION,
        listOf("Dismiss" to BannerAction.DismissBootNotice),
        absent = listOf("Reload"),
    ),
    BannerCase(
        WorkspaceBanner.NewerData,
        NEWER_DATA,
        listOf("Dismiss" to BannerAction.DismissNewerData),
        absent = listOf("Reload"),
    ),
)

@OptIn(ExperimentalTestApi::class)
class WorkspaceBannersTest {
    private val app = AppHarness()

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun cases_coverEveryBanner() {
        CASES.map { case -> case.banner } shouldBe WorkspaceBanner.entries
    }

    @Test
    fun eachBanner_saysItsMessageAndItsButtonsAskForTheirActions() =
        runComposeUiTest {
            assertCases(CASES, canReload = true)
        }

    @Test
    fun withoutReload_theNewerBannersSayTheirTextAndOfferDismissOnly() =
        runComposeUiTest {
            assertCases(NO_RELOAD_CASES, canReload = false)
        }

    @Test
    fun everythingAtOnce_stacksTopDownInOrder() =
        runComposeUiTest {
            val banners = listOf(
                WorkspaceBanner.Conflict,
                WorkspaceBanner.NewerVersion,
                WorkspaceBanner.StorageFull,
                WorkspaceBanner.StorageUnavailable,
                WorkspaceBanner.UnsavedTheme,
                WorkspaceBanner.NewerData,
            )
            setContent {
                Themed { BannerStack(banners = banners, canReload = true, onAction = {}) }
            }
            waitForIdle()

            val tops = banners.map { banner ->
                val message = CASES.first { case -> case.banner == banner }.message
                onNodeWithText(message).fetchSemanticsNode().boundsInRoot.top
            }

            tops shouldBe tops.sorted()
            tops.distinct().size shouldBe tops.size
        }

    @Test
    fun aBannerShowingUp_leavesFocusWhereItWas() =
        runComposeUiTest {
            var shown by mutableStateOf(emptyList<WorkspaceBanner>())
            val focus = FocusRequester()
            setContent {
                Themed {
                    Box(Modifier.fillMaxSize()) {
                        BuilderButton(onClick = {}, label = "Elsewhere", modifier = Modifier.focusRequester(focus))
                        BannerStack(banners = shown, canReload = true, onAction = {})
                    }
                }
            }
            waitForIdle()
            runOnIdle { focus.requestFocus() }
            onNodeWithText("Elsewhere").assertIsFocused()

            shown = listOf(WorkspaceBanner.Conflict, WorkspaceBanner.InvalidLink)
            waitForIdle()

            onNodeWithText(CONFLICT).assertExists()
            onNodeWithText("Elsewhere").assertIsFocused()
        }

    @Test
    fun aBannerShowingUp_isReadOutOnceInTheOrderItCame() =
        runComposeUiTest {
            var shown by mutableStateOf(emptyList<WorkspaceBanner>())
            var inset by mutableStateOf(0.dp)
            val announced = mutableListOf<String>()
            setContent {
                Themed {
                    CompositionLocalProvider(LocalAnnouncer provides Announcer { message -> announced += message }) {
                        BannerStack(
                            banners = shown,
                            canReload = true,
                            onAction = {},
                            modifier = Modifier.padding(inset),
                        )
                    }
                }
            }
            waitForIdle()

            shown = listOf(WorkspaceBanner.Conflict)
            waitForIdle()
            shown = listOf(WorkspaceBanner.Conflict, WorkspaceBanner.StorageFull)
            waitForIdle()
            announced shouldBe listOf(CONFLICT, STORAGE_FULL)

            // A recomposition and a move in the stack read nothing new.
            inset = 1.dp
            waitForIdle()
            shown = listOf(WorkspaceBanner.StorageFull, WorkspaceBanner.Conflict)
            waitForIdle()
            announced shouldBe listOf(CONFLICT, STORAGE_FULL)

            // A banner that goes and comes back is read out again.
            shown = listOf(WorkspaceBanner.StorageFull)
            waitForIdle()
            shown = listOf(WorkspaceBanner.Conflict, WorkspaceBanner.StorageFull)
            waitForIdle()
            announced shouldBe listOf(CONFLICT, STORAGE_FULL, CONFLICT)
        }

    @Test
    fun conflictBanner_eachButton_settlesTheClashItsOwnWay() =
        runComposeUiTest {
            val actions = mutableListOf<BannerAction>()
            showBanners(ProjectsModel.State(conflict = true), actions)

            onNodeWithText("Load latest").performClick()
            waitForIdle()
            onNodeWithText("Keep mine").performClick()
            waitForIdle()

            actions shouldBe listOf(
                BannerAction.ResolveConflict(keepMine = false),
                BannerAction.ResolveConflict(keepMine = true),
            )
        }

    @Test
    fun saveBanner_savesOnceAndGoesOnceTheThemeIsSaved() =
        runComposeUiTest {
            var projects by mutableStateOf(ProjectsModel.State(open = ProjectRef.Transient("code")))
            val actions = mutableListOf<BannerAction>()
            setContent {
                Themed {
                    WorkspaceBanners(
                        app = appState(),
                        projects = projects,
                        onAction = { action ->
                            actions += action
                            val saved = ProjectRef.Persisted("p1")
                            if (action == BannerAction.SaveTheme) projects = projects.copy(open = saved)
                        },
                    )
                }
            }
            waitForIdle()
            onNodeWithText(UNSAVED_THEME).assertExists()

            onNodeWithText("Save to my projects").performClick()
            waitForIdle()

            actions shouldBe listOf(BannerAction.SaveTheme)
            onAllNodes(hasText("Save to my projects")).assertCountEquals(0)
        }

    @Test
    fun newerDataBanner_asksForAReload() =
        runComposeUiTest {
            showBanners(ProjectsModel.State(newerData = true))

            onNode(hasText("A newer version of the builder", substring = true)).assertExists()
            onNodeWithText("Reload").assertExists()
        }

    @Test
    fun root_unknownPath_saysSoOnceAndDismissPutsItAway() =
        runComposeUiTest {
            showRoot(FakePlatform(router = FakeRouter(RoutePath.parse("/nope", query = ""))))
            waitUntil(
                timeoutMillis = WAIT_MILLIS,
            ) { onAllNodes(hasText(UNKNOWN_PATH)).fetchSemanticsNodes().isNotEmpty() }
            onAllNodes(hasText(UNKNOWN_PATH)).assertCountEquals(1)

            onNodeWithText("Dismiss").performClick()
            waitForIdle()

            onAllNodes(hasText(UNKNOWN_PATH)).assertCountEquals(0)
        }

    @Test
    fun root_sharedLink_showsTheSaveBannerOnceNowTheDrawerNoLongerDrawsIt() =
        runComposeUiTest {
            val code = ShareCodec.encode(ThemeDocument.Default.copy(themeName = "Harbour"))
            showRoot(FakePlatform(router = FakeRouter(RoutePath.parse("/t/$code", query = ""))))
            waitUntil(
                timeoutMillis = WAIT_MILLIS,
            ) { onAllNodes(hasText(UNSAVED_THEME)).fetchSemanticsNodes().isNotEmpty() }

            onAllNodes(hasText(UNSAVED_THEME)).assertCountEquals(1)
        }

    @Test
    fun root_storageUnavailable_closesForTheSession() =
        runComposeUiTest {
            val platform = FakePlatform()
            platform.environment.storageAvailable = false
            showRoot(platform)
            waitUntil(
                timeoutMillis = WAIT_MILLIS,
            ) { onAllNodes(hasText(STORAGE_UNAVAILABLE)).fetchSemanticsNodes().isNotEmpty() }

            onNodeWithContentDescription("Close").performClick()
            waitForIdle()

            onAllNodes(hasText(STORAGE_UNAVAILABLE)).assertCountEquals(0)
        }

    private fun ComposeUiTest.button(label: String) =
        if (label == "Close") onNodeWithContentDescription(label) else onNodeWithText(label)

    /**
     * Shows each of [cases] alone and checks what it says, what it leaves out and what its buttons ask for.
     */
    private fun ComposeUiTest.assertCases(
        cases: List<BannerCase>,
        canReload: Boolean,
    ) {
        var shown by mutableStateOf(emptyList<WorkspaceBanner>())
        val actions = mutableListOf<BannerAction>()
        setContent {
            Themed { BannerStack(banners = shown, canReload = canReload, onAction = { action -> actions += action }) }
        }
        cases.forEach { case ->
            withClue(case.banner.name) {
                shown = listOf(case.banner)
                actions.clear()
                waitForIdle()

                onNodeWithText(case.message).assertExists()
                case.absent.forEach { label -> onAllNodes(hasText(label)).assertCountEquals(0) }
                case.buttons.forEach { (label, _) ->
                    button(label).performClick()
                    waitForIdle()
                }

                actions shouldBe case.buttons.map { (_, action) -> action }
            }
        }
    }

    /**
     * Shows the stack [projects] raises beside an app with nothing to say, through [WorkspaceBanners].
     */
    private fun ComposeUiTest.showBanners(
        projects: ProjectsModel.State,
        actions: MutableList<BannerAction> = mutableListOf(),
    ) {
        setContent {
            Themed {
                WorkspaceBanners(app = appState(), projects = projects, onAction = { action -> actions += action })
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.showRoot(platform: FakePlatform) {
        with(app) { bootRoot(platform = platform) }
    }
}

/**
 * An app state with nothing to say, on a platform where a reload works.
 */
private fun appState(): AppModel.State =
    AppModel.State(
        appearance = Appearance.System,
        motion = MotionOverride.System,
        systemDark = false,
        systemReducedMotion = false,
        coarsePointer = false,
        canReload = true,
    )

@Composable
private fun Themed(content: @Composable () -> Unit) {
    val document = ThemeDocument.Default
    val result = remember { ThemeResolver().resolve(document) }
    BuilderTheme(
        expressive = false,
        result = result,
        isDark = false,
        reducedMotion = true,
    ) {
        ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) { content() }
    }
}
