package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.headless.CheckboxStyle
import com.materialkolor.builder.kit.headless.DisclosureStyle
import com.materialkolor.builder.kit.headless.HeadlessCheckbox
import com.materialkolor.builder.kit.headless.HeadlessDisclosure
import com.materialkolor.builder.kit.headless.HeadlessField
import com.materialkolor.builder.kit.headless.HeadlessSlider
import com.materialkolor.builder.kit.headless.HeadlessSwitch
import com.materialkolor.builder.kit.headless.HeadlessTabs
import com.materialkolor.builder.kit.headless.SliderRules
import com.materialkolor.builder.kit.headless.SliderStyle
import com.materialkolor.builder.kit.headless.SwitchStyle
import com.materialkolor.builder.kit.headless.TabsStyle
import com.materialkolor.builder.kit.skin.headless.FieldStyle
import com.materialkolor.builder.kit.skin.headless.heroFieldStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens

// fluent-placeholder

/**
 * The Fluent skin's inputs until B-403 swaps in Fluent's own. Each one is the headless input
 * drawn the way WinUI draws it, small corners, a thin edge and the accent on whatever is on.
 */
internal object FluentInputStyles {
    val switch: SwitchStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            SwitchStyle(
                trackWidth = 40.dp,
                trackHeight = 20.dp,
                thumbSize = 12.dp,
                trackShape = CircleShape,
                thumbShape = CircleShape,
                outlineWidth = 1.dp,
                trackOn = tokens.accent,
                trackOff = tokens.panel,
                outlineOff = tokens.textMuted,
                thumbOn = tokens.onAccent,
                thumbOff = tokens.textMuted,
                labelGap = tokens.spacing.medium,
                focus = tokens.focus,
                focusShape = RoundedCornerShape(4.dp),
            )
        }

    val checkbox: CheckboxStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            CheckboxStyle(
                boxSize = 20.dp,
                checkSize = 14.dp,
                boxShape = RoundedCornerShape(4.dp),
                outlineWidth = 1.dp,
                outline = tokens.textMuted,
                checkedFill = tokens.accent,
                checkInk = tokens.onAccent,
                labelGap = tokens.spacing.small,
                focus = tokens.focus,
                focusShape = RoundedCornerShape(4.dp),
            )
        }

    val slider: SliderStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            SliderStyle(
                trackHeight = 4.dp,
                trackShape = CircleShape,
                activeTrack = tokens.accent,
                inactiveTrack = tokens.textMuted,
                thumbSize = 20.dp,
                thumbShape = CircleShape,
                thumb = tokens.accent,
                thumbOutline = tokens.panelRaised,
                thumbOutlineWidth = 5.dp,
                stopSize = 2.dp,
                stop = tokens.panel,
                focus = tokens.focus,
            )
        }

    val tabs: TabsStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            TabsStyle(
                container = Color.Transparent,
                containerShape = RoundedCornerShape(0.dp),
                containerPadding = 0.dp,
                tabShape = RoundedCornerShape(4.dp),
                tabPadding = PaddingValues(horizontal = tokens.spacing.medium, vertical = tokens.spacing.small),
                gap = tokens.spacing.extraSmall,
                selectedContainer = Color.Transparent,
                selectedInk = tokens.textStrong,
                ink = tokens.textMuted,
                indicator = tokens.accent,
                indicatorHeight = 3.dp,
                indicatorWidth = 16.dp,
                focus = tokens.focus,
            )
        }

    val disclosure: DisclosureStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            DisclosureStyle(
                container = tokens.panelRaised,
                shape = RoundedCornerShape(4.dp),
                outline = tokens.border,
                outlineWidth = 1.dp,
                headerPadding = PaddingValues(horizontal = tokens.spacing.large, vertical = tokens.spacing.small),
                contentPadding = PaddingValues(
                    start = tokens.spacing.large,
                    end = tokens.spacing.large,
                    bottom = tokens.spacing.medium,
                ),
                focus = tokens.focus,
            )
        }

    val field: FieldStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            FieldStyle(
                shape = RoundedCornerShape(4.dp),
                container = tokens.panelRaised,
                outline = tokens.borderStrong,
                outlineWidth = 1.dp,
                active = tokens.accent,
                error = tokens.danger,
                activeWidth = 2.dp,
                activeAsUnderline = true,
                padding = PaddingValues(horizontal = tokens.spacing.medium, vertical = tokens.spacing.small),
                gap = tokens.spacing.extraSmall,
                cursor = tokens.textStrong,
                focusRing = tokens.focus,
            )
        }

    val hero: FieldStyle
        @Composable @ReadOnlyComposable
        get() = heroFieldStyle(LocalBuilderTokens.current, underline = 2.dp, shape = RoundedCornerShape(4.dp))
}

// fluent-placeholder
@Composable
internal fun FluentSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    HeadlessSwitch(checked, onCheckedChange, label, FluentInputStyles.switch, modifier, enabled)
}

// fluent-placeholder
@Composable
internal fun FluentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    HeadlessCheckbox(checked, onCheckedChange, label, FluentInputStyles.checkbox, modifier, enabled)
}

// fluent-placeholder
@Composable
internal fun FluentSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    rules: SliderRules,
    label: String,
    stateDescription: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    HeadlessSlider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        rules = rules,
        label = label,
        stateDescription = stateDescription,
        style = FluentInputStyles.slider,
        modifier = modifier,
        enabled = enabled,
    )
}

// fluent-placeholder
@Composable
internal fun FluentField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    message: String?,
    isError: Boolean,
    textStyle: TextStyle,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    HeadlessField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        message = message,
        isError = isError,
        textStyle = textStyle,
        large = false,
        enabled = enabled,
        onDone = onDone,
        style = FluentInputStyles.field,
        modifier = modifier,
    )
}

// fluent-placeholder
@Composable
internal fun <T> FluentTabs(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier,
) {
    HeadlessTabs(tabs, selected, onSelect, label, FluentInputStyles.tabs, modifier)
}

// fluent-placeholder
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
    HeadlessDisclosure(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        title = title,
        style = FluentInputStyles.disclosure,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
        content = content,
    )
}
