package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

@Immutable
internal data class Slot(
    val x: Int,
    val width: Int,
)

/**
 * Remembers where each option of a row sits, so a [SlidingThumb] can move between them.
 */
@Stable
internal class SlidingThumbState {
    private val slots = mutableStateMapOf<Any?, Slot>()

    internal fun slotOf(key: Any?): Slot? = slots[key]

    internal fun place(
        key: Any?,
        slot: Slot,
    ) {
        if (slots[key] != slot) slots[key] = slot
    }
}

@Composable
internal fun rememberSlidingThumbState(): SlidingThumbState = remember { SlidingThumbState() }

/**
 * Marks the option for [key] as a place the thumb can slide to.
 */
internal fun Modifier.thumbSlot(
    state: SlidingThumbState,
    key: Any?,
): Modifier =
    onPlaced { coordinates ->
        val slot = Slot(x = coordinates.positionInParent().x.roundToInt(), width = coordinates.size.width)
        state.place(key, slot)
    }

/**
 * Covers the option for [selected] and slides over when it changes.
 *
 * Put it in a Box before the row of options, so it draws underneath them. [modifier] styles the thumb itself.
 */
@Composable
internal fun BoxScope.SlidingThumb(
    state: SlidingThumbState,
    selected: Any?,
    modifier: Modifier = Modifier,
) {
    val slot = state.slotOf(selected) ?: return
    val x by animateIntAsState(targetValue = slot.x, label = "thumbX")
    val width by animateIntAsState(targetValue = slot.width, label = "thumbWidth")

    Box(
        modifier = Modifier
            .matchParentSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x, 0) }
            }.then(modifier),
    )
}
