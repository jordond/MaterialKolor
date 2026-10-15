package com.materialkolor.builder.preview.inklet

import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.inspect.previewRoles

/**
 * The Inklet components the gallery and Trips use, each with the roles it reads when left on its
 * default colors.
 *
 * Inklet has no color system of its own. Its components take Material's defaults, a button the
 * colors of the Material button it is built on and a control its own pick of Primary and
 * OnSurfaceVariant, and draw their pen in the strongest of them. The roles here follow those
 * defaults, container first.
 *
 * @property[refs] The roles, named the way the contrast audit names them.
 */
internal enum class InkletComponent(
    vararg roles: Role,
) {
    /**
     * A solid button, a Primary fill outlined in Primary under an OnPrimary label.
     */
    SolidButton(Role.Primary, Role.OnPrimary),

    /**
     * An outline or scribble button, whose pen and label both take the outlined button's OnSurfaceVariant.
     */
    OutlineButton(Role.OnSurfaceVariant),

    /**
     * An outline icon button, whose pen and icon take the local content color the surface sets.
     */
    OutlineIconButton(Role.OnSurface),
    SolidIconButton(Role.Primary, Role.OnPrimary),

    /**
     * An outline icon toggle, InverseSurface under an InverseOnSurface icon once checked.
     */
    IconToggleButton(Role.InverseSurface, Role.InverseOnSurface, Role.OnSurface),

    /**
     * A checkbox, radio button or toggle, drawn in Primary while on and OnSurfaceVariant while off.
     * Disabled, it keeps the same roles at a lower alpha.
     */
    Control(Role.Primary, Role.OnSurfaceVariant),
    AssistChip(Role.OnSurface, Role.Primary),
    SuggestionChip(Role.OnSurfaceVariant),

    /**
     * A filter or input chip, a SecondaryContainer fill once selected and a bare OnSurfaceVariant pen before.
     */
    SelectableChip(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.OnSurfaceVariant),
    Card(Role.Surface, Role.OnSurface, Role.OutlineVariant),
    Badge(Role.Primary),
    Divider(Role.OutlineVariant),

    /**
     * The outlined text field's colors, which Inklet also draws its pen with.
     */
    TextField(Role.Outline, Role.Primary, Role.OnSurface, Role.OnSurfaceVariant),
    ErrorTextField(Role.Error, Role.OnSurface),
    Slider(Role.Primary, Role.SecondaryContainer),
    TabIndicator(Role.Primary),
    Progress(Role.Primary, Role.SecondaryContainer),

    /**
     * The default `inkletSurface`, a Surface fill in an Outline pen.
     */
    Surface(Role.Surface, Role.Outline),
    Border(Role.Outline),

    /**
     * Most disabled components, faded OnSurface.
     */
    Disabled(Role.OnSurface),
    ;

    val refs: List<ColorRef> = roles.map { role -> ColorRef.OfRole(role) }
}

/**
 * Declare the roles [component] reads on its default colors, for the role usage check and Inspect.
 */
internal fun Modifier.previewRoles(component: InkletComponent): Modifier = previewRoles(*component.refs.toTypedArray())

/**
 * Declare the roles of [component] while [enabled], and of its [disabled] look otherwise.
 */
internal fun Modifier.previewRoles(
    enabled: Boolean,
    component: InkletComponent,
    disabled: InkletComponent = InkletComponent.Disabled,
): Modifier = previewRoles(if (enabled) component else disabled)
