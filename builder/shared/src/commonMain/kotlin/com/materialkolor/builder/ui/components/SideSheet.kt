package com.materialkolor.builder.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.materialkolor.builder.ui.ktx.conditional
import com.materialkolor.builder.ui.ktx.debugBorder
import com.materialkolor.builder.ui.ktx.whenNotNull
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SideSheetPosition {
    Start,
    End,
}

// TODO: Replace when Material3 includes the SideSheet component
@Composable
fun SideSheet(
    modifier: Modifier = Modifier,
    position: SideSheetPosition = SideSheetPosition.Start,
    initialExpanded: Boolean = false,
    maxWidthFraction: Float = 1f / 2.5f,
    minWidth: Dp = 200.dp,
    visibleWidth: Dp = 30.dp,
    sheetCornerRadius: Dp = 25.dp,
    isFloating: Boolean = false,
    displayOverContent: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    sheetContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var lastDragState by remember {
        mutableStateOf(if (initialExpanded) DragValue.Expanded else DragValue.Collapsed)
    }

    BoxWithConstraints {
        val maxSheetWidth = maxWidth * maxWidthFraction
        val sheetWidth = min(max(minWidth, maxSheetWidth), maxWidth)

        val maxWidthPx = with(density) { maxWidth.toPx() }
        val sheetWidthPx = with(density) { sheetWidth.toPx() }

        val anchors = remember(sheetWidth, visibleWidth, position) {
            DraggableAnchors {
                DragValue.Collapsed at with(density) { -sheetWidth.toPx() + visibleWidth.toPx() }
                DragValue.Expanded at 0f
            }
        }

        val velocityThreshold = AnchoredDraggableDefaults.VelocityThreshold()
        val state = remember(position, sheetWidth, maxWidth) {
            AnchoredDraggableState(
                initialValue = lastDragState,
                anchors = anchors,
                positionalThreshold = AnchoredDraggableDefaults.PositionalThreshold,
                velocityThreshold = velocityThreshold,
                snapAnimationSpec = AnchoredDraggableDefaults.SnapAnimationSpec,
                decayAnimationSpec = AnchoredDraggableDefaults.DecayAnimationSpec,
                confirmValueChange = { newValue ->
                    lastDragState = newValue
                    true
                },
            )
        }

        val contentWidth = remember(maxWidthPx, sheetWidthPx, state.offset) {
            if (displayOverContent) null
            else {
                val visibleSheetWidth = sheetWidthPx + state.offset
                val contentWidthPx = maxWidthPx - visibleSheetWidth
                (contentWidthPx / density.density).dp
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(contentContainerColor),
        ) {
            val contentAlignment = when (position) {
                SideSheetPosition.Start -> Alignment.CenterEnd
                SideSheetPosition.End -> Alignment.CenterStart
            }

            Box(
                contentAlignment = contentAlignment,
                modifier = Modifier
                    .fillMaxSize()
                    .background(contentContainerColor),
            ) {
                Surface(
                    color = contentContainerColor,
                    modifier = Modifier.whenNotNull(contentWidth) { Modifier.width(it) },
                ) {
                    content()
                }
            }

            Surface(
                color = containerColor,
                shape = position.sheetShape(sheetCornerRadius),
                modifier = Modifier
                    .align(
                        when (position) {
                            SideSheetPosition.Start -> Alignment.CenterStart
                            SideSheetPosition.End -> Alignment.CenterEnd
                        }
                    )
                    .width(sheetWidth)
                    .clipToBounds()
                    .conditional(isFloating) {
                        Modifier.padding(vertical = 32.dp)
                    }
                    .offset {
                        IntOffset(
                            y = 0,
                            x = when (position) {
                                SideSheetPosition.Start -> state.offset.roundToInt()
                                SideSheetPosition.End -> -state.offset.roundToInt()
                            },
                        )
                    }
                    .anchoredDraggable(
                        state = state,
                        orientation = Orientation.Horizontal,
                        reverseDirection = position == SideSheetPosition.End,
                        enabled = true,
                    )
                    .clip(position.sheetShape(sheetCornerRadius)),
            ) {
                val panel = @Composable {
                    ExpandCollapsePanel(
                        value = state.currentValue,
                        visibleWidth = visibleWidth,
                        sheetPosition = position,
                        onClick = {
                            scope.launch {
                                state.animateTo(state.currentValue.opposite)
                            }
                        },
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (position == SideSheetPosition.End) panel()

                    Box(modifier = Modifier.weight(1f)) {
                        sheetContent()
                    }

                    if (position == SideSheetPosition.Start) panel()
                }
            }
        }
    }
}

private fun SideSheetPosition.sheetShape(radius: Dp): RoundedCornerShape {
    return when (this) {
        SideSheetPosition.Start -> RoundedCornerShape(topEnd = radius, bottomEnd = radius)
        SideSheetPosition.End -> RoundedCornerShape(topStart = radius, bottomStart = radius)
    }
}

private enum class DragValue {
    Expanded,
    Collapsed
}

private val DragValue.opposite: DragValue
    get() = when (this) {
        DragValue.Expanded -> DragValue.Collapsed
        DragValue.Collapsed -> DragValue.Expanded
    }

@Composable
private fun ExpandCollapsePanel(
    value: DragValue,
    visibleWidth: Dp,
    sheetPosition: SideSheetPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpanded = value == DragValue.Expanded
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(visibleWidth)
            .clickable(onClick = onClick)
            .fillMaxHeight(),
    ) {
        val icon = when (sheetPosition) {
            SideSheetPosition.Start -> if (isExpanded) Default.ChevronLeft else Default.ChevronRight
            SideSheetPosition.End -> if (isExpanded) Default.ChevronRight else Default.ChevronLeft
        }

        Icon(
            imageVector = icon,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.size(32.dp),
        )
    }
}

object AnchoredDraggableDefaults {

    /** The default spec for snapping, a tween spec */
    val SnapAnimationSpec: AnimationSpec<Float> = tween()

    /** The default positional threshold, 50% of the distance */
    val PositionalThreshold: (Float) -> Float = { distance -> distance * 0.5f }

    /** The default velocity threshold, 125 dp per second */
    @Composable
    fun VelocityThreshold(): () -> Float {
        val density = LocalDensity.current
        return { with(density) { 125.dp.toPx() } }
    }

    /** The default spec for decaying, an exponential decay */
    val DecayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay()
}

