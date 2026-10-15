package com.materialkolor.builder.kit.control

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.materialkolor.builder.kit.headless.InnerTextWithoutHandles
import com.materialkolor.builder.kit.headless.fieldCommits
import com.materialkolor.builder.kit.headless.rememberFieldDraft
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A text field set inside a line of text, such as the name in a heading, drawn as its text over a
 * dashed rule rather than in a box, so the line reads as a sentence that can be edited in place.
 *
 * It commits as [BuilderTextField] does. Typing edits a draft, Enter or leaving the field commits
 * it and Esc throws it away. It is as wide as its text, never narrower than [minWidth] and never
 * wider than the room it is given. Focus turns the rule solid in the accent, and a draft [error]
 * finds fault with turns it the danger ink. The field shows no message of its own, so the caller
 * says what is wrong somewhere near it. Assistive tech hears the field as [label] with the error.
 *
 * @param[value] The committed text.
 * @param[onCommit] Called with the draft when it differs from [value] and [error] has nothing to
 * say about it.
 * @param[label] Names the field to assistive tech. It never shows, since the line around the field
 * says what it is.
 * @param[style] The type the text is set in, the type of the line it sits in.
 * @param[modifier] Applied to the field.
 * @param[color] The ink of the text, or unspecified for the skin's strong ink.
 * @param[error] What is wrong with a draft, or null when it can be committed.
 * @param[minWidth] The narrowest the field gets, so an empty one still shows a rule to type on.
 * @param[onDraftChange] Called with the draft each time someone changes its text, and with the
 * committed text again when Esc throws the draft away, as in [BuilderTextField].
 */
@Composable
public fun BuilderInlineField(
    value: String,
    onCommit: (String) -> Unit,
    label: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    error: (String) -> String? = { null },
    minWidth: Dp = InlineFieldMinWidth,
    onDraftChange: ((String) -> Unit)? = null,
) {
    val tokens = LocalBuilderTokens.current
    val draft = rememberFieldDraft(value)
    val problem = if (draft.dirty) error(draft.text) else null
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val textStyle = style.merge(color = if (color.isSpecified) color else tokens.textStrong)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textWidth = remember(draft.text, textStyle, density) {
        val measured = measurer.measure(draft.text, textStyle, softWrap = false, maxLines = 1)
        with(density) { measured.size.width.toDp() }
    }
    val rule = when {
        problem != null -> tokens.danger
        focused -> tokens.accent
        else -> tokens.borderStrong
    }
    val commit = {
        if (draft.dirty && error(draft.text) == null) {
            onCommit(draft.text)
            draft.settle(draft.text)
        }
    }
    BasicTextField(
        value = draft.value,
        onValueChange = { next ->
            val edited = next.text != draft.text
            draft.value = next
            if (edited) onDraftChange?.invoke(next.text)
        },
        modifier = modifier
            .fieldCommits(draft, commit, onRevert = { onDraftChange?.invoke(draft.text) })
            .semantics {
                contentDescription = label
                if (problem != null) error(problem)
            }.controlRing(interactions, RoundedCornerShape(tokens.radius.small / 2))
            .width(max(minWidth, textWidth + CaretRoom))
            .drawBehind {
                val stroke = (if (focused) RuleFocusedWidth else RuleWidth).toPx()
                val y = size.height - stroke / 2
                val dash = RuleDash.toPx()
                drawLine(
                    color = rule,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke,
                    pathEffect = if (focused) null else PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
            },
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        singleLine = true,
        interactionSource = interactions,
        cursorBrush = SolidColor(tokens.accent),
        decorationBox = { text ->
            Box(
                modifier = Modifier
                    .heightIn(min = LocalLayout.current.minTouchTarget)
                    .padding(bottom = RuleGap),
                contentAlignment = Alignment.CenterStart,
            ) {
                InnerTextWithoutHandles(text)
            }
        },
    )
}

/**
 * The narrowest an inline field gets by default.
 */
public val InlineFieldMinWidth: Dp = 48.dp

/**
 * The room past the text for the caret, so typing never scrolls the text inside the field.
 */
private val CaretRoom: Dp = 4.dp

/**
 * The room between the text and the rule under it.
 */
private val RuleGap: Dp = 2.dp

/**
 * How thick the rule is at rest and while the field has focus.
 */
private val RuleWidth: Dp = 1.5.dp
private val RuleFocusedWidth: Dp = 2.dp

/**
 * How long each dash of the resting rule is, and each gap between.
 */
private val RuleDash: Dp = 4.dp
