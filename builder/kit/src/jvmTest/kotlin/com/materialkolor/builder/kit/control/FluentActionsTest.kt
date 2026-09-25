package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.fluent.fluentTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.fluent.toFluentColors
import io.kotest.assertions.withClue
import kotlin.test.Test

private val Fluent = Skin(Library.Fluent, expressive = false)

/**
 * Draws [content] on a panel of the dark Fluent skin when [dark] is set, inside the light page
 * [tabOntoRing] draws, and as it is otherwise.
 */
@Composable
private fun InMode(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    if (!dark) return content()
    val result = remember { ThemeResolver().resolve(RingDocument) }
    BuilderTheme(Fluent, result, isDark = true, reducedMotion = false) {
        Box(Modifier.background(LocalBuilderTokens.current.panel).padding(8.dp)) { content() }
    }
}

/**
 * The ring colour of the dark Fluent skin, or null to leave the light one to [tabOntoRing].
 */
private fun ringsFor(dark: Boolean): (@Composable () -> List<Color>)? {
    if (!dark) return null
    return {
        remember {
            val scheme = ThemeResolver().resolve(RingDocument).chrome(isDark = true)
            listOf(fluentTokens(scheme.toFluentColors(), scheme, isDark = true).focus)
        }
    }
}

/**
 * Tabs onto [content] in the Fluent skin, light and dark, and checks that the ring covers every side
 * and stands 3 to 1 from what it covers all the way round.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ringsInBothModes(
    clue: String,
    content: @Composable () -> Unit,
) {
    for (dark in listOf(false, true)) {
        withClue("$clue, dark = $dark") {
            runComposeUiTest {
                val ring = tabOntoRing(Fluent, ringColors = ringsFor(dark)) { InMode(dark, content) }
                ring.shouldRingEverySide()
                ring.shouldCoverEverySide()
                ring.shouldRingAllTheWayRound()
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class, KitTestApi::class)
internal fun ComposeUiTest.showFluentFolded(content: @Composable () -> Unit) {
    setContent {
        ControlsHarness(Fluent) {
            ProvideWebFoldsForTest { Column { content() } }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
class FluentActionsTest {
    @Test
    fun button_everyEmphasis_lightAndDark_ringsEverySideAtThreeToOne() {
        for (emphasis in Emphasis.entries) {
            ringsInBothModes("$emphasis") {
                BuilderButton({}, emphasis.name, emphasis = emphasis, icon = IconId.Export)
            }
        }
    }

    @Test
    fun toggleButton_onAndOff_lightAndDark_ringsEverySideAtThreeToOne() {
        for (checked in listOf(false, true)) {
            ringsInBothModes("checked = $checked") {
                BuilderToggleButton(checked, {}, "Inspect", icon = IconId.Inspect)
            }
        }
    }

    @Test
    fun button_foldOn_namesTheNodeThatTakesThePressAndAddsTheDisabledNote() =
        runComposeUiTest {
            var enabled by mutableStateOf(true)
            showFluentFolded {
                for (emphasis in Emphasis.entries) {
                    BuilderButton({}, emphasis.name, Modifier.testTag(emphasis.name), emphasis, enabled = enabled)
                }
            }

            for (emphasis in Emphasis.entries) {
                onNodeWithTag(emphasis.name)
                    .assert(hasClickAction())
                    .assert(hasRole(Role.Button))
                    .assert(hasText(emphasis.name))
                    .assert(hasContentDescriptionExactly(emphasis.name))
                onAllNodesWithText(emphasis.name).assertCountEquals(1)
            }

            enabled = false
            waitForIdle()
            for (emphasis in Emphasis.entries) {
                onNodeWithTag(emphasis.name)
                    .assertIsNotEnabled()
                    .assert(hasContentDescriptionExactly("${emphasis.name}, disabled"))
            }
        }

    @Test
    fun toggleButton_foldOn_namesTheNodeThatTakesThePressWithItsState() =
        runComposeUiTest {
            var bold by mutableStateOf(false)
            showFluentFolded {
                BuilderToggleButton(bold, { on -> bold = on }, "Bold", Modifier.testTag("bold"))
                BuilderToggleButton(true, {}, "Italic", Modifier.testTag("italic"), enabled = false)
                BuilderToggleButton(false, {}, "Underline", Modifier.testTag("underline"), enabled = false)
            }

            onNodeWithTag("bold")
                .assert(hasClickAction())
                .assert(isToggleable())
                .assert(hasRole(Role.Checkbox))
                .assert(hasContentDescriptionExactly("Bold, not checked"))
            onAllNodesWithText("Bold").assertCountEquals(1)

            bold = true
            waitForIdle()
            onNodeWithTag("bold").assert(hasContentDescriptionExactly("Bold, checked"))
            onNodeWithTag("italic")
                .assertIsNotEnabled()
                .assert(hasContentDescriptionExactly("Italic, checked, disabled"))
            onNodeWithTag("underline").assert(hasContentDescriptionExactly("Underline, not checked, disabled"))
        }
}
