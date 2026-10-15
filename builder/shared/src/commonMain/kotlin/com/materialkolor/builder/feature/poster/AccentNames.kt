package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.materialkolor.builder.codegen.validate.ReservedNameClash
import com.materialkolor.builder.codegen.validate.ReservedNames
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.validate.MAX_ACCENT_NAME_BYTES
import com.materialkolor.builder.domain.validate.ValidationError
import com.materialkolor.builder.domain.validate.validateAccents
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.accents_name_case_clash
import com.materialkolor.builder.generated.resources.accents_name_duplicate
import com.materialkolor.builder.generated.resources.accents_name_invalid
import com.materialkolor.builder.generated.resources.accents_name_keyword
import com.materialkolor.builder.generated.resources.accents_name_role
import com.materialkolor.builder.generated.resources.accents_name_taken
import com.materialkolor.builder.generated.resources.accents_name_too_long
import com.materialkolor.hct.Hct
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How far round the hue circle from the seed each place down the list starts a new extra color, in degrees.
 */
private const val HUE_TURN = 40.0

private const val FULL_TURN = 360.0

/**
 * The extra color Add appends to [document]. It takes the first `accentN` that is free and valid
 * there, and its seed turns [HUE_TURN] degrees further round the hue circle from the theme seed for
 * each place down the list, keeping the seed's chroma and tone.
 */
internal fun newAccent(document: ThemeDocument): Accent {
    val place = document.accents.size
    val name = generateSequence(1) { number -> number + 1 }
        .map { number -> "accent$number" }
        .first { candidate -> accentNameProblems(document, place, candidate).isEmpty() }
    val seed = Hct.fromInt(document.seed.value)
    val hue = (seed.hue + HUE_TURN * (place + 1)) % FULL_TURN
    return Accent(name = name, seed = Argb(Hct.from(hue, seed.chroma, seed.tone).toInt()))
}

/**
 * Something that stops a name from naming an extra color.
 */
internal enum class AccentNameProblem {
    /**
     * It is not a Kotlin name.
     */
    Invalid,

    /**
     * Kotlin keeps it for itself.
     */
    Keyword,

    /**
     * It is longer than an export takes.
     */
    TooLong,

    /**
     * Another extra color has it.
     */
    Duplicate,

    /**
     * Another extra color has it with other capitals, which the export would write the same.
     */
    CaseClash,

    /**
     * A role of the scheme has it.
     */
    Role,

    /**
     * The export's own files use it for something else.
     */
    Taken,
}

/**
 * Everything that stops [name] from naming the extra color at [index] of [document], one problem per
 * error, the way the export checks them. An [index] just past the end is a color about to be added.
 *
 * The list it checks moves that color to the end, so a clash with any other color, before or after
 * it, lands on this one and never on a color nobody touched.
 */
internal fun accentNameProblems(
    document: ThemeDocument,
    index: Int,
    name: String,
): List<AccentNameProblem> {
    val others = document.accents.filterIndexed { at, _ -> at != index }
    val renamed = document.accents.getOrNull(index)?.copy(name = name) ?: Accent(name = name, seed = document.seed)
    val candidates = others + renamed
    val place = others.size
    val invalid = validateAccents(candidates).mapNotNull { error -> error.problemAt(place) }
    val taken = ReservedNames.clashes(document.copy(accents = candidates)).any { clash ->
        clash is ReservedNameClash.AccentName && clash.index == place
    }
    return (if (taken) invalid + AccentNameProblem.Taken else invalid).distinct()
}

/**
 * The problem this error is for the extra color at [index], or null when it is about something else.
 */
private fun ValidationError.problemAt(index: Int): AccentNameProblem? =
    when (this) {
        is ValidationError.AccentNameInvalid -> AccentNameProblem.Invalid.takeIf { this.index == index }
        is ValidationError.AccentNameKeyword -> AccentNameProblem.Keyword.takeIf { this.index == index }
        is ValidationError.AccentNameTooLong -> AccentNameProblem.TooLong.takeIf { this.index == index }
        is ValidationError.AccentNameDuplicate -> AccentNameProblem.Duplicate.takeIf { this.index == index }
        is ValidationError.AccentNameCaseClash -> AccentNameProblem.CaseClash.takeIf { this.index == index }
        is ValidationError.AccentNameRole -> AccentNameProblem.Role.takeIf { this.index == index }
        // The list as a whole, or nothing to do with extra colors.
        is ValidationError.TooManyAccents,
        is ValidationError.PackageSegmentInvalid,
        is ValidationError.PackageSegmentKeyword,
        is ValidationError.ThemeNameInvalid,
        is ValidationError.ThemeNameKeyword,
        is ValidationError.ProjectNameTooLong,
        -> null
    }

/**
 * What a name field says about each [AccentNameProblem], resolved once so the field can ask outside
 * composition.
 */
@Immutable
internal class AccentNameMessages(
    private val messages: Map<AccentNameProblem, String>,
) {
    /**
     * One message for each of [problems], in their order, or null when there are none.
     */
    fun messageFor(problems: List<AccentNameProblem>): String? =
        problems.takeIf { found -> found.isNotEmpty() }?.joinToString(separator = " ") { problem ->
            messages.getValue(problem)
        }
}

/**
 * What a name field says about each problem a name can have.
 */
@Composable
internal fun rememberAccentNameMessages(): AccentNameMessages {
    val messages = AccentNameProblem.entries.associateWith { problem ->
        if (problem == AccentNameProblem.TooLong) {
            stringResource(problemText(problem), MAX_ACCENT_NAME_BYTES)
        } else {
            stringResource(problemText(problem))
        }
    }
    return remember(messages) { AccentNameMessages(messages) }
}

/**
 * The words for [problem]. Only [AccentNameProblem.TooLong] has an argument, the most bytes a name takes.
 */
private fun problemText(problem: AccentNameProblem): StringResource =
    when (problem) {
        AccentNameProblem.Invalid -> Res.string.accents_name_invalid
        AccentNameProblem.Keyword -> Res.string.accents_name_keyword
        AccentNameProblem.TooLong -> Res.string.accents_name_too_long
        AccentNameProblem.Duplicate -> Res.string.accents_name_duplicate
        AccentNameProblem.CaseClash -> Res.string.accents_name_case_clash
        AccentNameProblem.Role -> Res.string.accents_name_role
        AccentNameProblem.Taken -> Res.string.accents_name_taken
    }
