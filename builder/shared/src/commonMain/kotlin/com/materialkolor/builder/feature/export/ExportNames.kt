package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.codegen.validate.ReservedNameClash
import com.materialkolor.builder.codegen.validate.ReservedNames
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.validate.validatePackageName
import com.materialkolor.builder.domain.validate.validateThemeName
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_names_note
import com.materialkolor.builder.generated.resources.export_package
import com.materialkolor.builder.generated.resources.export_package_invalid
import com.materialkolor.builder.generated.resources.export_theme_name
import com.materialkolor.builder.generated.resources.export_theme_name_invalid
import com.materialkolor.builder.generated.resources.export_theme_name_taken
import com.materialkolor.builder.kit.control.BuilderInlineField
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * What is wrong with the package and theme name drafts right now, for the sheet to hold the export
 * back on. A draft that is fine is in the export already, so only a wrong one is kept.
 */
@Stable
internal class DraftProblems {
    /**
     * What is wrong with the package draft, or null when nothing is.
     */
    var packageName: ExportProblem? by mutableStateOf(null)

    /**
     * What is wrong with the theme name draft, or null when nothing is.
     */
    var themeName: ExportProblem? by mutableStateOf(null)

    /**
     * Every problem a draft has, the package first.
     */
    val all: List<ExportProblem>
        get() = listOfNotNull(packageName, themeName)
}

/**
 * How a name field draws, boxed with its label over it, or inline in the header's line of text.
 */
internal sealed interface FieldLook {
    /**
     * The skin's text field, its label over it and what is wrong with a draft under it.
     */
    data object Boxed : FieldLook

    /**
     * Set in [style] and [color] inside a line of text, as the header draws the names. What is
     * wrong with a draft shows in the notices over the files.
     */
    class Inline(
        val style: TextStyle,
        val color: Color = Color.Unspecified,
    ) : FieldLook
}

/**
 * The package and the theme name side by side, with one note under both on where each is kept, for
 * the Options a phone folds them into. Wider sheets set them inline in the header instead.
 *
 * Both go out as they are typed, so the export is always built from what the fields show. A draft
 * that is not valid stays in its field, says what is wrong under it and lands in [drafts], which
 * holds the export back. The theme name goes to the document, so it travels with the project and
 * its share link, while the package stays in this browser under the target.
 *
 * @param[drafts] Where the two fields say what is wrong with their drafts.
 */
@Composable
internal fun ExportNames(
    state: ExportModel.State,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
    drafts: DraftProblems,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        NamePair(
            first = { fieldModifier -> PackageField(state, dispatcher, drafts, FieldLook.Boxed, fieldModifier) },
            second = { fieldModifier -> ThemeNameField(state, workspace, drafts, FieldLook.Boxed, fieldModifier) },
        )
        BuilderText(text = stringResource(Res.string.export_names_note), emphasis = Emphasis.Secondary)
    }
}

/**
 * Two fields side by side, each half the width, or one over the other where the strip is narrower
 * than [NAME_PAIR_MIN_WIDTH]. Each slot is handed the modifier that sizes it.
 */
@Composable
private fun NamePair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= NAME_PAIR_MIN_WIDTH) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * The narrowest the strip gets while the package and the theme name still share a row.
 */
private val NAME_PAIR_MIN_WIDTH = 480.dp

/**
 * The package the target's export goes in, which stays in this browser under the target.
 *
 * @param[drafts] Where the field says what is wrong with its draft.
 */
@Composable
internal fun PackageField(
    state: ExportModel.State,
    dispatcher: Dispatcher<ExportAction>,
    drafts: DraftProblems,
    look: FieldLook,
    modifier: Modifier = Modifier,
) {
    val invalid = stringResource(Res.string.export_package_invalid)
    // Each target keeps its own package, so a switch starts the field over on the new one's.
    key(state.target) {
        LiveField(
            value = state.prefs.packageName,
            onChange = { name -> dispatcher.dispatch(ExportAction.SetPackageName(name)) },
            onProblem = { problem -> drafts.packageName = problem },
            label = stringResource(Res.string.export_package),
            problemOf = { draft ->
                if (validatePackageName(draft).isEmpty()) null else ExportProblem.PackageName(draft)
            },
            errorOf = { invalid },
            look = look,
            modifier = modifier,
        )
    }
}

/**
 * The theme name, checked against Kotlin and against the names the target's export already uses.
 *
 * @param[drafts] Where the field says what is wrong with its draft.
 */
@Composable
internal fun ThemeNameField(
    state: ExportModel.State,
    workspace: Dispatcher<WorkspaceAction>,
    drafts: DraftProblems,
    look: FieldLook,
    modifier: Modifier = Modifier,
) {
    val invalid = stringResource(Res.string.export_theme_name_invalid)
    val taken = stringResource(Res.string.export_theme_name_taken)
    val targeted = state.document.forTarget(state.target)
    LiveField(
        value = state.document.themeName,
        onChange = { name ->
            workspace.dispatch(WorkspaceAction.Edit(DocumentChange.SetThemeName(name), EditPhase.Discrete))
        },
        onProblem = { problem -> drafts.themeName = problem },
        label = stringResource(Res.string.export_theme_name),
        problemOf = { draft ->
            when {
                validateThemeName(draft).isNotEmpty() -> ExportProblem.ThemeName(draft)
                targeted.takesReservedName(draft) -> ExportProblem.NameTaken(draft)
                else -> null
            }
        },
        errorOf = { problem -> if (problem is ExportProblem.NameTaken) taken else invalid },
        look = look,
        modifier = modifier,
    )
}

/**
 * A text field that hands over every valid draft as it is typed. A draft [problemOf] finds fault
 * with stays in the field and goes to [onProblem], and Esc puts the committed text back and clears
 * it. Theme name edits that close together fold into one undo step, so typing is not an undo step
 * per key.
 *
 * While drafts are going out the field keeps the text it started from as its committed value, so
 * the drafts coming back as [value] never move the cursor or undo a newer keystroke. Enter settles
 * it on the text typed. Only once the field is left on a valid draft does it follow [value] again,
 * since an echo can come back late, after Esc or after typing back to the start, and a stale
 * [value] that happens to match the field says nothing about the echoes still on their way.
 *
 * A problem only lasts as long as the draft behind it. A new [value] that arrives while nothing is
 * going out replaces the draft, and the field leaving composition takes its draft along, so both
 * clear it. Otherwise a collapsed Options would hold the export back under a notice the field no
 * longer shows.
 */
@Composable
private fun LiveField(
    value: String,
    onChange: (String) -> Unit,
    onProblem: (ExportProblem?) -> Unit,
    label: String,
    problemOf: (String) -> ExportProblem?,
    errorOf: (ExportProblem) -> String,
    look: FieldLook,
    modifier: Modifier = Modifier,
) {
    // The committed text the field keeps while its drafts are out, or null while it follows value.
    var held by remember { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }
    var draftInvalid by remember { mutableStateOf(false) }
    var lastValue by remember { mutableStateOf(value) }
    val currentOnProblem by rememberUpdatedState(onProblem)
    DisposableEffect(Unit) {
        onDispose { currentOnProblem(null) }
    }
    SideEffect {
        // Nothing is going out, so this value is not a draft coming back and the field shows it now.
        if (held == null && value != lastValue) onProblem(null)
        lastValue = value
        // Left on a valid draft, the field has sent everything it holds, so it follows value again.
        if (held != null && !focused && !draftInvalid) held = null
    }
    // The drafts went out as they were typed, so a commit only has to settle on the last one.
    val onCommit = { text: String ->
        if (held == null) onChange(text) else held = text
    }
    val error = { draft: String -> problemOf(draft)?.let(errorOf) }
    val onDraftChange = { draft: String ->
        val problem = problemOf(draft)
        onProblem(problem)
        draftInvalid = problem != null
        if (problem == null) {
            held = held ?: value
            onChange(draft)
        }
    }
    val tracked = modifier.onFocusChanged { state -> focused = state.hasFocus }
    when (look) {
        FieldLook.Boxed -> {
            BuilderTextField(
                value = held ?: value,
                onCommit = onCommit,
                label = label,
                modifier = tracked.fillMaxWidth(),
                error = error,
                onDraftChange = onDraftChange,
            )
        }
        is FieldLook.Inline -> {
            BuilderInlineField(
                value = held ?: value,
                onCommit = onCommit,
                label = label,
                style = look.style,
                modifier = tracked,
                color = look.color,
                error = error,
                onDraftChange = onDraftChange,
            )
        }
    }
}

/**
 * Whether the export of this document would clash with a name it already uses, were the theme called [name].
 */
private fun ThemeDocument.takesReservedName(name: String): Boolean =
    ReservedNames.clashes(copy(themeName = name)).any { clash -> clash is ReservedNameClash.ThemeName }
