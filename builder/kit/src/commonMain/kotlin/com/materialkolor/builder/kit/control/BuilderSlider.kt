package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessSlider
import com.materialkolor.builder.kit.headless.SliderRules
import com.materialkolor.builder.kit.headless.sliderValueDescription
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentSlider
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialSlider

/**
 * A continuous slider in the surrounding skin, with named stops a drag snaps to.
 *
 * A drag reports every frame through [onValueChange] and reports once through
 * [onValueChangeFinished] when it lets go, so a caller can preview live and commit one undo entry.
 * Keys are the stepped alternative to dragging (AR-07). The arrows move one [step] and ten with
 * Shift, and each key press ends with one [onValueChangeFinished]. Keys never snap, so a stop never
 * swallows an arrow press next to it.
 *
 * The slider sets set-progress and speaks [stateDescription]. On the web the value follows the name
 * as well, until the CMP mirror learns slider semantics (AR-10, D37). The slider has no role there,
 * so the name and value go in as text, which assistive tech keeps where it drops a name (S5).
 *
 * @param[value] Where the thumb is.
 * @param[onValueChange] Called with every new value, snapped when a drag lands near a stop.
 * @param[label] What the slider sets, read out as its name.
 * @param[modifier] Applied to the slider.
 * @param[onValueChangeFinished] Called once when a drag, a tap or a key press ends.
 * @param[valueRange] The values the slider covers.
 * @param[step] How far one arrow press moves, a hundredth of the range unless given.
 * @param[stops] Named values a drag snaps to, drawn on the track where the skin can.
 * @param[snapDistance] How close a drag has to land to a stop to snap onto it.
 * @param[stateDescription] How the value reads out, such as "0.5, Medium". The value to two
 * decimals unless given.
 * @param[enabled] Whether it takes input.
 */
@Composable
public fun BuilderSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float = (valueRange.endInclusive - valueRange.start) / DefaultStepsPerRange,
    stops: List<Float> = emptyList(),
    snapDistance: Float = 0f,
    stateDescription: String = sliderValueDescription(value),
    enabled: Boolean = true,
) {
    val rules = remember(valueRange, step, stops, snapDistance) { SliderRules(valueRange, step, stops, snapDistance) }
    when (LocalSkin.current.library) {
        Library.Material3 -> {
            MaterialSlider(
                value,
                onValueChange,
                onValueChangeFinished,
                rules,
                label,
                stateDescription,
                modifier,
                enabled,
            )
        }
        Library.Unstyled -> {
            HeadlessSlider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                rules = rules,
                label = label,
                stateDescription = stateDescription,
                style = UnstyledInputStyles.slider,
                modifier = modifier,
                enabled = enabled,
            )
        }
        Library.Fluent -> {
            FluentSlider(value, onValueChange, onValueChangeFinished, rules, label, stateDescription, modifier, enabled)
        }
        Library.Custom -> {
            HeadlessSlider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                rules = rules,
                label = label,
                stateDescription = stateDescription,
                style = CustomInputStyles.slider,
                modifier = modifier,
                enabled = enabled,
            )
        }
    }
}

/** A hundred arrow presses cross the range unless the caller picks its own step. */
private const val DefaultStepsPerRange: Float = 100f
