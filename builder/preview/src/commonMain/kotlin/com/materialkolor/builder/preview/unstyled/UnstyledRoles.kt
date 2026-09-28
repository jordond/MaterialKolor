package com.materialkolor.builder.preview.unstyled

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.inspect.previewRoles
import com.materialkolor.unstyled.MaterialKolorTokens

/**
 * The MaterialKolor tokens the Trips app and the gallery paint with, each with the role the pane
 * writes into it.
 *
 * Every color is read through [color], and each token is named after its role, which is also the
 * name Inspect gives it.
 *
 * @property[token] The token, read from the `MaterialKolorTokens.colors` property of the theme.
 * @property[role] The role behind it, the way the contrast audit names it.
 */
internal enum class UnstyledToken(
    val token: ThemeToken<Color>,
    val role: Role,
) {
    Primary(MaterialKolorTokens.primary, Role.Primary),
    OnPrimary(MaterialKolorTokens.onPrimary, Role.OnPrimary),
    PrimaryContainer(MaterialKolorTokens.primaryContainer, Role.PrimaryContainer),
    OnPrimaryContainer(MaterialKolorTokens.onPrimaryContainer, Role.OnPrimaryContainer),
    Secondary(MaterialKolorTokens.secondary, Role.Secondary),
    SecondaryContainer(MaterialKolorTokens.secondaryContainer, Role.SecondaryContainer),
    OnSecondaryContainer(MaterialKolorTokens.onSecondaryContainer, Role.OnSecondaryContainer),
    Tertiary(MaterialKolorTokens.tertiary, Role.Tertiary),
    TertiaryContainer(MaterialKolorTokens.tertiaryContainer, Role.TertiaryContainer),
    OnTertiaryContainer(MaterialKolorTokens.onTertiaryContainer, Role.OnTertiaryContainer),
    Error(MaterialKolorTokens.error, Role.Error),
    OnError(MaterialKolorTokens.onError, Role.OnError),
    ErrorContainer(MaterialKolorTokens.errorContainer, Role.ErrorContainer),
    OnErrorContainer(MaterialKolorTokens.onErrorContainer, Role.OnErrorContainer),
    Surface(MaterialKolorTokens.surface, Role.Surface),
    OnSurface(MaterialKolorTokens.onSurface, Role.OnSurface),
    OnSurfaceVariant(MaterialKolorTokens.onSurfaceVariant, Role.OnSurfaceVariant),
    SurfaceContainerLowest(MaterialKolorTokens.surfaceContainerLowest, Role.SurfaceContainerLowest),
    SurfaceContainerLow(MaterialKolorTokens.surfaceContainerLow, Role.SurfaceContainerLow),
    SurfaceContainer(MaterialKolorTokens.surfaceContainer, Role.SurfaceContainer),
    SurfaceContainerHigh(MaterialKolorTokens.surfaceContainerHigh, Role.SurfaceContainerHigh),
    SurfaceContainerHighest(MaterialKolorTokens.surfaceContainerHighest, Role.SurfaceContainerHighest),
    InverseSurface(MaterialKolorTokens.inverseSurface, Role.InverseSurface),
    InverseOnSurface(MaterialKolorTokens.inverseOnSurface, Role.InverseOnSurface),
    Outline(MaterialKolorTokens.outline, Role.Outline),
    OutlineVariant(MaterialKolorTokens.outlineVariant, Role.OutlineVariant),
    Scrim(MaterialKolorTokens.scrim, Role.Scrim),
}

/**
 * The color the pane's Unstyled theme holds for this token.
 */
internal val UnstyledToken.color: Color
    @Composable get() = Theme[MaterialKolorTokens.colors][token]

/**
 * The parts the Trips app draws, each with the roles it reads.
 *
 * Compose Unstyled ships no colors, so every part here is the app's own styling on an Unstyled
 * primitive, the container first and then what is drawn on it. The buttons, the switch, the
 * checkboxes, the progress bar, the field and the separator look the way the gallery draws them,
 * so they declare the gallery's [UnstyledGalleryComponent] instead. A part whose colors come from
 * its data, a trip's thumbnail, declares them through the [Role] overload of [previewRoles].
 *
 * @property[refs] The roles, named the way the contrast audit names them.
 */
internal enum class UnstyledComponent(
    vararg roles: Role,
) {
    App(Role.Surface, Role.OnSurface),
    Rail(Role.Surface, Role.OutlineVariant),
    NavItem(Role.Surface, Role.OnSurfaceVariant),
    SelectedNavItem(Role.SecondaryContainer, Role.OnSecondaryContainer),
    NewTripButton(Role.Primary, Role.OnPrimary),
    NotificationBadge(Role.Error, Role.OnError),
    Search(Role.SurfaceContainerHigh, Role.OnSurfaceVariant),
    FilterChip(Role.Outline, Role.OnSurfaceVariant),
    SelectedFilterChip(Role.SecondaryContainer, Role.OnSecondaryContainer),
    TripRow(Role.Surface, Role.OnSurface, Role.OnSurfaceVariant),
    SelectedTripRow(Role.SecondaryContainer, Role.OnSecondaryContainer),
    TripPane(Role.SurfaceContainerLow, Role.OnSurface, Role.OnSurfaceVariant),
    Scene(Role.PrimaryContainer, Role.TertiaryContainer, Role.Secondary, Role.Primary, Role.OnPrimaryContainer),
    LinkButton(Role.Primary),
    DayLabel(Role.Primary),
    Stop(Role.SurfaceContainerHighest, Role.OnSurfaceVariant),
    Alert(Role.ErrorContainer, Role.OnErrorContainer),
    AlertButton(Role.Error, Role.OnError),
    OfflineMapsCard(Role.SurfaceContainerLowest, Role.OnSurface, Role.OnSurfaceVariant, Role.OutlineVariant),
    PackingCard(Role.SurfaceContainer, Role.OnSurface, Role.OnSurfaceVariant, Role.OutlineVariant),
    NoteCard(Role.SurfaceContainerLowest, Role.OutlineVariant),
    Tooltip(Role.InverseSurface, Role.InverseOnSurface),
    ;

    val refs: List<ColorRef> = roles.map { role -> ColorRef.OfRole(role) }
}

/**
 * Declare the roles [component] reads, for the role usage check and Inspect.
 */
internal fun Modifier.previewRoles(component: UnstyledComponent): Modifier =
    previewRoles(*component.refs.toTypedArray())

/**
 * Declare [roles] of the scheme, for a part whose colors come from its data.
 */
internal fun Modifier.previewRoles(vararg roles: Role): Modifier =
    previewRoles(*roles.map { role -> ColorRef.OfRole(role) }.toTypedArray())
