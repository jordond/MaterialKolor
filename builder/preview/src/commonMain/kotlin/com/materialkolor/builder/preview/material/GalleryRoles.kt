package com.materialkolor.builder.preview.material

import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.inspect.previewRoles

/**
 * The Material 3 components only the gallery shows, and the disabled looks of every component in
 * it, each with the roles it reads when left on its default colors.
 *
 * The roles follow the component tokens of the Material 3 library the preview ships with, the same
 * way [MaterialComponent] does, container first. A disabled component paints faded copies of the
 * roles it names here, so Inspect still points at the right ones.
 *
 * @property[refs] The roles, named the way the contrast audit names them.
 */
internal enum class GalleryComponent(
    vararg roles: Role,
) {
    ElevatedButton(Role.SurfaceContainerLow, Role.Primary),
    FilledIconButton(Role.Primary, Role.OnPrimary),
    TonalIconButton(Role.SecondaryContainer, Role.OnSecondaryContainer),

    /** Outline and icon both take the local content color, which the card's surface sets to OnSurface. */
    OutlinedIconButton(Role.OnSurface),
    SegmentedButton(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.Outline, Role.OnSurface),
    TextField(Role.SurfaceContainerHighest, Role.OnSurface, Role.OnSurfaceVariant, Role.Primary),
    Slider(Role.Primary, Role.SecondaryContainer),
    RadioButton(Role.Primary, Role.OnSurfaceVariant),
    SuggestionChip(Role.OutlineVariant, Role.OnSurfaceVariant),
    InputChip(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.OutlineVariant, Role.OnSurfaceVariant),
    Menu(Role.SurfaceContainer, Role.OnSurface),
    MenuItem(Role.OnSurface, Role.OnSurfaceVariant),
    Dialog(Role.SurfaceContainerHigh, Role.OnSurface, Role.OnSurfaceVariant, Role.Secondary),
    TopAppBar(Role.Surface, Role.OnSurface),

    /** An icon button among a top app bar's actions, which the bar tints OnSurfaceVariant. */
    AppBarAction(Role.OnSurfaceVariant),
    NavigationBar(Role.SurfaceContainer, Role.OnSurfaceVariant),
    NavigationBarItem(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.Secondary, Role.OnSurfaceVariant),
    TabRow(Role.Surface, Role.Primary, Role.OutlineVariant),

    /** A tab of a primary tab row, which the gallery gives OnSurfaceVariant while unselected. */
    Tab(Role.Primary, Role.OnSurfaceVariant),
    CircularProgressIndicator(Role.Primary, Role.SecondaryContainer),
    Snackbar(Role.InverseSurface, Role.InverseOnSurface),
    SnackbarAction(Role.InversePrimary),
    PlainTooltip(Role.InverseSurface, Role.InverseOnSurface),
    RichTooltip(Role.SurfaceContainer, Role.OnSurfaceVariant, Role.Primary),

    /** Most disabled components, faded OnSurface on a faded OnSurface container if they have one. */
    Disabled(Role.OnSurface),

    /** A disabled filled, elevated or text button, whose label fades OnSurfaceVariant instead. */
    DisabledButton(Role.OnSurface, Role.OnSurfaceVariant),
    DisabledOutlinedButton(Role.OutlineVariant, Role.OnSurfaceVariant),
    DisabledSwitch(Role.SurfaceContainerHighest, Role.OnSurface),
    DisabledFilledCard(Role.SurfaceVariant, Role.OnSurface),
    DisabledElevatedCard(Role.Surface, Role.OnSurface),
    DisabledOutlinedCard(Role.Surface, Role.OnSurface, Role.Outline),

    /** A disabled text button, navigation item or tab, faded OnSurfaceVariant with no container. */
    DisabledVariant(Role.OnSurfaceVariant),
    ;

    val refs: List<ColorRef> = roles.map { role -> ColorRef.OfRole(role) }
}

/** Declare the roles [component] reads on its default colors, for the role usage check and Inspect. */
internal fun Modifier.previewRoles(component: GalleryComponent): Modifier = previewRoles(*component.refs.toTypedArray())

/** Declare the roles of [component] while [enabled], and of its [disabled] look otherwise. */
internal fun Modifier.previewRoles(
    enabled: Boolean,
    component: MaterialComponent,
    disabled: GalleryComponent = GalleryComponent.Disabled,
): Modifier = previewRoles(*(if (enabled) component.refs else disabled.refs).toTypedArray())

/** Declare the roles of [component] while [enabled], and of its [disabled] look otherwise. */
internal fun Modifier.previewRoles(
    enabled: Boolean,
    component: GalleryComponent,
    disabled: GalleryComponent = GalleryComponent.Disabled,
): Modifier = previewRoles(if (enabled) component else disabled)
