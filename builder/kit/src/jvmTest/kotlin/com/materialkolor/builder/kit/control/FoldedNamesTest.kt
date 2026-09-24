package com.materialkolor.builder.kit.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.skin.Skin
import kotlin.test.Test

/** Shows one of every control an app draws for itself and names through the kit, with the fold as [folds]. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showAppDrawn(
    skin: Skin,
    folds: Boolean,
) {
    setContent {
        ControlsHarness(skin) {
            CompositionLocalProvider(LocalFoldsStateIntoName provides folds) { AppDrawnControls() }
        }
    }
}

@Composable
private fun AppDrawnControls() {
    Column {
        Box(
            Modifier
                .testTag("details")
                .clickable(interactionSource = null, indication = null) {}
                .foldedExpandedName("Filters", expanded = true)
                .size(48.dp),
        )
        Box(
            Modifier
                .testTag("tab")
                .selectable(true, interactionSource = null, indication = null, role = Role.Tab) {}
                .foldedTabName("Orders", selected = true)
                .size(48.dp),
        )
        Box(
            Modifier
                .testTag("choice")
                .selectable(false, interactionSource = null, indication = null, role = Role.RadioButton) {}
                .foldedChoiceName("Oat", selected = false)
                .size(48.dp),
        )
        Box(
            Modifier
                .testTag("option")
                .selectable(true, interactionSource = null, indication = null) {}
                .foldedOptionName("Weekly", selected = true)
                .size(48.dp),
        )
        Box(
            Modifier
                .testTag("command")
                .clickable(interactionSource = null, indication = null) {}
                .foldedMenuItemName("Duplicate")
                .size(48.dp),
        )
        Box(
            Modifier
                .testTag("current")
                .clickable(interactionSource = null, indication = null, enabled = false) {}
                .foldedMenuItemName("Dark", checked = true, enabled = false)
                .size(48.dp),
        )
    }
}

@OptIn(ExperimentalTestApi::class)
class FoldedNamesTest {
    @Test
    fun foldedNames_foldOn_everySkin_readTheRoleAndStateAfterTheName() =
        forEachSkin { _, skin ->
            showAppDrawn(skin, folds = true)

            onNodeWithTag("details")
                .assert(hasContentDescriptionExactly("Filters, expanded"))
                .assert(hasStateDescription("Expanded"))
            onNodeWithTag("tab").assert(hasContentDescriptionExactly("Orders, tab, selected"))
            onNodeWithTag("choice").assert(hasContentDescriptionExactly("Oat, radio, not selected"))
            onNodeWithTag("option").assert(hasContentDescriptionExactly("Weekly, option, selected"))
            onNodeWithTag("command").assert(hasContentDescriptionExactly("Duplicate, menu item"))
            onNodeWithTag("current").assert(hasContentDescriptionExactly("Dark, menu item, checked, disabled"))
        }

    @Test
    fun foldedNames_foldOff_everySkin_keepTheNameAndLeaveTheStateToTheControl() =
        forEachSkin { _, skin ->
            showAppDrawn(skin, folds = false)

            onNodeWithTag("details")
                .assert(hasContentDescriptionExactly("Filters"))
                .assert(hasStateDescription("Expanded"))
            onNodeWithTag("tab").assert(hasContentDescriptionExactly("Orders")).assert(hasRole(Role.Tab))
            onNodeWithTag("choice").assert(hasContentDescriptionExactly("Oat")).assert(hasRole(Role.RadioButton))
            onNodeWithTag("option").assert(hasContentDescriptionExactly("Weekly"))
            onNodeWithTag("command").assert(hasContentDescriptionExactly("Duplicate"))
            onNodeWithTag("current").assert(hasContentDescriptionExactly("Dark"))
        }
}
