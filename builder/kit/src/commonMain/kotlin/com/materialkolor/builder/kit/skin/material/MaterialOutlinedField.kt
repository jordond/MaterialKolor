package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.kit.headless.InnerTextWithoutHandles

// b-228a

/**
 * Material3's single line outlined text field, put together from the parts `OutlinedTextField` is
 * made of, the foundation field inside Material's outlined decoration box. It looks and reads the
 * same as `OutlinedTextField` with the same arguments, but its inner text goes without the touch
 * selection handles where overlays render in the page (D45), which `OutlinedTextField` gives no way
 * to reach.
 *
 * `OutlinedTextField` also names an error field "Invalid input" when nothing else does. Every
 * builder field in error carries its own message, so that part is left out.
 */
@Composable
internal fun MaterialOutlinedField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    supportingText: String? = null,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val colors = OutlinedTextFieldDefaults.colors()
    val textColor = textStyle.color.takeOrElse { colors.textColor(enabled, isError, focused) }
    // The label sits across the outline, so the field gives it half its line above the box.
    val labelLine = MaterialTheme.typography.bodySmall.lineHeight
    val labelHalf = with(LocalDensity.current) { (if (labelLine.isSp) labelLine else 16.sp).toDp() / 2 }
    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .semantics(mergeDescendants = true) {}
                .padding(top = labelHalf)
                .defaultMinSize(
                    minWidth = OutlinedTextFieldDefaults.MinWidth,
                    minHeight = OutlinedTextFieldDefaults.MinHeight,
                ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.merge(TextStyle(color = textColor)),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            interactionSource = interactions,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value.text,
                    innerTextField = { InnerTextWithoutHandles(innerTextField) },
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactions,
                    isError = isError,
                    label = { Text(label) },
                    trailingIcon = trailingIcon,
                    supportingText = supportingText?.let { text -> { Text(text) } },
                    colors = colors,
                )
            },
        )
    }
}
