package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.topbar_expressive_apply
import com.materialkolor.builder.generated.resources.topbar_expressive_keep
import com.materialkolor.builder.generated.resources.topbar_expressive_message
import com.materialkolor.builder.generated.resources.topbar_expressive_title
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import org.jetbrains.compose.resources.stringResource

/**
 * Offers the Expressive style on the 2025 spec after a switch to Expressive left the document on
 * something else (F-03).
 *
 * Nothing changes until someone presses Apply. Keep mine, Esc and a click on the veil all leave
 * the document as it is.
 *
 * @param[visible] Whether the suggestion is up.
 * @param[onApply] Called for Apply. The caller makes [expressiveStyleChange] and closes it.
 * @param[onKeepMine] Called for Keep mine and for any other way of closing it.
 * @param[returnFocusTo] The switcher, where focus goes once it closes.
 */
@Composable
internal fun ExpressiveSuggestion(
    visible: Boolean,
    onApply: () -> Unit,
    onKeepMine: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    BuilderDialog(
        visible = visible,
        onDismissRequest = onKeepMine,
        title = stringResource(Res.string.topbar_expressive_title),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        actions = {
            BuilderButton(
                onClick = onKeepMine,
                label = stringResource(Res.string.topbar_expressive_keep),
                emphasis = Emphasis.Secondary,
            )
            BuilderButton(
                onClick = onApply,
                label = stringResource(Res.string.topbar_expressive_apply),
                emphasis = Emphasis.Primary,
            )
        },
    ) {
        BuilderText(stringResource(Res.string.topbar_expressive_message))
    }
}

/**
 * Whether [change], taking the document from [before] to [after], is a switch onto Expressive that
 * should offer the Expressive style (F-03). Only a library switch raises it, never an undo, an
 * import or a shuffle.
 */
internal fun raisesExpressiveSuggestion(
    change: DocumentChange,
    before: ThemeDocument,
    after: ThemeDocument,
): Boolean =
    change is DocumentChange.SetLibrary &&
        LibraryChoice.of(before) != LibraryChoice.Expressive &&
        LibraryChoice.of(after) == LibraryChoice.Expressive &&
        suggestsExpressiveStyle(after)

/**
 * The one edit Apply makes, the Expressive style on the 2025 spec, so a single undo takes both
 * back.
 */
internal fun expressiveStyleChange(document: ThemeDocument): DocumentChange =
    DocumentChange.Replace(document.copy(style = Style.Expressive, spec = SpecVersion.Spec2025))
