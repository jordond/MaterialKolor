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
import com.materialkolor.unstyled.dynamicColorSchemes

private const val COLOR_TRANSITION_MILLIS = 300

/**
 * Themes [content] with the colors `material-kolor-unstyled` generates from [seed].
 *
 * The Unstyled theme is built once. Its builder is composable and reads the seed every time it runs, so a new seed
 * regenerates the light and dark schemes and the transition spec animates every token to its new color. [isDark]
 * picks which of the two schemes applies, the way the system setting would.
 *
 * @param[seed] The color both schemes are generated from.
 * @param[isDark] Whether the dark scheme applies.
 * @param[content] The app. Text and icons in it default to `onSurface`.
 */
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
        dynamicColorSchemes(seedColor = seed())
        defaultTextStyle = TasksType.Body
        // Left unset, Unstyled hands out an indication that foundation's clickable refuses at runtime.
        defaultIndication = StateLayer
    }

/**
 * The color the current theme gives this MaterialKolor token. It animates while the theme changes.
 */
internal val ThemeToken<Color>.color: Color
    @Composable
    get() = Theme[MaterialKolorTokens.colors][this]
