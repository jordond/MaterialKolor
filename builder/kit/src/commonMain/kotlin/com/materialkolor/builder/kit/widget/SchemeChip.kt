package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.stateName
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/** The diameter of the colored circle, design D. */
private val SchemeChipDiameter: Dp = 46.dp

/** The width of the ring around a chosen chip. */
private val SchemeChipRingWidth: Dp = 2.dp

/** The gap between the circle and its ring. */
private val SchemeChipRingGap: Dp = 2.dp

/**
 * A scheme at a glance, for the style picker, the seed candidates and the project list.
 *
 * Design D draws a circle with [primary] across the top half, [secondaryContainer] in the bottom
 * left and [tertiaryContainer] in the bottom right. The colors are passed in, so the chip never
 * resolves a scheme itself.
 *
 * It reads out as a radio button named [label] with its selected state, and shows [label] as a
 * tooltip on hover and focus. The chosen chip carries a ring and a check, so the choice never rests
 * on color alone. Place it inside a selectable group so assistive technology hears the set.
 *
 * @param[primary] The top half.
 * @param[secondaryContainer] The bottom left quarter.
 * @param[tertiaryContainer] The bottom right quarter.
 * @param[selected] Whether this is the current choice.
 * @param[onClick] Called when the chip is chosen.
 * @param[label] What the chip stands for, such as a style name.
 * @param[modifier] Applied to the chip.
 */
@Composable
public fun SchemeChip(
    primary: Color,
    secondaryContainer: Color,
    tertiaryContainer: Color,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val name = stateName(label, ControlState.Selected(selected))
    val ringColor = if (selected) tokens.accent else Color.Transparent
    BuilderTooltip(text = label, modifier = modifier) {
        Box(
            modifier = Modifier
                .semantics { contentDescription = name }
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).hoverable(interactionSource)
                .widgetOutline(interactionSource, CircleShape, hovered)
                .padding(WidgetFocusWidth)
                .background(ringColor, CircleShape)
                .padding(SchemeChipRingWidth)
                .background(tokens.panel, CircleShape)
                .padding(SchemeChipRingGap)
                .size(SchemeChipDiameter),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
                val half = Size(size.width, size.height / 2f)
                val quarter = Size(size.width / 2f, size.height / 2f)
                drawRect(primary, size = half)
                drawRect(secondaryContainer, topLeft = Offset(0f, size.height / 2f), size = quarter)
                drawRect(tertiaryContainer, topLeft = Offset(size.width / 2f, size.height / 2f), size = quarter)
            }
            if (selected) {
                Box(
                    modifier = Modifier.background(tokens.accent, CircleShape).padding(SchemeChipRingGap),
                    contentAlignment = Alignment.Center,
                ) {
                    BuilderIcon(IconId.Check, contentDescription = null, tint = tokens.onAccent)
                }
            }
        }
    }
}
