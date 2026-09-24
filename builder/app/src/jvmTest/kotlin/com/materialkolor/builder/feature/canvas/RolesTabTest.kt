package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.toSize
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.roundToInt
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RolesTabTest {
    private val material = ThemeDocument.Default.copy(library = Library.Material3, expressive = false)

    @Test
    fun split_2025_showsEachModesOwnToneAndHex() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(spec = SpecVersion.Spec2025))
            val light = result.roles[Role.Primary, false]
            val dark = result.roles[Role.Primary, true]
            light.tone.roundToInt() shouldNotBe dark.tone.roundToInt()

            showRoles(result, PreviewMode.Split)

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).assertExists()
            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = true)).assertExists()
        }

    @Test
    fun split_wide_laysTwoLabeledColumnsSideBySide() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            showRoles(result, PreviewMode.Split)

            onNodeWithText("Light").assertExists()
            onNodeWithText("Dark").assertExists()
            val light = primaryBounds(result, isDark = false)
            val dark = primaryBounds(result, isDark = true)
            dark.left shouldBeGreaterThan light.right
            dark.top shouldBe light.top
        }

    @Test
    fun split_compact_stacksLightAboveDark() =
        runDesktopComposeUiTest(width = TABS_PHONE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            showRoles(result, PreviewMode.Split)

            onNodeWithText("Light").assertExists()
            onNodeWithText("Dark").assertExists()
            val light = primaryBounds(result, isDark = false)
            val dark = primaryBounds(result, isDark = true)
            light.bottom shouldBeLessThan dark.top
            dark.left shouldBe light.left
        }

    @Test
    fun light_showsTheLightColumnAlone() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            showRoles(result, PreviewMode.Light)

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false)).assertExists()
            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = true)).assertDoesNotExist()
            onNodeWithText("Light").assertDoesNotExist()
            onNodeWithText("Dark").assertDoesNotExist()
        }

    @Test
    fun pinnedRole_showsThePinsOwnTone() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val pin = Argb(0x777777)
            val result = resolvedFor(material.copy(pins = mapOf(Role.Primary to RolePin(light = pin))))
            showRoles(result, PreviewMode.Light)

            val tone = HctReadout.of(pin).tone.roundToInt()
            onNodeWithContentDescription("primary, ${pin.toHex()}, tone $tone").assertExists()
        }

    @Test
    fun pinnedRole_readsPinnedInItsModeOnly() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(pins = mapOf(Role.Primary to RolePin(light = Argb(0x777777)))))
            showRoles(result, PreviewMode.Split)

            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "pinned"))
            onNodeWithContentDescription(tileName(result, Role.Primary, isDark = true))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        }

    @Test
    fun columns_sitOnThePanelSurface() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            var panel = Color.Unspecified
            var canvas = Color.Unspecified
            setContent {
                DataTabTheme(result) {
                    panel = LocalBuilderTokens.current.panel
                    canvas = LocalBuilderTokens.current.canvas
                    RolesTab(result, PreviewMode.Split, filter = null, dispatcher = TabActions().dispatcher)
                }
            }
            waitForIdle()

            panel.toArgb() shouldNotBe canvas.toArgb()
            val panels = onAllNodesWithTag(DATA_PANEL_TAG)
            panels.assertCountEquals(2)
            val frame = onRoot().captureToImage().toPixelMap()
            for (node in panels.fetchSemanticsNodes()) {
                val box = node.boundsInRoot
                // Past the border and the corner, before the first heading.
                frame[box.center.x.toInt(), box.top.toInt() + PANEL_PROBE_Y].toArgb() shouldBe panel.toArgb()
            }
        }

    @Test
    fun groups_listTheRolesThenKeyColorsThenAccents() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material.copy(accents = listOf(Accent(name = "Brand", seed = Argb(0xB3261E)))))
            showRoles(result, PreviewMode.Light)

            for (group in listOf("Accent", "Surface", "Fixed", "Outline and inverse", "Key colors", "Accents")) {
                onNodeWithText(group).assertExists()
            }
            val key = result.ramps[KeyColor.Primary, false].keyColor
            val keyTone = HctReadout.of(key).tone.roundToInt()
            onNodeWithContentDescription("primaryPaletteKeyColor, ${key.toHex()}, tone $keyTone").assertExists()
            val brand = result.accents.families[0][AccentPart.Container, false]
            val brandTone = HctReadout.of(brand).tone.roundToInt()
            onNodeWithContentDescription("Brand container, ${brand.toHex()}, tone $brandTone").assertExists()
        }

    @Test
    fun arrowKeys_moveFocusInsideAGroupAndStopAtItsEdge() =
        runDesktopComposeUiTest(width = TABS_WIDE, height = TABS_HEIGHT) {
            val result = resolvedFor(material)
            showRoles(result, PreviewMode.Light)
            val primary = onNodeWithContentDescription(tileName(result, Role.Primary, isDark = false))
            val onPrimary = onNodeWithContentDescription(tileName(result, Role.OnPrimary, isDark = false))
            val lastAccent = onNodeWithContentDescription(tileName(result, Role.OnErrorContainer, isDark = false))

            primary.requestFocus()
            primary.performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            onPrimary.assertIsFocused()

            lastAccent.requestFocus()
            lastAccent.performKeyInput { pressKey(Key.DirectionDown) }
            waitForIdle()
            lastAccent.assertIsFocused()
        }

    private fun ComposeUiTest.showRoles(
        result: ThemeResult,
        mode: PreviewMode,
    ) {
        val actions = TabActions()
        setContent { DataTabTheme(result) { RolesTab(result, mode, filter = null, dispatcher = actions.dispatcher) } }
        waitForIdle()
    }

    private fun ComposeUiTest.primaryBounds(
        result: ThemeResult,
        isDark: Boolean,
    ): Rect {
        // Read unclipped, since the dark column of a phone sits below the fold of the scroll area.
        val node = onNodeWithContentDescription(tileName(result, Role.Primary, isDark)).fetchSemanticsNode()
        return Rect(node.positionInRoot, node.size.toSize())
    }
}

/** A row of a data panel inside its padding and above its content, where only the panel shows. */
private const val PANEL_PROBE_Y = 8

/** What the swatch of [role] reads out as in the mode [isDark] picks. */
internal fun tileName(
    result: ThemeResult,
    role: Role,
    isDark: Boolean,
): String {
    val entry = result.roles[role, isDark]
    val name = role.name.replaceFirstChar { char -> char.lowercaseChar() }
    return "$name, ${entry.argb.toHex()}, tone ${entry.tone.roundToInt()}"
}
