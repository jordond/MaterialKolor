package com.materialkolor.builder.preview.unstyled

import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.inspect.previewRoles

/**
 * The parts the Unstyled gallery draws, each with the roles it reads.
 *
 * Compose Unstyled ships no colors, so like [UnstyledComponent] on the dashboard every part is the
 * gallery's own styling on an Unstyled primitive, the container first and then what is drawn on it.
 * A disabled part paints faded [Role.OnSurface] whatever it is, and declares [Disabled].
 *
 * @property[refs] The roles, named the way the contrast audit names them.
 */
internal enum class UnstyledGalleryComponent(
    vararg roles: Role,
) {
    Gallery(Role.Surface, Role.OnSurface),
    Card(Role.SurfaceContainerLow, Role.OnSurface, Role.OutlineVariant),
    FilledButton(Role.Primary, Role.OnPrimary),
    TonalButton(Role.SecondaryContainer, Role.OnSecondaryContainer),
    OutlinedButton(Role.Outline, Role.Primary),
    IconButton(Role.OnSurfaceVariant),
    ToggleButton(Role.Outline, Role.OnSurfaceVariant),
    CheckedToggleButton(Role.TertiaryContainer, Role.OnTertiaryContainer),

    /**
     * The container, the text, the label and the line under it, and the caret and focused line.
     */
    TextField(Role.SurfaceContainerHighest, Role.OnSurface, Role.OnSurfaceVariant, Role.Primary),

    /**
     * The filled part of the track and the thumb, then the rest of the track.
     */
    Slider(Role.Primary, Role.SurfaceContainerHighest),
    Checkbox(Role.Outline, Role.OnSurface),
    CheckedCheckbox(Role.Primary, Role.OnPrimary, Role.OnSurface),
    Switch(Role.SurfaceContainerHighest, Role.Outline, Role.OnSurface),
    CheckedSwitch(Role.Primary, Role.OnPrimary, Role.OnSurface),
    RadioButton(Role.Outline, Role.OnSurface),
    SelectedRadioButton(Role.Primary, Role.OnSurface),

    /**
     * The outline, the label over the value, and the value.
     */
    SelectField(Role.Outline, Role.OnSurfaceVariant, Role.OnSurface),
    Menu(Role.SurfaceContainerHighest, Role.OutlineVariant),
    MenuItem(Role.SurfaceContainerHighest, Role.OnSurface),
    SelectedMenuItem(Role.SecondaryContainer, Role.OnSecondaryContainer),
    DangerMenuItem(Role.SurfaceContainerHighest, Role.Error),
    SampleCard(Role.SurfaceContainerHigh, Role.OnSurface, Role.OnSurfaceVariant),
    Disclosure(Role.SurfaceContainer, Role.OnSurface, Role.OnSurfaceVariant),
    Separator(Role.OutlineVariant),
    ScrollArea(Role.SurfaceContainerLowest, Role.OnSurface, Role.OutlineVariant),
    ScrollbarThumb(Role.Outline),
    TabList(Role.SurfaceContainerHigh, Role.OnSurfaceVariant),
    Tab(Role.SurfaceContainerHigh, Role.OnSurfaceVariant),
    SelectedTab(Role.SurfaceContainerLowest, Role.OnSurface),
    NavItem(Role.SurfaceContainerLow, Role.OnSurfaceVariant),
    SelectedNavItem(Role.SecondaryContainer, Role.OnSecondaryContainer),
    Progress(Role.SurfaceContainerHighest, Role.Primary),
    Badge(Role.PrimaryContainer, Role.OnPrimaryContainer),
    TertiaryBadge(Role.TertiaryContainer, Role.OnTertiaryContainer),
    ErrorBadge(Role.ErrorContainer, Role.OnErrorContainer),

    /**
     * Any disabled part, faded OnSurface on a fainter OnSurface container or outline.
     */
    Disabled(Role.OnSurface),
    ;

    val refs: List<ColorRef> = roles.map { role -> ColorRef.OfRole(role) }
}

/**
 * Declare the roles [component] reads, for the role usage check and Inspect.
 */
internal fun Modifier.previewRoles(component: UnstyledGalleryComponent): Modifier =
    previewRoles(*component.refs.toTypedArray())

/**
 * Declare the roles of [component] while [enabled], and of its [disabled] look otherwise.
 */
internal fun Modifier.previewRoles(
    enabled: Boolean,
    component: UnstyledGalleryComponent,
    disabled: UnstyledGalleryComponent = UnstyledGalleryComponent.Disabled,
): Modifier = previewRoles(if (enabled) component else disabled)
