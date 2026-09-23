package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorInput
import com.materialkolor.builder.domain.color.InvalidReason
import com.materialkolor.builder.domain.color.ParseNote
import com.materialkolor.builder.domain.color.ParseResult
import com.materialkolor.builder.kit.token.LocalBuilderType
import kotlinx.coroutines.delay

/**
 * A color field that reads anything `ColorInput` does, from `#6750A4` to `oklch(0.6 0.1 280)` or a CSS
 * name (F-05).
 *
 * It commits on Enter, on leaving the field, or [CommitDelayMillis] after the last keystroke that
 * left a valid color. Text that is not a color shows an error and never commits. Esc puts back the
 * committed color. When the color had to change to fit, because its alpha was dropped or it was
 * clamped into sRGB, the note shows under the field and goes along with the commit. Enter and
 * leaving the field also tidy the text into `#RRGGBB`.
 *
 * @param[value] The committed color.
 * @param[onCommit] Called with a new color and what had to change to read it. Only called when the
 * color differs from [value].
 * @param[label] Names the field, on screen and to assistive tech.
 * @param[modifier] Applied to the field.
 * @param[large] Draw it as the poster's 72 sp seed headline (F-66), which shows no label.
 * @param[enabled] Whether the field takes input.
 */
@Composable
public fun BuilderHexField(
    value: Argb,
    onCommit: (Argb, Set<ParseNote>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    enabled: Boolean = true,
) {
    val draft = rememberFieldDraft(value.toHex())
    val parsed = remember(draft.text) { ColorInput.parse(draft.text) }
    var committedNotes by remember { mutableStateOf(emptySet<ParseNote>()) }
    val current by rememberUpdatedState(value)
    val commit by rememberUpdatedState(onCommit)

    fun commitDraft(tidy: Boolean) {
        val read = ColorInput.parse(draft.text) as? ParseResult.Ok ?: return
        if (read.argb != current) commit(read.argb, read.notes)
        committedNotes = read.notes
        if (tidy) draft.settle(read.argb.toHex())
    }

    LaunchedEffect(draft.text) {
        if (!draft.dirty || parsed !is ParseResult.Ok) return@LaunchedEffect
        delay(CommitDelayMillis)
        if (!draft.composing) commitDraft(tidy = false)
    }

    val error = (parsed as? ParseResult.Invalid)?.takeIf { draft.dirty }?.reason?.message()
    val notes = (parsed as? ParseResult.Ok)?.notes?.takeIf { it.isNotEmpty() } ?: committedNotes
    val type = LocalBuilderType.current
    SkinField(
        draft = draft,
        label = label,
        message = error ?: notes.message(),
        isError = error != null,
        textStyle = if (large) type.posterHero else type.value,
        large = large,
        enabled = enabled,
        onCommit = { commitDraft(tidy = true) },
        modifier = modifier,
        onEdit = { committedNotes = emptySet() },
    )
}

/** How long the field waits after the last valid keystroke before it commits (F-05). */
internal const val CommitDelayMillis: Long = 400L

private fun InvalidReason.message(): String =
    when (this) {
        InvalidReason.Empty -> "Type a color"
        InvalidReason.BadHex -> "Hex takes 3, 6 or 8 digits"
        InvalidReason.BadArguments -> "Check the values inside the brackets"
        InvalidReason.UnknownFunction -> "Try rgb(), hsl() or oklch()"
        InvalidReason.UnknownName -> "Not a CSS color name"
        InvalidReason.Unrecognized -> "Not a color"
    }

private fun Set<ParseNote>.message(): String? {
    if (isEmpty()) return null
    return sorted().joinToString(". ") { note ->
        when (note) {
            ParseNote.AlphaDropped -> "Alpha dropped, the seed is always opaque"
            ParseNote.Clamped -> "Clamped to sRGB"
        }
    }
}
