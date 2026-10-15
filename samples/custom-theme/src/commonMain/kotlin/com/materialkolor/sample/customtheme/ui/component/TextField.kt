package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

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
    val line = when {
        control.isFocused -> colors.pink
        control.isHovered -> colors.ink
        else -> colors.inkSoft
    }
    val weight = if (control.isFocused) 4.dp else Rule

    BasicTextField(
        modifier = modifier
            .height(ControlHeight)
            .hoverable(interactionSource),
        state = state,
        textStyle = AppType.Body.copy(color = colors.ink),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { onSubmit() },
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colors.pink),
        decorator = TextFieldDecorator { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val stroke = weight.toPx()
                        val y = size.height - stroke / 2
                        drawLine(
                            color = line,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = stroke,
                            blendMode = colors.overprint,
                        )
                    },
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = AppType.Body,
                        color = colors.inkSoft,
                    )
                }

                innerTextField()
            }
        },
    )
}
