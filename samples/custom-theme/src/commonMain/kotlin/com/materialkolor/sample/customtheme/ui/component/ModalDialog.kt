package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Composable
internal fun ModalDialog(
    title: String,
    body: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties(scrim = colors.scrim.copy(alpha = SCRIM_ALPHA)),
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .rotate(SLIP_TILT),
        ) {
            Column(
                modifier = modifier
                    .width(440.dp)
                    .halftone(color = colors.blue, colors = colors, shape = SlipShape, offset = SlipShadow) { _, _ ->
                        SHADOW_COVERAGE
                    }.background(colors.paper, SlipShape)
                    .padding(horizontal = 28.dp, vertical = 36.dp),
            ) {
                Text(
                    text = title.uppercase(),
                    style = AppType.Display,
                    color = colors.ink,
                )

                Text(
                    text = body,
                    style = AppType.Body,
                    color = colors.inkSoft,
                    modifier = Modifier.padding(top = 12.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                    content = buttons,
                )
            }
        }
    }
}

/**
 * Straight sides with a ragged top and bottom, the edge a sheet gets when it is torn along a perforation.
 */
internal class TornShape(
    private val seed: Int,
    private val tooth: Dp = 7.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val depth = with(density) { tooth.toPx() }
        val random = Random(seed)
        val path = Path()

        path.moveTo(0f, depth * random.nextFloat())
        var x = 0f
        while (x < size.width) {
            x = min(size.width, x + depth * (TOOTH_MIN + random.nextFloat() * TOOTH_RANGE))
            path.lineTo(x, depth * random.nextFloat())
        }

        path.lineTo(size.width, size.height - depth * random.nextFloat())
        while (x > 0f) {
            x = max(0f, x - depth * (TOOTH_MIN + random.nextFloat() * TOOTH_RANGE))
            path.lineTo(x, size.height - depth * random.nextFloat())
        }
        path.close()

        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean = other is TornShape && other.seed == seed && other.tooth == tooth

    override fun hashCode(): Int = 31 * seed + tooth.hashCode()
}

private val SlipShape = TornShape(seed = 7)
private val SlipShadow = DpOffset(12.dp, 12.dp)

private const val SCRIM_ALPHA = 0.55f
private const val SLIP_TILT = -1.2f
private const val SHADOW_COVERAGE = 0.45f
private const val TOOTH_MIN = 0.6f
private const val TOOTH_RANGE = 1.4f
