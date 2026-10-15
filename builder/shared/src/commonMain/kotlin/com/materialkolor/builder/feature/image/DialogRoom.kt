package com.materialkolor.builder.feature.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The tallest a row of dialog buttons stands, the touch target a phone asks of a primary action.
 */
private val TallestButtonRow: Dp = LayoutInfo(widthDp = 0.dp, heightDp = 0.dp, coarsePointer = true).primaryTouchTarget

/**
 * The room a kit dialog needs under its body, the gap over its buttons and the buttons themselves.
 */
@Composable
internal fun dialogButtonRoom(): Dp = LocalBuilderTokens.current.spacing.large + TallestButtonRow

/**
 * Keeps [room] free under a dialog's body, so a body that grows to the height it is offered never
 * squeezes the buttons out, as a picture or a list would on a phone on its side.
 *
 * A kit dialog offers its body the window's height less its margin, its padding, its title and the
 * rest of the body above it. So once [room] is kept as well, the body gets the height left after
 * all of the chrome. A dialog that already keeps its buttons' room, as Material's own does, only
 * gives up a little more height. A body offered no bound at all keeps none.
 */
internal fun Modifier.leaveRoomBelow(room: Dp): Modifier =
    layout { measurable, constraints ->
        val offered = if (constraints.hasBoundedHeight) {
            val most = (constraints.maxHeight - room.roundToPx()).coerceAtLeast(0)
            constraints.copy(minHeight = minOf(constraints.minHeight, most), maxHeight = most)
        } else {
            constraints
        }
        val placeable = measurable.measure(offered)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
