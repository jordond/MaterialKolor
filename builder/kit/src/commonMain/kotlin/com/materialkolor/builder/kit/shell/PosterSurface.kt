package com.materialkolor.builder.kit.shell

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.composeunstyled.theme.ThemeToken
import com.composeunstyled.theme.buildThemeV2
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.StatusColors
import com.materialkolor.builder.kit.skin.builderCodePalette
import com.materialkolor.builder.kit.skin.custom.BuilderIdentity
import com.materialkolor.builder.kit.skin.custom.LocalBuilderIdentity
import com.materialkolor.builder.kit.skin.fluent.FluentPosterInk
import com.materialkolor.builder.kit.skin.fluent.LocalFluentPosterInk
import com.materialkolor.builder.kit.skin.fluent.rememberFluentTypography
import com.materialkolor.builder.kit.skin.headless.ScrimAlpha
import com.materialkolor.builder.kit.skin.unstyled.UnstyledIndication
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.brandFontFamily
import com.materialkolor.fluent.toFluentShades
import com.materialkolor.ktx.toneColor
import com.materialkolor.unstyled.MaterialKolorTokens
import io.github.composefluent.Colors
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentThemeConfiguration
import com.composeunstyled.theme.ColorScheme as UnstyledColorScheme
import io.github.composefluent.LocalContentColor as FluentContentColor

/**
 * Themes [content] with the active skin, re-coloured from [poster], so the controls on the poster
 * stand on the seed in the seed's own tones.
 *
 * The poster is content rather than chrome. It keeps its seed coloured look in every skin, and only
 * the shapes of its controls follow the skin. One exhaustive `when` re-provides the skin's theme the
 * way `BuilderTheme` does. Material3 gets a `ColorScheme` and Unstyled the same roles as token
 * values, both mapped from the poster rather than generated. Custom gets an identity cut from the
 * same colours, and Fluent gets its own colours built from the poster's ramp. Every branch also
 * overrides the builder tokens, so a builder widget on the poster reads ink on the seed too.
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
    // pf-1
    // Every theme result brings a poster of its own, but the poster follows the seed alone. A new
    // paint re-themes the poster, which recomposes every control on it, so it is kept while the seed
    // stays and an edit that leaves the seed alone never re-themes the poster.
    val paint = remember(poster.seed) { PosterPaint(poster) }
    val tokens = remember(paint, outer) { paint.builderTokens(outer) }
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialPoster(paint, tokens, content)
        Library.Unstyled -> UnstyledPoster(paint, tokens, content)
        Library.Fluent -> FluentPoster(poster, paint, tokens, content)
        Library.Custom -> CustomPoster(paint, tokens, content)
    }
}

/**
 * The handful of colours every skin's roles are cut from.
 *
 * @property[page] The exact seed.
 * @property[inkMuted] Muted ink, floored at 3 to 1 on the seed, so only for strokes and never text.
 * @property[shade] The ramp's darkest tone, for shadows and the scrim role.
 * @property[isLight] Whether the seed is light, so ink is darker than the page.
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
    val isLight: Boolean = poster.isLight
}

private const val ShadeTone = 0

/**
 * The skin's tokens with every colour swapped for the poster's. Radii, spacing and icon size stay
 * the skin's own, since the shapes of the controls follow the skin.
 *
 * No status hue is sure to read on every seed, so success, warning and danger all take the ink.
 * The poster says what a state means in words. Muted text takes the ink too, since muted ink is
 * only floored at 3 to 1 on the seed (D39).
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

@Composable
private fun ProvidePosterTokens(
    tokens: BuilderTokens,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBuilderTokens provides tokens, content = content)
}

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

/**
 * A Compose Unstyled theme whose tokens are the Material mapping above.
 *
 * It hands out the skin's own indication for the same reason the Unstyled skin does. Left unset,
 * Compose Unstyled falls back to a legacy indication that foundation's `clickable` refuses.
 */
@Composable
private fun UnstyledPoster(
    paint: PosterPaint,
    tokens: BuilderTokens,
    content: @Composable () -> Unit,
) {
    val brand = brandFontFamily()
    val theme = remember(paint, brand) {
        val values = paint.colorScheme().unstyledValues(paint)
        buildThemeV2 {
            properties[MaterialKolorTokens.colors] = values
            defaultTextStyle = TextStyle.Default.copy(fontFamily = brand)
            defaultIndication = UnstyledIndication
        }
    }
    theme(colorScheme = if (paint.isLight) UnstyledColorScheme.Light else UnstyledColorScheme.Dark) {
        ProvidePosterTokens(tokens, content)
    }
}

/**
 * Every MaterialKolor token, read off the poster's Material roles. The key colours are the seed, and
 * the resting control stroke is muted ink, the one part here that is never text.
 */
private fun ColorScheme.unstyledValues(paint: PosterPaint): Map<ThemeToken<Color>, Color> =
    mapOf(
        MaterialKolorTokens.primaryPaletteKeyColor to paint.page,
        MaterialKolorTokens.secondaryPaletteKeyColor to paint.page,
        MaterialKolorTokens.tertiaryPaletteKeyColor to paint.page,
        MaterialKolorTokens.errorPaletteKeyColor to paint.page,
        MaterialKolorTokens.neutralPaletteKeyColor to paint.page,
        MaterialKolorTokens.neutralVariantPaletteKeyColor to paint.page,
        MaterialKolorTokens.background to background,
        MaterialKolorTokens.onBackground to onBackground,
        MaterialKolorTokens.surface to surface,
        MaterialKolorTokens.surfaceDim to surfaceDim,
        MaterialKolorTokens.surfaceBright to surfaceBright,
        MaterialKolorTokens.surfaceContainerLowest to surfaceContainerLowest,
        MaterialKolorTokens.surfaceContainerLow to surfaceContainerLow,
        MaterialKolorTokens.surfaceContainer to surfaceContainer,
        MaterialKolorTokens.surfaceContainerHigh to surfaceContainerHigh,
        MaterialKolorTokens.surfaceContainerHighest to surfaceContainerHighest,
        MaterialKolorTokens.onSurface to onSurface,
        MaterialKolorTokens.surfaceVariant to surfaceVariant,
        MaterialKolorTokens.onSurfaceVariant to onSurfaceVariant,
        MaterialKolorTokens.inverseSurface to inverseSurface,
        MaterialKolorTokens.inverseOnSurface to inverseOnSurface,
        MaterialKolorTokens.outline to outline,
        MaterialKolorTokens.outlineVariant to outlineVariant,
        MaterialKolorTokens.shadow to paint.shade,
        MaterialKolorTokens.scrim to scrim,
        MaterialKolorTokens.surfaceTint to surfaceTint,
        MaterialKolorTokens.primary to primary,
        MaterialKolorTokens.onPrimary to onPrimary,
        MaterialKolorTokens.primaryContainer to primaryContainer,
        MaterialKolorTokens.onPrimaryContainer to onPrimaryContainer,
        MaterialKolorTokens.inversePrimary to inversePrimary,
        MaterialKolorTokens.secondary to secondary,
        MaterialKolorTokens.onSecondary to onSecondary,
        MaterialKolorTokens.secondaryContainer to secondaryContainer,
        MaterialKolorTokens.onSecondaryContainer to onSecondaryContainer,
        MaterialKolorTokens.tertiary to tertiary,
        MaterialKolorTokens.onTertiary to onTertiary,
        MaterialKolorTokens.tertiaryContainer to tertiaryContainer,
        MaterialKolorTokens.onTertiaryContainer to onTertiaryContainer,
        MaterialKolorTokens.error to error,
        MaterialKolorTokens.onError to onError,
        MaterialKolorTokens.errorContainer to errorContainer,
        MaterialKolorTokens.onErrorContainer to onErrorContainer,
        MaterialKolorTokens.primaryFixed to primaryFixed,
        MaterialKolorTokens.primaryFixedDim to primaryFixedDim,
        MaterialKolorTokens.onPrimaryFixed to onPrimaryFixed,
        MaterialKolorTokens.onPrimaryFixedVariant to onPrimaryFixedVariant,
        MaterialKolorTokens.secondaryFixed to secondaryFixed,
        MaterialKolorTokens.secondaryFixedDim to secondaryFixedDim,
        MaterialKolorTokens.onSecondaryFixed to onSecondaryFixed,
        MaterialKolorTokens.onSecondaryFixedVariant to onSecondaryFixedVariant,
        MaterialKolorTokens.tertiaryFixed to tertiaryFixed,
        MaterialKolorTokens.tertiaryFixedDim to tertiaryFixedDim,
        MaterialKolorTokens.onTertiaryFixed to onTertiaryFixed,
        MaterialKolorTokens.onTertiaryFixedVariant to onTertiaryFixedVariant,
        MaterialKolorTokens.controlActivated to primaryContainer,
        MaterialKolorTokens.controlNormal to paint.inkMuted,
        MaterialKolorTokens.controlHighlight to surfaceVariant,
        MaterialKolorTokens.textPrimaryInverse to inverseOnSurface,
        MaterialKolorTokens.textSecondaryAndTertiaryInverse to inverseOnSurface,
        MaterialKolorTokens.textPrimaryInverseDisableOnly to inverseOnSurface,
        MaterialKolorTokens.textSecondaryAndTertiaryInverseDisabled to inverseOnSurface,
        MaterialKolorTokens.textHintInverse to inverseOnSurface,
    )

/**
 * The Custom skin with an identity cut from the poster, so Custom widgets reach for seed tones.
 */
@Composable
private fun CustomPoster(
    paint: PosterPaint,
    tokens: BuilderTokens,
    content: @Composable () -> Unit,
) {
    val identity = remember(paint) {
        BuilderIdentity(CustomSlot.entries.associateWith { slot -> paint.slot(slot) })
    }
    CompositionLocalProvider(LocalBuilderIdentity provides identity) {
        ProvidePosterTokens(tokens, content)
    }
}

/**
 * Which poster colour each Custom slot takes, on the same lines as the Material roles.
 */
private fun PosterPaint.slot(slot: CustomSlot): Color =
    when (slot) {
        CustomSlot.Primary,
        CustomSlot.PrimaryPressed,
        CustomSlot.PrimaryRaised,
        CustomSlot.OnPrimaryContainer,
        CustomSlot.Secondary,
        CustomSlot.OnSecondaryContainer,
        CustomSlot.Tertiary,
        CustomSlot.OnTertiaryContainer,
        CustomSlot.Error,
        CustomSlot.OnErrorContainer,
        CustomSlot.SurfaceInverse,
        CustomSlot.OnSurface,
        CustomSlot.TextStrong,
        CustomSlot.BorderStrong,
        CustomSlot.FocusRing,
        CustomSlot.TextMuted,
        -> ink
        CustomSlot.OnPrimary,
        CustomSlot.OnSecondary,
        CustomSlot.OnTertiary,
        CustomSlot.OnError,
        CustomSlot.Surface,
        CustomSlot.OnSurfaceInverse,
        -> page
        CustomSlot.PrimaryContainer,
        CustomSlot.SecondaryContainer,
        CustomSlot.TertiaryContainer,
        CustomSlot.ErrorContainer,
        CustomSlot.SurfaceRaised,
        -> raised
        CustomSlot.SurfaceSunken -> sunken
        CustomSlot.BorderFaint,
        CustomSlot.BorderSoft,
        -> outline
        CustomSlot.Scrim,
        CustomSlot.Shadow,
        -> shade
    }

/**
 * Fluent's theme over colours built from the poster's ramp, in Fluent's own type.
 *
 * Fluent's text is black or white whatever the shades, and on a mid tone seed neither reads at 4.5
 * to 1. So the Fluent controls here draw their labels, glyphs and strokes in the poster's ink, which
 * they find through [LocalFluentPosterInk], the way the Material and Unstyled ones find it in their
 * roles. The dark flag still follows the ink rather than the chrome, so the tints Fluent lays on the
 * seed lean the way the ink does. Fluent's content colour is the ink, so a Fluent `Text` that takes
 * its colour from there reads like the builder's own.
 */
@OptIn(ExperimentalFluentApi::class)
@Composable
private fun FluentPoster(
    poster: PosterColors,
    paint: PosterPaint,
    tokens: BuilderTokens,
    content: @Composable () -> Unit,
) {
    val colors = remember(poster.seed) { Colors(poster.ramp.toFluentShades(), darkMode = !poster.isLight) } // pf-1
    val ink = remember(paint) { FluentPosterInk(ink = paint.ink, page = paint.page, outline = paint.outline) }
    FluentThemeConfiguration(colors = colors, typography = rememberFluentTypography()) {
        CompositionLocalProvider(
            FluentContentColor provides paint.ink,
            LocalFluentPosterInk provides ink,
        ) {
            ProvidePosterTokens(tokens, content)
        }
    }
}
