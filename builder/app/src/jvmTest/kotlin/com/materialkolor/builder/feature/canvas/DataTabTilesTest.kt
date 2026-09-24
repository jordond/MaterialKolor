package com.materialkolor.builder.feature.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import dev.stateholder.dispatcher.Dispatcher
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import kotlin.test.Test

// pf-1
@OptIn(ExperimentalTestApi::class)
class DataTabTilesTest {
    private val material = ThemeDocument.Default.copy(library = Library.Material3, expressive = false)

    /** One resolver for both results, so the chrome stays on the same schemes as the app's does. */
    private val resolver = ThemeResolver()

    @Test
    fun roles_anEditThatLeavesTheColorsAlone_composesNoSwatchAgain() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val composed = showTiles { result, dispatcher -> RolesTab(result, PreviewMode.Split, null, dispatcher) }

            composed.shouldBeEmpty()
        }

    @Test
    fun palettes_anEditThatLeavesTheColorsAlone_composesNoRampAgain() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val composed = showTiles { result, dispatcher ->
                PalettesTab(result, PreviewMode.Split, null, highlight = null, generation = 0, dispatcher = dispatcher)
            }

            composed.shouldBeEmpty()
        }

    /**
     * Shows [tab] over the material document, then renames it, which makes a new theme result with
     * the same colors. Hands back every tile the rename composed again.
     */
    private fun ComposeUiTest.showTiles(
        tab: @Composable (result: ThemeResult, dispatcher: Dispatcher<WorkspaceAction>) -> Unit,
    ): List<String> {
        val actions = TabActions()
        val composed = mutableListOf<String>()
        val probe: (name: String) -> Unit = { name -> composed += name }
        var result by mutableStateOf(resolver.resolve(material))
        setContent {
            DataTabTheme(result) {
                CompositionLocalProvider(LocalTileProbe provides probe) {
                    tab(result, actions.dispatcher)
                }
            }
        }
        waitForIdle()
        composed.shouldNotBeEmpty()
        composed.clear()

        result = resolver.resolve(material.copy(themeName = "Harbour"))
        waitForIdle()
        return composed.toList()
    }
}
