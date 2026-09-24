package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.stateWords
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.BackgroundSizing
import io.github.composefluent.background.Layer
import io.github.composefluent.component.CheckBoxDefaults
import io.github.composefluent.component.SwitcherDefaults
import io.github.composefluent.component.SwitcherStyle
import io.github.composefluent.scheme.collectVisualState

/*
 * Fluent's switch and checkbox, built from Fluent's own parts.
 *
 * Fluent's `Switcher` takes no modifier and presses a row of its own with the button role, and its
 * `CheckBox` keeps taking presses while disabled and hides the interaction source the focus ring
 * reads. So the row here takes the press, with the kit's role, state, fold and ring, and draws the
 * track, thumb and box from Fluent's own style schemes. Each moves on the skin's motion rather than
 * Fluent's fixed timings, so reduced motion holds them still.
 */

/**
 * Fluent's switch with its label. The row is the target, label included, and carries the switch
 * role, the spoken state and, on the web, the state in its name.
 */
@Composable
internal fun FluentSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    val interactions = remember { MutableInteractionSource() }
    val words = stateWords()
    val state = ControlState.Switched(checked)
    val scheme = if (checked) SwitcherDefaults.selectedSwitcherStyle() else SwitcherDefaults.defaultSwitcherStyle()
    val look = scheme.schemeFor(interactions.collectVisualState(disabled = !enabled))
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .controlRing(interactions, FluentTheme.shapes.control)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ).semantics { stateDescription = words.of(state) }
            .foldState(label, state, enabled, words, FoldedRole.Switch),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(
            text = label,
            modifier = Modifier.weight(1f, fill = false).padding(end = SwitchLabelGap),
            style = BuilderTextStyle.Label,
            color = look.labelColor,
        )
        SwitchTrack(checked, look, Modifier.controlPress(interactions))
    }
}

/** Fluent's switch track and thumb, the thumb sliding to the end while on and swelling under the pointer. */
@Composable
private fun SwitchTrack(
    checked: Boolean,
    look: SwitcherStyle,
    modifier: Modifier,
) {
    val motion = LocalBuilderMotion.current
    val fill by animateColorAsState(look.fillColor, motion.effects(), label = "track")
    val thumb by animateColorAsState(look.controlColor, motion.effects(), label = "thumb")
    val width by animateDpAsState(look.controlSize.width, motion.effects(), label = "thumbWidth")
    val height by animateDpAsState(look.controlSize.height, motion.effects(), label = "thumbHeight")
    val travel by animateDpAsState(
        targetValue = if (checked) SwitchThumbEnd - look.controlSize.width / 2 else 0.dp,
        animationSpec = motion.spatial(),
        label = "thumbTravel",
    )
    Box(
        modifier = modifier
            .size(SwitchTrackWidth, SwitchTrackHeight)
            .border(1.dp, look.borderBrush, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .padding(horizontal = SwitchTrackInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = travel)
                .size(width, height)
                .background(thumb, CircleShape),
        )
    }
}

/** Fluent's switch track. */
private val SwitchTrackWidth = 40.dp
private val SwitchTrackHeight = 20.dp

/** How far in from either end of the track the thumb travels. */
private val SwitchTrackInset = 4.dp

/** Where the thumb's centre comes to rest while on, measured from the inset start. */
private val SwitchThumbEnd = 26.dp

/** Between the label and the track. */
private val SwitchLabelGap = 12.dp

/**
 * Fluent's checkbox with its label. The row is the target and carries the checkbox role, the spoken
 * state and, on the web, the state in its name. The box is Fluent's layer in Fluent's checkbox
 * colours, with a check while it is ticked, so the state never rests on the fill alone.
 */
@Composable
internal fun FluentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    val interactions = remember { MutableInteractionSource() }
    val motion = LocalBuilderMotion.current
    val words = stateWords()
    val state = ControlState.Checked(checked)
    val scheme = if (checked) CheckBoxDefaults.selectedCheckBoxColors() else CheckBoxDefaults.defaultCheckBoxColors()
    val look = scheme.schemeFor(interactions.collectVisualState(disabled = !enabled))
    val fill by animateColorAsState(look.fillColor, motion.effects(), label = "box")
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .controlRing(interactions, FluentTheme.shapes.control)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ).semantics { stateDescription = words.of(state) }
            .foldState(label, state, enabled, words, FoldedRole.Checkbox),
        horizontalArrangement = Arrangement.spacedBy(CheckboxLabelGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Layer(
            modifier = Modifier.controlPress(interactions).size(CheckboxSize),
            shape = FluentTheme.shapes.control,
            color = fill,
            contentColor = look.contentColor,
            border = BorderStroke(1.dp, look.borderColor),
            backgroundSizing = if (checked) BackgroundSizing.OuterBorderEdge else BackgroundSizing.InnerBorderEdge,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (checked) {
                    BuilderIcon(IconId.Check, contentDescription = null, tint = look.contentColor, size = CheckSize)
                }
            }
        }
        BuilderText(
            text = label,
            style = BuilderTextStyle.Label,
            // Fluent keeps a disabled checkbox label in the primary ink, so it dims here the way the box does.
            color = if (enabled) look.labelTextColor else FluentTheme.colors.text.text.disabled,
        )
    }
}

/** Fluent's checkbox. */
private val CheckboxSize = 20.dp

/** Fluent's check inside a ticked box. */
private val CheckSize = 12.dp

/** Between the box and its label. */
private val CheckboxLabelGap = 8.dp
