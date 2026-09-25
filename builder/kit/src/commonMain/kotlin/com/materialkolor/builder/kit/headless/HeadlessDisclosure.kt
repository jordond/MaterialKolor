package com.materialkolor.builder.kit.headless

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.DisclosedContent
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledDisclosure
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.stateWords
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.BuilderMotion
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.enabledAlpha

/**
 * How [HeadlessDisclosure] draws its row and what it opens.
 *
 * @property[container] The fill behind the row and its content.
 * @property[shape] The corners around both.
 * @property[outline] The edge around both.
 * @property[outlineWidth] How thick that edge is, zero for none.
 * @property[headerPadding] Between the edge and the title row.
 * @property[contentPadding] Around the content once it is open.
 * @property[focus] The keyboard focus ring around the title row.
 * @property[rule] The hairline drawn across the top, where a row sits in a column rather than a box.
 * @property[ruleWidth] How thick that hairline is, zero for none.
 * @property[title] The type the title is set in.
 * @property[summary] The type the summary under it is set in.
 */
@Immutable
internal class DisclosureStyle(
    val container: Color,
    val shape: Shape,
    val outline: Color,
    val outlineWidth: Dp,
    val headerPadding: PaddingValues,
    val contentPadding: PaddingValues,
    val focus: Color,
    // b-510
    val rule: Color = Color.Transparent,
    val ruleWidth: Dp = 0.dp,
    val title: BuilderTextStyle = BuilderTextStyle.Label,
    val summary: BuilderTextStyle = BuilderTextStyle.Body,
)

/**
 * What a disclosure row says, its title and then its summary, the name the web folds its state into.
 */
internal fun disclosureName(
    title: String,
    summary: String?,
): String = listOfNotNull(title, summary).joinToString(", ")

/**
 * Content opening under a disclosure, which under reduced motion only fades.
 */
internal fun disclosureEnter(motion: BuilderMotion): EnterTransition =
    expandVertically(motion.spatial()) + fadeIn(motion.effects())

/**
 * Content closing under a disclosure.
 */
internal fun disclosureExit(motion: BuilderMotion): ExitTransition =
    shrinkVertically(motion.spatial()) + fadeOut(motion.effects())

/**
 * The chevron at the end of a disclosure row, turned up while it is open.
 */
@Composable
internal fun DisclosureChevron(expanded: Boolean) {
    val motion = LocalBuilderMotion.current
    val turn by animateFloatAsState(if (expanded) 180f else 0f, motion.spatial(), label = "disclosureChevron")
    BuilderIcon(
        id = IconId.ChevronDown,
        contentDescription = null,
        modifier = Modifier.rotate(turn),
        emphasis = Emphasis.Secondary,
    )
}

/**
 * A row that opens and closes the content under it, over Compose Unstyled's disclosure.
 *
 * The row is a button that speaks its state, with expand and collapse actions while it is enabled.
 * It is a plain Compose Unstyled button rather than the disclosure's own, since that one keeps the
 * two actions even when disabled.
 */
@Composable
internal fun HeadlessDisclosure(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    style: DisclosureStyle,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val motion = LocalBuilderMotion.current
    val state = ControlState.Expanded(expanded)
    val words = stateWords()
    val spoken = words.of(state)
    UnstyledDisclosure(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
            .topRule(style.rule, style.ruleWidth) // b-510
            .background(style.container, style.shape)
            .border(style.outlineWidth, style.outline, style.shape),
    ) {
        Column {
            UnstyledButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LocalLayout.current.primaryTouchTarget)
                    .controlRing(interactions, style.shape, style.focus)
                    .disclosureSemantics(spoken, expanded, enabled, onExpandedChange)
                    .foldState(disclosureName(title, summary), state, enabled, words)
                    .alpha(enabledAlpha(enabled)),
                enabled = enabled,
                contentPadding = style.headerPadding,
                interactionSource = interactions,
                contentAlignment = Alignment.CenterStart,
            ) {
                DisclosureHeading(title, summary, expanded, style)
            }
            DisclosedContent(enter = disclosureEnter(motion), exit = disclosureExit(motion)) {
                Box(Modifier.padding(style.contentPadding)) {
                    content()
                }
            }
        }
    }
}

/**
 * The spoken state, and the expand or collapse action while the row is enabled.
 */
private fun Modifier.disclosureSemantics(
    spoken: String,
    expanded: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
): Modifier =
    semantics {
        stateDescription = spoken
        if (!enabled) return@semantics
        if (expanded) {
            collapse {
                onExpandedChange(false)
                true
            }
        } else {
            expand {
                onExpandedChange(true)
                true
            }
        }
    }

@Composable
private fun DisclosureHeading(
    title: String,
    summary: String?,
    expanded: Boolean,
    style: DisclosureStyle,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            BuilderText(title, style = style.title)
            if (summary != null) {
                BuilderText(summary, style = style.summary, emphasis = Emphasis.Secondary)
            }
        }
        DisclosureChevron(expanded)
    }
}

// b-510

/**
 * A hairline of [color] across the top edge, or nothing while [width] is zero.
 */
private fun Modifier.topRule(
    color: Color,
    width: Dp,
): Modifier =
    if (width <= 0.dp) {
        this
    } else {
        drawBehind {
            val stroke = width.toPx()
            drawRect(color, size = Size(size.width, stroke))
        }
    }
