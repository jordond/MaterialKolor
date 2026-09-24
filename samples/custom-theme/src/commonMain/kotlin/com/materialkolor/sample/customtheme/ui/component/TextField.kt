package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

/**
 * [modifier] lands on the editable node itself, so a focus requester set there reaches the field.
 */
@Composable
internal fun TextField(
    state: TextFieldState,
    placeholder: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val control = interactionSource.collectControlState()
    val border = when {
        control.isFocused -> colors.primary
        control.isHovered -> colors.borderStrong
        else -> colors.borderSoft
    }
    val shape = AppShapes.Control

    BasicTextField(
        state = state,
        modifier = modifier
            .height(ControlHeight)
            .hoverable(interactionSource),
        textStyle = AppType.Body.copy(color = colors.textStrong),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { onSubmit() },
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colors.primary),
        decorator = TextFieldDecorator { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(colors.surfaceRaised)
                    .border(if (control.isFocused) 2.dp else 1.dp, border, shape)
                    .padding(horizontal = 12.dp),
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = AppType.Body,
                        color = colors.textMuted,
                    )
                }

                innerTextField()
            }
        },
    )
}
