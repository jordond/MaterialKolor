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
import com.materialkolor.builder.generated.resources.banners_close
import com.materialkolor.builder.generated.resources.banners_dismiss
import com.materialkolor.builder.generated.resources.banners_reload
import com.materialkolor.builder.generated.resources.projects_conflict
import com.materialkolor.builder.generated.resources.projects_conflict_keep
import com.materialkolor.builder.generated.resources.projects_conflict_load
import com.materialkolor.builder.generated.resources.projects_newer_data
import com.materialkolor.builder.generated.resources.projects_storage_share
import com.materialkolor.builder.generated.resources.projects_storage_unavailable
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderIconButton
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
 *
 * @param[onClose] Puts the banner away, or null where it stays, as it does in the drawer.
 */
@Composable
internal fun StorageUnavailableBanner(
    onGetLink: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null, // b-314b
) {
    ProjectBanner(
        icon = IconId.Warning,
        message = stringResource(Res.string.projects_storage_unavailable),
        modifier = modifier,
        onClose = onClose, // b-314b
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
 * reload picks it up (D41). Reload loads the builder again at `/`.
 *
 * @param[onReload] What Reload does, or null where a reload does nothing and the button stays out.
 * @param[onDismiss] Puts the banner away from a Dismiss button, or null for a banner without one.
 */
@Composable
internal fun NewerDataBanner(
    modifier: Modifier = Modifier,
    onReload: (() -> Unit)? = null, // b-314b
    onDismiss: (() -> Unit)? = null, // b-314ba
) {
    ProjectBanner(
        icon = IconId.Info,
        message = stringResource(Res.string.projects_newer_data),
        modifier = modifier,
    ) {
        // b-314ba
        if (onDismiss != null) {
            BuilderButton(
                onClick = onDismiss,
                label = stringResource(Res.string.banners_dismiss),
                emphasis = Emphasis.Subtle,
            )
        }
        // b-314b
        if (onReload != null) {
            BuilderButton(
                onClick = onReload,
                label = stringResource(Res.string.banners_reload),
                emphasis = Emphasis.Primary,
            )
        }
    }
}

/**
 * A card with an [icon] and a [message], and its [actions], when it has any, lined up at the end
 * below them.
 *
 * @param[onClose] Puts the banner away from a close button at the end of the message, or null for
 * a banner without one.
 */
@Composable
internal fun ProjectBanner(
    icon: IconId,
    message: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null, // b-314b
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
                // b-314b
                if (onClose != null) {
                    BuilderIconButton(
                        onClick = onClose,
                        icon = IconId.Close,
                        contentDescription = stringResource(Res.string.banners_close),
                    )
                }
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
