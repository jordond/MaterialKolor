package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.materialkolor.builder.kit.skin.headless.DisabledAlpha
import io.github.composefluent.component.ButtonColor
import io.github.composefluent.component.CheckBoxColor
import io.github.composefluent.component.SelectorBarItemColor
import io.github.composefluent.component.SwitcherStyle
import io.github.composefluent.scheme.PentaVisualScheme
import io.github.composefluent.scheme.VisualState
import io.github.composefluent.scheme.VisualStateScheme

/**
 * The poster's two colours, for the Fluent controls that stand on it.
 *
 * Fluent's own text is black or white whatever its shades, and on a mid tone seed neither side
 * reaches 4.5 to 1. So on the poster every Fluent control draws its labels, glyphs and strokes in
 * [ink], the way the Material and Unstyled controls there reach the ink through their poster roles.
 * An accent fill takes the ink as well, with [page] on it, the way Material's primary does. Fills
 * that only tint the ground stay Fluent's own, and a disabled control dims its ink the way the
 * headless controls dim.
 *
 * @property[ink] The poster's ink, which holds 4.5 to 1 on the seed.
 * @property[page] The exact seed.
 */
@Immutable
internal class FluentPosterInk(
    val ink: Color,
    val page: Color,
) {
    /** A label, a glyph or a stroke. */
    fun line(enabled: Boolean): Color = if (enabled) ink else ink.copy(alpha = DisabledAlpha)

    /** An accent fill, the ink as strong as Fluent draws [fluent] in this state. */
    fun accent(fluent: Color): Color = ink.copy(alpha = fluent.alpha)

    /** What stands on an accent fill. */
    fun onAccent(enabled: Boolean): Color = if (enabled) page else line(enabled = false)

    /** A stroke in the ink wherever Fluent draws [fluent] at all. */
    fun stroke(
        fluent: Color,
        enabled: Boolean,
    ): Color = if (fluent.alpha == 0f) fluent else line(enabled)

    /** A stroke in the ink wherever Fluent draws [fluent] at all. */
    fun stroke(
        fluent: Brush,
        enabled: Boolean,
    ): Brush = if (fluent is SolidColor && fluent.value.alpha == 0f) fluent else SolidColor(line(enabled))
}

/** The poster's colours inside `PosterSurface` in Fluent, and null everywhere else. */
internal val LocalFluentPosterInk: ProvidableCompositionLocal<FluentPosterInk?> = staticCompositionLocalOf { null }

/** Fluent's button colours on the poster, an ink fill under the seed where [accent] is set. */
internal fun FluentPosterInk.buttons(
    fluent: VisualStateScheme<ButtonColor>,
    accent: Boolean,
): VisualStateScheme<ButtonColor> =
    fluent.eachState { look, enabled ->
        ButtonColor(
            fillColor = if (accent) accent(look.fillColor) else look.fillColor,
            contentColor = if (accent) onAccent(enabled) else line(enabled),
            borderBrush = stroke(look.borderBrush, enabled),
        )
    }

/** Fluent's switch on the poster, its track an ink fill while [checked]. */
internal fun FluentPosterInk.switches(
    fluent: VisualStateScheme<SwitcherStyle>,
    checked: Boolean,
): VisualStateScheme<SwitcherStyle> =
    fluent.eachState { look, enabled ->
        look.copy(
            fillColor = if (checked) accent(look.fillColor) else look.fillColor,
            labelColor = line(enabled),
            controlColor = if (checked) onAccent(enabled) else line(enabled),
            borderBrush = stroke(look.borderBrush, enabled),
        )
    }

/** Fluent's checkbox on the poster, its box an ink fill while [checked]. */
internal fun FluentPosterInk.checkboxes(
    fluent: VisualStateScheme<CheckBoxColor>,
    checked: Boolean,
): VisualStateScheme<CheckBoxColor> =
    fluent.eachState { look, enabled ->
        CheckBoxColor(
            fillColor = if (checked) accent(look.fillColor) else look.fillColor,
            contentColor = onAccent(enabled),
            borderColor = stroke(look.borderColor, enabled),
            labelTextColor = line(enabled),
        )
    }

/** Fluent's selector bar item on the poster. */
internal fun FluentPosterInk.tabs(
    fluent: VisualStateScheme<SelectorBarItemColor>,
): VisualStateScheme<SelectorBarItemColor> =
    fluent.eachState { look, enabled ->
        look.copy(contentColor = line(enabled), indicatorColor = accent(look.indicatorColor))
    }

/** This scheme with [each] applied to the look of every state, told whether that state is enabled. */
private inline fun <T> VisualStateScheme<T>.eachState(each: (look: T, enabled: Boolean) -> T): VisualStateScheme<T> =
    PentaVisualScheme(
        default = each(schemeFor(VisualState.Default), true),
        hovered = each(schemeFor(VisualState.Hovered), true),
        pressed = each(schemeFor(VisualState.Pressed), true),
        disabled = each(schemeFor(VisualState.Disabled), false),
        focused = each(schemeFor(VisualState.Focused), true),
    )
