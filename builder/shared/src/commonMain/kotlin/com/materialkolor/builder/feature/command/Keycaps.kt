package com.materialkolor.builder.feature.command

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * [keys] drawn as keycaps, the way [Shortcut.text] writes them. "Ctrl+Shift+Z, Ctrl+Y" shows a cap
 * for each key, the keys of one chord close together and the chords further apart. It reads out as
 * [keys] whole, so a screen reader hears "Ctrl+K" rather than two separate letters.
 */
@Composable
internal fun Keycaps(
    keys: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = keys },
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keycapChords(keys).forEach { chord ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                chord.forEach { key ->
                    Keycap { BuilderText(key, style = BuilderTextStyle.Value, emphasis = Emphasis.Secondary) }
                }
            }
        }
    }
}

/**
 * One keycap, a small outlined key holding [content], a key's name or its glyph.
 */
@Composable
internal fun Keycap(content: @Composable () -> Unit) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small)
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = KeycapSize, minHeight = KeycapSize)
            .background(tokens.panel, shape)
            .border(tokens.outlineWidth, tokens.border, shape)
            .padding(horizontal = tokens.spacing.small),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * The keys of each chord in [keys], split the way [Shortcut.text] joins them, chords at a comma and
 * a space and keys at a plus sign. "Ctrl+Shift+Z, Ctrl+Y" gives Ctrl, Shift and Z, then Ctrl and Y.
 */
internal fun keycapChords(keys: String): List<List<String>> =
    keys.split(CHORD_SEPARATOR).filter { chord -> chord.isNotBlank() }.map { chord -> chord.split(KEY_SEPARATOR) }

private const val CHORD_SEPARATOR = ", "
private const val KEY_SEPARATOR = '+'

/**
 * The narrowest and the shortest a keycap gets, so a one letter key is square.
 */
private val KeycapSize: Dp = 24.dp

/**
 * How big a glyph on a keycap is, such as an arrow.
 */
internal val KeycapGlyphSize: Dp = 14.dp
