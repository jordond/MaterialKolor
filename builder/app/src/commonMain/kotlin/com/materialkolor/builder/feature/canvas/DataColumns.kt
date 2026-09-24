package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.canvas_mode_dark
import com.materialkolor.builder.generated.resources.canvas_mode_light
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * The light and dark columns a data tab lays out for [mode], in the tab's one scroll area, drawn
 * through [filter].
 *
 * Split puts Light and Dark side by side under their names, or Light above Dark in a Compact
 * window. Light or Dark shows that column alone.
 *
 * @param[mode] Which modes to show.
 * @param[filter] The vision filter the canvas is drawn through, or null for none.
 * @param[modifier] Applied to the scroll area.
 * @param[tabStop] Whether the scroll area takes Tab itself. Pass false when the columns hold
 * something that takes focus.
 * @param[column] What one mode shows, the dark one when its flag is true.
 */
@Composable
internal fun DataColumns(
    mode: PreviewMode,
    filter: ColorMatrix?,
    modifier: Modifier = Modifier,
    tabStop: Boolean = true,
    column: @Composable ColumnScope.(isDark: Boolean) -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val compact = LocalLayout.current.windowClass == WindowClass.Compact
    BuilderScrollArea(modifier.fillMaxSize().visionFilter(filter), tabStop = tabStop) {
        val padded = Modifier.fillMaxWidth().padding(spacing.large)
        when {
            mode != PreviewMode.Split -> {
                ModeColumn(isDark = mode == PreviewMode.Dark, labeled = false, modifier = padded, column = column)
            }
            compact -> {
                Column(padded, verticalArrangement = Arrangement.spacedBy(spacing.section)) {
                    ModeColumn(isDark = false, labeled = true, modifier = Modifier.fillMaxWidth(), column = column)
                    ModeColumn(isDark = true, labeled = true, modifier = Modifier.fillMaxWidth(), column = column)
                }
            }
            else -> {
                Row(padded, horizontalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
                    ModeColumn(isDark = false, labeled = true, modifier = Modifier.weight(1f), column = column)
                    ModeColumn(isDark = true, labeled = true, modifier = Modifier.weight(1f), column = column)
                }
            }
        }
    }
}

/** One mode's column, under its name when [labeled]. */
@Composable
private fun ModeColumn(
    isDark: Boolean,
    labeled: Boolean,
    modifier: Modifier,
    column: @Composable ColumnScope.(isDark: Boolean) -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        if (labeled) {
            BuilderText(
                text = stringResource(if (isDark) Res.string.canvas_mode_dark else Res.string.canvas_mode_light),
                modifier = Modifier.semantics { heading() },
                style = BuilderTextStyle.Title,
            )
        }
        column(isDark)
    }
}

/**
 * Draw the tab through a layer whose paint carries [filter], the way the preview panes wear the
 * vision filter, or leave it be when there is none.
 */
private fun Modifier.visionFilter(filter: ColorMatrix?): Modifier {
    if (filter == null) return this
    return drawWithCache {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(filter) }
        onDrawWithContent {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(size.toRect(), paint)
                drawContent()
                canvas.restore()
            }
        }
    }
}
