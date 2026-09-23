package com.materialkolor.builder.preview.split

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.generated.resources.Res
import com.materialkolor.builder.preview.generated.resources.split_handle_label
import com.materialkolor.builder.preview.generated.resources.split_handle_state
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The handle between the two copies of a split preview.
 *
 * It places itself with an offset and draws its line in its draw block, and those are the only
 * places it reads [split], so a drag touches placement and drawing and nothing else. A drag moves it,
 * each arrow key moves it by five percent, Home and End send it to either edge, and a double click
 * or Enter puts it back in the middle. Assistive tech sees a slider that says how much of the start
 * copy shows. Its words load in composition and the percentage goes in inside the semantics block,
 * so a drag still recomposes nothing.
 *
 * @param[split] Where the handle sits.
 * @param[orientation] Horizontal for side by side, vertical for top and bottom.
 * @param[startLabel] The start copy's label, read out after the percentage.
 * @param[size] The size of the preview, read only outside composition.
 * @param[modifier] Applied to the layer the handle moves in, which should match the preview.
 */
@Composable
internal fun SplitHandle(
    split: SplitState,
    orientation: Orientation,
    startLabel: String,
    size: () -> IntSize,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val horizontal = orientation == Orientation.Horizontal
    val label = stringResource(Res.string.split_handle_label)
    val stateFormat = stringResource(Res.string.split_handle_state)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val dragState = rememberDraggableState { delta ->
        val extent = size().along(horizontal)
        if (extent > 0) split.fraction += delta / extent
    }
    val thickness = tokens.spacing.section
    val span = if (horizontal) Modifier.fillMaxHeight().width(thickness) else Modifier.fillMaxWidth().height(thickness)
    Box(modifier) {
        Box(
            Modifier
                .offset {
                    val start = handleStart(size().along(horizontal), split.fraction, thickness.roundToPx())
                    if (horizontal) IntOffset(start, 0) else IntOffset(0, start)
                }.then(span)
                .semantics {
                    val fraction = split.fraction
                    contentDescription = label
                    stateDescription = stateFormat.fillIn(percent = (fraction * 100).roundToInt(), label = startLabel)
                    progressBarRangeInfo = ProgressBarRangeInfo(current = fraction, range = 0f..1f)
                    setProgress(label) { target ->
                        split.fraction = target
                        true
                    }
                }.onKeyEvent { event -> split.onKey(event, horizontal, isRtl) }
                .focusable(interactionSource = interactionSource)
                .pointerInput(split) { detectTapGestures(onDoubleTap = { split.reset() }) }
                .draggable(
                    state = dragState,
                    orientation = orientation,
                    reverseDirection = isRtl && horizontal,
                    interactionSource = interactionSource,
                ).drawBehind {
                    val extent = size().along(horizontal)
                    val fromStart = extent * split.fraction - handleStart(extent, split.fraction, thickness.roundToPx())
                    val along = if (isRtl && horizontal) thickness.toPx() - fromStart else fromStart
                    drawHandle(tokens, along, horizontal, focused)
                },
        )
    }
}

/** Move, send to an edge or reset the split for one key press, and say whether the key was used. */
private fun SplitState.onKey(
    event: KeyEvent,
    horizontal: Boolean,
    isRtl: Boolean,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val forward = if (isRtl) -SplitState.STEP else SplitState.STEP
    val step = when (event.key) {
        Key.DirectionRight -> if (horizontal) forward else null
        Key.DirectionLeft -> if (horizontal) -forward else null
        Key.DirectionDown -> if (horizontal) null else SplitState.STEP
        Key.DirectionUp -> if (horizontal) null else -SplitState.STEP
        else -> null
    }
    when {
        step != null -> fraction += step
        event.key == Key.MoveHome -> fraction = 0f
        event.key == Key.MoveEnd -> fraction = 1f
        event.key == Key.Enter || event.key == Key.NumPadEnter -> reset()
        else -> return false
    }
    return true
}

/**
 * The handle's state text, with [percent] and [label] put in where the string resource's `%1$d`
 * and `%2$s` stand.
 *
 * It runs in the semantics block, outside composition, so it cannot take the resource's own
 * formatting.
 */
private fun String.fillIn(
    percent: Int,
    label: String,
): String = replace("%1\$d", percent.toString()).replace("%2\$s", label)

/** The handle's line across the preview, the grip on it, and the focus ring round the grip. */
private fun DrawScope.drawHandle(
    tokens: BuilderTokens,
    along: Float,
    horizontal: Boolean,
    focused: Boolean,
) {
    val line = tokens.spacing.extraSmall.toPx() / 2
    val across = if (horizontal) size.width else size.height
    val radius = across / 2 - tokens.spacing.extraSmall.toPx()
    val length = if (horizontal) size.height else size.width
    val gripAlong = along.coerceIn(radius, across - radius)
    if (horizontal) {
        drawLine(tokens.borderStrong, Offset(along, 0f), Offset(along, length), strokeWidth = line)
    } else {
        drawLine(tokens.borderStrong, Offset(0f, along), Offset(length, along), strokeWidth = line)
    }
    val grip = if (horizontal) Offset(gripAlong, length / 2) else Offset(length / 2, gripAlong)
    drawCircle(tokens.panelRaised, radius, grip)
    drawCircle(tokens.borderStrong, radius, grip, style = Stroke(line))
    if (focused) drawCircle(tokens.focus, radius + line * 2, grip, style = Stroke(line))
}

/** The handle's distance from the start edge, centred on the split and kept inside the preview. */
private fun handleStart(
    extent: Int,
    fraction: Float,
    thickness: Int,
): Int = (extent * fraction - thickness / 2f).roundToInt().coerceIn(0, max(0, extent - thickness))

private fun IntSize.along(horizontal: Boolean): Int = if (horizontal) width else height
