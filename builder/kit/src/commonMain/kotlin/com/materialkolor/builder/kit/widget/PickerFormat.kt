package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.color.ColorFormat
import com.materialkolor.builder.domain.color.ColorInput
import com.materialkolor.builder.domain.color.InvalidReason
import com.materialkolor.builder.domain.color.ParseNote
import com.materialkolor.builder.domain.color.ParseResult
import com.materialkolor.builder.domain.color.textOf
import com.materialkolor.builder.kit.control.BuilderHexField
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.picker_color
import com.materialkolor.builder.kit.generated.resources.picker_error_bad_arguments
import com.materialkolor.builder.kit.generated.resources.picker_error_bad_hex
import com.materialkolor.builder.kit.generated.resources.picker_error_empty
import com.materialkolor.builder.kit.generated.resources.picker_error_unknown_function
import com.materialkolor.builder.kit.generated.resources.picker_error_unknown_name
import com.materialkolor.builder.kit.generated.resources.picker_error_unrecognized
import com.materialkolor.builder.kit.generated.resources.picker_format
import com.materialkolor.builder.kit.generated.resources.picker_format_hex
import com.materialkolor.builder.kit.generated.resources.picker_format_hsl
import com.materialkolor.builder.kit.generated.resources.picker_format_oklch
import com.materialkolor.builder.kit.generated.resources.picker_format_rgb
import com.materialkolor.builder.kit.generated.resources.picker_note_alpha_dropped
import com.materialkolor.builder.kit.generated.resources.picker_note_clamped
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The format switch and the field under it, which shows the picker's color in the chosen format and
 * takes a typed color in any form [ColorInput] reads.
 *
 * Hex uses the skin's [BuilderHexField]. The other formats write the color with [textOf] and read a
 * typed one back with [ColorInput.parse]. A typed color reports as a discrete edit.
 */
@Composable
internal fun PickerFormat(
    picker: PickerState,
    modifier: Modifier = Modifier,
) {
    var format by remember { mutableStateOf(ColorFormat.Hex) }
    val names = ColorFormat.entries.associateWith { entry -> stringResource(entry.nameResource()) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
    ) {
        BuilderSegmented(
            options = ColorFormat.entries,
            selected = format,
            onSelect = { chosen -> format = chosen },
            label = stringResource(Res.string.picker_format),
            // The picker's column is narrow, and a check beside OKLCH leaves no room for the labels.
            compact = true,
        ) { entry -> names.getValue(entry) }
        FormatField(picker, format)
    }
}

/**
 * The color in [format], read from [picker] here so a drag recomposes the field and nothing around it.
 *
 * Outside hex, what the last typed color had to change to fit shows under the field for as long as
 * the picker stays on that color, the way [BuilderHexField] shows it.
 */
@Composable
private fun FormatField(
    picker: PickerState,
    format: ColorFormat,
) {
    val color = picker.color
    val label = stringResource(Res.string.picker_color)
    val messages = parseMessages()
    val modifier = Modifier.fillMaxWidth()
    if (format == ColorFormat.Hex) {
        BuilderHexField(
            value = color,
            onCommit = { argb, _ -> picker.type(argb) },
            label = label,
            errorMessage = messages::errorOf,
            noteMessage = messages::notesOf,
            modifier = modifier,
        )
    } else {
        var typed by remember(format) { mutableStateOf<ParseResult.Ok?>(null) }
        val noted = typed?.takeIf { read -> read.argb == color && read.notes.isNotEmpty() }
        BuilderTextField(
            value = format.textOf(color),
            onCommit = { text ->
                val read = ColorInput.parse(text)
                if (read is ParseResult.Ok) {
                    typed = read
                    picker.type(read.argb)
                }
            },
            label = label,
            modifier = modifier,
            error = { draft -> (ColorInput.parse(draft) as? ParseResult.Invalid)?.reason?.let(messages::errorOf) },
            supportingText = noted?.let { read -> messages.notesOf(read.notes) },
            style = BuilderTextStyle.Value,
        )
    }
}

/**
 * What the color field says about text it cannot read and about what it had to change.
 */
@Immutable
private class ParseMessages(
    private val errors: Map<InvalidReason, String>,
    private val notes: Map<ParseNote, String>,
) {
    fun errorOf(reason: InvalidReason): String = errors.getValue(reason)

    /**
     * Every note in [set], one sentence each, in a steady order.
     */
    fun notesOf(set: Set<ParseNote>): String =
        ParseNote.entries.filter { note -> note in set }.joinToString(" ") { note -> notes.getValue(note) }
}

@Composable
private fun parseMessages(): ParseMessages {
    val errors = InvalidReason.entries.associateWith { reason -> stringResource(reason.messageResource()) }
    val notes = ParseNote.entries.associateWith { note -> stringResource(note.messageResource()) }
    return remember(errors, notes) { ParseMessages(errors, notes) }
}

private fun ColorFormat.nameResource(): StringResource =
    when (this) {
        ColorFormat.Hex -> Res.string.picker_format_hex
        ColorFormat.Rgb -> Res.string.picker_format_rgb
        ColorFormat.Hsl -> Res.string.picker_format_hsl
        ColorFormat.Oklch -> Res.string.picker_format_oklch
    }

private fun InvalidReason.messageResource(): StringResource =
    when (this) {
        InvalidReason.Empty -> Res.string.picker_error_empty
        InvalidReason.BadHex -> Res.string.picker_error_bad_hex
        InvalidReason.BadArguments -> Res.string.picker_error_bad_arguments
        InvalidReason.UnknownFunction -> Res.string.picker_error_unknown_function
        InvalidReason.UnknownName -> Res.string.picker_error_unknown_name
        InvalidReason.Unrecognized -> Res.string.picker_error_unrecognized
    }

private fun ParseNote.messageResource(): StringResource =
    when (this) {
        ParseNote.AlphaDropped -> Res.string.picker_note_alpha_dropped
        ParseNote.Clamped -> Res.string.picker_note_clamped
    }
