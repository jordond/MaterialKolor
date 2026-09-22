package com.materialkolor.fluent

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.InternalMaterialKolorApi
import com.materialkolor.ktx.defaultColorSpring
import io.github.composefluent.Colors
import io.github.composefluent.Shades

/**
 * Animates the accent ramp behind [colors] so a change of seed fades rather than jumps.
 *
 * ```kotlin
 * val colors = animateFluentColors(rememberFluentColors(seedColor = ThemeSettings.seedColor))
 *
 * FluentTheme(colors = colors) {
 *     Button(onClick = { ThemeSettings.seedColor = Color(0xFF00695C) }) {
 *         Text("Teal")
 *     }
 * }
 * ```
 *
 * Only the seven shades animate, and that is the whole of what Fluent exposes. Every field on
 * [Colors] has an `internal set` and the twelve groups are derived in the constructor, so a new
 * [Colors] is built each frame from the seven colors as they move. The groups that come from the
 * ramp follow along. The ones built from Fluent's own constants, `system` and `controlOnImage`,
 * hold still because nothing in them ever depended on the accent.
 *
 * Flipping [Colors.darkMode] is a cut rather than a fade for the same reason. Fluent derives light
 * and dark from one set of shades and a flag, so there is no pair of colors to move between.
 *
 * @param[colors] The colors to animate towards, usually from [rememberFluentColors].
 * @param[animationSpec] How each of the seven shades animates.
 * @param[label] A debugging label for the transition.
 * @return [Colors] whose ramp tracks [colors] over time.
 */
@OptIn(InternalMaterialKolorApi::class)
@Composable
public fun animateFluentColors(
    colors: Colors,
    animationSpec: @Composable Transition.Segment<Shades>.() -> FiniteAnimationSpec<Color> = {
        defaultColorSpring
    },
    label: String = "FluentColorsAnimation",
): Colors {
    // Shades is a data class, so the transition restarts on a real change of ramp rather than on
    // every new Colors instance.
    val transition = updateTransition(targetState = colors.shades, label = label)

    val base by transition.animateColor(
        label = "shade_base",
        targetValueByState = { shades -> shades.base },
        transitionSpec = animationSpec,
    )
    val light1 by transition.animateColor(
        label = "shade_light1",
        targetValueByState = { shades -> shades.light1 },
        transitionSpec = animationSpec,
    )
    val light2 by transition.animateColor(
        label = "shade_light2",
        targetValueByState = { shades -> shades.light2 },
        transitionSpec = animationSpec,
    )
    val light3 by transition.animateColor(
        label = "shade_light3",
        targetValueByState = { shades -> shades.light3 },
        transitionSpec = animationSpec,
    )
    val dark1 by transition.animateColor(
        label = "shade_dark1",
        targetValueByState = { shades -> shades.dark1 },
        transitionSpec = animationSpec,
    )
    val dark2 by transition.animateColor(
        label = "shade_dark2",
        targetValueByState = { shades -> shades.dark2 },
        transitionSpec = animationSpec,
    )
    val dark3 by transition.animateColor(
        label = "shade_dark3",
        targetValueByState = { shades -> shades.dark3 },
        transitionSpec = animationSpec,
    )

    val animated = Shades(
        base = base,
        light1 = light1,
        light2 = light2,
        light3 = light3,
        dark1 = dark1,
        dark2 = dark2,
        dark3 = dark3,
    )

    return Colors(shades = animated, darkMode = colors.darkMode)
}
