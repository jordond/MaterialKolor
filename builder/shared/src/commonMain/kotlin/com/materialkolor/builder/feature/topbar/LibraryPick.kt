package com.materialkolor.builder.feature.topbar

import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument

/**
 * The one edit a pick of [choice] makes to [document], so a single undo takes all of it back.
 *
 * The style follows M3 Expressive. Entering it from any other choice sets the Expressive style and
 * moves a 2021 spec to 2025, unless the style is Expressive already, when only the library moves.
 * Leaving it while the style is still Expressive puts back the style and spec the document had right
 * before it last entered, read from [steps]. With no such step, a project opened on M3 Expressive or
 * a history trimmed past it, the style goes to Tonal spot and the spec stays. A style picked while on
 * M3 Expressive stays whatever the next pick is.
 *
 * @param[steps] The history steps that are applied, oldest first, the ones an undo would walk back.
 */
internal fun libraryPick(
    choice: LibraryChoice,
    document: ThemeDocument,
    steps: List<HistoryEntry>,
): DocumentChange {
    val from = LibraryChoice.of(document)
    val entering = choice == LibraryChoice.M3Expressive && from != LibraryChoice.M3Expressive
    val leaving = from == LibraryChoice.M3Expressive && choice != LibraryChoice.M3Expressive
    val onExpressiveStyle = document.style == Style.Expressive
    return when {
        entering && !onExpressiveStyle -> {
            DocumentChange.SetLibrary(
                library = choice.library,
                expressive = true,
                style = Style.Expressive,
                spec = SpecVersion.Spec2025.takeIf { document.spec == SpecVersion.Spec2021 },
            )
        }
        leaving && onExpressiveStyle -> {
            val before = entryBefore(steps)
            DocumentChange.SetLibrary(
                library = choice.library,
                expressive = false,
                style = before?.style ?: Style.TonalSpot,
                spec = before?.spec,
            )
        }
        else -> {
            DocumentChange.SetLibrary(choice.library, choice.expressive)
        }
    }
}

/**
 * The document right before the latest step in [steps] that moved it onto M3 Expressive, or null when
 * that step was no library pick, an opened link or an import, or no step did.
 */
private fun entryBefore(steps: List<HistoryEntry>): ThemeDocument? =
    steps
        .lastOrNull { step -> !step.before.onM3Expressive && step.after.onM3Expressive }
        ?.takeIf { step -> step.label.kind == ChangeKind.Library }
        ?.before

/**
 * Whether this document is on M3 Expressive.
 */
private val ThemeDocument.onM3Expressive: Boolean
    get() = LibraryChoice.of(this) == LibraryChoice.M3Expressive
