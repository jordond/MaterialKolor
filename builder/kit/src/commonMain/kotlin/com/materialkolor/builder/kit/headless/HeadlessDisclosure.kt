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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.composeunstyled.DisclosedContent
import com.composeunstyled.DisclosureButton
import com.composeunstyled.UnstyledDisclosure
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.BuilderMotion
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.inputAlpha
import com.materialkolor.builder.kit.skin.headless.inputFocusRing
import com.materialkolor.builder.kit.skin.headless.inputStateDescription

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
)

/** What a disclosure reads out, "Expanded" or "Collapsed". */
internal fun disclosureStateDescription(
    expanded: Boolean,
    enabled: Boolean,
): String = inputStateDescription(if (expanded) "Expanded" else "Collapsed", enabled)

/** Content opening under a disclosure, which under reduced motion only fades. */
internal fun disclosureEnter(motion: BuilderMotion): EnterTransition =
    expandVertically(motion.spatial()) + fadeIn(motion.effects())

/** Content closing under a disclosure. */
internal fun disclosureExit(motion: BuilderMotion): ExitTransition =
    shrinkVertically(motion.spatial()) + fadeOut(motion.effects())

/** The chevron at the end of a disclosure row, turned up while it is open. */
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
 * The row is a button with expand and collapse actions and speaks its state.
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
    UnstyledDisclosure(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
            .background(style.container, style.shape)
            .border(style.outlineWidth, style.outline, style.shape),
    ) {
        Column {
            DisclosureButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LocalLayout.current.primaryTouchTarget)
                    .inputFocusRing(interactions, style.focus, style.shape)
                    .semantics { stateDescription = disclosureStateDescription(expanded, enabled) }
                    .alpha(inputAlpha(enabled)),
                enabled = enabled,
                contentPadding = style.headerPadding,
                interactionSource = interactions,
                contentAlignment = Alignment.CenterStart,
            ) {
                DisclosureHeading(title, summary, expanded)
            }
            DisclosedContent(enter = disclosureEnter(motion), exit = disclosureExit(motion)) {
                Box(Modifier.padding(style.contentPadding)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DisclosureHeading(
    title: String,
    summary: String?,
    expanded: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            BuilderText(title, style = BuilderTextStyle.Label)
            if (summary != null) {
                BuilderText(summary, style = BuilderTextStyle.Body, emphasis = Emphasis.Secondary)
            }
        }
        DisclosureChevron(expanded)
    }
}
