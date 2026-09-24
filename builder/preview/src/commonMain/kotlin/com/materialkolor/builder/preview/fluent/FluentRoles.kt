package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.audit.FluentText
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.inspect.previewRoles
import io.github.composefluent.Shades

// The colors the Fluent screens declare and the names Inspect gives them, shared by the Settings
// app and the gallery.
//
// Fluent paints its accent fill, `fillAccent.default`, in Dark1 in light mode and Light2 in dark
// mode. Those are tones 40 and 80 of the primary ramp, the tones primary takes, so the contrast
// audit names that fill Role.Primary and rates Fluent's two on-accent text colors over it and
// nothing else. An element painted with the accent fill therefore declares an on-accent text
// color first and Role.Primary second, never the shade itself. Declared as a shade, the pair would
// match no row of the audit and its Inspect card would show no contrast badge. Only the shade
// legend declares shades, one per swatch.

/** The accent fill and the on-accent ink on it, the one Fluent pair the contrast audit rates. */
internal val FluentAccentRefs: List<ColorRef> =
    listOf(ColorRef.OfFluentText(FluentText.OnAccentPrimary), ColorRef.OfRole(Role.Primary))

/**
 * Declare an element painted with Fluent's accent fill, a switch that is on, a checked box, an
 * accent button or the selected page's pill, with [text] as the ink on it.
 *
 * It declares [text] and then [Role.Primary], exactly the pair the contrast audit rates, so the
 * element's Inspect card finds its row.
 */
internal fun Modifier.fluentAccentRoles(text: FluentText = FluentText.OnAccentPrimary): Modifier =
    previewRoles(ColorRef.OfFluentText(text), ColorRef.OfRole(Role.Primary))

/**
 * Declare a control that paints only Fluent's fixed greys, which no scheme color names.
 *
 * It declares no colors at all, so Inspect still finds the control and says it has none.
 */
internal fun Modifier.fluentNeutralRoles(): Modifier = previewRoles()

/** Declare one swatch of the shade legend, the only place a Fluent shade is declared as itself. */
internal fun Modifier.fluentShadeRoles(shade: FluentShade): Modifier = previewRoles(ColorRef.OfFluentShade(shade))

/** The shade the accent fill takes in the mode [isDark] picks. */
internal fun fillAccentShade(isDark: Boolean): FluentShade = if (isDark) FluentShade.Light2 else FluentShade.Dark1

/**
 * The Fluent shade [this] paints in the mode [isDark] picks, or null when it names no shade.
 *
 * [Role.Primary] stands for the accent fill, the only role a Fluent screen declares.
 */
internal fun ColorRef.paintedShade(isDark: Boolean): FluentShade? =
    when (this) {
        is ColorRef.OfFluentShade -> shade
        ColorRef.OfRole(Role.Primary) -> fillAccentShade(isDark)
        else -> null
    }

/** The color [shade] takes in [shades]. */
internal fun Shades.color(shade: FluentShade): Color =
    when (shade) {
        FluentShade.Dark3 -> dark3
        FluentShade.Dark2 -> dark2
        FluentShade.Dark1 -> dark1
        FluentShade.Base -> base
        FluentShade.Light1 -> light1
        FluentShade.Light2 -> light2
        FluentShade.Light3 -> light3
    }

/**
 * Draws a compose-fluent control whose own clickable no modifier reaches, with [control] laid
 * over it to take its place.
 *
 * Fluent's `Switcher` takes no modifier at all, and a navigation item or an expander keeps its
 * clickable inside. Neither can carry the colors it declares or the name the web reads. The
 * layer over it takes every pointer event and all of the semantics instead, and the control
 * underneath is kept out of focus and out of the semantics tree. Pass [control] the same
 * interaction source as the control, so the control still shows hover and press. Put
 * `previewRoles` first in [control], then the toggle or selection, then the fold.
 *
 * @param[control] The declared colors, the toggle or selection and the web fold.
 * @param[modifier] Applied to the control and its layer together.
 * @param[content] The compose-fluent control.
 */
@Composable
internal fun FluentOverlaid(
    control: Modifier,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(Modifier.focusProperties { canFocus = false }.clearAndSetSemantics {}) { content() }
        Box(Modifier.matchParentSize().then(control))
    }
}
