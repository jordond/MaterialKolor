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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.stateName
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType

/** The diameter of the colored circle, design D. */
internal val SchemeChipDiameter: Dp = 46.dp // b-311a

/** The width of the ring around a chosen chip. */
internal val SchemeChipRingWidth: Dp = 2.dp // b-311a

/** The gap between the circle and its ring. */
internal val SchemeChipRingGap: Dp = 2.dp // b-311a

/**
 * A scheme at a glance, for the style picker, the seed candidates and the project list.
 *
 * Design D draws a circle with [primary] across the top half, [secondaryContainer] in the bottom
 * left and [tertiaryContainer] in the bottom right. The colors are passed in, so the chip never
 * resolves a scheme itself.
 *
 * It reads out as a radio button named [label] with its selected state, and shows [tooltip] on
 * hover and focus, [label] unless it says more. The chosen chip carries a ring and a check, so the choice never rests
 * on color alone. Keyboard focus rings it outside its hairline, on whatever it sits on, so the ring
 * never lies on the chip's own edge (S5 rerun). Place it inside a selectable group so assistive
 * technology hears the set. Inside
 * something that is itself the control, the other [SchemeChip], with no click, only shows the scheme.
 *
 * @param[primary] The top half.
 * @param[secondaryContainer] The bottom left quarter.
 * @param[tertiaryContainer] The bottom right quarter.
 * @param[selected] Whether this is the current choice.
 * @param[onClick] Called when the chip is chosen.
 * @param[label] What the chip stands for, such as a style name.
 * @param[modifier] Applied to the chip.
 * @param[tooltip] What the chip shows on hover and focus.
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
    tooltip: String = label, // b-510
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val name = stateName(label, ControlState.Selected(selected), role = FoldedRole.Radio)
    BuilderTooltip(text = tooltip, modifier = modifier) {
        SchemeChipFace(
            primary = primary,
            secondaryContainer = secondaryContainer,
            tertiaryContainer = tertiaryContainer,
            selected = selected,
            modifier = Modifier
                .semantics { contentDescription = name }
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).hoverable(interactionSource)
                .controlRing(interactionSource, CircleShape)
                .widgetHairline(CircleShape, hovered),
        )
    }
}

// b-230c

/**
 * A scheme at a glance that only shows it, for a chip drawn inside something that is itself the
 * control, such as a card that chooses a starter.
 *
 * It draws the circle of the other [SchemeChip] in the same room, [SchemeChipFootprint], with no
 * ring or check. It takes no click, no focus and no hover, has no tooltip and adds nothing to the
 * semantics, so a press on it goes to the control around it and that control names it.
 *
 * @param[primary] The top half.
 * @param[secondaryContainer] The bottom left quarter.
 * @param[tertiaryContainer] The bottom right quarter.
 * @param[modifier] Applied to the chip.
 */
@Composable
public fun SchemeChip(
    primary: Color,
    secondaryContainer: Color,
    tertiaryContainer: Color,
    modifier: Modifier = Modifier,
) {
    SchemeChipFace(primary, secondaryContainer, tertiaryContainer, selected = false, modifier = modifier)
}

/**
 * The chip as it draws, the circle in its ring inside the room the hairline takes. [modifier] goes
 * on the outside of that room, where a pressable chip draws its hairline and its focus ring past it.
 */
@Composable
private fun SchemeChipFace(
    primary: Color,
    secondaryContainer: Color,
    tertiaryContainer: Color,
    selected: Boolean,
    modifier: Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val ringColor = if (selected) tokens.accent else Color.Transparent
    Box(
        modifier = modifier
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

// b-510

/**
 * A chip's name, set under it on one line in the builder's label type at [SchemeChipNameSize]. A
 * name too long for the width it is given steps down in size until it fits, so a row of equal cells
 * never wraps or cuts one.
 *
 * The chip already names what it stands for, so the name only shows and adds nothing to what
 * assistive tech reads.
 *
 * @param[name] What to show.
 * @param[modifier] Applied to the text, whose width the name fits.
 */
@Composable
public fun SchemeChipName(
    name: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val style = LocalBuilderType.current.label.merge(
        color = tokens.textStrong,
        fontSize = SchemeChipNameSize,
        lineHeight = SchemeChipNameLine,
        textAlign = TextAlign.Center,
    )
    BasicText(
        text = name,
        modifier = modifier.clearAndSetSemantics {},
        style = style,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(
            minFontSize = SchemeChipNameMinSize,
            maxFontSize = SchemeChipNameSize,
            stepSize = SchemeChipNameStep,
        ),
    )
}

/** How big a chip's name is set, design D. */
internal val SchemeChipNameSize: TextUnit = 11.sp

/** The line a chip's name takes. */
internal val SchemeChipNameLine: TextUnit = 14.sp

/** The smallest a chip's name steps down to in a narrow cell. */
internal val SchemeChipNameMinSize: TextUnit = 9.sp

/** How far each step down takes a chip's name. */
internal val SchemeChipNameStep: TextUnit = 0.5.sp
