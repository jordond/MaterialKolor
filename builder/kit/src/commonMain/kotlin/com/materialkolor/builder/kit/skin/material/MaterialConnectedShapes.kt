package com.materialkolor.builder.kit.skin.material

import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable

/**
 * The connected shapes for the option at [index] of [count], by where it sits in the row. Checked
 * keeps the resting shape, round on the row's outer ends and the group's small radius inside, where
 * Material would morph it into a full pill. Picking then changes the fill and the content, never
 * the row's silhouette.
 */
@Composable
internal fun connectedShapes(
    index: Int,
    count: Int,
): ToggleButtonShapes {
    if (count == 1) {
        val full = ButtonGroupDefaults.connectedButtonCheckedShape
        return ToggleButtonShapes(shape = full, pressedShape = full, checkedShape = full)
    }
    val shapes = when {
        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
    return ToggleButtonShapes(shape = shapes.shape, pressedShape = shapes.pressedShape, checkedShape = shapes.shape)
}
