package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_collapse
import com.materialkolor.builder.generated.resources.poster_projects
import com.materialkolor.builder.generated.resources.poster_save_failed
import com.materialkolor.builder.generated.resources.poster_saved
import com.materialkolor.builder.generated.resources.poster_saving
import com.materialkolor.builder.generated.resources.poster_wordmark
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
 * The top of the poster, the wordmark, the collapse button and the Projects button with the open
 * project's name and whether it is saved.
 *
 * The phone sheet has no rail to collapse to, so it shows no collapse button (D38).
 */
@Composable
internal fun PosterHeader(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val collapsible = LocalLayout.current.posterMode != PosterMode.Sheet
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BuilderText(
                text = stringResource(Res.string.poster_wordmark),
                modifier = Modifier.weight(1f),
                style = BuilderTextStyle.Wordmark,
                maxLines = 1,
            )
            if (collapsible) {
                PosterIconButton(
                    icon = IconId.Collapse,
                    description = stringResource(Res.string.poster_collapse),
                    onClick = { dispatcher.dispatch(WorkspaceAction.SetPosterCollapsed(collapsed = true)) },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Projects)) },
                label = context.projectName.ifBlank { stringResource(Res.string.poster_projects) },
                modifier = Modifier.weight(1f, fill = false),
                icon = IconId.Folder,
            )
            BuilderText(
                text = stringResource(saveStatusLabel(context.saveStatus)),
                style = BuilderTextStyle.Label,
                emphasis = Emphasis.Secondary,
                maxLines = 1,
            )
        }
    }
}

/** What the header says about [status]. */
internal fun saveStatusLabel(status: SaveStatus): StringResource =
    when (status) {
        SaveStatus.Idle -> Res.string.poster_saved
        SaveStatus.Pending -> Res.string.poster_saving
        is SaveStatus.Failed -> Res.string.poster_save_failed
    }

/** An icon button under a tooltip that repeats what it does, for the header and the rail. */
@Composable
internal fun PosterIconButton(
    icon: IconId,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BuilderTooltip(text = description, modifier = modifier) {
        BuilderIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = description,
            enabled = enabled,
        )
    }
}
