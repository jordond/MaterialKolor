package com.materialkolor.builder.feature.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.ProjectMeta
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.projects_copy_name
import com.materialkolor.builder.generated.resources.projects_delete
import com.materialkolor.builder.generated.resources.projects_duplicate
import com.materialkolor.builder.generated.resources.projects_more
import com.materialkolor.builder.generated.resources.projects_name
import com.materialkolor.builder.generated.resources.projects_name_empty
import com.materialkolor.builder.generated.resources.projects_rename
import com.materialkolor.builder.generated.resources.projects_target_custom
import com.materialkolor.builder.generated.resources.projects_target_expressive
import com.materialkolor.builder.generated.resources.projects_target_fluent
import com.materialkolor.builder.generated.resources.projects_target_m3
import com.materialkolor.builder.generated.resources.projects_target_unstyled
import com.materialkolor.builder.generated.resources.projects_updated_days
import com.materialkolor.builder.generated.resources.projects_updated_hours
import com.materialkolor.builder.generated.resources.projects_updated_minutes
import com.materialkolor.builder.generated.resources.projects_updated_months
import com.materialkolor.builder.generated.resources.projects_updated_now
import com.materialkolor.builder.generated.resources.projects_updated_weeks
import com.materialkolor.builder.generated.resources.projects_updated_years
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChip
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One saved project in the drawer, its thumbnail, name, age and target, and a menu to rename,
 * duplicate or delete it.
 *
 * The thumbnail is the row's radio, named by the project, so a screen reader hears the list as one
 * choice with the open project chosen. The name opens the project too for a pointer, without a
 * second stop for the keyboard. Rename swaps the name for a field, which commits on Enter or when
 * it loses focus and gives up on Esc.
 *
 * @param[meta] The project.
 * @param[open] Whether it is the open project.
 * @param[now] The time now, in milliseconds since the epoch.
 * @param[renaming] Whether the name is being edited.
 * @param[onRenamingChange] Called to start or stop editing the name.
 * @param[onAction] Called with what the row asks for.
 */
@Composable
internal fun ProjectRow(
    meta: ProjectMeta,
    open: Boolean,
    now: Long,
    renaming: Boolean,
    onRenamingChange: (Boolean) -> Unit,
    onAction: (ProjectsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val openThis = { onAction(ProjectsAction.Open(meta.id)) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SchemeChip(
            primary = meta.previewColors[0].toColor(),
            secondaryContainer = meta.previewColors[1].toColor(),
            tertiaryContainer = meta.previewColors[2].toColor(),
            selected = open,
            onClick = openThis,
            label = meta.name,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            if (renaming) {
                RenameField(meta, onRenamingChange, onAction)
            } else {
                BuilderText(
                    text = meta.name,
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .focusProperties { canFocus = false }
                        .clickable(onClick = openThis),
                    style = BuilderTextStyle.Label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderText(
                    text = ageText(ProjectAge.of(meta.updatedAt, now)),
                    emphasis = Emphasis.Subtle,
                    maxLines = 1,
                )
                BuilderBadge(label = targetLabel(ExportTarget.of(meta.library, meta.expressive)))
            }
        }
        RowMenu(meta, onRenamingChange, onAction)
    }
}

@Composable
private fun RenameField(
    meta: ProjectMeta,
    onRenamingChange: (Boolean) -> Unit,
    onAction: (ProjectsAction) -> Unit,
) {
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val emptyMessage = stringResource(Res.string.projects_name_empty)
    BuilderTextField(
        value = meta.name,
        onCommit = { name ->
            onAction(ProjectsAction.Rename(meta.id, name))
            onRenamingChange(false)
        },
        label = stringResource(Res.string.projects_name),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .onFocusChanged { state ->
                if (focused && !state.hasFocus) onRenamingChange(false)
                focused = state.hasFocus
            },
        error = { draft -> if (draft.isBlank()) emptyMessage else null },
    )
    LaunchedEffect(focus) { focus.requestFocus() }
}

@Composable
private fun RowMenu(
    meta: ProjectMeta,
    onRenamingChange: (Boolean) -> Unit,
    onAction: (ProjectsAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val copyName = stringResource(Res.string.projects_copy_name, meta.name)

    // Every item closes the menu before it acts.
    fun item(
        label: String,
        icon: IconId? = null,
        emphasis: Emphasis = Emphasis.Primary,
        act: () -> Unit,
    ): BuilderMenuItem =
        BuilderMenuItem(
            label = label,
            onClick = {
                expanded = false
                act()
            },
            icon = icon,
            emphasis = emphasis,
        )

    BuilderMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        items = listOf(
            item(stringResource(Res.string.projects_rename)) { onRenamingChange(true) },
            item(stringResource(Res.string.projects_duplicate), IconId.Copy) {
                onAction(ProjectsAction.Duplicate(meta.id, copyName.ifBlank { meta.name }))
            },
            item(stringResource(Res.string.projects_delete), IconId.Trash, Emphasis.Danger) {
                onAction(ProjectsAction.Delete(meta.id))
            },
        ),
    ) {
        BuilderIconButton(
            onClick = { expanded = true },
            icon = IconId.More,
            contentDescription = stringResource(Res.string.projects_more, meta.name),
        )
    }
}

@Composable
private fun ageText(age: ProjectAge): String =
    when (age) {
        ProjectAge.JustNow -> stringResource(Res.string.projects_updated_now)
        is ProjectAge.Minutes -> pluralStringResource(Res.plurals.projects_updated_minutes, age.count, age.count)
        is ProjectAge.Hours -> pluralStringResource(Res.plurals.projects_updated_hours, age.count, age.count)
        is ProjectAge.Days -> pluralStringResource(Res.plurals.projects_updated_days, age.count, age.count)
        is ProjectAge.Weeks -> pluralStringResource(Res.plurals.projects_updated_weeks, age.count, age.count)
        is ProjectAge.Months -> pluralStringResource(Res.plurals.projects_updated_months, age.count, age.count)
        is ProjectAge.Years -> pluralStringResource(Res.plurals.projects_updated_years, age.count, age.count)
    }

@Composable
private fun targetLabel(target: ExportTarget): String =
    when (target) {
        ExportTarget.Material3 -> stringResource(Res.string.projects_target_m3)
        ExportTarget.Material3Expressive -> stringResource(Res.string.projects_target_expressive)
        ExportTarget.Unstyled -> stringResource(Res.string.projects_target_unstyled)
        ExportTarget.Fluent -> stringResource(Res.string.projects_target_fluent)
        ExportTarget.Custom -> stringResource(Res.string.projects_target_custom)
    }
