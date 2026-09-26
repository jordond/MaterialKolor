package com.materialkolor.sample.unstyled.theme

import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.theme.ColorScheme
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeComposableV2
import com.composeunstyled.theme.ThemeToken
import com.composeunstyled.theme.buildThemeV2
import com.materialkolor.unstyled.MaterialKolorTokens
import com.materialkolor.unstyled.rememberDynamicLightDarkColors

private const val COLOR_TRANSITION_MILLIS = 300

@Composable
internal fun TasksTheme(
    seed: Color,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val currentSeed by rememberUpdatedState(seed)
    val theme = remember { tasksTheme(seed = { currentSeed }) }
    theme(colorScheme = if (isDark) ColorScheme.Dark else ColorScheme.Light) {
        ProvideContentColor(color = MaterialKolorTokens.onSurface.color, content = content)
    }
}

private fun tasksTheme(seed: () -> Color): ThemeComposableV2 =
    buildThemeV2 {
        colorSchemeTransitionSpec = tween(durationMillis = COLOR_TRANSITION_MILLIS)
        defaultTextStyle = TasksType.Body
        defaultIndication = StateLayer

        val (light, dark) = rememberDynamicLightDarkColors(seedColor = seed())
        properties[MaterialKolorTokens.colors] = light
        properties[ShapeTokens.shapes] = TasksShapes
        properties[ShadowTokens.shadows] = LightShadows
        properties[GradientTokens.gradients] = LightGradients

        colorScheme(ColorScheme.Dark) {
            properties[MaterialKolorTokens.colors] = dark
            properties[ShadowTokens.shadows] = DarkShadows
            properties[GradientTokens.gradients] = DarkGradients
        }
    }

internal val ThemeToken<Color>.color: Color
    @Composable
    get() = Theme[MaterialKolorTokens.colors][this]
