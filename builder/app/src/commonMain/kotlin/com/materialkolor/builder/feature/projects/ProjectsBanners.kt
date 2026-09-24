package com.materialkolor.builder.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.projects_conflict
import com.materialkolor.builder.generated.resources.projects_conflict_keep
import com.materialkolor.builder.generated.resources.projects_conflict_load
import com.materialkolor.builder.generated.resources.projects_newer_data
import com.materialkolor.builder.generated.resources.projects_storage_share
import com.materialkolor.builder.generated.resources.projects_storage_unavailable
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * Another tab saved the open project while this one was editing it. Load latest takes the other
 * tab's save as an undo step, and Keep mine keeps this tab's document (D36).
 */
@Composable
internal fun ConflictBanner(
    onResolve: (keepMine: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProjectBanner(
        icon = IconId.Warning,
        message = stringResource(Res.string.projects_conflict),
        modifier = modifier,
    ) {
        BuilderButton(
            onClick = { onResolve(false) },
            label = stringResource(Res.string.projects_conflict_load),
        )
        BuilderButton(
            onClick = { onResolve(true) },
            label = stringResource(Res.string.projects_conflict_keep),
            emphasis = Emphasis.Primary,
        )
    }
}

/**
 * Nothing saved here outlives the session, private browsing for example, so the banner points to
 * the share link as the way to keep a theme.
 */
@Composable
internal fun StorageUnavailableBanner(
    onGetLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProjectBanner(
        icon = IconId.Warning,
        message = stringResource(Res.string.projects_storage_unavailable),
        modifier = modifier,
    ) {
        BuilderButton(
            onClick = onGetLink,
            label = stringResource(Res.string.projects_storage_share),
            icon = IconId.Share,
        )
    }
}

/**
 * A newer version of the builder saved some of the data here, and this one leaves it alone until a
 * reload picks it up (D41). The platform has no way to reload from here, so the banner only says so.
 */
@Composable
internal fun NewerDataBanner(modifier: Modifier = Modifier) {
    ProjectBanner(
        icon = IconId.Info,
        message = stringResource(Res.string.projects_newer_data),
        modifier = modifier,
    )
}

/**
 * A card with an [icon] and a [message], and its [actions], when it has any, lined up at the end
 * below them.
 */
@Composable
internal fun ProjectBanner(
    icon: IconId,
    message: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val spacing = LocalBuilderTokens.current.spacing
    BuilderCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderIcon(icon, contentDescription = null)
                BuilderText(text = message, modifier = Modifier.weight(1f))
            }
            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
