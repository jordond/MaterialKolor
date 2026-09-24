package com.materialkolor.builder.kit.control

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import kotlin.test.Test

/**
 * A disabled select still reads as a disabled pop-up button on the web, in every skin. The web
 * mirror reads a node with a click as a button (D40 P3), so a trigger with no click reads as a bare
 * group, which Material's did once its menu anchor was disabled (S5 row 10, gap G).
 */
@OptIn(ExperimentalTestApi::class, KitTestApi::class)
class DisabledSelectWebRoleTest {
    @Test
    fun disabledSelect_onTheWeb_everySkin_readsAsADisabledPopUpButton() =
        forEachSkin { _, skin ->
            setContent {
                ControlsHarness(skin) {
                    ProvideWebFoldsForTest {
                        BuilderSelect("Platform", listOf("Android", "iOS"), "Android", {}, enabled = false)
                    }
                }
            }
            onNode(hasContentDescriptionExactly("Platform, pop-up button, Android, disabled"))
                .assert(hasClickAction())
                .assert(hasRole(Role.DropdownList))
                .assertIsNotEnabled()
        }
}
