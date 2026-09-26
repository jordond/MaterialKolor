package com.materialkolor.sample.unstyled.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.unstyled.MaterialKolorTokens

internal enum class GradientDirection {
    Horizontal,
    Vertical,
    Diagonal,
}

@Immutable
internal data class Gradient(
    val start: ThemeToken<Color>,
    val end: ThemeToken<Color>,
    val direction: GradientDirection = GradientDirection.Diagonal,
)

internal object GradientTokens {
    val gradients: ThemeProperty<Gradient> = ThemeProperty("gradients")

    val accent: ThemeToken<Gradient> = ThemeToken("accent")
    val raised: ThemeToken<Gradient> = ThemeToken("raised")
    val sunken: ThemeToken<Gradient> = ThemeToken("sunken")
    val backdrop: ThemeToken<Gradient> = ThemeToken("backdrop")
}

internal val LightGradients: Map<ThemeToken<Gradient>, Gradient> = mapOf(
    GradientTokens.accent to Gradient(
        start = MaterialKolorTokens.primary,
        end = MaterialKolorTokens.tertiary,
    ),
    GradientTokens.raised to Gradient(
        start = MaterialKolorTokens.surfaceContainerLowest,
        end = MaterialKolorTokens.surfaceContainerLow,
        direction = GradientDirection.Vertical,
    ),
    GradientTokens.sunken to Gradient(
        start = MaterialKolorTokens.surfaceContainerHighest,
        end = MaterialKolorTokens.surfaceContainerHigh,
        direction = GradientDirection.Vertical,
    ),
    GradientTokens.backdrop to Gradient(
        start = MaterialKolorTokens.primaryContainer,
        end = MaterialKolorTokens.surface,
        direction = GradientDirection.Vertical,
    ),
)

internal val DarkGradients: Map<ThemeToken<Gradient>, Gradient> = mapOf(
    GradientTokens.raised to Gradient(
        start = MaterialKolorTokens.surfaceContainerHighest,
        end = MaterialKolorTokens.surfaceContainer,
        direction = GradientDirection.Vertical,
    ),
    GradientTokens.sunken to Gradient(
        start = MaterialKolorTokens.surfaceContainerLowest,
        end = MaterialKolorTokens.surfaceContainerLow,
        direction = GradientDirection.Vertical,
    ),
    GradientTokens.backdrop to Gradient(
        start = MaterialKolorTokens.secondaryContainer,
        end = MaterialKolorTokens.surface,
        direction = GradientDirection.Vertical,
    ),
)

internal val ThemeToken<Gradient>.brush: Brush
    @Composable
    get() {
        val gradient = Theme[GradientTokens.gradients][this]
        val colors = listOf(gradient.start.color, gradient.end.color)
        return when (gradient.direction) {
            GradientDirection.Horizontal -> Brush.horizontalGradient(colors)
            GradientDirection.Vertical -> Brush.verticalGradient(colors)
            GradientDirection.Diagonal -> Brush.linearGradient(colors)
        }
    }
