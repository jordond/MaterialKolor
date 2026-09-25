package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.feature.workspace.skinOf
import com.materialkolor.builder.kit.control.BottomSheetDetent
import com.materialkolor.builder.kit.control.BottomSheetState
import com.materialkolor.builder.kit.control.rememberBottomSheetState
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.shell.WorkspaceShell
import com.materialkolor.builder.kit.skin.BuilderTheme
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

/**
 * A phone, upright or on its side.
 */
private const val PHONE_SHORT = 390
private const val PHONE_LONG = 844

private val Peek = listOf(
    SheetSection.SeedPeek,
    SheetSection.SeedActions,
    SheetSection.FirstRunHint,
    SheetSection.ImageCandidates,
    SheetSection.StyleChips,
    SheetSection.Contrast,
)

private val Half = Peek +
    listOf(
        SheetSection.ContrastDetails,
        SheetSection.StyleDetails,
        SheetSection.Explainer,
        SheetSection.FineTune,
    )
private val Full = Half + listOf(SheetSection.Hero, SheetSection.Header)

@OptIn(ExperimentalTestApi::class)
class PosterSheetTest {
    @Test
    fun inView_uprightPhone_eachDetentAddsItsSections() {
        sheetSectionsInView(BottomSheetDetent.Peek, short = false) shouldBe Peek
        sheetSectionsInView(BottomSheetDetent.Half, short = false) shouldBe Half
        sheetSectionsInView(BottomSheetDetent.Full, short = false) shouldBe Full
    }

    @Test
    fun inView_phoneOnItsSide_peekIsTheSeedRowAlone() {
        sheetSectionsInView(BottomSheetDetent.Peek, short = true) shouldBe listOf(SheetSection.SeedPeek)
        sheetSectionsInView(BottomSheetDetent.Half, short = true) shouldBe Half
        sheetSectionsInView(BottomSheetDetent.Full, short = true) shouldBe Full
    }

    @Test
    fun sheet_uprightPhone_followsTheSheetFromPeekToFull() =
        runDesktopComposeUiTest(width = PHONE_SHORT, height = PHONE_LONG) {
            val (sheet, view) = showSheet()

            view().detent shouldBe BottomSheetDetent.Peek
            view().inView shouldBe Peek
            snap(sheet, BottomSheetDetent.Half)
            view().detent shouldBe BottomSheetDetent.Half
            view().inView shouldBe Half
            snap(sheet, BottomSheetDetent.Full)
            view().inView shouldBe Full
        }

    @Test
    fun sheet_uprightPhone_laysTheSectionsOutInTheirOrder() =
        runDesktopComposeUiTest(width = PHONE_SHORT, height = PHONE_LONG) {
            showSheet()

            val shuffle = top(hasContentDescription("Shuffle"))
            val pick = top(hasText("Pick"))
            val header = top(hasText("MaterialKolor"))
            shuffle shouldBeLessThan pick
            pick shouldBeLessThan header
        }

    @Test
    fun sheet_phoneOnItsSide_peeksAtTheSeedRowAndShuffleAlone() =
        runDesktopComposeUiTest(width = PHONE_LONG, height = PHONE_SHORT) {
            val (sheet, view) = showSheet()

            view().short shouldBe true
            view().inView shouldBe listOf(SheetSection.SeedPeek)
            snap(sheet, BottomSheetDetent.Half)
            view().inView shouldBe Half
        }

    private fun ComposeUiTest.snap(
        sheet: BottomSheetState,
        detent: BottomSheetDetent,
    ) {
        runOnUiThread { runBlocking { sheet.snapTo(detent) } }
        waitForIdle()
    }

    /**
     * Where the first node [matcher] finds starts down the sheet, unclipped.
     */
    private fun ComposeUiTest.top(matcher: SemanticsMatcher): Float =
        onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().minOf { node -> node.positionInRoot.y }

    /**
     * The poster in the workspace's sheet on a touch screen, with the sheet it rests in and the
     * sheet's view as the poster last composed it.
     */
    private fun ComposeUiTest.showSheet(): Pair<BottomSheetState, () -> PosterSheetView> {
        val document = ThemeDocument.Default
        val result = ThemeResolver().resolve(document)
        val state = WorkspaceModel.State(
            document = document,
            capabilities = capabilitiesOf(document),
            history = HistoryState(),
            view = ProjectViewState(),
            preferences = Preferences(),
        )
        var sheet: BottomSheetState? = null
        var view: PosterSheetView? = null
        setContent {
            val dispatcher = rememberDispatcher<WorkspaceAction> {}
            val sheetState = rememberBottomSheetState()
            sheet = sheetState
            BuilderTheme(skin = skinOf(document), result = result, isDark = false, reducedMotion = true) {
                ProvideBuilderLayout(coarsePointer = true, modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(
                        LocalThemeResult provides result,
                        LocalPosterSheetState provides sheetState,
                        LocalPosterSheetProbe provides { shown -> view = shown },
                    ) {
                        WorkspaceShell(
                            posterColors = result.poster,
                            posterCollapsed = false,
                            poster = { rail -> PosterPanel(state, rail, dispatcher) },
                            topBar = {},
                            canvas = {},
                            dock = {},
                            sheetState = sheetState,
                        )
                    }
                }
            }
        }
        waitForIdle()
        return checkNotNull(sheet) to { checkNotNull(view) }
    }
}
