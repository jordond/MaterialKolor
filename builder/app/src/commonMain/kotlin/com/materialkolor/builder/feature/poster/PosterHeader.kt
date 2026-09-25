package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 * The top of the poster, the wordmark, the Projects button with the open project's name and the
 * collapse button on one row. The 320 dp poster has no room for all three, so there the Projects
 * button takes a row of its own. Whether the project is saved shows under the hex, see [SeedHero].
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
    val spacing = LocalBuilderTokens.current.spacing
    val mode = LocalLayout.current.posterMode
    val collapsible = mode != PosterMode.Sheet
    val projects: @Composable (Modifier) -> Unit = { projectsModifier ->
        ProjectsButton(
            projectName = context.projectName,
            dispatcher = dispatcher,
            modifier = projectsModifier.then(triggerFocus(focus?.projects)),
        )
    }
    val narrow = mode == PosterMode.Docked320
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderText(
                text = stringResource(Res.string.poster_wordmark),
                modifier = if (narrow) Modifier.weight(1f) else Modifier,
                style = BuilderTextStyle.Wordmark,
                maxLines = 1,
            )
            if (!narrow) {
                // The project's name takes what the row has left, so a long one gives way first.
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { projects(Modifier) }
            }
            if (collapsible) {
                PosterIconButton(
                    icon = IconId.Collapse,
                    description = stringResource(Res.string.poster_collapse),
                    onClick = { dispatcher.dispatch(WorkspaceAction.SetPosterCollapsed(collapsed = true)) },
                )
            }
        }
        if (narrow) projects(Modifier)
    }
}

/**
 * Whether the open project is saved, as small words. Saved and saving read in the muted ink, and a
 * failed save keeps its badge and warning glyph so it never rests on colour alone.
 */
@Composable
internal fun SaveState(status: SaveStatus) {
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
 * it never sounds like a title. Before the session has named a project it says Projects.
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
        emphasis = Emphasis.Subtle,
        icon = IconId.Folder,
    )
}

/**
 * What the header's badge says about a save, in words and a glyph so it never rests on color alone.
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

/**
 * The badge the header shows for [status].
 */
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
