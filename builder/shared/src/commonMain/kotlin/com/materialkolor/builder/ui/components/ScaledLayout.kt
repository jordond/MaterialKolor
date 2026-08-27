package com.materialkolor.builder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

@Composable
fun ScaledLayoutByHeight(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Layout(
        content = { Box(content = content) },
        modifier = modifier,
    ) { measurables, constraints ->
        val placeable =
            measurables
                .single()
                .measure(Constraints(maxWidth = constraints.maxWidth))

        val scale =
            if (placeable.height <= 0) {
                1f
            } else {
                (constraints.maxHeight.toFloat() / placeable.height).coerceAtMost(1f)
            }

        val offsetX = (constraints.maxWidth - placeable.width) / 2
        val offsetY = (constraints.maxHeight - placeable.height) / 2
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelativeWithLayer(x = offsetX, y = offsetY) {
                this.scaleX = scale
                this.scaleY = scale
            }
        }
    }
}

@Composable
fun ScaledLayoutByWidth(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Layout(
        content = { Box(content = content) },
        modifier = modifier,
    ) { measurables, constraints ->
        val placeable =
            measurables
                .single()
                .measure(Constraints(maxHeight = constraints.maxHeight))

        val scale =
            if (placeable.width <= 0) {
                1f
            } else {
                (constraints.maxWidth.toFloat() / placeable.width).coerceAtMost(1f)
            }

        val offsetX = (constraints.maxWidth - placeable.width) / 2
        val offsetY = (constraints.maxHeight - placeable.height) / 2
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelativeWithLayer(x = offsetX, y = offsetY) {
                this.scaleX = scale
                this.scaleY = scale
            }
        }
    }
}
