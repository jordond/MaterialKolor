package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentField
import com.materialkolor.builder.kit.skin.fluent.FluentInputStyles
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.FieldStyle
import com.materialkolor.builder.kit.skin.headless.UnstyledInputStyles
import com.materialkolor.builder.kit.skin.headless.inputAlpha
import com.materialkolor.builder.kit.skin.material.MaterialField
import com.materialkolor.builder.kit.skin.material.materialHeroFieldStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType

/**
 * A text field that hands its text over only once someone is done with it.
 *
 * Typing edits a draft. Enter or leaving the field commits it, Esc throws it away, and neither key
 * does anything while an input method is still composing. Cmd or Ctrl+Z inside the field stays the
 * field's own text undo.
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
 * The text a field is showing, and the committed text it came from.
 *
 * A field only takes a new committed text while it is not being edited, so a value arriving
 * mid-edit, such as the result of its own debounced commit, never yanks the text out from under
 * someone typing.
 */
@Stable
internal class FieldDraft(
    committed: String,
) {
    var value: TextFieldValue by mutableStateOf(TextFieldValue(committed, TextRange(committed.length)))

    /** Whether the field has focus, which the commit on leaving needs to know. */
    var focused: Boolean by mutableStateOf(false)

    /** The committed text this draft started from. */
    var committed: String by mutableStateOf(committed)
        private set

    val text: String
        get() = value.text

    /** True when the draft differs from what was committed. */
    val dirty: Boolean
        get() = value.text != committed

    /** True while an input method is still composing, when Enter and Esc belong to it. */
    val composing: Boolean
        get() = value.composition != null

    /** Takes a committed text from outside, unless someone is halfway through an edit. */
    fun sync(external: String) {
        if (external == committed) return
        val clean = !dirty
        committed = external
        if (!focused || clean) show(external)
    }

    /** Shows [text] and treats it as committed. */
    fun settle(text: String) {
        committed = text
        if (value.text != text) show(text)
    }

    /** Throws the draft away. False when there was nothing to throw away. */
    fun revert(): Boolean {
        if (!dirty) return false
        show(committed)
        return true
    }

    private fun show(text: String) {
        value = TextFieldValue(text, TextRange(text.length))
    }
}

/** A draft that follows [committed] whenever nobody is editing it. */
@Composable
internal fun rememberFieldDraft(committed: String): FieldDraft {
    val draft = remember { FieldDraft(committed) }
    SideEffect { draft.sync(committed) }
    return draft
}

/**
 * Enter commits, Esc reverts, and leaving the field commits. Enter and Esc are left to the input
 * method while it is composing, and Esc on a clean field is left for whatever sits around it.
 */
internal fun Modifier.fieldCommits(
    draft: FieldDraft,
    onCommit: () -> Unit,
    onRevert: () -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || draft.composing) return@onPreviewKeyEvent false
        when (event.key) {
            Key.Enter, Key.NumPadEnter -> {
                onCommit()
                true
            }
            Key.Escape -> {
                draft.revert().also { reverted -> if (reverted) onRevert() }
            }
            else -> {
                false
            }
        }
    }.onFocusChanged { state ->
        val left = draft.focused && !state.hasFocus
        draft.focused = state.hasFocus
        if (left) onCommit()
    }

/**
 * Draws [draft] in the surrounding skin. The large field is the poster's seed headline, which every
 * skin draws as headless text with its own underline.
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
    val field = modifier.fieldCommits(draft, onCommit, onRevert = onEdit)
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

/**
 * A text field on the foundation field, with its label above, its message under and the box drawn
 * from [style]. The label and message sit inside the field so a tap on either focuses it.
 *
 * A large field hides its label and only names itself to assistive tech, since the text is the
 * heading.
 */
@Composable
internal fun HeadlessField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    message: String?,
    isError: Boolean,
    textStyle: TextStyle,
    large: Boolean,
    enabled: Boolean,
    onDone: () -> Unit,
    style: FieldStyle,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val edge = when {
        isError -> style.error
        focused -> style.active
        else -> null
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .semantics {
                contentDescription = label
                if (isError && message != null) error(message)
            }.alpha(inputAlpha(enabled)),
        enabled = enabled,
        textStyle = textStyle.merge(color = tokens.textStrong),
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        interactionSource = interactions,
        cursorBrush = SolidColor(style.cursor),
        decorationBox = { text ->
            Column(verticalArrangement = Arrangement.spacedBy(style.gap)) {
                if (!large) {
                    BuilderText(label, style = BuilderTextStyle.Label, emphasis = Emphasis.Secondary)
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = LocalLayout.current.primaryTouchTarget)
                        .fieldBox(style, edge),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    text()
                }
                if (message != null) {
                    BuilderText(
                        text = message,
                        style = BuilderTextStyle.Body,
                        emphasis = if (isError) Emphasis.Danger else Emphasis.Secondary,
                    )
                }
            }
        },
    )
}

/** The box around the text, with the focused or error edge drawn around it or under it. */
private fun Modifier.fieldBox(
    style: FieldStyle,
    edge: Color?,
): Modifier {
    val box = background(style.container, style.shape)
    val outlined = when {
        edge == null -> {
            box.border(style.outlineWidth, style.outline, style.shape)
        }
        style.activeAsUnderline -> {
            box.border(style.outlineWidth, style.outline, style.shape).drawBehind {
                val height = style.activeWidth.toPx()
                drawRect(edge, topLeft = Offset(0f, size.height - height), size = Size(size.width, height))
            }
        }
        else -> {
            box.border(style.activeWidth, edge, style.shape)
        }
    }
    return outlined.fillMaxWidth().padding(style.padding)
}
