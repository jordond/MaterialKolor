package com.materialkolor.builder.kit.control

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onNodeWithTag
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.Skin
import kotlin.test.Test

private const val InfoTag = "info"

/** Shows the seed info button in [skin], with the fold as [folds] and the panel as [expanded]. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showInfoButton(
    skin: Skin,
    folds: Boolean,
    expanded: () -> Boolean?,
) {
    setContent {
        ControlsHarness(skin) {
            CompositionLocalProvider(LocalFoldsStateIntoName provides folds) {
                BuilderIconButton(
                    onClick = {},
                    icon = IconId.Info,
                    contentDescription = "Seed info",
                    modifier = Modifier.testTag(InfoTag),
                    expanded = expanded(),
                )
            }
        }
    }
}

/** Matches a node with no state description at all. */
private fun hasNoStateDescription(): SemanticsMatcher =
    SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription)

@OptIn(ExperimentalTestApi::class)
class IconButtonExpandedTest {
    @Test
    fun iconButton_expandedFoldOn_everySkin_readsTheStateAfterTheName() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(false)
            showInfoButton(skin, folds = true) { open }

            onNodeWithTag(InfoTag).assert(hasContentDescriptionExactly("Seed info, collapsed"))

            open = true
            waitForIdle()
            onNodeWithTag(InfoTag).assert(hasContentDescriptionExactly("Seed info, expanded"))
        }

    @Test
    fun iconButton_expandedFoldOff_everySkin_keepsTheNameAndCarriesTheState() =
        forEachSkin { _, skin ->
            var open by mutableStateOf(false)
            showInfoButton(skin, folds = false) { open }

            onNodeWithTag(InfoTag)
                .assert(hasContentDescriptionExactly("Seed info"))
                .assert(hasStateDescription("Collapsed"))

            open = true
            waitForIdle()
            onNodeWithTag(InfoTag)
                .assert(hasContentDescriptionExactly("Seed info"))
                .assert(hasStateDescription("Expanded"))
        }

    @Test
    fun iconButton_noExpanded_everySkin_addsNothingEitherWay() {
        forEachSkin { _, skin ->
            showInfoButton(skin, folds = true) { null }
            onNodeWithTag(InfoTag).assert(hasContentDescriptionExactly("Seed info")).assert(hasNoStateDescription())
        }
        forEachSkin { _, skin ->
            showInfoButton(skin, folds = false) { null }
            onNodeWithTag(InfoTag).assert(hasContentDescriptionExactly("Seed info")).assert(hasNoStateDescription())
        }
    }
}
