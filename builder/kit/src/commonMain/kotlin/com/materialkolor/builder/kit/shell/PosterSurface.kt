package com.materialkolor.builder.kit.shell

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.skin.StatusColors
import com.materialkolor.builder.kit.skin.builderCodePalette
import com.materialkolor.builder.kit.skin.headless.ScrimAlpha
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.ktx.toneColor

/**
 * Themes [content] with the shell's Material skin, re-coloured from [poster], so the controls on
 * the poster stand on the seed in the seed's own tones.
 *
 * The poster is content rather than chrome. It keeps its seed coloured look, and only the shapes of
 * its controls follow the skin. It re-provides a `MaterialTheme` over a `ColorScheme` mapped from
 * the poster rather than generated, and overrides the builder tokens, so a builder widget on the
 * poster reads ink on the seed too.
 *
 * Nothing here paints. The shell lays the page under it.
 *
 * @param[poster] The poster's colours, one ramp built on the seed.
 * @param[content] Whatever stands on the poster.
 */
@Composable
public fun PosterSurface(
    poster: PosterColors,
    content: @Composable () -> Unit,
) {
    val outer = LocalBuilderTokens.current
    // Every theme result brings a poster of its own, but the poster follows the seed alone. A new
    // paint re-themes the poster, which recomposes every control on it, so it is kept while the seed
    // and the page stay and an edit that leaves the seed alone never re-themes the poster.
    val kept = remember(poster.seed, poster.background) { poster }
    val paint = remember(kept) { PosterPaint(kept) }
    val tokens = remember(paint, outer) { paint.builderTokens(outer) }
    CompositionLocalProvider(LocalPosterColors provides kept) {
        MaterialPoster(paint, tokens, content)
    }
}

/**
 * Themes [content] with the poster around it turned over, so a sheet that stands on the poster
 * reads as its inverse.
 *
 * The page is the poster's ink, and the ink is a light tone of the seed's ramp on a light seed or
 * a dark one on a dark seed, from `PosterColors.inverse`. The controls inside follow it with no
 * work of their own, the way they follow [PosterSurface]. Call it inside a
 * [PosterSurface]. Inside another inverse it turns the poster back.
 *
 * @param[content] Whatever stands on the inverse poster.
 */
@Composable
public fun InversePosterSurface(content: @Composable () -> Unit) {
    val poster = LocalPosterColors.current
    require(poster != null) { "InversePosterSurface turns a poster over, so call it inside a PosterSurface" }
    PosterSurface(poster.inverse(), content)
}

/**
 * The poster colours [PosterSurface] themes with, or null outside one.
 */
internal val LocalPosterColors: ProvidableCompositionLocal<PosterColors?> = staticCompositionLocalOf { null }

/**
 * The handful of colours the poster's roles are cut from.
 *
 * @property[page] The exact seed, or the poster's ink on the inverse.
 * @property[inkMuted] Muted ink, floored at 3 to 1 on the page, so only for strokes and never text.
 * @property[shade] The ramp's darkest tone, for shadows and the scrim role.
 */
@Immutable
private class PosterPaint(
    poster: PosterColors,
) {
    val page: Color = poster.background.toColor()
    val ink: Color = poster.ink.toColor()
    val inkMuted: Color = poster.inkMuted.toColor()
    val raised: Color = poster.raised.toColor()
    val sunken: Color = poster.sunken.toColor()
    val outline: Color = poster.outline.toColor()
    val shade: Color = poster.ramp.toneColor(ShadeTone)
}

private const val ShadeTone = 0

/**
 * The skin's tokens with every colour swapped for the poster's. Radii, spacing and icon size stay
 * the skin's own, since the shapes of the controls follow the skin.
 *
 * No status hue is sure to read on every seed, so success, warning and danger all take the ink.
 * The poster says what a state means in words. Muted text takes the ink too, since muted ink is
 * only floored at 3 to 1 on the seed.
 */
private fun PosterPaint.builderTokens(outer: BuilderTokens): BuilderTokens =
    outer.copy(
        canvas = page,
        panel = page,
        panelRaised = raised,
        border = outline,
        borderStrong = ink,
        textStrong = ink,
        textMuted = ink,
        accent = ink,
        onAccent = page,
        focus = ink,
        codeBackground = sunken,
        codePalette = builderCodePalette(
            plain = ink,
            muted = ink,
            primary = ink,
            secondary = ink,
            tertiary = ink,
            status = StatusColors(success = ink, warning = ink),
        ),
        success = ink,
        warning = ink,
        danger = ink,
        scrim = page.copy(alpha = ScrimAlpha),
    )

/**
 * A `MaterialTheme` over the poster's scheme, keeping the skin's shapes, type and motion.
 */
@Composable
private fun MaterialPoster(
    paint: PosterPaint,
    tokens: BuilderTokens,
    content: @Composable () -> Unit,
) {
    val scheme = remember(paint) { paint.colorScheme() }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalBuilderTokens provides tokens,
            LocalContentColor provides paint.ink,
            content = content,
        )
    }
}

/**
 * Material's roles mapped from the poster. Surface is the seed, onSurface, onSurfaceVariant and
 * primary are ink, and onPrimary is the seed. The containers and the outline come from the ramp.
 *
 * Every role is named, so nothing of the baseline scheme shows through. Each container is a surface
 * the ink still reads on. The surface tint is the page, so tonal elevation never pulls a surface
 * away from the ink.
 */
private fun PosterPaint.colorScheme(): ColorScheme =
    lightColorScheme(
        primary = ink,
        onPrimary = page,
        primaryContainer = raised,
        onPrimaryContainer = ink,
        inversePrimary = page,
        secondary = ink,
        onSecondary = page,
        secondaryContainer = raised,
        onSecondaryContainer = ink,
        tertiary = ink,
        onTertiary = page,
        tertiaryContainer = raised,
        onTertiaryContainer = ink,
        background = page,
        onBackground = ink,
        surface = page,
        onSurface = ink,
        surfaceVariant = sunken,
        onSurfaceVariant = ink,
        surfaceTint = page,
        inverseSurface = ink,
        inverseOnSurface = page,
        error = ink,
        onError = page,
        errorContainer = raised,
        onErrorContainer = ink,
        outline = outline,
        outlineVariant = outline,
        scrim = shade,
        surfaceBright = raised,
        surfaceContainer = sunken,
        surfaceContainerHigh = raised,
        surfaceContainerHighest = raised,
        surfaceContainerLow = sunken,
        surfaceContainerLowest = raised,
        surfaceDim = page,
        primaryFixed = raised,
        primaryFixedDim = sunken,
        onPrimaryFixed = ink,
        onPrimaryFixedVariant = ink,
        secondaryFixed = raised,
        secondaryFixedDim = sunken,
        onSecondaryFixed = ink,
        onSecondaryFixedVariant = ink,
        tertiaryFixed = raised,
        tertiaryFixedDim = sunken,
        onTertiaryFixed = ink,
        onTertiaryFixedVariant = ink,
    )
