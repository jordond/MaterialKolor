package com.materialkolor.builder.preview.split

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Where the split handle sits.
 *
 * @param[initial] The fraction to open at, half and half unless told otherwise.
 */
@Stable
public class SplitState(
    initial: Float = HALF,
) {
    init {
        require(initial in 0f..1f) { "A split fraction lies between 0 and 1, got $initial" }
    }

    private val state = mutableFloatStateOf(initial)

    /**
     * How much of the preview the start copy shows, from 0 at the start edge to 1 at the end edge.
     *
     * A value past either end is held at that end. Read it only in layer, placement and draw
     * blocks, so moving the handle never recomposes a pane.
     */
    public var fraction: Float
        get() = state.floatValue
        set(value) {
            state.floatValue = value.coerceIn(0f, 1f)
        }

    /** Put the handle back in the middle. */
    public fun reset() {
        fraction = HALF
    }

    internal companion object {
        /** Where a split opens and where a reset puts it. */
        const val HALF: Float = 0.5f

        /** How far one arrow key moves the handle. */
        const val STEP: Float = 0.05f
    }
}

/**
 * The part of the end copy past the handle.
 *
 * A new shape per fraction keeps the layer's outline cache honest. Side by side, the start copy
 * holds the start edge, so the kept rectangle mirrors in right to left. Stacked, the start copy is
 * on top whatever the direction.
 *
 * @param[fraction] How much of the pane the start copy shows.
 * @param[orientation] Horizontal for side by side, vertical for top and bottom.
 */
public class SplitShape(
    private val fraction: Float,
    private val orientation: Orientation = Orientation.Horizontal,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val rect = when (orientation) {
            Orientation.Horizontal -> {
                if (layoutDirection == LayoutDirection.Ltr) {
                    Rect(left = size.width * fraction, top = 0f, right = size.width, bottom = size.height)
                } else {
                    Rect(left = 0f, top = 0f, right = size.width * (1f - fraction), bottom = size.height)
                }
            }
            Orientation.Vertical -> {
                Rect(left = 0f, top = size.height * fraction, right = size.width, bottom = size.height)
            }
        }
        return Outline.Rectangle(rect)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is SplitShape && fraction == other.fraction && orientation == other.orientation)

    override fun hashCode(): Int = 31 * fraction.hashCode() + orientation.hashCode()

    override fun toString(): String = "SplitShape(fraction=$fraction, orientation=$orientation)"
}
