package com.materialkolor.sample.fluent.theme

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.materialkolor.fluent.animateFluentColors
import com.materialkolor.fluent.rememberFluentColors
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.theme.isDark
import io.github.composefluent.FluentTheme
import io.github.composefluent.Shades

/**
 * A new seed fades from one ramp to the next. A new mode cuts straight over, because Fluent keeps light and dark
 * as a flag on one set of shades and there is nothing in between to animate.
 */
@Composable
internal fun FluentSampleTheme(
    seed: SampleSeed,
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val generated = rememberFluentColors(seedColor = seed.color, isDark = mode.isDark())
    val colors = animateFluentColors(generated)

    // The tag ramps are built from the accent the fade is heading to, so the hue turns run once per seed rather
    // than on every frame. Each ramp then fades on its own, in step with the accent.
    val targetWork = remember(generated.shades) { tagShades(accent = generated.shades, tag = TaskTag.Work) }
    val targetErrand = remember(generated.shades) { tagShades(accent = generated.shades, tag = TaskTag.Errand) }
    val work = animateShades(target = targetWork, label = "WorkShades")
    val errand = animateShades(target = targetErrand, label = "ErrandShades")

    val sampleColors = remember(colors.shades, work, errand, colors.darkMode, generated.shades) {
        SampleColors(
            accent = colors.shades,
            work = work,
            errand = errand,
            isDark = colors.darkMode,
            accentTarget = generated.shades,
        )
    }

    FluentTheme(colors = colors) {
        CompositionLocalProvider(LocalSampleColors provides sampleColors, content = content)
    }
}

@Composable
private fun animateShades(
    target: Shades,
    label: String,
): Shades {
    val transition = updateTransition(targetState = target, label = label)
    val base by transition.animateColor(label = "base") { shades -> shades.base }
    val light1 by transition.animateColor(label = "light1") { shades -> shades.light1 }
    val light2 by transition.animateColor(label = "light2") { shades -> shades.light2 }
    val light3 by transition.animateColor(label = "light3") { shades -> shades.light3 }
    val dark1 by transition.animateColor(label = "dark1") { shades -> shades.dark1 }
    val dark2 by transition.animateColor(label = "dark2") { shades -> shades.dark2 }
    val dark3 by transition.animateColor(label = "dark3") { shades -> shades.dark3 }

    return Shades(
        base = base,
        light1 = light1,
        light2 = light2,
        light3 = light3,
        dark1 = dark1,
        dark2 = dark2,
        dark3 = dark3,
    )
}

internal object SampleTheme {
    val colors: SampleColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleColors.current
}

private val LocalSampleColors = compositionLocalOf<SampleColors> {
    error("No SampleColors provided, wrap the content in FluentSampleTheme.")
}
