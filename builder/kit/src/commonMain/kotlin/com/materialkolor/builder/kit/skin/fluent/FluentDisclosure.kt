package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.stateWords
import com.materialkolor.builder.kit.headless.DisclosureChevron
import com.materialkolor.builder.kit.headless.disclosureEnter
import com.materialkolor.builder.kit.headless.disclosureExit
import com.materialkolor.builder.kit.headless.disclosureName
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import io.github.composefluent.FluentTheme

/**
 * Fluent's expander, built from Fluent's parts. It has Fluent's card coloured header, its outline
 * and the separator under the header, with the chevron that turns as it opens. On the poster the
 * outline and the separator take the poster's ink.
 *
 * Fluent's own `Expander` takes its press on an inner header and again on a chevron button, two
 * stops for one control, and clips everything to its outline, which would cut the header's focus
 * ring. Here the whole header is one button that speaks its state, with expand and collapse actions
 * while it is enabled, and it draws the ring outside the outline. The content opens on the skin's
 * motion, which reduced motion turns to a fade.
 */
@Composable
internal fun FluentDisclosure(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier,
    summary: String?,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val motion = LocalBuilderMotion.current
    val colors = FluentTheme.colors
    val shape = FluentTheme.shapes.control
    val headerShape = if (expanded) shape.topOnly() else shape
    val state = ControlState.Expanded(expanded)
    val words = stateWords()
    val spoken = words.of(state)
    val interactions = remember { MutableInteractionSource() }
    val stroke = LocalFluentPosterInk.current?.line(enabled = true) ?: colors.stroke.card.default
    Column(modifier.border(1.dp, stroke, shape)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LocalLayout.current.primaryTouchTarget)
                .controlRing(interactions, headerShape)
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                ) { onExpandedChange(!expanded) }
                .semantics {
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
                }.foldState(disclosureName(title, summary), state, enabled, words)
                .background(colors.background.card.default, headerShape)
                .alpha(enabledAlpha(enabled))
                .padding(start = HeaderStart, end = HeaderEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = HeaderVertical)) {
                BuilderText(title, style = BuilderTextStyle.Label)
                if (summary != null) {
                    BuilderText(summary, style = BuilderTextStyle.Body, emphasis = Emphasis.Secondary)
                }
            }
            Box(Modifier.defaultMinSize(ChevronBox, ChevronBox), contentAlignment = Alignment.Center) {
                DisclosureChevron(expanded)
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = disclosureEnter(motion),
            exit = disclosureExit(motion),
        ) {
            Column {
                Box(Modifier.fillMaxWidth().height(1.dp).background(stroke))
                Box(Modifier.padding(ContentPadding)) { content() }
            }
        }
    }
}

/**
 * This shape with its bottom corners squared, for a header with content open under it.
 */
private fun Shape.topOnly(): Shape =
    if (this is CornerBasedShape) copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)) else this

/**
 * Fluent's room before an expander's heading.
 */
private val HeaderStart = 16.dp

/**
 * Fluent's room after an expander's chevron.
 */
private val HeaderEnd = 8.dp

/**
 * Fluent's room above and below an expander's heading.
 */
private val HeaderVertical = 13.dp

/**
 * The square Fluent keeps for an expander's chevron.
 */
private val ChevronBox = 32.dp

/**
 * Round what an expander opens.
 */
private val ContentPadding = 16.dp
