package com.materialkolor.builder.preview.material

import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.inspect.previewRoles

/**
 * The Material 3 components the sample apps use, each with the roles it reads when left on its
 * default colors.
 *
 * The roles follow the component tokens of the Material 3 library the preview ships with, the
 * container first and then what is drawn on it. A component whose colors the screen passes itself
 * declares those roles instead, through the [Role] overload of [previewRoles].
 *
 * @property[refs] The roles, named the way the contrast audit names them.
 */
internal enum class MaterialComponent(
    vararg roles: Role,
) {
    FilledButton(Role.Primary, Role.OnPrimary),
    TonalButton(Role.SecondaryContainer, Role.OnSecondaryContainer),
    OutlinedButton(Role.OutlineVariant, Role.OnSurfaceVariant),
    TextButton(Role.Primary),
    IconButton(Role.OnSurfaceVariant),
    Fab(Role.PrimaryContainer, Role.OnPrimaryContainer),
    FilledCard(Role.SurfaceContainerHighest, Role.OnSurface),
    ElevatedCard(Role.SurfaceContainerLow, Role.OnSurface),
    OutlinedCard(Role.Surface, Role.OnSurface, Role.OutlineVariant),
    ListItem(Role.Surface, Role.OnSurface, Role.OnSurfaceVariant),
    NavigationRail(Role.Surface, Role.OnSurfaceVariant),
    NavigationRailItem(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.Secondary, Role.OnSurfaceVariant),
    FilterChip(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.OutlineVariant, Role.OnSurfaceVariant),
    AssistChip(Role.OutlineVariant, Role.OnSurface, Role.Primary),
    Switch(Role.Primary, Role.OnPrimary, Role.OnPrimaryContainer, Role.SurfaceContainerHighest, Role.Outline),
    Checkbox(Role.Primary, Role.OnPrimary, Role.OnSurfaceVariant),
    LinearProgressIndicator(Role.Primary, Role.SecondaryContainer),
    OutlinedTextField(Role.Outline, Role.Primary, Role.OnSurface, Role.OnSurfaceVariant),
    HorizontalDivider(Role.OutlineVariant),
    Badge(Role.Error, Role.OnError),
    ;

    val refs: List<ColorRef> = roles.map { role -> ColorRef.OfRole(role) }
}

/** Declare the roles [component] reads on its default colors, for the role usage check and Inspect. */
internal fun Modifier.previewRoles(component: MaterialComponent): Modifier =
    previewRoles(*component.refs.toTypedArray())

/** Declare [roles] of the Material 3 scheme, for an element that passes its colors itself. */
internal fun Modifier.previewRoles(vararg roles: Role): Modifier =
    previewRoles(*roles.map { role -> ColorRef.OfRole(role) }.toTypedArray())
