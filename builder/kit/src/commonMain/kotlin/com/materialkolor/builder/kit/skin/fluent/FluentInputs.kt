package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.headless.CheckboxStyle
import com.materialkolor.builder.kit.headless.HeadlessField
import com.materialkolor.builder.kit.headless.HeadlessSlider
import com.materialkolor.builder.kit.headless.SliderRules
import com.materialkolor.builder.kit.headless.SliderStyle
import com.materialkolor.builder.kit.headless.SwitchStyle
import com.materialkolor.builder.kit.headless.TabsStyle
import com.materialkolor.builder.kit.skin.headless.FieldStyle
import com.materialkolor.builder.kit.skin.headless.heroFieldStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.github.composefluent.FluentTheme

/**
 * The Fluent inputs' colours and metrics.
 *
 * The switch, the checkbox and the tabs are Fluent's own, so their styles here are the colours those
 * components show at rest, which the contrast checks read. The field and the slider stay the
 * headless ones in Fluent's look. Fluent's inks and fills are translucent, so each is laid over the
 * panel it sits on, the way the tokens are.
 */
internal object FluentInputStyles {
    val switch: SwitchStyle
        @Composable @ReadOnlyComposable
        get() {
            val tokens = LocalBuilderTokens.current
            val colors = FluentTheme.colors
            val panel = tokens.panel
            return SwitchStyle(
                trackWidth = 40.dp,
                trackHeight = 20.dp,
                thumbSize = 12.dp,
                trackShape = CircleShape,
                thumbShape = CircleShape,
                outlineWidth = 1.dp,
                trackOn = colors.fillAccent.default,
                trackOff = colors.controlAlt.secondary.compositeOver(panel),
                outlineOff = colors.controlStrong.default.compositeOver(panel),
                thumbOn = colors.text.onAccent.primary,
                thumbOff = colors.text.text.secondary
                    .compositeOver(panel),
                labelGap = 12.dp,
                focus = tokens.focus,
                focusShape = FluentTheme.shapes.control,
            )
        }

    val checkbox: CheckboxStyle
        @Composable @ReadOnlyComposable
        get() {
            val tokens = LocalBuilderTokens.current
            val colors = FluentTheme.colors
            return CheckboxStyle(
                boxSize = 20.dp,
                checkSize = 12.dp,
                boxShape = FluentTheme.shapes.control,
                outlineWidth = 1.dp,
                outline = colors.controlStrong.default.compositeOver(tokens.panel),
                checkedFill = colors.fillAccent.default,
                checkInk = colors.text.onAccent.primary,
                labelGap = 8.dp,
                focus = tokens.focus,
                focusShape = FluentTheme.shapes.control,
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
        get() {
            val tokens = LocalBuilderTokens.current
            val ink = FluentTheme.colors.text.text.primary
                .compositeOver(tokens.panel)
            return TabsStyle(
                container = Color.Transparent,
                containerShape = RoundedCornerShape(0.dp),
                containerPadding = 0.dp,
                tabShape = FluentTheme.shapes.control,
                tabPadding = PaddingValues(horizontal = 12.dp),
                gap = 0.dp,
                selectedContainer = Color.Transparent,
                selectedInk = ink,
                ink = ink,
                indicator = FluentTheme.colors.fillAccent.default,
                indicatorHeight = 3.dp,
                indicatorWidth = 16.dp,
                focus = tokens.focus,
            )
        }

    /**
     * Fluent's text box, a control fill with the strong line along the bottom that turns to a
     * thicker accent line while it has focus. The edge all round keeps the strong stroke, which
     * carries the 3 to 1 boundary Fluent's own faint outline would not. On the poster the accent
     * line is the poster's ink.
     */
    val field: FieldStyle
        @Composable @ReadOnlyComposable
        get() {
            val tokens = LocalBuilderTokens.current
            val colors = FluentTheme.colors
            return FieldStyle(
                shape = FluentTheme.shapes.control,
                container = colors.control.default.compositeOver(tokens.panel),
                outline = tokens.borderStrong,
                outlineWidth = 1.dp,
                active = LocalFluentPosterInk.current?.ink ?: colors.fillAccent.default,
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
        get() = heroFieldStyle(LocalBuilderTokens.current, underline = 2.dp, shape = FluentTheme.shapes.control)
}

// fluent-placeholder
// The slider stays headless, since Fluent's own opens a popup while it drags, and keeps its first style.
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

/**
 * The headless field in Fluent's text box look. Fluent's own text field bakes in its decoration,
 * with no room for the kit's clip that keeps touch selection handles off the web (D45).
 */
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
