package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.skin.Skin
import kotlin.test.Test

private const val HeartTag = "heart"

private const val HeartName = "Favourite Flat white"

/**
 * Shows an app drawn heart in [skin] that starts off, with the fold as [folds].
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showHeart(
    skin: Skin,
    folds: Boolean,
    enabled: Boolean = true,
) {
    setContent {
        ControlsHarness(skin) {
            CompositionLocalProvider(LocalFoldsStateIntoName provides folds) {
                var favourite by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .testTag(HeartTag)
                        .toggleable(value = favourite, enabled = enabled, role = Role.Checkbox) { on -> favourite = on }
                        .foldedToggleName(HeartName, favourite, enabled)
                        .size(48.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
class FoldedToggleNameTest {
    @Test
    fun foldedToggleName_foldOn_everySkin_readsTheRoleAndStateAfterTheName() =
        forEachSkin { _, skin ->
            showHeart(skin, folds = true)
            val heart = onNodeWithTag(HeartTag)
            heart.assert(hasContentDescriptionExactly("$HeartName, checkbox, not checked"))

            heart.performClick()
            waitForIdle()
            heart.assert(hasContentDescriptionExactly("$HeartName, checkbox, checked"))
        }

    @Test
    fun foldedToggleName_foldOff_everySkin_keepsTheNameAndLeavesTheStateToTheToggle() =
        forEachSkin { _, skin ->
            showHeart(skin, folds = false)
            val heart = onNodeWithTag(HeartTag)
            heart.assert(hasContentDescriptionExactly(HeartName)).assert(hasRole(Role.Checkbox)).assertIsOff()

            heart.performClick()
            waitForIdle()
            heart.assert(hasContentDescriptionExactly(HeartName)).assertIsOn()
        }

    @Test
    fun foldedToggleName_disabledFoldOn_everySkin_carriesTheDisabledNote() =
        forEachSkin { _, skin ->
            showHeart(skin, folds = true, enabled = false)
            onNodeWithTag(HeartTag).assert(hasContentDescriptionExactly("$HeartName, checkbox, not checked, disabled"))
        }
}
