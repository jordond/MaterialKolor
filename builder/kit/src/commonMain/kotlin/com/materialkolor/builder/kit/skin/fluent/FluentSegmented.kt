package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.FittedLabel
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.headless.radioGroupOption
import com.materialkolor.builder.kit.headless.rememberRadioGroupFocus
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import io.github.composefluent.FluentTheme
import io.github.composefluent.LocalContentColor
import io.github.composefluent.component.SegmentedButton
import io.github.composefluent.component.SegmentedItemPosition

/**
 * Fluent's segmented control, a row of Fluent's own `SegmentedButton`s with the radio group's roving
 * focus and arrow keys laid over it, since Fluent's row moves neither.
 *
 * The frame is drawn here in Fluent's colours rather than taken from Fluent's `SegmentedControl`,
 * which clips its buttons to its own outline and so would cut the focus ring off every side but the
 * inner ones. Each button takes its press on the node its modifier lands on, so the folded name, the
 * radio role and the test tag all sit on the node that is pressed. The chosen option shows Fluent's
 * indicator and a check, so the choice never rests on its fill alone. A compact row leaves the check
 * out, and the indicator marks the choice.
 */
@Composable
internal fun <T> FluentSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    optionIcon: (T) -> IconId?,
    selectOnFocus: Boolean,
    optionLabel: (T) -> String,
    compact: Boolean = false,
) {
    val selectedIndex = options.indexOf(selected)
    val focus = rememberRadioGroupFocus(options.size, selectedIndex)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val folds = LocalFoldsStateIntoName.current
    val target = LocalLayout.current.primaryTouchTarget
    val colors = FluentTheme.colors
    val shape = FluentTheme.shapes.control
    val frame = colors.stroke.control.default
    Row(
        modifier = modifier
            .semantics { roleLessName(label, folds) }
            .selectableGroup()
            .background(colors.controlAlt.secondary, shape)
            .border(SegmentedFrameWidth, LocalFluentPosterInk.current?.stroke(frame, enabled) ?: frame, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, value ->
            key(index) {
                val interactionSource = remember { MutableInteractionSource() }
                val isSelected = index == selectedIndex
                val name = optionLabel(value)
                SegmentedButton(
                    checked = isSelected,
                    onCheckedChanged = { onSelect(value) },
                    // Fluent's own pair, a standard button for the chosen option and a subtle one for the rest.
                    colors = fluentButtonColors(if (isSelected) Emphasis.Secondary else Emphasis.Subtle),
                    indicator = { FluentIndicator(visible = isSelected, enabled = enabled) },
                    modifier = Modifier
                        .weight(1f)
                        .radioGroupOption(focus, index, selectedIndex, rtl, selectOnFocus) { next ->
                            onSelect(options[next])
                        }.semantics {
                            role = Role.RadioButton
                            this.selected = isSelected
                        }.foldState(name, ControlState.Selected(isSelected), enabled, role = FoldedRole.Radio)
                        .controlTouchTarget(target)
                        .controlPress(interactionSource)
                        .controlRing(interactionSource, shape),
                    enabled = enabled,
                    position = segmentPosition(index, options.size),
                    interactionSource = interactionSource,
                    icon = optionGlyph(isSelected, compact) { optionIcon(value) }?.let { glyph ->
                        { BuilderIcon(glyph, contentDescription = null, tint = LocalContentColor.current) }
                    },
                    text = {
                        if (compact) {
                            FittedLabel(name, LocalContentColor.current)
                        } else {
                            BuilderText(
                                name,
                                style = BuilderTextStyle.Label,
                                color = LocalContentColor.current,
                                maxLines = 1,
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * The glyph an option shows, a check while chosen, none at all in a compact row.
 */
private fun optionGlyph(
    selected: Boolean,
    compact: Boolean,
    own: () -> IconId?,
): IconId? =
    when {
        compact -> null
        selected -> IconId.Check
        else -> own()
    }

/**
 * Fluent's outline round its segmented control.
 */
private val SegmentedFrameWidth = 1.dp

/**
 * Where the option at [index] of [count] sits, which sets how Fluent insets an unchosen one.
 */
private fun segmentPosition(
    index: Int,
    count: Int,
): SegmentedItemPosition =
    when (index) {
        0 -> SegmentedItemPosition.Start
        count - 1 -> SegmentedItemPosition.End
        else -> SegmentedItemPosition.Center
    }

/**
 * Fluent's short accent bar under a chosen segment or tab, drawn the way Fluent's own
 * `HorizontalIndicator` draws it but grown and shrunk on the skin's motion, so reduced motion holds
 * it still. On the poster it is drawn in the poster's ink.
 */
@Composable
internal fun FluentIndicator(
    visible: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = FluentTheme.colors
    val accent = if (enabled) colors.fillAccent.default else colors.fillAccent.disabled
    val poster = LocalFluentPosterInk.current
    val width by animateDpAsState(
        targetValue = if (visible) IndicatorWidth else 0.dp,
        animationSpec = LocalBuilderMotion.current.spatial(),
        label = "indicator",
    )
    Box(
        modifier = modifier
            .padding(bottom = SegmentedFrameWidth)
            .size(width = width, height = IndicatorHeight)
            .background(
                color = if (visible) poster?.accent(accent) ?: accent else Color.Transparent,
                shape = CircleShape,
            ),
    )
}

/**
 * How wide Fluent's indicator is under a chosen item.
 */
private val IndicatorWidth = 16.dp

/**
 * How tall Fluent's indicator is.
 */
private val IndicatorHeight = 3.dp
