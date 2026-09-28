package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_copied
import com.materialkolor.builder.generated.resources.export_download
import com.materialkolor.builder.generated.resources.export_save_failed
import com.materialkolor.builder.generated.resources.export_share
import com.materialkolor.builder.generated.resources.export_share_failed
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * The narrow footer's buttons. Copy all runs across the width, over Copy file and the zip button,
 * which share the last row half and half as the board for a phone pairs them.
 *
 * @param[copyAll] Copy all, given the modifier that places it.
 * @param[copyFile] Copy file, given the modifier that places it.
 * @param[zip] Download zip or Share files, given the modifier that places it.
 */
@Composable
internal fun NarrowFooterButtons(
    copyAll: @Composable (place: Modifier) -> Unit,
    copyFile: @Composable (place: Modifier) -> Unit,
    zip: @Composable (place: Modifier) -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        copyAll(Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            copyFile(Modifier.weight(1f))
            zip(Modifier.weight(1f))
        }
    }
}

/**
 * A copy button that shows Copied, with a check, while [copied].
 *
 * @param[tonal] Whether it carries its own fill.
 * @param[hint] The key that does the same, drawn as a keycap, or null for none.
 */
@Composable
internal fun CopyButton(
    copied: Boolean,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    hint: String? = null,
) {
    BuilderButton(
        onClick = onClick,
        label = if (copied) stringResource(Res.string.export_copied) else label,
        modifier = modifier,
        icon = if (copied) IconId.Check else IconId.Copy,
        enabled = enabled,
        hint = hint,
        tonal = tonal,
    )
}

/**
 * Download zip, or Share files on a touch screen whose share sheet takes the zip. Which one is
 * settled before the click, so the click never falls back to a download once the share is spent.
 */
@Composable
internal fun ZipButton(
    ready: ExportOutcome.Ready?,
    files: FileSaver,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val share = zipShares(ready, files)
    BuilderButton(
        onClick = {
            if (ready == null) return@BuilderButton
            scope.launchZip(files, ready.zip, share) { result ->
                if (result.isSuccess) {
                    dispatcher.dispatch(ExportAction.Exported)
                } else {
                    val failed = if (share) Res.string.export_share_failed else Res.string.export_save_failed
                    workspace.dispatch(WorkspaceAction.ShowToast(getString(failed)))
                }
            }
        },
        label = stringResource(if (share) Res.string.export_share else Res.string.export_download),
        modifier = modifier,
        emphasis = Emphasis.Primary,
        icon = if (share) IconId.Share else IconId.Download,
        enabled = ready != null,
    )
}

/**
 * Whether the zip goes to the share sheet rather than a download, as on a touch screen whose sheet
 * takes it. Settled before any click, and read by both the zip button and the manual copy hint.
 */
@Composable
internal fun zipShares(
    ready: ExportOutcome.Ready?,
    files: FileSaver,
): Boolean {
    val coarse = LocalLayout.current.coarsePointer
    val shareable = remember(ready, files) { ready != null && files.canShare(listOf(ready.zip)) }
    return coarse && shareable
}
