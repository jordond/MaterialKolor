package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_collapse
import com.materialkolor.builder.generated.resources.poster_projects
import com.materialkolor.builder.generated.resources.poster_projects_not_saved
import com.materialkolor.builder.generated.resources.poster_projects_saved
import com.materialkolor.builder.generated.resources.poster_projects_saving
import com.materialkolor.builder.generated.resources.poster_save_failed
import com.materialkolor.builder.generated.resources.poster_wordmark
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.ButtonSize
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.BrandMark
import com.materialkolor.builder.kit.widget.MarkColors
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The top of the poster, the mark and wordmark, the Projects button with the open project's name and the
 * collapse button on one row. The 320 dp poster has no room for the wordmark as well, so there the
 * mark stands alone and carries the wordmark as its name. Whether the project is saved shows on the
 * Projects button, a turning glyph while it saves, a check once it is saved and a danger badge
 * beside it when a save did not land.
 *
 * The phone sheet has no rail to collapse to, so it shows no collapse button.
 *
 * @param[focus] Where the projects drawer hands focus back once it closes.
 */
@Composable
internal fun PosterHeader(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    focus: PosterFocus? = null,
) {
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val mode = LocalLayout.current.posterMode
    val collapsible = mode != PosterMode.Sheet
    // b-526 The 320 dp poster keeps one row by dropping the wordmark, never the Projects button.
    val markOnly = mode == PosterMode.Docked320
    val wordmark = stringResource(Res.string.poster_wordmark)
    Row(
        modifier = modifier.fillMaxWidth(),
        // b-522 A tight gap, so a name of about a dozen letters shows whole in the compact pill.
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The mark sits close to the wordmark, so the project's name keeps the room it had.
        Row(
            horizontalArrangement = Arrangement.spacedBy(HeaderMarkGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(
                colors = MarkColors.inked(ink = tokens.textStrong, page = tokens.canvas),
                modifier = if (markOnly) Modifier.semantics { contentDescription = wordmark } else Modifier,
                size = HeaderMarkSize,
            )
            if (!markOnly) {
                BuilderText(
                    text = wordmark,
                    style = BuilderTextStyle.Wordmark,
                    maxLines = 1,
                )
            }
        }
        // The project's name takes what the row has left, so a long one gives way first.
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            ProjectsButton(
                projectName = context.projectName,
                saveStatus = context.saveStatus,
                dispatcher = dispatcher,
                modifier = triggerFocus(focus?.projects),
            )
        }
        if (collapsible) {
            PosterIconButton(
                icon = IconId.Collapse,
                description = stringResource(Res.string.poster_collapse),
                onClick = { dispatcher.dispatch(WorkspaceAction.SetPosterCollapsed(collapsed = true)) },
            )
        }
    }
}

/**
 * The danger badge a failed save puts beside the Projects button, in words and a warning glyph so
 * it never rests on colour alone. A saved or saving project shows nothing here.
 */
@Composable
internal fun SaveState(status: SaveStatus) {
    if (status !is SaveStatus.Failed) return
    BuilderBadge(
        label = stringResource(Res.string.poster_save_failed),
        status = BadgeStatus.Danger,
        icon = IconId.Warning,
    )
}

/**
 * The Projects button. It shows the open project's name and reads out as Projects, the name and
 * whether it is saved, so it never sounds like a title. A glyph at the end of the pill turns while
 * the project saves and becomes a check once it is saved, and a long name ends in an ellipsis so the
 * glyph always shows. Before the session has named a project it says Projects and nothing more.
 */
@Composable
private fun ProjectsButton(
    projectName: String,
    saveStatus: SaveStatus,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val named = projectName.isNotBlank()
    val mark = saveMarkOf(saveStatus)
    val spoken = if (named) stringResource(mark.spoken, projectName) else null
    // b-522 The button gives way first, so a long name never pushes the badge out of the row.
    Row(
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val button = Modifier.weight(1f, fill = false).then(modifier)
        BuilderButton(
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Projects)) },
            label = if (named) projectName else stringResource(Res.string.poster_projects),
            modifier = if (spoken == null) button else button.semantics { contentDescription = spoken },
            emphasis = Emphasis.Subtle,
            icon = IconId.Folder,
            trailingIcon = mark.glyph.takeIf { named },
            size = ButtonSize.Compact,
            // b-526 Board E fills the pill with a quiet tint of the seed.
            tonal = true,
        )
        if (named) SaveState(saveStatus)
    }
}

/**
 * How the Projects button tells whether the open project is saved.
 *
 * @property[spoken] What the button reads out, Projects with the project's name and its save state.
 * @property[glyph] The glyph at the end of the button, a turning progress glyph while a save is under
 * way and a check once saved. None on a failed save, whose danger badge says it.
 */
@Immutable
internal data class SaveMark(
    val spoken: StringResource,
    val glyph: IconId?,
)

/**
 * How the Projects button shows [status].
 */
internal fun saveMarkOf(status: SaveStatus): SaveMark =
    when (status) {
        SaveStatus.Idle -> SaveMark(Res.string.poster_projects_saved, IconId.Check)
        SaveStatus.Pending -> SaveMark(Res.string.poster_projects_saving, IconId.Progress)
        is SaveStatus.Failed -> SaveMark(Res.string.poster_projects_not_saved, glyph = null)
    }

/**
 * An icon button under a tooltip, for the header and the rail. The tooltip repeats what it does
 * unless [tooltip] says more, such as why it is turned off.
 *
 * @param[modifier] Applied to the tooltip around the button.
 * @param[expanded] Whether the panel the button shows and hides is open, or null for a button that
 * discloses nothing.
 * @param[buttonModifier] Applied to the button itself, such as the requester a panel hands focus
 * back to.
 */
@Composable
internal fun PosterIconButton(
    icon: IconId,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Subtle,
    enabled: Boolean = true,
    tooltip: String = description,
    expanded: Boolean? = null,
    buttonModifier: Modifier = Modifier,
) {
    BuilderTooltip(text = tooltip, modifier = modifier) {
        BuilderIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = description,
            modifier = buttonModifier,
            emphasis = emphasis,
            enabled = enabled,
            expanded = expanded,
        )
    }
}

private val HeaderMarkSize = 18.dp
private val HeaderMarkGap = 2.dp
