package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.poster.kotlinLiteralOf
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RolePopoverTest {
    private val material = ThemeDocument.Default.copy(library = Library.Material3, expressive = false)

    @Test
    fun menu_eachItem_dispatchesItsAction() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            val actions = showRoles(result, PreviewMode.Light)
            val primary = result.roles[Role.Primary, false].argb

            choose(result, "Copy hex")
            actions.sent.last().withoutFocus() shouldBe WorkspaceAction.CopyText(primary.toHex(), "primary hex")

            choose(result, "Copy Kotlin literal")
            actions.sent.last().withoutFocus() shouldBe
                WorkspaceAction.CopyText(kotlinLiteralOf(primary), "primary as Kotlin")

            choose(result, "Pin this role")
            actions.sent.last() shouldBe WorkspaceAction.Edit(
                DocumentChange.SetPin(Role.Primary, PinMode.Light, primary),
                EditPhase.Discrete,
            )

            choose(result, "Show on ramp")
            actions.sent.last() shouldBe WorkspaceAction.ShowOnRamp(RampTarget.OfRole(Role.Primary, isDark = false))
        }

    // b-308ba
    @Test
    fun copy_handsFocusBackToTheTile() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            val actions = showRoles(result, PreviewMode.Light)

            for (item in listOf("Copy hex", "Copy Kotlin literal")) {
                choose(result, item)
                val copy = actions.sent.last() as WorkspaceAction.CopyText
                val tile = copy.returnFocusTo.shouldNotBeNull()
                onNodeWithContentDescription(tileName(result, Role.OnPrimary, isDark = false)).requestFocus()
                waitForIdle()

                runOnIdle { tile.requestFocus() }
                waitForIdle()
                onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).assertIsFocused()
            }
        }

    @Test
    fun pin_fluent_isDisabled() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(library = Library.Fluent))
            val actions = showRoles(result, PreviewMode.Light)

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).performClick()
            waitForIdle()
            onNodeWithText("Pin this role").assertIsNotEnabled()
            onNodeWithText("Pins set Material roles, which Fluent does not use.").assertExists()
            onNodeWithText("Pin this role").performClick()
            waitForIdle()

            actions.sent.none { action -> action is WorkspaceAction.Edit } shouldBe true
        }

    @Test
    fun pin_material_hasNoReasonRow() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            showRoles(result, PreviewMode.Light)

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).performClick()
            waitForIdle()
            onNodeWithText("Pin this role").assertIsEnabled()
            onNodeWithText("Pins set Material roles, which Fluent does not use.").assertDoesNotExist()
        }

    @Test
    fun pin_pinnedMode_offersUnpinAndTheOtherModePin() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val pinned = material.copy(pins = mapOf(Role.Primary to RolePin(light = Argb(0x777777))))
            val result = resolvedFor(pinned)
            val actions = showRoles(result, PreviewMode.Split)

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).performClick()
            waitForIdle()
            onNodeWithText("Unpin this role").assertIsEnabled().performClick()
            waitForIdle()
            actions.sent.last() shouldBe WorkspaceAction.Edit(
                DocumentChange.SetPin(Role.Primary, PinMode.Light, null),
                EditPhase.Discrete,
            )

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = true)).performClick()
            waitForIdle()
            onNodeWithText("Pin this role").performClick()
            waitForIdle()
            actions.sent.last() shouldBe WorkspaceAction.Edit(
                DocumentChange.SetPin(Role.Primary, PinMode.Dark, result.roles[Role.Primary, true].argb),
                EditPhase.Discrete,
            )
        }

    private fun ComposeUiTest.showRoles(
        result: ThemeResult,
        mode: PreviewMode,
    ): TabActions {
        val actions = TabActions()
        setContent { DataTabTheme(result) { RolesTab(result, mode, filter = null, dispatcher = actions.dispatcher) } }
        waitForIdle()
        return actions
    }

    /** The action as the test compares it, a copy without the tile it hands focus back to. */
    private fun WorkspaceAction.withoutFocus(): WorkspaceAction =
        (this as? WorkspaceAction.CopyText)?.copy(returnFocusTo = null) ?: this

    /** Open the light primary swatch's menu and choose [item]. */
    private fun ComposeUiTest.choose(
        result: ThemeResult,
        item: String,
    ) {
        onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).performClick()
        waitForIdle()
        onNodeWithText(item).performClick()
        waitForIdle()
    }
}
