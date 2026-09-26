package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape

/**
 * The connected shapes for the option at [index] of [count], by where it sits in the row. Checked
 * keeps the resting shape, round on the row's outer ends and the group's small radius inside, where
 * Material would morph it into a full pill. Picking then changes the fill and the content, never
 * the row's silhouette. With [connectedEnd] the last option carries on into a control after the row,
 * so its end corners drop to the theme's extra small radius, the same as that control's start.
 */
@Composable
internal fun connectedShapes(
    index: Int,
    count: Int,
    connectedEnd: Boolean,
): ToggleButtonShapes {
    if (count == 1) {
        val full = ButtonGroupDefaults.connectedButtonCheckedShape
        return ToggleButtonShapes(shape = full, pressedShape = full, checkedShape = full)
    }
    if (connectedEnd && index == count - 1) {
        val shape = joinedEnd(ButtonGroupDefaults.connectedMiddleButtonShapes().shape)
        return ToggleButtonShapes(shape = shape, pressedShape = shape, checkedShape = shape)
    }
    val shapes = when {
        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
    return ToggleButtonShapes(shape = shapes.shape, pressedShape = shapes.pressedShape, checkedShape = shapes.shape)
}

/**
 * [shape] with its end corners at the theme's extra small radius, where a row's last option meets
 * the control joined after it. A shape with no corners to change stays as it is.
 */
@Composable
internal fun joinedEnd(shape: Shape): Shape {
    val inner = MaterialTheme.shapes.extraSmall
    return (shape as? CornerBasedShape)?.copy(topEnd = inner.topEnd, bottomEnd = inner.bottomEnd) ?: shape
}
