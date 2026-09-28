package com.materialkolor.builder.kit.headless

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.composeunstyled.SwitchThumb
import com.composeunstyled.UnstyledSwitch
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.SwitchLabel
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.stateWords
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.enabledAlpha

/**
 * How [HeadlessSwitch] draws its track and thumb.
 *
 * @property[trackWidth] The track's width.
 * @property[trackHeight] The track's height. The thumb sits centred inside it.
 * @property[thumbSize] The thumb's width and height.
 * @property[trackShape] The track's corners.
 * @property[thumbShape] The thumb's corners.
 * @property[outlineWidth] The track's edge.
 * @property[trackOn] The track fill while on.
 * @property[trackOff] The track fill while off.
 * @property[outlineOff] The track edge while off, which carries the 3:1 boundary.
 * @property[thumbOn] The thumb while on.
 * @property[thumbOff] The thumb while off.
 * @property[labelGap] The least room between the label and the track.
 * @property[focus] The keyboard focus ring.
 * @property[focusShape] The corners of the ring around the whole row.
 */
@Immutable
internal class SwitchStyle(
    val trackWidth: Dp,
    val trackHeight: Dp,
    val thumbSize: Dp,
    val trackShape: Shape,
    val thumbShape: Shape,
    val outlineWidth: Dp,
    val trackOn: Color,
    val trackOff: Color,
    val outlineOff: Color,
    val thumbOn: Color,
    val thumbOff: Color,
    val labelGap: Dp,
    val focus: Color,
    val focusShape: Shape,
)

/**
 * How [HeadlessCheckbox] draws its box.
 *
 * @property[boxSize] The box's width and height.
 * @property[checkSize] The check glyph inside a checked box.
 * @property[boxShape] The box's corners.
 * @property[outlineWidth] The edge of an empty box.
 * @property[outline] The edge colour of an empty box.
 * @property[checkedFill] The fill of a checked box.
 * @property[checkInk] The check glyph.
 * @property[labelGap] Between the box and the label.
 * @property[focus] The keyboard focus ring.
 * @property[focusShape] The corners of the ring around the whole row.
 */
@Immutable
internal class CheckboxStyle(
    val boxSize: Dp,
    val checkSize: Dp,
    val boxShape: Shape,
    val outlineWidth: Dp,
    val outline: Color,
    val checkedFill: Color,
    val checkInk: Color,
    val labelGap: Dp,
    val focus: Color,
    val focusShape: Shape,
)

/**
 * A labelled switch over Compose Unstyled's switch.
 *
 * The whole row is the target, label included, so the switch itself only draws. The row carries
 * the switch role, the toggle state and a spoken state, and on the web the state in its name.
 */
@Composable
internal fun HeadlessSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    style: SwitchStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    caption: String? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val motion = LocalBuilderMotion.current
    val words = stateWords()
    val state = ControlState.Switched(checked)
    val track by animateColorAsState(if (checked) style.trackOn else style.trackOff, motion.effects())
    val edge by animateColorAsState(if (checked) style.trackOn else style.outlineOff, motion.effects())
    val thumb by animateColorAsState(if (checked) style.thumbOn else style.thumbOff, motion.effects())
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .controlRing(interactions, style.focusShape, style.focus)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ).semantics { stateDescription = words.of(state) }
            .foldState(label, state, enabled, words, FoldedRole.Switch)
            .alpha(enabledAlpha(enabled)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwitchLabel(
            label = label,
            caption = caption,
            modifier = Modifier.weight(1f, fill = false).padding(end = style.labelGap),
        )
        UnstyledSwitch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .controlPress(interactions)
                .size(style.trackWidth, style.trackHeight)
                .background(track, style.trackShape)
                .border(style.outlineWidth, edge, style.trackShape)
                .padding((style.trackHeight - style.thumbSize) / 2),
            enabled = enabled,
            interactionSource = interactions,
        ) {
            SwitchThumb(
                modifier = Modifier
                    .size(style.thumbSize)
                    .background(thumb, style.thumbShape),
                animationSpec = motion.spatial(),
            )
        }
    }
}

/**
 * A labelled checkbox. The row is the target and carries the checkbox role, so the box only draws.
 *
 * Compose Unstyled's checkbox always owns its own click, which would leave the label outside the
 * target, so the box is drawn here instead.
 */
@Composable
internal fun HeadlessCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    style: CheckboxStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    val motion = LocalBuilderMotion.current
    val words = stateWords()
    val state = ControlState.Checked(checked)
    val fill by animateColorAsState(if (checked) style.checkedFill else Color.Transparent, motion.effects())
    val edge by animateColorAsState(if (checked) style.checkedFill else style.outline, motion.effects())
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .controlRing(interactions, style.focusShape, style.focus)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ).semantics { stateDescription = words.of(state) }
            .foldState(label, state, enabled, words, FoldedRole.Checkbox)
            .alpha(enabledAlpha(enabled)),
        horizontalArrangement = Arrangement.spacedBy(style.labelGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .controlPress(interactions)
                .size(style.boxSize)
                .background(fill, style.boxShape)
                .border(style.outlineWidth, edge, style.boxShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                BuilderIcon(IconId.Check, contentDescription = null, tint = style.checkInk, size = style.checkSize)
            }
        }
        BuilderText(text = label, style = BuilderTextStyle.Label)
    }
}
