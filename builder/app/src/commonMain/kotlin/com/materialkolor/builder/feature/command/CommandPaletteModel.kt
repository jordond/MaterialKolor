package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.materialkolor.builder.di.AppScope
import dev.stateholder.extensions.viewmodel.StateViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey

/**
 * What the command palette keeps between key presses, the search and the commands run lately.
 *
 * The recents last as long as the page, since nothing saves them. Each open starts with an empty
 * search.
 */
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class CommandPaletteModel : StateViewModel<CommandPaletteModel.State>(State()) {
    /**
     * @property[query] The search as it is typed, which the rows follow.
     * @property[committed] The search as the field last committed it, the field's own value. It only
     * moves on Enter and when focus leaves the field, so typing never rewrites the field under an
     * input method.
     * @property[recents] The ids of the commands run lately, the latest first.
     */
    @Immutable
    data class State(
        val query: String = "",
        val committed: String = "",
        val recents: List<String> = emptyList(),
    )

    /** The field's draft changed to [text]. */
    fun type(text: String) {
        updateState { state -> state.copy(query = text) }
    }

    /** The field committed [text]. */
    fun commit(text: String) {
        updateState { state -> state.copy(query = text, committed = text) }
    }

    /** Empty the search, for Esc and for the next time the palette opens. */
    fun clear() {
        updateState { state -> state.copy(query = "", committed = "") }
    }

    /** The command [id] ran, so it leads the next empty search. */
    fun ran(id: String) {
        updateState { state -> state.copy(recents = (listOf(id) + (state.recents - id)).take(MAX_RECENTS)) }
    }

    private companion object {
        const val MAX_RECENTS = 5
    }
}

/**
 * One row the palette can list.
 *
 * @property[id] The command's id, or one of the palette's own for the rows it adds.
 * @property[label] What the row says it does.
 * @property[category] The group it belongs to, in words.
 * @property[keys] Its shortcut written out for this platform, or null.
 * @property[state] Whether it runs now, and why not.
 * @property[selected] Whether what it picks or switches is on now, or null.
 * @property[words] Other words a search finds it by, never shown.
 * @property[onlyWhenAsked] Whether it only lists for a search, as the roles and the poster's sections
 * do, which would bury the commands otherwise.
 * @property[run] Does it. Call it from inside the key press or the click.
 */
@Immutable
internal class PaletteEntry(
    val id: String,
    val label: String,
    val category: String,
    val keys: String?,
    val state: CommandState,
    val selected: Boolean?,
    val words: List<String> = emptyList(),
    val onlyWhenAsked: Boolean = false,
    val run: () -> Unit,
)

/**
 * The rows for [query] out of [entries], best first.
 *
 * With nothing typed every entry but those listed only when asked comes in its own order, the ones
 * in [recents] first, latest first. A query keeps the entries it matches as a subsequence of the
 * label, the category or one of the other words, ranked by how well it matches, a recent entry
 * winning a tie, then the shorter label, then the entry's own order.
 */
internal fun searchPalette(
    entries: List<PaletteEntry>,
    query: String,
    recents: List<String>,
): List<PaletteEntry> {
    val recency = recents.withIndex().associate { (index, id) -> id to index }

    fun recent(entry: PaletteEntry): Int = recency[entry.id] ?: recents.size
    if (query.isBlank()) {
        return entries.filter { entry -> !entry.onlyWhenAsked }.sortedBy(::recent)
    }
    val scored = entries.mapNotNull { entry -> matchScore(query, entry)?.let { score -> entry to score } }
    return scored
        .sortedWith(
            compareByDescending<Pair<PaletteEntry, Int>> { (_, score) -> score }
                .thenBy { (entry, _) -> recent(entry) }
                .thenBy { (entry, _) -> entry.label.length },
        ).map { (entry, _) -> entry }
}

/** How well [query] matches [entry], the label counting most, or null when it does not. */
private fun matchScore(
    query: String,
    entry: PaletteEntry,
): Int? {
    val label = fuzzyScore(query, entry.label)?.plus(LABEL_BONUS)
    val others = (listOf(entry.category) + entry.words).mapNotNull { text -> fuzzyScore(query, text) }
    return (listOfNotNull(label) + others).maxOrNull()
}

/**
 * How well [query] matches [text] as a subsequence, ignoring case and the query's spaces, or null
 * when it does not.
 *
 * Every letter matched scores, more at the start of a word, camel case humps included, and more
 * again right after the letter before it, while each letter skipped between two matches costs a
 * little. So "tsp" finds TonalSpot ahead of a label with a t, an s and a p scattered through it.
 */
internal fun fuzzyScore(
    query: String,
    text: String,
): Int? {
    val wanted = query.filterNot { char -> char.isWhitespace() }.map { char -> char.lowercaseChar() }
    if (wanted.isEmpty()) return 0
    val length = text.length
    if (wanted.size > length) return null
    val folded = CharArray(length) { index -> text[index].lowercaseChar() }
    // best[j] is the best score with the letters so far matched and the last one at j.
    var best = IntArray(length) { index -> if (folded[index] == wanted[0]) letterScore(text, index) else NONE }
    for (letter in 1 until wanted.size) {
        val next = IntArray(length) { NONE }
        // The best of best[k] + GAP * k over every k at least two before j, so a gap costs GAP a letter.
        var carried = NONE
        for (j in 1 until length) {
            if (j >= 2 && best[j - 2] != NONE) carried = maxOf(carried, best[j - 2] + GAP * (j - 2))
            if (folded[j] != wanted[letter]) continue
            val afterGap = if (carried == NONE) NONE else carried - GAP * (j - 1)
            val adjacent = if (best[j - 1] == NONE) NONE else best[j - 1] + ADJACENT
            val before = maxOf(afterGap, adjacent)
            if (before != NONE) next[j] = before + letterScore(text, j)
        }
        best = next
    }
    return best.max().takeIf { score -> score != NONE }
}

/** What a letter matched at [index] of [text] scores, more at the start of a word. */
private fun letterScore(
    text: String,
    index: Int,
): Int {
    val char = text[index]
    val previous = text.getOrNull(index - 1)
    val wordStart = previous == null ||
        !previous.isLetterOrDigit() ||
        (previous.isLowerCase() && char.isUpperCase()) ||
        (previous.isLetter() && char.isDigit())
    return if (wordStart) LETTER + WORD_START else LETTER
}

private const val NONE = Int.MIN_VALUE
private const val LETTER = 16
private const val WORD_START = 24
private const val ADJACENT = 24
private const val GAP = 1
private const val LABEL_BONUS = 8
