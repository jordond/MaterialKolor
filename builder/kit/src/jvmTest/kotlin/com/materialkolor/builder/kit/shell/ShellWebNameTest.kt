package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ShellWebNameTest {
    @Test
    fun posterPanel_dockedAndRail_carriesItsNameAsTextOnlyWhereNamesFold() {
        // Docked at Expanded and Medium, the rail at Expanded, and the narrow Medium rail shut and open.
        val windows = listOf(1280 to false, 1280 to true, 840 to false, 600 to true, 600 to false)
        for (folds in listOf(true, false)) {
            for ((width, collapsed) in windows) {
                withClue("$width dp, collapsed $collapsed, folds $folds") {
                    runSkikoComposeUiTest(size = Size(width.toFloat(), 900f)) {
                        setContent {
                            ShellHarness(Skin(Library.Material3, expressive = false)) {
                                CompositionLocalProvider(LocalFoldsStateIntoName provides folds) {
                                    WorkspaceShell(
                                        posterColors = ShellPosterColors,
                                        posterCollapsed = collapsed,
                                        poster = { Box(Modifier.fillMaxSize()) },
                                        topBar = { TopBarRegion {} },
                                        canvas = { ShellSlot(ShellCanvasTag) },
                                        dock = {},
                                    )
                                }
                            }
                        }
                        waitForIdle()

                        val pane = onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, ShellPosterLabel))
                            .fetchSemanticsNode()
                            .config
                        pane.getOrNull(SemanticsProperties.Text) shouldBe
                            if (folds) listOf(AnnotatedString(ShellPosterLabel)) else null
                        pane.getOrNull(SemanticsProperties.ContentDescription) shouldBe null
                    }
                }
            }
        }
    }
}
