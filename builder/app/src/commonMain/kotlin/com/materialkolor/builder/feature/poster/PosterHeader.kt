package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_collapse
import com.materialkolor.builder.generated.resources.poster_projects
import com.materialkolor.builder.generated.resources.poster_projects_named
import com.materialkolor.builder.generated.resources.poster_save_failed
import com.materialkolor.builder.generated.resources.poster_saved
import com.materialkolor.builder.generated.resources.poster_saving
import com.materialkolor.builder.generated.resources.poster_wordmark
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The top of the poster on one row, the wordmark, the Projects button with the open project's name,
 * whether it is saved, and the collapse button.
 *
 * The phone sheet has no rail to collapse to, so it shows no collapse button (D38).
 *
 * @param[focus] Where the projects drawer hands focus back once it closes (AR-09).
 */
@Composable
internal fun PosterHeader(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    focus: PosterFocus? = null, // b-221f
) {
    val spacing = LocalBuilderTokens.current.spacing
    val collapsible = LocalLayout.current.posterMode != PosterMode.Sheet
    // b-510
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(
            text = stringResource(Res.string.poster_wordmark),
            style = BuilderTextStyle.Wordmark,
            maxLines = 1,
        )
        // The project's name takes what the row has left, so a long one gives way before the rest.
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProjectsButton(
                projectName = context.projectName,
                dispatcher = dispatcher,
                modifier = Modifier.weight(1f, fill = false).then(triggerFocus(focus?.projects)), // b-221f
            )
            SaveState(context.saveStatus)
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

// b-510

/**
 * Whether the open project is saved, as small words beside the Projects button. Saved and saving
 * read in the muted ink, and a failed save keeps its warning glyph so it never rests on the words'
 * colour alone (AR-03).
 */
@Composable
private fun SaveState(status: SaveStatus) {
    val badge = saveBadgeOf(status)
    val label = stringResource(badge.label)
    if (badge.status == BadgeStatus.Danger) {
        BuilderBadge(label = label, status = badge.status, icon = badge.icon)
    } else {
        BuilderText(text = label, style = BuilderTextStyle.Label, emphasis = Emphasis.Secondary, maxLines = 1)
    }
}

/**
 * The Projects button. It shows the open project's name and reads out as Projects and the name, so
 * it never sounds like a title. Before the session has named a project it just says Projects.
 */
@Composable
private fun ProjectsButton(
    projectName: String,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val named = projectName.isNotBlank()
    val spoken = if (named) stringResource(Res.string.poster_projects_named, projectName) else null
    BuilderButton(
        onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Projects)) },
        label = if (named) projectName else stringResource(Res.string.poster_projects),
        modifier = if (spoken == null) modifier else modifier.semantics { contentDescription = spoken },
        emphasis = Emphasis.Subtle, // b-510
        icon = IconId.Folder,
    )
}

/**
 * What the header's badge says about a save, in words and a glyph so it never rests on color
 * alone (AR-03).
 *
 * @property[label] The status in words.
 * @property[status] What the badge reports, which picks its color.
 * @property[icon] The glyph before the words, none while a save is under way.
 */
@Immutable
internal data class SaveBadge(
    val label: StringResource,
    val status: BadgeStatus,
    val icon: IconId?,
)

/** The badge the header shows for [status]. */
internal fun saveBadgeOf(status: SaveStatus): SaveBadge =
    when (status) {
        SaveStatus.Idle -> SaveBadge(Res.string.poster_saved, BadgeStatus.Success, IconId.Check)
        SaveStatus.Pending -> SaveBadge(Res.string.poster_saving, BadgeStatus.Neutral, icon = null)
        is SaveStatus.Failed -> SaveBadge(Res.string.poster_save_failed, BadgeStatus.Danger, IconId.Warning)
    }

/**
 * An icon button under a tooltip, for the header and the rail. The tooltip repeats what it does
 * unless [tooltip] says more, such as why it is turned off.
 *
 * @param[modifier] Applied to the tooltip around the button.
 * @param[expanded] Whether the panel the button shows and hides is open, or null for a button that
 * discloses nothing (D37).
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
    // b-221f
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
