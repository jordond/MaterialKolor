package com.materialkolor.builder.engine

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.MaterialKolors
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.unstyled.MaterialKolorTokens

/*
 * Where each library adapter keeps a role. These are written out by hand, apart from the engine's
 * own role mapping, so a role wired to the wrong color on either side fails the parity gate.
 */

/** The Material 3 color scheme's color for [role]. */
internal fun ColorScheme.roleColor(role: Role): Color =
    when (role) {
        Role.Primary -> primary
        Role.OnPrimary -> onPrimary
        Role.PrimaryContainer -> primaryContainer
        Role.OnPrimaryContainer -> onPrimaryContainer
        Role.InversePrimary -> inversePrimary
        Role.Secondary -> secondary
        Role.OnSecondary -> onSecondary
        Role.SecondaryContainer -> secondaryContainer
        Role.OnSecondaryContainer -> onSecondaryContainer
        Role.Tertiary -> tertiary
        Role.OnTertiary -> onTertiary
        Role.TertiaryContainer -> tertiaryContainer
        Role.OnTertiaryContainer -> onTertiaryContainer
        Role.Background -> background
        Role.OnBackground -> onBackground
        Role.Surface -> surface
        Role.OnSurface -> onSurface
        Role.SurfaceVariant -> surfaceVariant
        Role.OnSurfaceVariant -> onSurfaceVariant
        Role.SurfaceTint -> surfaceTint
        Role.InverseSurface -> inverseSurface
        Role.InverseOnSurface -> inverseOnSurface
        Role.Error -> error
        Role.OnError -> onError
        Role.ErrorContainer -> errorContainer
        Role.OnErrorContainer -> onErrorContainer
        Role.Outline -> outline
        Role.OutlineVariant -> outlineVariant
        Role.Scrim -> scrim
        Role.SurfaceBright -> surfaceBright
        Role.SurfaceDim -> surfaceDim
        Role.SurfaceContainer -> surfaceContainer
        Role.SurfaceContainerHigh -> surfaceContainerHigh
        Role.SurfaceContainerHighest -> surfaceContainerHighest
        Role.SurfaceContainerLow -> surfaceContainerLow
        Role.SurfaceContainerLowest -> surfaceContainerLowest
        Role.PrimaryFixed -> primaryFixed
        Role.PrimaryFixedDim -> primaryFixedDim
        Role.OnPrimaryFixed -> onPrimaryFixed
        Role.OnPrimaryFixedVariant -> onPrimaryFixedVariant
        Role.SecondaryFixed -> secondaryFixed
        Role.SecondaryFixedDim -> secondaryFixedDim
        Role.OnSecondaryFixed -> onSecondaryFixed
        Role.OnSecondaryFixedVariant -> onSecondaryFixedVariant
        Role.TertiaryFixed -> tertiaryFixed
        Role.TertiaryFixedDim -> tertiaryFixedDim
        Role.OnTertiaryFixed -> onTertiaryFixed
        Role.OnTertiaryFixedVariant -> onTertiaryFixedVariant
    }

/** What core's [MaterialKolors] hands back for [role], the call the Custom export writes. */
internal fun MaterialKolors.roleColor(role: Role): Color =
    when (role) {
        Role.Primary -> primary()
        Role.OnPrimary -> onPrimary()
        Role.PrimaryContainer -> primaryContainer()
        Role.OnPrimaryContainer -> onPrimaryContainer()
        Role.InversePrimary -> inversePrimary()
        Role.Secondary -> secondary()
        Role.OnSecondary -> onSecondary()
        Role.SecondaryContainer -> secondaryContainer()
        Role.OnSecondaryContainer -> onSecondaryContainer()
        Role.Tertiary -> tertiary()
        Role.OnTertiary -> onTertiary()
        Role.TertiaryContainer -> tertiaryContainer()
        Role.OnTertiaryContainer -> onTertiaryContainer()
        Role.Background -> background()
        Role.OnBackground -> onBackground()
        Role.Surface -> surface()
        Role.OnSurface -> onSurface()
        Role.SurfaceVariant -> surfaceVariant()
        Role.OnSurfaceVariant -> onSurfaceVariant()
        Role.SurfaceTint -> surfaceTint()
        Role.InverseSurface -> inverseSurface()
        Role.InverseOnSurface -> inverseOnSurface()
        Role.Error -> error()
        Role.OnError -> onError()
        Role.ErrorContainer -> errorContainer()
        Role.OnErrorContainer -> onErrorContainer()
        Role.Outline -> outline()
        Role.OutlineVariant -> outlineVariant()
        Role.Scrim -> scrim()
        Role.SurfaceBright -> surfaceBright()
        Role.SurfaceDim -> surfaceDim()
        Role.SurfaceContainer -> surfaceContainer()
        Role.SurfaceContainerHigh -> surfaceContainerHigh()
        Role.SurfaceContainerHighest -> surfaceContainerHighest()
        Role.SurfaceContainerLow -> surfaceContainerLow()
        Role.SurfaceContainerLowest -> surfaceContainerLowest()
        Role.PrimaryFixed -> primaryFixed()
        Role.PrimaryFixedDim -> primaryFixedDim()
        Role.OnPrimaryFixed -> onPrimaryFixed()
        Role.OnPrimaryFixedVariant -> onPrimaryFixedVariant()
        Role.SecondaryFixed -> secondaryFixed()
        Role.SecondaryFixedDim -> secondaryFixedDim()
        Role.OnSecondaryFixed -> onSecondaryFixed()
        Role.OnSecondaryFixedVariant -> onSecondaryFixedVariant()
        Role.TertiaryFixed -> tertiaryFixed()
        Role.TertiaryFixedDim -> tertiaryFixedDim()
        Role.OnTertiaryFixed -> onTertiaryFixed()
        Role.OnTertiaryFixedVariant -> onTertiaryFixedVariant()
    }

/** The Unstyled token `toThemeValues` files [this] role under. */
internal fun Role.unstyledToken(): ThemeToken<Color> =
    when (this) {
        Role.Primary -> MaterialKolorTokens.primary
        Role.OnPrimary -> MaterialKolorTokens.onPrimary
        Role.PrimaryContainer -> MaterialKolorTokens.primaryContainer
        Role.OnPrimaryContainer -> MaterialKolorTokens.onPrimaryContainer
        Role.InversePrimary -> MaterialKolorTokens.inversePrimary
        Role.Secondary -> MaterialKolorTokens.secondary
        Role.OnSecondary -> MaterialKolorTokens.onSecondary
        Role.SecondaryContainer -> MaterialKolorTokens.secondaryContainer
        Role.OnSecondaryContainer -> MaterialKolorTokens.onSecondaryContainer
        Role.Tertiary -> MaterialKolorTokens.tertiary
        Role.OnTertiary -> MaterialKolorTokens.onTertiary
        Role.TertiaryContainer -> MaterialKolorTokens.tertiaryContainer
        Role.OnTertiaryContainer -> MaterialKolorTokens.onTertiaryContainer
        Role.Background -> MaterialKolorTokens.background
        Role.OnBackground -> MaterialKolorTokens.onBackground
        Role.Surface -> MaterialKolorTokens.surface
        Role.OnSurface -> MaterialKolorTokens.onSurface
        Role.SurfaceVariant -> MaterialKolorTokens.surfaceVariant
        Role.OnSurfaceVariant -> MaterialKolorTokens.onSurfaceVariant
        Role.SurfaceTint -> MaterialKolorTokens.surfaceTint
        Role.InverseSurface -> MaterialKolorTokens.inverseSurface
        Role.InverseOnSurface -> MaterialKolorTokens.inverseOnSurface
        Role.Error -> MaterialKolorTokens.error
        Role.OnError -> MaterialKolorTokens.onError
        Role.ErrorContainer -> MaterialKolorTokens.errorContainer
        Role.OnErrorContainer -> MaterialKolorTokens.onErrorContainer
        Role.Outline -> MaterialKolorTokens.outline
        Role.OutlineVariant -> MaterialKolorTokens.outlineVariant
        Role.Scrim -> MaterialKolorTokens.scrim
        Role.SurfaceBright -> MaterialKolorTokens.surfaceBright
        Role.SurfaceDim -> MaterialKolorTokens.surfaceDim
        Role.SurfaceContainer -> MaterialKolorTokens.surfaceContainer
        Role.SurfaceContainerHigh -> MaterialKolorTokens.surfaceContainerHigh
        Role.SurfaceContainerHighest -> MaterialKolorTokens.surfaceContainerHighest
        Role.SurfaceContainerLow -> MaterialKolorTokens.surfaceContainerLow
        Role.SurfaceContainerLowest -> MaterialKolorTokens.surfaceContainerLowest
        Role.PrimaryFixed -> MaterialKolorTokens.primaryFixed
        Role.PrimaryFixedDim -> MaterialKolorTokens.primaryFixedDim
        Role.OnPrimaryFixed -> MaterialKolorTokens.onPrimaryFixed
        Role.OnPrimaryFixedVariant -> MaterialKolorTokens.onPrimaryFixedVariant
        Role.SecondaryFixed -> MaterialKolorTokens.secondaryFixed
        Role.SecondaryFixedDim -> MaterialKolorTokens.secondaryFixedDim
        Role.OnSecondaryFixed -> MaterialKolorTokens.onSecondaryFixed
        Role.OnSecondaryFixedVariant -> MaterialKolorTokens.onSecondaryFixedVariant
        Role.TertiaryFixed -> MaterialKolorTokens.tertiaryFixed
        Role.TertiaryFixedDim -> MaterialKolorTokens.tertiaryFixedDim
        Role.OnTertiaryFixed -> MaterialKolorTokens.onTertiaryFixed
        Role.OnTertiaryFixedVariant -> MaterialKolorTokens.onTertiaryFixedVariant
    }
