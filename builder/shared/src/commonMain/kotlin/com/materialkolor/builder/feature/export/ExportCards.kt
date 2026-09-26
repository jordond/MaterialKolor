package com.materialkolor.builder.feature.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.foldedChoiceName
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * How strongly a chosen card or file row is washed with its accent.
 */
internal const val SELECTED_TINT_ALPHA = 0.14f

/**
 * One choice of an option group drawn as a card, a [title] over a [caption], for a
 * `BuilderChoiceGroup` to hand its option modifier to.
 *
 * It reads as a radio button named [title]. The chosen card takes the accent's border and fill and
 * a check, so the choice never rests on colour alone. Keyboard focus rings it outside its border.
 *
 * @param[cellGap] Whether the card keeps a little room either side, for a grid whose cells touch.
 */
@Composable
internal fun OptionCard(
    title: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cellGap: Boolean = false,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.medium)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
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
            .background(if (selected) tokens.accent.copy(alpha = SELECTED_TINT_ALPHA) else tokens.panel)
            .border(
                width = if (selected) tokens.highlightWidth else tokens.outlineWidth,
                color = if (selected) tokens.accent else tokens.border,
                shape = shape,
            ).heightIn(min = LocalLayout.current.primaryTouchTarget)
            .padding(horizontal = tokens.spacing.medium, vertical = tokens.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BuilderText(text = title, style = BuilderTextStyle.Title)
            BuilderText(text = caption, style = BuilderTextStyle.Label, emphasis = Emphasis.Secondary)
        }
        if (selected) BuilderIcon(id = IconId.Check, contentDescription = null, tint = tokens.accent)
    }
}

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
