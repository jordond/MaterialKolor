package com.materialkolor.builder.feature.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.foldedChoiceName
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType

/**
 * How strongly a chosen card or file row is washed with its accent.
 */
internal const val SELECTED_TINT_ALPHA = 0.14f

/**
 * One choice of an option group drawn as a card, a [title] over a [caption], for a
 * `BuilderChoiceGroup` to hand its option modifier to.
 *
 * It reads as a radio button named [title]. The chosen card takes a thicker border in the accent
 * and a fill, and with [radio] a filled radio before its title, so the choice never rests on colour
 * alone. Keyboard focus rings it outside its border.
 *
 * @param[radio] Whether a radio sits before the title, for cards that stack in a column. Without
 * one the chosen card takes the accent's wash, as the library cards do.
 * @param[cellGap] Whether the card keeps a little room either side, for a grid whose cells touch.
 */
@Composable
internal fun OptionCard(
    title: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    radio: Boolean = false,
    cellGap: Boolean = false,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.medium)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val fill = when {
        !selected -> Color.Transparent
        radio -> tokens.canvas
        else -> tokens.accent.copy(alpha = SELECTED_TINT_ALPHA)
    }
    Row(
        modifier = Modifier
            .then(if (cellGap) Modifier.padding(horizontal = tokens.spacing.extraSmall) else Modifier)
            .fillMaxWidth()
            .then(modifier)
            .focusOutline(focused, tokens.focus, tokens.highlightWidth, shape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ).foldedChoiceName(title, selected)
            .clip(shape)
            .background(fill)
            .border(
                width = if (selected) tokens.highlightWidth else tokens.outlineWidth,
                color = if (selected) tokens.accent else tokens.border,
                shape = shape,
            ).heightIn(min = maxOf(CardMinHeight, LocalLayout.current.primaryTouchTarget))
            .padding(horizontal = tokens.spacing.medium, vertical = tokens.spacing.small + tokens.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        verticalAlignment = if (radio) Alignment.Top else Alignment.CenterVertically,
    ) {
        if (radio) RadioMark(selected)
        Column(Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = cardTitleType().merge(color = if (selected && !radio) tokens.accent else tokens.textStrong),
            )
            BuilderText(text = caption, style = BuilderTextStyle.Label, emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * A radio as a card draws it, a ring filled with a dot while [selected]. It only shows, since the
 * card is the radio button.
 */
@Composable
private fun RadioMark(selected: Boolean) {
    val tokens = LocalBuilderTokens.current
    Box(
        modifier = Modifier
            .padding(top = RadioNudge)
            .size(RadioSize)
            .border(tokens.highlightWidth, if (selected) tokens.accent else tokens.textMuted, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(RadioDot).background(tokens.accent, CircleShape))
    }
}

/**
 * The type a card's title is set in, the title's face a size down and bolder, so two sit side by
 * side in the options column.
 */
@Composable
private fun cardTitleType(): TextStyle =
    LocalBuilderType.current.title.merge(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)

/**
 * The shortest a card gets, so the two cards of a row line up whatever their captions.
 */
private val CardMinHeight: Dp = 60.dp

/**
 * The radio's ring, its dot, and how far down it sits to meet the title's first line.
 */
private val RadioSize: Dp = 20.dp
private val RadioDot: Dp = 10.dp
private val RadioNudge: Dp = 1.dp

/**
 * A ring of [width] in [color] around [shape], standing [width] off it, while [shown].
 */
internal fun Modifier.focusOutline(
    shown: Boolean,
    color: Color,
    width: Dp,
    shape: Shape,
): Modifier =
    if (!shown) {
        this
    } else {
        drawWithContent {
            drawContent()
            val stroke = width.toPx()
            val grow = stroke * 1.5f
            val grown = Size(size.width + grow * 2, size.height + grow * 2)
            val outline = shape.createOutline(grown, layoutDirection, this)
            translate(-grow, -grow) { drawOutline(outline, color, style = Stroke(stroke)) }
        }
    }
