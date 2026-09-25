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
 * The MaterialKolor tokens the dashboard paints with, each with the role the pane writes into it.
 *
 * Every color on the dashboard is read through [color], so this list is exactly what the token
 * side panel shows. Each token is named after its role, which is also the name Inspect gives it.
 *
 * @property[token] The token, read from the `MaterialKolorTokens.colors` property of the theme.
 * @property[role] The role behind it, the way the contrast audit names it.
 */
internal enum class DashboardToken(
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
internal val DashboardToken.color: Color
    @Composable get() = Theme[MaterialKolorTokens.colors][token]

/**
 * The parts the dashboard draws, each with the roles it reads.
 *
 * Compose Unstyled ships no colors, so every part here is the dashboard's own styling on an
 * Unstyled primitive, the container first and then what is drawn on it. A part whose colors
 * come from its data, a metric icon or a status mark, declares them through the [Role] overload
 * of [previewRoles] instead.
 *
 * @property[refs] The roles, named the way the contrast audit names them.
 */
internal enum class UnstyledComponent(
    vararg roles: Role,
) {
    App(Role.Surface, Role.OnSurface),
    TopBar(Role.Surface, Role.OnSurface, Role.OutlineVariant),
    Sidebar(Role.SurfaceContainerLow, Role.OnSurface, Role.OutlineVariant),
    Brand(Role.Primary, Role.OnPrimary),
    NavItem(Role.SurfaceContainerLow, Role.OnSurfaceVariant),
    SelectedNavItem(Role.SecondaryContainer, Role.OnSecondaryContainer),
    ToggledIconButton(Role.SecondaryContainer, Role.OnSecondaryContainer),
    FilledButton(Role.Primary, Role.OnPrimary),
    OutlinedButton(Role.Outline, Role.OnSurface),
    AlertButton(Role.Error, Role.OnError),
    TabList(Role.SurfaceContainerHigh, Role.OnSurfaceVariant),
    Tab(Role.SurfaceContainerHigh, Role.OnSurfaceVariant),
    SelectedTab(Role.SurfaceContainerLowest, Role.OnSurface),
    Alert(Role.ErrorContainer, Role.OnErrorContainer, Role.Error),
    MetricCard(Role.SurfaceContainer, Role.OnSurface, Role.OnSurfaceVariant),
    Progress(Role.SurfaceContainerHighest, Role.Primary),
    ChartCard(Role.SurfaceContainerLow, Role.OnSurface, Role.OnSurfaceVariant),
    Chart(Role.Primary, Role.Secondary, Role.OutlineVariant),
    TableHeader(Role.SurfaceContainerHigh, Role.OnSurfaceVariant),
    TableRow(Role.SurfaceContainerLowest, Role.OnSurface, Role.OnSurfaceVariant, Role.OutlineVariant),
    OutlinedMark(Role.Outline, Role.OnSurfaceVariant),
    Menu(Role.SurfaceContainerHighest, Role.OnSurface, Role.OutlineVariant),
    MenuItem(Role.SurfaceContainerHighest, Role.OnSurface),
    SelectedMenuItem(Role.SecondaryContainer, Role.OnSecondaryContainer),
    Tooltip(Role.InverseSurface, Role.InverseOnSurface),
    Drawer(Role.SurfaceContainerLow, Role.OnSurface, Role.OnSurfaceVariant, Role.OutlineVariant),
    Scrim(Role.Scrim),
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
