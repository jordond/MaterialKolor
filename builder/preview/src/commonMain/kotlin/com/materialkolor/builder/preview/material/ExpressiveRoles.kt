package com.materialkolor.builder.preview.material

import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.inspect.previewRoles

/**
 * The Material 3 Expressive components the preview draws, each with the roles it paints when left
 * on its default colors.
 *
 * The roles follow the component tokens of the Material 3 library the preview ships with, 1.12.0
 * alpha 3, the container first and then what is drawn on it. A component that changes color with
 * its state names the roles of every state, the way [MaterialComponent] does for a navigation rail
 * item. A screen that passes a component colors of its own declares those roles instead.
 *
 * @property[refs] The roles, named the way the contrast audit names them.
 */
internal enum class ExpressiveComponent(
    vararg roles: Role,
) {
    /** A medium or large flexible top app bar, which turns to the container level as it collapses. */
    FlexibleTopAppBar(Role.Surface, Role.OnSurface, Role.OnSurfaceVariant, Role.SurfaceContainer),

    /** A toggle button, tonal when off and filled when on. */
    ToggleButton(Role.SurfaceContainer, Role.OnSurfaceVariant, Role.Primary, Role.OnPrimary),

    /** The button of a floating action button menu, which fills in as its menu opens. */
    ToggleFloatingActionButton(Role.PrimaryContainer, Role.OnPrimaryContainer, Role.Primary, Role.OnPrimary),

    /** One item of an open floating action button menu. */
    FloatingActionButtonMenuItem(Role.PrimaryContainer, Role.OnPrimaryContainer),

    /** A determinate wavy progress bar, its wave on the track. */
    LinearWavyProgressIndicator(Role.Primary, Role.SecondaryContainer),

    /** A wide navigation rail, collapsed or expanded but never modal. */
    WideNavigationRail(Role.Surface, Role.OnSurface),

    /** One destination of a wide navigation rail. */
    WideNavigationRailItem(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.Secondary, Role.OnSurfaceVariant),

    /** The short navigation bar along the bottom of a phone. */
    ShortNavigationBar(Role.SurfaceContainer, Role.OnSurface),

    /** One destination of a short navigation bar. */
    ShortNavigationBarItem(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.Secondary, Role.OnSurfaceVariant),

    /** A circular wavy progress indicator, its wave on the track. */
    CircularWavyProgressIndicator(Role.Primary, Role.SecondaryContainer),

    /** A loading indicator on no container, its shape in the primary color. */
    LoadingIndicator(Role.Primary),

    /** A loading indicator on its own container. */
    ContainedLoadingIndicator(Role.PrimaryContainer, Role.OnPrimaryContainer),

    /** A standard floating toolbar, whose buttons take the content color it sets. */
    FloatingToolbar(Role.SurfaceContainer, Role.OnSurface),
    ;

    val refs: List<ColorRef> = roles.map { role -> ColorRef.OfRole(role) }
}

/** Declare the roles [component] paints on its default colors, for the role usage check and Inspect. */
internal fun Modifier.previewRoles(component: ExpressiveComponent): Modifier =
    previewRoles(*component.refs.toTypedArray())

/** Declare the roles of [component] while [enabled], and of its [disabled] look otherwise. */
internal fun Modifier.previewRoles(
    enabled: Boolean,
    component: ExpressiveComponent,
    disabled: GalleryComponent = GalleryComponent.Disabled,
): Modifier = if (enabled) previewRoles(component) else previewRoles(disabled)
