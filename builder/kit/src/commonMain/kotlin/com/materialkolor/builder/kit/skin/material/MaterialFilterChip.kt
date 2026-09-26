package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.icon.IconId

/**
 * Material's filter chip, a check in place of its icon while it is on. Joined after another
 * control with [connectedStart], its start corners drop to the extra small radius and it draws an
 * outlined field's outline on every edge but its start.
 */
@Composable
internal fun MaterialFilterChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    icon: IconId?,
    enabled: Boolean,
    connectedStart: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val glyph = if (selected) IconId.Check else icon
    val shape = if (connectedStart) connectedStartShape() else FilterChipDefaults.shape
    // Carrying on from another control, it takes an outlined field's outline, on or off, and leaves
    // its start edge to the control it meets, so the seam is one line or none.
    val outline = OutlinedTextFieldDefaults.colors().unfocusedIndicatorColor
    val joinedOutline = if (connectedStart) {
        Modifier.openStartOutline(outline, OutlinedTextFieldDefaults.UnfocusedBorderThickness, shape)
    } else {
        Modifier
    }
    val border = if (connectedStart) null else FilterChipDefaults.filterChipBorder(enabled, selected)
    MaterialTarget {
        FilterChip(
            selected = selected,
            onClick = { onSelectedChange(!selected) },
            label = {
                BuilderText(
                    label,
                    style = BuilderTextStyle.Label,
                    color = LocalContentColor.current,
                    maxLines = 1,
                )
            },
            modifier = modifier
                .semantics { toggleableState = ToggleableState(selected) }
                .foldState(label, ControlState.Selected(selected), enabled)
                .materialFeedback(interactionSource, shape)
                .then(joinedOutline),
            enabled = enabled,
            shape = shape,
            border = border,
            leadingIcon = glyph?.let { id ->
                { BuilderIcon(id, contentDescription = null, tint = LocalContentColor.current) }
            },
            interactionSource = interactionSource,
        )
    }
}

/**
 * Draws [shape]'s outline in [color] at [width] inside the bounds, all but the straight start edge,
 * which the control before it already draws or has no need of.
 */
private fun Modifier.openStartOutline(
    color: Color,
    width: Dp,
    shape: Shape,
): Modifier =
    drawWithContent {
        drawContent()
        val stroke = width.toPx()
        val rtl = layoutDirection == LayoutDirection.Rtl
        val inset = Size(size.width - stroke, size.height - stroke)
        val outline = shape.createOutline(inset, layoutDirection, this)
        clipRect(left = if (rtl) 0f else stroke, right = if (rtl) size.width - stroke else size.width) {
            translate(stroke / 2, stroke / 2) { drawOutline(outline, color, style = Stroke(stroke)) }
        }
    }

/**
 * A filter chip's shape with its start corners down to the theme's extra small radius, the inner
 * radius of a group it carries on.
 */
@Composable
private fun connectedStartShape(): Shape {
    val shapes = MaterialTheme.shapes
    return shapes.small.copy(topStart = shapes.extraSmall.topStart, bottomStart = shapes.extraSmall.bottomStart)
}
