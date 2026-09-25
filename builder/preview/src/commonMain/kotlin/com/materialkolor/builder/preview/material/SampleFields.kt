package com.materialkolor.builder.preview.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.kit.headless.InnerTextWithoutHandles

// b-228b

/**
 * Material3's single line filled text field, put together from the parts `TextField` is made of, the
 * foundation field inside Material's filled decoration box. It looks and reads the same as
 * `TextField` with the same arguments, but on the web a long press on its text puts up no selection
 * handles, which would take the page's accessibility mirror over (D45).
 *
 * @param[value] The text in the field.
 * @param[onValueChange] Called with the text as it is typed.
 * @param[label] The label over the text.
 * @param[modifier] Applied to the field.
 * @param[enabled] Whether the field takes input.
 */
@Composable
internal fun SampleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.colors()
    val sized = modifier.defaultMinSize(minWidth = TextFieldDefaults.MinWidth, minHeight = TextFieldDefaults.MinHeight)
    SampleField(value, onValueChange, enabled, colors, interactions, sized) { innerTextField ->
        TextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = { InnerTextWithoutHandles(innerTextField) },
            enabled = enabled,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactions,
            label = { Text(label) },
            colors = colors,
        )
    }
}

/**
 * Material3's single line outlined text field, put together from the parts `OutlinedTextField` is
 * made of, the foundation field inside Material's outlined decoration box. It looks and reads the
 * same as `OutlinedTextField` with the same arguments, but on the web a long press on its text puts
 * up no selection handles, which would take the page's accessibility mirror over (D45).
 *
 * @param[value] The text in the field.
 * @param[onValueChange] Called with the text as it is typed.
 * @param[label] The label across the outline.
 * @param[modifier] Applied to the field.
 * @param[enabled] Whether the field takes input.
 */
@Composable
internal fun SampleOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    // The label sits across the outline, so the field gives it half its line above the box.
    val sized = modifier
        .semantics(mergeDescendants = true) {}
        .padding(top = labelHalfHeight())
        .defaultMinSize(minWidth = OutlinedTextFieldDefaults.MinWidth, minHeight = OutlinedTextFieldDefaults.MinHeight)
    SampleField(value, onValueChange, enabled, colors, interactions, sized) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = { InnerTextWithoutHandles(innerTextField) },
            enabled = enabled,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactions,
            label = { Text(label) },
            colors = colors,
        )
    }
}

/**
 * The foundation field both sample fields are built on, set up the way Material3's own fields set
 * it up, with [decoration] around its inner text. The [modifier] comes with the field's least size.
 */
@Composable
private fun SampleField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    colors: TextFieldColors,
    interactions: MutableInteractionSource,
    modifier: Modifier,
    decoration: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
) {
    val focused by interactions.collectIsFocusedAsState()
    val textStyle = LocalTextStyle.current
    val textColor = textStyle.color.takeOrElse { colors.textColor(enabled, isError = false, focused = focused) }
    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            textStyle = textStyle.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(colors.cursorColor(isError = false)),
            interactionSource = interactions,
            singleLine = true,
            decorationBox = decoration,
        )
    }
}

/**
 * Half the line of the small label that sits across an outlined field's outline.
 */
@Composable
private fun labelHalfHeight(): Dp {
    val line = MaterialTheme.typography.bodySmall.lineHeight
    return with(LocalDensity.current) { (if (line.isSp) line else 16.sp).toDp() / 2 }
}
