package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.dynamiccolor.DynamicScheme

/**
 * The three colors a chip is drawn in, read from its scheme once.
 */
internal class ChipColors(
    val primary: Color,
    val secondaryContainer: Color,
    val tertiaryContainer: Color,
) {
    companion object {
        fun of(scheme: DynamicScheme): ChipColors =
            ChipColors(Color(scheme.primary), Color(scheme.secondaryContainer), Color(scheme.tertiaryContainer))
    }
}

/**
 * The chip colours of [document] in the mode [isDark] picks, drawn all at once the first time and
 * brought up to date one chip per [pause] after that.
 */
@Composable
internal fun rememberChipShelf(
    document: ThemeDocument,
    isDark: Boolean,
    lookup: StyleSchemeLookup,
    pause: ChipPause,
): ChipShelf {
    val shelf = remember { ChipShelf(document, isDark, lookup) }
    val currentLookup by rememberUpdatedState(lookup)
    val currentPause by rememberUpdatedState(pause)
    LaunchedEffect(shelf, document, isDark) { shelf.catchUp(document, isDark, currentLookup, currentPause) }
    return shelf
}

/**
 * The colours each style chip shows, kept apart from the document so a change to it never draws a
 * chip inside the frame that brings it.
 *
 * Each chip remembers what it was drawn from. [catchUp] draws again every chip the document has moved
 * on from, the one left waiting longest first, and pauses before each. A drag that moves the scheme
 * every frame starts a new catch up every frame, so the chips take turns, each a few frames behind at
 * most, and all of them are current again about ten frames after the drag stops.
 *
 * The pause is a frame rather than a `yield`, since on the web Compose runs a yielded effect again
 * inside the same frame.
 */
@Stable
internal class ChipShelf(
    document: ThemeDocument,
    isDark: Boolean,
    lookup: StyleSchemeLookup,
) {
    /**
     * What each chip was last drawn from. Only the catch up reads it.
     */
    private val drawnFrom = mutableMapOf<Style, ChipKey>()

    /**
     * When each chip was last drawn, counted in draws, so the longest waiting goes first.
     */
    private val drawnAt = mutableMapOf<Style, Int>()
    private var draws = 0

    private val shown: Map<Style, MutableState<ChipColors>> =
        Style.entries.associateWith { style ->
            mutableStateOf(draw(style, ChipKey.of(document, style, isDark), lookup))
        }

    /**
     * The colours [style]'s chip shows now.
     */
    operator fun get(style: Style): ChipColors = shown.getValue(style).value

    /**
     * Draws again every chip [document] in the mode [isDark] picks has moved on from, pausing before each.
     */
    suspend fun catchUp(
        document: ThemeDocument,
        isDark: Boolean,
        lookup: StyleSchemeLookup,
        pause: ChipPause,
    ) {
        val behind = Style.entries
            .map { style -> style to ChipKey.of(document, style, isDark) }
            .filter { (style, key) -> drawnFrom[style] != key }
            .sortedBy { (style, _) -> drawnAt.getValue(style) }
        for ((style, key) in behind) {
            pause()
            shown.getValue(style).value = draw(style, key, lookup)
        }
    }

    private fun draw(
        style: Style,
        key: ChipKey,
        lookup: StyleSchemeLookup,
    ): ChipColors {
        drawnFrom[style] = key
        drawnAt[style] = draws++
        return ChipColors.of(lookup(key.inputs, key.isDark))
    }
}

/**
 * Waits for the next frame, so a catch up draws one chip a frame.
 */
internal val NextFrame: ChipPause = { withFrameNanos {} }

/**
 * What one chip is drawn from.
 *
 * @property[inputs] The scheme inputs its style makes of the document.
 * @property[isDark] The mode the preview shows.
 */
private data class ChipKey(
    val inputs: SchemeInputs,
    val isDark: Boolean,
) {
    companion object {
        fun of(
            document: ThemeDocument,
            style: Style,
            isDark: Boolean,
        ): ChipKey = ChipKey(SchemeInputs.from(document.copy(style = style)), isDark)
    }
}
