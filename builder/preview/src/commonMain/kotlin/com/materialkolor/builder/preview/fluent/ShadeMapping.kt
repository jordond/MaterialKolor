package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.fluent.toFluentShades
import com.materialkolor.ktx.toHct
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Layer
import io.github.composefluent.component.Text
import kotlin.math.roundToInt

private const val LegendTitle = "Where your accent goes"
private const val LegendNote = "Only the accent follows your theme. Fluent keeps its neutrals, text colors and " +
    "system colors fixed."
private const val AccentFillUse = "Switches that are on, accent buttons, the current page"

/**
 * The shade legend, the seven accent shades Fluent cuts from the pane's primary ramp, in order
 * from darkest to lightest, each with its color, its tone and where the real controls paint it in
 * this mode.
 *
 * Every swatch declares its own shade, which makes the legend the one place Light1 shows, since no
 * Fluent control paints it. Under the swatches sits the note that Fluent keeps its neutrals and
 * system colors fixed.
 *
 * @param[spec] The pane, for its scheme and its mode.
 * @param[modifier] Applied to the legend.
 */
@Composable
internal fun ShadeMapping(
    spec: PaneSpec,
    modifier: Modifier = Modifier,
) {
    val scheme = spec.result.scheme(spec.isDark)
    val shades = remember(scheme) { scheme.primaryPalette.toFluentShades() }
    Layer(modifier = modifier, shape = FluentTheme.shapes.overlay) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(LegendTitle, style = FluentTheme.typography.subtitle)
            for (shade in FluentShade.entries) {
                ShadeRow(shade, shades.color(shade), spec.isDark)
            }
            Text(
                text = LegendNote,
                style = FluentTheme.typography.caption,
                color = FluentTheme.colors.text.text.secondary,
            )
        }
    }
}

@Composable
private fun ShadeRow(
    shade: FluentShade,
    color: Color,
    isDark: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val swatch = RoundedCornerShape(4.dp)
        Box(
            Modifier
                .fluentShadeRoles(shade)
                .size(32.dp)
                .background(color, swatch)
                .border(1.dp, FluentTheme.colors.stroke.card.default, swatch),
        )
        Column {
            val name = shade.name.replaceFirstChar { first -> first.lowercaseChar() }
            Text("$name, tone ${color.toHct().tone.roundToInt()}")
            Text(
                text = shade.usage(isDark),
                style = FluentTheme.typography.caption,
                color = FluentTheme.colors.text.text.secondary,
            )
        }
    }
}

/**
 * Where the real Fluent controls paint each shade in each mode, from how compose-fluent builds its
 * colors out of the seven shades.
 */
private fun FluentShade.usage(isDark: Boolean): String =
    when (this) {
        FluentShade.Dark3 -> if (isDark) "Not used in dark mode" else "Secondary accent text"
        FluentShade.Dark2 -> if (isDark) "Accent acrylic backdrop" else "Accent text and links"
        FluentShade.Dark1 -> if (isDark) "Accent acrylic fill" else AccentFillUse
        FluentShade.Base -> "Selected text highlight"
        FluentShade.Light1 -> "Not drawn by any control today"
        FluentShade.Light2 -> if (isDark) AccentFillUse else "Not used in light mode"
        FluentShade.Light3 -> if (isDark) "Accent text and links" else "Accent acrylic fill"
    }
