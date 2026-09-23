package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/**
 * Where B-213's recording job writes the baselines. Nothing is written unless a Roborazzi task
 * turns capture on, and baselines are only ever recorded on the Linux runner.
 */
private const val ScreenshotDir = "src/jvmTest/screenshots/widgets"

/** Each widget of the scene, tagged so it is captured on its own. */
private val WidgetTags: List<String> = listOf("swatches", "chips", "ramp", "code", "frame")

/** The roles the scene shows as swatches. */
private val WidgetRoles: List<Role> = listOf(Role.Primary, Role.SecondaryContainer, Role.Error)

@OptIn(ExperimentalTestApi::class)
class WidgetsScreenshotTest {
    @Test
    fun widgets_everySkinBothModes_render() = forEachWidgetSkin { name, skin -> captureWidgets(name, skin) }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.captureWidgets(
    name: String,
    skin: Skin,
) {
    var isDark by mutableStateOf(false)
    val code = widgetGoldenColorFile()
    setContent {
        WidgetHarness(skin, isDark) {
            val result = remember { ThemeResolver().resolve(WidgetDocument) }
            val candidate = remember { ThemeResolver().resolve(ThemeDocument(seed = Argb(0x006A6A))) }
            WidgetScene(result, candidate, isDark, code.lines)
        }
    }

    for (dark in listOf(false, true)) {
        isDark = dark
        waitForIdle()
        val mode = if (dark) "dark" else "light"
        for (tag in WidgetTags) {
            onNodeWithTag(tag).captureRoboImage("$ScreenshotDir/$name-$mode-$tag.png")
        }
    }
}

/** Every widget on the canvas of the surrounding skin, drawn from [result] in the mode [isDark] picks. */
@Composable
private fun WidgetScene(
    result: ThemeResult,
    candidate: ThemeResult,
    isDark: Boolean,
    code: List<List<Token>>,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = Modifier.fillMaxSize().background(tokens.canvas).padding(tokens.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.large),
    ) {
        Column(Modifier.width(480.dp), verticalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
            Row(Modifier.testTag("swatches"), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
                for (role in WidgetRoles) {
                    val entry = result.roles[role, isDark]
                    val on = result.roles[requireNotNull(role.onPair), isDark]
                    SwatchTile(
                        name = role.name.replaceFirstChar { char -> char.lowercaseChar() },
                        color = entry.argb.toColor(),
                        onColor = on.argb.toColor(),
                        tone = entry.tone,
                        contrast = widgetContrast(entry.argb.toColor(), on.argb.toColor()),
                        onCopy = {},
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.testTag("chips").selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            ) {
                for ((index, scheme) in listOf(result, candidate).withIndex()) {
                    SchemeChip(
                        primary = scheme.roles[Role.Primary, isDark].argb.toColor(),
                        secondaryContainer = scheme.roles[Role.SecondaryContainer, isDark].argb.toColor(),
                        tertiaryContainer = scheme.roles[Role.TertiaryContainer, isDark].argb.toColor(),
                        selected = index == 0,
                        onClick = {},
                        label = "Scheme $index",
                    )
                }
            }
            RampStrip(result.ramps[KeyColor.Primary, isDark], onCopyTone = {}, Modifier.testTag("ramp").fillMaxWidth())
        }
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
            CodeView(code, onCopy = {}, Modifier.testTag("code").size(440.dp, 240.dp))
            DeviceFrame(DeviceWidth.Phone, enabled = true, modifier = Modifier.testTag("frame")) {
                Box(Modifier.fillMaxWidth().height(96.dp).background(result.roles[Role.Surface, isDark].argb.toColor()))
            }
        }
    }
}

/** The WCAG contrast ratio of two opaque colours. */
private fun widgetContrast(
    a: Color,
    b: Color,
): Double {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05) / (darker + 0.05)
}
