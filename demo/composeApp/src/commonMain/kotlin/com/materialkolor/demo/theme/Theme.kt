package com.materialkolor.demo.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val AppTypography = Typography(
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )
)

@Composable
internal fun AppTheme(
    seedColor: Color? = null,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit,
) {
    val colors =
        if (seedColor != null) dynamicColorScheme(seedColor, useDarkTheme, paletteStyle)
        else {
            if (!useDarkTheme) LightColorScheme else DarkColorScheme
        }

    MaterialTheme(
        colorScheme = colors.switch(),
        typography = AppTypography,
        shapes = AppShapes,
        content = {
            Surface(content = content)
        }
    )
}

@Composable
private fun animateColor(target: Color, durationMillis: Int = 400) = animateColorAsState(
    targetValue = target,
    animationSpec = tween(durationMillis = durationMillis),
).value

@Composable
fun ColorScheme.switch() = copy(
    primary = animateColor(primary),
    primaryContainer = animateColor(primaryContainer),
    secondary = animateColor(secondary),
    secondaryContainer = animateColor(secondaryContainer),
    tertiary = animateColor(tertiary),
    tertiaryContainer = animateColor(tertiaryContainer),
    background = animateColor(background),
    surface = animateColor(surface),
    error = animateColor(error),
    onPrimary = animateColor(onPrimary),
    onSecondary = animateColor(onSecondary),
    onTertiary = animateColor(onTertiary),
    onBackground = animateColor(onBackground),
    onSurface = animateColor(onSurface),
    onError = animateColor(onError)
)
