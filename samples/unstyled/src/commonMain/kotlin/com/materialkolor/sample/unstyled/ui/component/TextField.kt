package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledTextField
import com.materialkolor.sample.unstyled.theme.ControlHeight
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

private const val SELECTION_ALPHA = 0.32f

@Composable
internal fun TextField(
    state: TextFieldState,
    placeholder: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val primary = MaterialKolorTokens.primary.color
    val outline = when {
        focused -> primary
        hovered -> MaterialKolorTokens.outline.color
        else -> MaterialKolorTokens.outlineVariant.color
    }

    UnstyledTextField(
        state = state,
        cursorBrush = SolidColor(primary),
        selectionColors = TextSelectionColors(
            handleColor = primary,
            backgroundColor = primary.copy(alpha = SELECTION_ALPHA),
        ),
        textStyle = TasksType.Body,
        lineLimits = TextFieldLineLimits.SingleLine,
        onKeyboardAction = KeyboardActionHandler { onSubmit() },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        interactionSource = interactionSource,
        textColor = MaterialKolorTokens.onSurface.color,
        modifier = modifier
            .heightIn(min = ControlHeight)
            .hoverable(interactionSource)
            .clip(Shapes.Control)
            .background(MaterialKolorTokens.surfaceContainerLowest.color)
            .border(width = if (focused) 2.dp else 1.dp, color = outline, shape = Shapes.Control)
            .padding(horizontal = Spacing.Medium, vertical = 9.dp),
    ) {
        TextInput(
            placeholder = {
                Text(text = placeholder, color = MaterialKolorTokens.onSurfaceVariant.color, maxLines = 1)
            },
        )
    }
}
