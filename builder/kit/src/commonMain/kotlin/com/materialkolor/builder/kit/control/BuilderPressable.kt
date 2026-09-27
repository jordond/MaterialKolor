package com.materialkolor.builder.kit.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A press target around something an app draws for itself, such as a picture that opens a larger
 * copy of itself.
 *
 * It draws nothing of its own but the press and the focus ring every skin's buttons show, so what it
 * holds is the whole control. It reads as a button named [label], and on the web a disabled one says
 * so in its name, "Open the eyedropper, disabled". What it holds is only drawn and reads as
 * nothing, since [label] already says what pressing it does. A card whose own text should be read
 * out is a [BuilderCard] with a click instead.
 *
 * A [selected] one is the current one of a set, such as the preset a theme started from. It draws
 * a ring in the skin's accent a little outside [shape], and on the web it says so in its name,
 * "Kelp picture, selected". The focus ring still draws over it.
 *
 * It takes at least the layout's primary touch target, as the other kit controls do, with [content]
 * in the middle of it.
 *
 * @param[onClick] Called when it is pressed.
 * @param[label] What pressing it does, read out as its name.
 * @param[modifier] Applied to the pressable.
 * @param[enabled] Whether it can be pressed. A disabled one draws faint.
 * @param[shape] The outline [content] is clipped to and the focus ring follows.
 * @param[selected] Whether it is the current one of a set.
 * @param[content] What it shows.
 */
@Composable
public fun BuilderPressable(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(LocalBuilderTokens.current.radius.small),
    selected: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val name = stateName(label, state = if (selected) ControlState.Selected(true) else null, enabled)
    val ring = if (selected) LocalBuilderTokens.current.accent else Color.Unspecified
    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).semantics {
                contentDescription = name
                if (selected) this.selected = true
            }
            .clearAndSetSemantics { }
            .controlTouchTarget(LocalLayout.current.primaryTouchTarget)
            .controlPress(interactionSource)
            .alpha(enabledAlpha(enabled))
            // Drawn ahead of the focus ring, so the focus ring lands over it.
            .selectedRing(ring, shape)
            .controlRing(interactionSource, shape)
            .clip(shape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * How thick the ring around a selected pressable is.
 */
private val SelectedRingWidth: Dp = 3.dp

/**
 * How far the ring around a selected pressable stands off its shape.
 */
private val SelectedRingOffset: Dp = 3.dp

/**
 * Draws a ring in [color] [SelectedRingOffset] outside [shape], or nothing for an unspecified [color].
 */
private fun Modifier.selectedRing(
    color: Color,
    shape: Shape,
): Modifier {
    if (color == Color.Unspecified) return this
    return drawBehind {
        val inset = (SelectedRingOffset + SelectedRingWidth / 2).toPx()
        val grown = Size(size.width + inset * 2, size.height + inset * 2)
        val outline = shape.createOutline(grown, layoutDirection, this)
        translate(left = -inset, top = -inset) {
            drawOutline(outline, color, style = Stroke(width = SelectedRingWidth.toPx()))
        }
    }
}
