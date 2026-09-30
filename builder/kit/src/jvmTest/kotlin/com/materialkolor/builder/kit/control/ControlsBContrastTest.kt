package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.kit.headless.CheckboxStyle
import com.materialkolor.builder.kit.headless.SliderStyle
import com.materialkolor.builder.kit.headless.SwitchStyle
import com.materialkolor.builder.kit.headless.TabsStyle
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.FieldStyle
import com.materialkolor.builder.kit.skin.material.materialHeroFieldStyle
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.test.Test

private const val ErrorField = "error-field"

@OptIn(ExperimentalTestApi::class)
class ControlsBContrastTest {
    @Test
    fun inputTokens_everySkinBothModes_meetTheirContrastMinimums() {
        readInEverySkin { seenStyles().inkPairs() }.shortfalls().shouldBeEmpty()
    }

    @Test
    fun inputSheet_everySkin_rendersAndTakesTypingInAField() =
        forEverySkin { variant ->
            setSkinnedContent(variant) { ControlsSheet() }
            onNodeWithTag(ErrorField).requestFocus()
            onNodeWithTag(ErrorField).performTextReplacement("#12345")
            waitForIdle()
            onNodeWithTag(ErrorField).assertIsFocused()
        }
}

/**
 * The colours a skin draws its inputs with. The Custom skin draws them from headless styles, and
 * Material3 through Material's own components, whose colours come from their defaults.
 */
private class SeenStyles(
    val tokens: BuilderTokens,
    val switch: SwitchStyle?,
    val checkbox: CheckboxStyle?,
    val slider: SliderStyle?,
    val tabs: TabsStyle?,
    val field: FieldStyle?,
    val hero: FieldStyle?,
    val material: MaterialInputColors?,
)

/**
 * The defaults the Material3 skin's switch, checkbox, slider and field are drawn with.
 */
private class MaterialInputColors(
    val switch: SwitchColors,
    val checkbox: CheckboxColors,
    val slider: SliderColors,
    val field: TextFieldColors,
)

@Composable
private fun seenStyles(): SeenStyles {
    val tokens = LocalBuilderTokens.current
    return when (LocalSkin.current.library) {
        SkinLibrary.Material3 -> {
            val material = MaterialInputColors(
                switch = SwitchDefaults.colors(),
                checkbox = CheckboxDefaults.colors(),
                slider = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.outline),
                field = OutlinedTextFieldDefaults.colors(),
            )
            SeenStyles(tokens, null, null, null, null, null, materialHeroFieldStyle(), material)
        }
        SkinLibrary.Custom -> {
            SeenStyles(
                tokens = tokens,
                switch = CustomInputStyles.switch,
                checkbox = CustomInputStyles.checkbox,
                slider = CustomInputStyles.slider,
                tabs = CustomInputStyles.tabs,
                field = CustomInputStyles.field,
                hero = null,
                material = null,
            )
        }
    }
}

/**
 * The parts of each input that have to stand out, on what they stand on. Text keeps to 4.5, the
 * edges and marks that say what an input is or which state it is in keep to 3 (WCAG 1.4.11).
 */
private fun SeenStyles.inkPairs(): List<InkPair> {
    val panel = tokens.panel

    fun Color.onPanel(): Color = compositeOver(panel)
    return buildList {
        add(InkPair("focus on panel", tokens.focus, panel, 3.0))
        hero?.let { style ->
            add(InkPair("hero text on panel", style.active, panel, 4.5))
            add(InkPair("hero error on panel", style.error, panel, 4.5))
        }
        switch?.let { style ->
            add(InkPair("switch edge off", style.outlineOff, panel, 3.0))
            add(InkPair("switch thumb off", style.thumbOff, style.trackOff.onPanel(), 3.0))
            add(InkPair("switch track on", style.trackOn, panel, 3.0))
            add(InkPair("switch thumb on", style.thumbOn, style.trackOn, 3.0))
        }
        checkbox?.let { style ->
            add(InkPair("checkbox edge", style.outline, panel, 3.0))
            add(InkPair("checkbox fill", style.checkedFill, panel, 3.0))
            add(InkPair("checkbox check", style.checkInk, style.checkedFill, 3.0))
        }
        slider?.let { style ->
            add(InkPair("slider thumb", style.thumb, panel, 3.0))
            add(InkPair("slider active track", style.activeTrack, panel, 3.0))
        }
        tabs?.let { style ->
            val ground = style.container.onPanel()
            add(InkPair("tab label", style.ink, ground, 4.5))
            add(
                InkPair(
                    "selected tab label",
                    style.selectedInk,
                    style.selectedContainer.compositeOver(ground),
                    4.5,
                ),
            )
        }
        field?.let { style ->
            add(InkPair("field edge", style.outline, panel, 3.0))
            add(InkPair("field text", tokens.textStrong, style.container.onPanel(), 4.5))
            add(InkPair("field focus edge", style.active, panel, 3.0))
        }
        material?.let { colors ->
            add(InkPair("switch edge off", colors.switch.uncheckedBorderColor, panel, 3.0))
            add(InkPair("switch thumb off", colors.switch.uncheckedThumbColor, colors.switch.uncheckedTrackColor, 3.0))
            add(InkPair("switch track on", colors.switch.checkedTrackColor, panel, 3.0))
            add(InkPair("switch thumb on", colors.switch.checkedThumbColor, colors.switch.checkedTrackColor, 3.0))
            add(InkPair("checkbox edge", colors.checkbox.uncheckedBorderColor, panel, 3.0))
            add(InkPair("checkbox fill", colors.checkbox.checkedBoxColor, panel, 3.0))
            add(InkPair("checkbox check", colors.checkbox.checkedCheckmarkColor, colors.checkbox.checkedBoxColor, 3.0))
            add(InkPair("slider thumb", colors.slider.thumbColor, panel, 3.0))
            add(InkPair("slider active track", colors.slider.activeTrackColor, panel, 3.0))
            add(InkPair("field edge", colors.field.unfocusedIndicatorColor, panel, 3.0))
            add(InkPair("field text", colors.field.focusedTextColor, panel, 4.5))
            add(InkPair("field focus edge", colors.field.focusedIndicatorColor, panel, 3.0))
        }
    }
}

/**
 * Every input of this batch in its states, on one panel.
 */
@Composable
private fun ControlsSheet() {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = Modifier
            .background(tokens.canvas)
            .padding(tokens.spacing.medium),
    ) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .background(tokens.panel, RoundedCornerShape(tokens.radius.large))
                .padding(tokens.spacing.large),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            InputHexField(Argb(0x6750A4), { _, _ -> }, "Seed", large = true)
            BuilderTabs(listOf("App", "Components", "Roles"), "Components", {}, { it })
            BuilderSwitch(true, {}, "Dark mode")
            BuilderSwitch(false, {}, "AMOLED black")
            BuilderSwitch(true, {}, "Locked by the target", enabled = false)
            BuilderCheckbox(true, {}, "Show pins")
            BuilderCheckbox(false, {}, "Show extra colors")
            BuilderCheckbox(false, {}, "Not in this spec", enabled = false)
            BuilderSlider(
                value = 0.5f,
                onValueChange = {},
                label = "Contrast",
                valueRange = -1f..1f,
                stops = listOf(-1f, 0f, 0.5f, 1f),
                snapDistance = 0.04f,
            )
            BuilderSlider(0.3f, {}, "Chroma", enabled = false)
            BuilderTextField("Ocean", {}, "Project name", supportingText = "Shown in the projects list")
            InputHexField(Argb(0x006A6A), { _, _ -> }, "Key color", Modifier.testTag(ErrorField))
            BuilderDisclosure(true, {}, "Core colors and pins", summary = "Primary, secondary and tertiary") {
                BuilderText("The content of an open row.")
            }
            BuilderDisclosure(false, {}, "Spec, platform, extra colors and target options") {
                BuilderText("Hidden")
            }
        }
    }
}
