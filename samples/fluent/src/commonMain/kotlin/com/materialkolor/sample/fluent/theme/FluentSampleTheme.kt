package com.materialkolor.sample.fluent.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.materialkolor.fluent.animateFluentColors
import com.materialkolor.fluent.rememberFluentColors
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.theme.isDark
import io.github.composefluent.FluentTheme

/**
 * Themes [content] from [seed] and [mode] through material-kolor-fluent.
 *
 * A new seed fades from one ramp to the next. A new mode cuts straight over, because Fluent keeps light and dark
 * as a flag on one set of shades and there is nothing in between to animate.
 *
 * @param[seed] The seed the accent ramp is generated from.
 * @param[mode] Light, dark or whatever the system says.
 * @param[content] The themed content.
 */
@Composable
internal fun FluentSampleTheme(
    seed: SampleSeed,
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val generated = rememberFluentColors(seedColor = seed.color, isDark = mode.isDark())
    val colors = animateFluentColors(generated)
    val sampleColors = remember(colors.shades, colors.darkMode) {
        sampleColors(accent = colors.shades, isDark = colors.darkMode)
    }

    FluentTheme(colors = colors) {
        CompositionLocalProvider(LocalSampleColors provides sampleColors, content = content)
    }
}

/**
 * Reads the colors this sample adds on top of Fluent's, the same way `FluentTheme.colors` reads Fluent's own.
 */
internal object SampleTheme {
    /** The sample's extra colors for the current subtree. */
    val colors: SampleColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleColors.current
}

private val LocalSampleColors = compositionLocalOf<SampleColors> {
    error("No SampleColors provided, wrap the content in FluentSampleTheme.")
}
