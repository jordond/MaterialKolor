package com.materialkolor.builder.feature.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.projects.ProjectBanner
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.share_transient
import com.materialkolor.builder.generated.resources.share_transient_save
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import org.jetbrains.compose.resources.stringResource

/**
 * The banner over a theme opened from a link, until it is saved. The first edit saves it on its own,
 * and Save to my projects saves it straight away (architecture 4.2).
 */
@Composable
internal fun SharedLinkBanner(
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProjectBanner(
        icon = IconId.Info,
        message = stringResource(Res.string.share_transient),
        modifier = modifier,
    ) {
        BuilderButton(
            onClick = onSave,
            label = stringResource(Res.string.share_transient_save),
            emphasis = Emphasis.Primary,
            icon = IconId.Folder,
        )
    }
}
