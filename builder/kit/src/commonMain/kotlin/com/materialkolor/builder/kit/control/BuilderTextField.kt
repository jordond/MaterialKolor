package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.FieldDraft
import com.materialkolor.builder.kit.headless.HeadlessField
import com.materialkolor.builder.kit.headless.fieldCommits
import com.materialkolor.builder.kit.headless.rememberFieldDraft
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentField
import com.materialkolor.builder.kit.skin.fluent.FluentInputStyles
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialField
import com.materialkolor.builder.kit.skin.material.materialHeroFieldStyle
import com.materialkolor.builder.kit.token.LocalBuilderType

/**
 * A text field that hands its text over only once someone is done with it.
 *
 * Typing edits a draft. Enter or leaving the field commits it, Esc throws it away, and neither key
 * does anything while an input method is still composing. A new [value] from outside replaces the
 * draft, even halfway through an edit.
 *
 * Cmd or Ctrl+Z inside the field stays the field's own text undo. The field takes the key as it
 * bubbles back up from the focused text, so the app's global undo shortcut has to listen with
 * `onKeyEvent` and never with `onPreviewKeyEvent`, which would see the key before the field does.
 *
 * @param[value] The committed text.
 * @param[onCommit] Called with the draft when it differs from [value] and [error] has nothing to
 * say about it.
 * @param[label] Names the field, on screen and to assistive tech.
 * @param[modifier] Applied to the field.
 * @param[error] What is wrong with a draft, or null when it can be committed. Shown under the field.
 * @param[supportingText] A hint under the field while there is no error.
 * @param[enabled] Whether the field takes input.
 * @param[style] The part of the builder's type the text is set in.
 */
@Composable
public fun BuilderTextField(
    value: String,
    onCommit: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: (String) -> String? = { null },
    supportingText: String? = null,
    enabled: Boolean = true,
    style: BuilderTextStyle = BuilderTextStyle.Body,
) {
    val draft = rememberFieldDraft(value)
    val problem = if (draft.dirty) error(draft.text) else null
    SkinField(
        draft = draft,
        label = label,
        message = problem ?: supportingText,
        isError = problem != null,
        textStyle = LocalBuilderType.current[style],
        large = false,
        enabled = enabled,
        onCommit = {
            if (draft.dirty && error(draft.text) == null) {
                onCommit(draft.text)
                draft.settle(draft.text)
            }
        },
        modifier = modifier,
    )
}

/**
 * Draws [draft] in the surrounding skin. The large field is the poster's seed headline, which every
 * skin draws as headless text with its own underline.
 *
 * On the web the field node is named [label], with the disabled note while it is disabled (D37).
 * Material's field is otherwise nameless there, since its editable text overwrites the label, and
 * the page marks every field editable, disabled or not.
 */
@Composable
internal fun SkinField(
    draft: FieldDraft,
    label: String,
    message: String?,
    isError: Boolean,
    textStyle: TextStyle,
    large: Boolean,
    enabled: Boolean,
    onCommit: () -> Unit,
    modifier: Modifier,
    onEdit: () -> Unit = {},
) {
    val field = modifier.fieldCommits(draft, onCommit, onRevert = onEdit).foldState(label, null, enabled)
    val onValueChange = { next: TextFieldValue ->
        if (next.text != draft.text) onEdit()
        draft.value = next
    }
    val library = LocalSkin.current.library
    if (large) {
        val hero = when (library) {
            Library.Material3 -> materialHeroFieldStyle()
            Library.Unstyled -> UnstyledInputStyles.hero
            Library.Fluent -> FluentInputStyles.hero
            Library.Custom -> CustomInputStyles.hero
        }
        HeadlessField(
            draft.value,
            onValueChange,
            label,
            message,
            isError,
            textStyle,
            true,
            enabled,
            onCommit,
            hero,
            field,
        )
        return
    }
    when (library) {
        Library.Material3 -> {
            MaterialField(draft.value, onValueChange, label, message, isError, textStyle, enabled, onCommit, field)
        }
        Library.Unstyled -> {
            val style = UnstyledInputStyles.field
            HeadlessField(
                draft.value,
                onValueChange,
                label,
                message,
                isError,
                textStyle,
                false,
                enabled,
                onCommit,
                style,
                field,
            )
        }
        Library.Fluent -> {
            FluentField(draft.value, onValueChange, label, message, isError, textStyle, enabled, onCommit, field)
        }
        Library.Custom -> {
            val style = CustomInputStyles.field
            HeadlessField(
                draft.value,
                onValueChange,
                label,
                message,
                isError,
                textStyle,
                false,
                enabled,
                onCommit,
                style,
                field,
            )
        }
    }
}
