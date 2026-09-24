package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
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
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.FieldStyle
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The text a field is showing, and the committed value it came from.
 *
 * The committed value is kept twice. [committed] is the value as its owner writes it, and
 * [committedText] is the text that stands for it on screen. The two only differ after the field
 * committed text its owner writes another way, such as "red" for `#FF0000`. When that value comes
 * back the text stays as typed, while any other value from outside replaces the draft.
 */
@Stable
internal class FieldDraft(
    committed: String,
) {
    var value: TextFieldValue by mutableStateOf(TextFieldValue(committed, TextRange(committed.length)))

    /** Whether the field has focus, which the commit on leaving needs to know. */
    var focused: Boolean by mutableStateOf(false)

    /** The committed value as its owner writes it. */
    var committed: String by mutableStateOf(committed)
        private set

    /** The text that stands for [committed], which is [committed] itself unless the field sent it. */
    var committedText: String by mutableStateOf(committed)
        private set

    val text: String
        get() = value.text

    /** True when the draft differs from the text that stands for the committed value. */
    val dirty: Boolean
        get() = value.text != committedText

    /** True while an input method is still composing, when Enter and Esc belong to it. */
    val composing: Boolean
        get() = value.composition != null

    /**
     * Takes a committed value from its owner. The field's own commit coming back leaves the text
     * alone, and any other value replaces it, halfway through an edit or not.
     */
    fun sync(external: String) {
        if (external == committed) return
        committed = external
        committedText = external
        show(external)
    }

    /**
     * Notes that the field committed [text], which its owner writes as [canonical]. The text stays as
     * typed and counts as committed.
     */
    fun commit(
        text: String,
        canonical: String,
    ) {
        committed = canonical
        committedText = text
    }

    /** Shows [text] and treats it as committed. */
    fun settle(text: String) {
        committed = text
        committedText = text
        if (value.text != text) show(text)
    }

    /** Throws the draft away for the committed value. False when there was nothing to throw away. */
    fun revert(): Boolean {
        if (!dirty) return false
        committedText = committed
        show(committed)
        return true
    }

    private fun show(text: String) {
        value = TextFieldValue(text, TextRange(text.length))
    }
}

/** A draft that follows [committed] whenever it changes from outside. */
@Composable
internal fun rememberFieldDraft(committed: String): FieldDraft {
    val draft = remember { FieldDraft(committed) }
    SideEffect { draft.sync(committed) }
    return draft
}

/**
 * Enter commits, Esc reverts, and leaving the field commits. Enter and Esc are left to the input
 * method while it is composing, and Esc on a clean field is left for whatever sits around it.
 *
 * Enter also calls [onSubmit] once the commit is done, clean draft or not. Leaving the field never
 * does.
 */
internal fun Modifier.fieldCommits(
    draft: FieldDraft,
    onCommit: () -> Unit,
    onRevert: () -> Unit,
    onSubmit: (() -> Unit)? = null, // b-315a
): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || draft.composing) return@onPreviewKeyEvent false
        when (event.key) {
            Key.Enter, Key.NumPadEnter -> {
                onCommit()
                onSubmit?.invoke() // b-315a
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
            }.alpha(enabledAlpha(enabled)),
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
                        .fieldBox(style, edge, interactions),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // b-228a
                    InnerTextWithoutHandles(text)
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

/**
 * The box around the text, with the focused or error edge drawn around it or under it. A style with
 * a [FieldStyle.focusRing] also rings the box under keyboard focus, the way the other controls do,
 * since an edge under the text marks one side only.
 */
@Composable
private fun Modifier.fieldBox(
    style: FieldStyle,
    edge: Color?,
    interactions: InteractionSource,
): Modifier {
    val ring = style.focusRing
    val ringed = if (ring == null) this else controlRing(interactions, style.shape, ring)
    val box = ringed.background(style.container, style.shape)
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
