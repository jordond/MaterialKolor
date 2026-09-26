package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorInput
import com.materialkolor.builder.domain.color.InvalidReason
import com.materialkolor.builder.domain.color.ParseNote
import com.materialkolor.builder.domain.color.ParseResult
import com.materialkolor.builder.kit.headless.rememberFieldDraft
import com.materialkolor.builder.kit.token.LocalBuilderType
import kotlinx.coroutines.delay

/**
 * A color field that takes hex, `#6750A4` or `6750A4`, and drops any other character as it is typed
 * or pasted. The poster's [large] seed headline reads anything `ColorInput` does instead, from
 * `#6750A4` to `oklch(0.6 0.1 280)` or a CSS name, see [anyFormat].
 *
 * It commits on Enter, on leaving the field, or [CommitDelayMillis] after the last keystroke that
 * left a valid color. Text that is not a color shows an error and never commits. Esc puts back the
 * committed color. When the color had to change to fit, because its alpha was dropped or it was
 * clamped into sRGB, the note shows under the field and goes along with the commit. Enter and
 * leaving the field also tidy the text into `#RRGGBB`, while the pause keeps it as typed.
 *
 * A new [value] from outside, such as a shuffle or an undo, replaces whatever the field shows and
 * cancels a pending commit.
 *
 * @param[value] The committed color.
 * @param[onCommit] Called with a new color and what had to change to read it. Only called when the
 * color differs from [value].
 * @param[label] Names the field, on screen and to assistive tech.
 * @param[errorMessage] What to say under the field about text that is not a color.
 * @param[noteMessage] What to say under the field about what had to change to read the color. Only
 * asked about a set with at least one note in it, and the words depend on what the color is for.
 * @param[modifier] Applied to the field.
 * @param[large] Draw it as the poster's 72 sp seed headline, which shows no label. A color
 * that would not fit the width the field is offered at 72 sp is set smaller until it does.
 * @param[enabled] Whether the field takes input.
 * @param[anyFormat] Whether the field keeps every character, for text in any format `ColorInput`
 * reads. Off by default for the regular field, which keeps only what [hexOnly] lets through.
 */
@Composable
public fun BuilderHexField(
    value: Argb,
    onCommit: (Argb, Set<ParseNote>) -> Unit,
    label: String,
    errorMessage: (InvalidReason) -> String,
    noteMessage: (Set<ParseNote>) -> String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    enabled: Boolean = true,
    anyFormat: Boolean = large,
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
        val canonical = read.argb.toHex()
        if (tidy) draft.settle(canonical) else draft.commit(draft.text, canonical)
    }

    // Keyed on the value as well, so a color arriving from outside cancels the wait, and on focus,
    // so leaving the field cancels it too.
    LaunchedEffect(draft.text, value, draft.focused) {
        if (!draft.focused || !draft.dirty || parsed !is ParseResult.Ok) return@LaunchedEffect
        delay(CommitDelayMillis)
        if (!draft.composing) commitDraft(tidy = false)
    }

    val error = (parsed as? ParseResult.Invalid)?.takeIf { draft.dirty }?.reason?.let(errorMessage)
    val notes = (parsed as? ParseResult.Ok)?.notes?.takeIf { it.isNotEmpty() } ?: committedNotes
    val type = LocalBuilderType.current
    val message = error ?: notes.takeIf { it.isNotEmpty() }?.let(noteMessage)
    val field: @Composable (style: TextStyle, fieldModifier: Modifier) -> Unit = { style, fieldModifier ->
        SkinField(
            draft = draft,
            label = label,
            message = message,
            isError = error != null,
            textStyle = style,
            large = large,
            enabled = enabled,
            onCommit = { commitDraft(tidy = true) },
            modifier = fieldModifier,
            onEdit = { _ ->
                committedNotes = emptySet()
                if (!anyFormat) draft.value = hexOnly(draft.value)
            },
        )
    }
    if (!large) {
        field(type.value, modifier)
        return
    }
    // The caller's modifier stays on the field, which fills the width the box is offered.
    BoxWithConstraints {
        val fitted = rememberFittedHero(draft.text, type.posterHero, maxWidth)
        field(fitted, modifier)
    }
}

/**
 * [hero] set small enough for [text] to fit on one line in [width], never larger than it is and
 * never below [HeroMinScale] of it, past which the field scrolls instead.
 */
@Composable
private fun rememberFittedHero(
    text: String,
    hero: TextStyle,
    width: Dp,
): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, hero, width, density) {
        val room = with(density) { (width - HeroCaretRoom).toPx() }
        val natural = measurer.measure(text.ifEmpty { " " }, hero, maxLines = 1, softWrap = false).size.width
        val scale = if (natural <= room || natural == 0) 1f else (room / natural).coerceAtLeast(HeroMinScale)
        if (scale == 1f) {
            hero
        } else {
            hero.copy(
                fontSize = hero.fontSize.scaled(scale),
                lineHeight = hero.lineHeight.scaled(scale),
                letterSpacing = hero.letterSpacing.scaled(scale),
            )
        }
    }
}

/**
 * This size times [scale], or unspecified as it was.
 */
private fun TextUnit.scaled(scale: Float): TextUnit = if (isSpecified) this * scale else this

/**
 * Room kept past the hex for the caret, so the last digit never sits under it.
 */
private val HeroCaretRoom: Dp = 4.dp

/**
 * The least the headline is scaled to fit. Text longer than that scrolls in the field.
 */
private const val HeroMinScale: Float = 0.4f

/**
 * [value] with only what a hex color is written with, one leading `#` and at most [HexDigitCap] hex
 * digits. Every other character is dropped, and so is any digit past the cap, so a paste keeps the
 * hex it holds. The cursor and selection stay by the characters they were next to. Text that needs
 * no change comes back as it was, composition and all.
 */
internal fun hexOnly(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val kept = StringBuilder(text.length)
    // Where each index of the text lands in the kept text, for the cursor and selection.
    val landing = IntArray(text.length + 1)
    var digits = 0
    text.forEachIndexed { index, char ->
        landing[index] = kept.length
        val keep = when {
            char == '#' -> kept.isEmpty()
            char.isHexDigit() -> digits < HexDigitCap
            else -> false
        }
        if (keep) {
            kept.append(char)
            if (char != '#') digits++
        }
    }
    landing[text.length] = kept.length
    if (kept.length == text.length) return value
    val selection = TextRange(landing[value.selection.start], landing[value.selection.end])
    return TextFieldValue(kept.toString(), selection)
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

/**
 * The most hex digits `ColorInput` reads, eight for `AARRGGBB`, whose alpha the field then drops
 * with a note.
 */
internal const val HexDigitCap: Int = 8

/**
 * How long the field waits after the last valid keystroke before it commits.
 */
internal const val CommitDelayMillis: Long = 400L
