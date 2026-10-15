package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import com.materialkolor.builder.kit.a11y.LocalWebKeyboard
import com.materialkolor.builder.kit.skin.Skin
import kotlin.test.Test

/**
 * Opens [pane] titled [title] in [skin], with the overlays in the page when [inTree] is set, and
 * checks it reads its title once with the fold on and then off. On the web the pane reads its title
 * with the dialog word as its text and the title it shows stays out of the tree. Elsewhere the
 * shown title reads and the pane has no text of its own.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.checkPaneName(
    skin: Skin,
    inTree: Boolean,
    title: String,
    pane: @Composable () -> Unit,
) {
    var folds by mutableStateOf(true)
    setContent {
        HostOverlays(skin, inTree) {
            CompositionLocalProvider(
                LocalFoldsStateIntoName provides folds,
                LocalWebKeyboard provides folds,
                content = pane,
            )
        }
    }
    waitForIdle()
    onNode(hasOverlayPaneTitle(title)).assert(hasTextExactly("$title, dialog"))
    onAllNodes(hasText(title), useUnmergedTree = true).assertCountEquals(0)

    folds = false
    waitForIdle()
    onNode(hasOverlayPaneTitle(title)).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    onAllNodes(hasText(title), useUnmergedTree = true).assertCountEquals(1)
}

@OptIn(ExperimentalTestApi::class)
class OverlaysWebNameTest {
    @Test
    fun dialog_eachWay_everySkin_readsItsTitleWithTheDialogWordOnTheWebOnly() =
        hostEachWay { skin, inTree ->
            checkPaneName(skin, inTree, "Delete theme?") {
                BuilderDialog(
                    visible = true,
                    onDismissRequest = {},
                    title = "Delete theme?",
                    actions = { BuilderButton({}, "Delete") },
                ) { BuilderText("Sunset goes for good.") }
            }
        }

    @Test
    fun sheet_eachWay_everySkin_readsItsTitleWithTheDialogWordOnTheWebOnly() =
        hostEachWay { skin, inTree ->
            checkPaneName(skin, inTree, "Export") {
                BuilderSheet(true, {}, "Export", SheetPresentation.EndPanel) { BuilderButton({}, "Copy all") }
            }
        }

    @Test
    fun sidePanel_eachWay_everySkin_readsItsTitleWithTheDialogWordOnTheWebOnly() =
        hostEachWay { skin, inTree ->
            checkPaneName(skin, inTree, "Projects") {
                BuilderSidePanel(true, {}, "Projects") { BuilderButton({}, "New project") }
            }
        }
}
