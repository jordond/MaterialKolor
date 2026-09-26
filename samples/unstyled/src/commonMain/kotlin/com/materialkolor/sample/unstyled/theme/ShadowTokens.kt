package com.materialkolor.sample.unstyled.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.unstyled.MaterialKolorTokens

@Immutable
internal data class ShadowStyle(
    val color: ThemeToken<Color>,
    val radius: Dp,
    val alpha: Float,
    val offset: DpOffset = DpOffset.Zero,
    val spread: Dp = 0.dp,
)

internal object ShadowTokens {
    val shadows: ThemeProperty<ShadowStyle> = ThemeProperty("shadows")

    val resting: ThemeToken<ShadowStyle> = ThemeToken("resting")
    val lifted: ThemeToken<ShadowStyle> = ThemeToken("lifted")
    val accent: ThemeToken<ShadowStyle> = ThemeToken("accent")
    val inset: ThemeToken<ShadowStyle> = ThemeToken("inset")
}

internal val LightShadows: Map<ThemeToken<ShadowStyle>, ShadowStyle> = mapOf(
    ShadowTokens.resting to ShadowStyle(
        color = MaterialKolorTokens.shadow,
        radius = 10.dp,
        alpha = 0.08f,
        offset = DpOffset(0.dp, 3.dp),
    ),
    ShadowTokens.lifted to ShadowStyle(
        color = MaterialKolorTokens.shadow,
        radius = 28.dp,
        alpha = 0.16f,
        offset = DpOffset(0.dp, 10.dp),
    ),
    ShadowTokens.accent to ShadowStyle(
        color = MaterialKolorTokens.primary,
        radius = 14.dp,
        alpha = 0.4f,
        offset = DpOffset(0.dp, 5.dp),
    ),
    ShadowTokens.inset to ShadowStyle(
        color = MaterialKolorTokens.shadow,
        radius = 4.dp,
        alpha = 0.14f,
        offset = DpOffset(0.dp, 1.5.dp),
    ),
)

/**
 * The dark scheme's overrides. A drop shadow barely shows on a dark surface, so the raised shadows turn into glows
 * in the primary color.
 */
internal val DarkShadows: Map<ThemeToken<ShadowStyle>, ShadowStyle> = mapOf(
    ShadowTokens.resting to ShadowStyle(
        color = MaterialKolorTokens.shadow,
        radius = 12.dp,
        alpha = 0.5f,
        offset = DpOffset(0.dp, 4.dp),
    ),
    ShadowTokens.lifted to ShadowStyle(
        color = MaterialKolorTokens.primary,
        radius = 24.dp,
        alpha = 0.32f,
    ),
    ShadowTokens.accent to ShadowStyle(
        color = MaterialKolorTokens.primary,
        radius = 18.dp,
        alpha = 0.5f,
    ),
    ShadowTokens.inset to ShadowStyle(
        color = MaterialKolorTokens.shadow,
        radius = 6.dp,
        alpha = 0.6f,
        offset = DpOffset(0.dp, 2.dp),
    ),
)

internal val ThemeToken<ShadowStyle>.shadow: Shadow
    @Composable
    get() {
        val style = Theme[ShadowTokens.shadows][this]
        return Shadow(
            radius = style.radius,
            color = style.color.color,
            spread = style.spread,
            offset = style.offset,
            alpha = style.alpha,
        )
    }
