package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.feature.topbar.LibrarySwitcher
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_blocked_extra_colors
import com.materialkolor.builder.generated.resources.export_blocked_package
import com.materialkolor.builder.generated.resources.export_blocked_taken
import com.materialkolor.builder.generated.resources.export_blocked_theme_name
import com.materialkolor.builder.generated.resources.export_copied
import com.materialkolor.builder.generated.resources.export_copy_all
import com.materialkolor.builder.generated.resources.export_copy_file
import com.materialkolor.builder.generated.resources.export_download
import com.materialkolor.builder.generated.resources.export_expressive_2021
import com.materialkolor.builder.generated.resources.export_manual_done
import com.materialkolor.builder.generated.resources.export_manual_hint
import com.materialkolor.builder.generated.resources.export_manual_title
import com.materialkolor.builder.generated.resources.export_mode
import com.materialkolor.builder.generated.resources.export_mode_dynamic
import com.materialkolor.builder.generated.resources.export_mode_frozen
import com.materialkolor.builder.generated.resources.export_options
import com.materialkolor.builder.generated.resources.export_options_summary
import com.materialkolor.builder.generated.resources.export_save_failed
import com.materialkolor.builder.generated.resources.export_share
import com.materialkolor.builder.generated.resources.export_share_failed
import com.materialkolor.builder.generated.resources.export_title
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderDisclosure
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSheet
import com.materialkolor.builder.kit.control.BuilderTabs
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.SheetPresentation
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.CodeView
import dev.stateholder.dispatcher.Dispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** How long Copy shows as Copied after it worked (F-26). */
internal const val COPIED_MILLIS = 1_200L

/** How much of the window the manual copy dialog's text may take before it scrolls. */
private const val MANUAL_COPY_HEIGHT_FRACTION = 0.5f

/** What a copy button copies. */
private enum class CopyKind {
    File,
    All,
}

/**
 * The export sheet (F-26). The target, the mode and the options on top, the files under them, and
 * Copy file, Copy all and Download zip at the bottom.
 *
 * It is an end panel at 60% of the window from 840 dp up, which takes in Expanded, and the full
 * screen below that, as [SheetPresentation.of] decides. Switching the target is the same edit the
 * top bar makes, so the app re-skins behind the sheet.
 *
 * Every copy, download and share starts inside the click, with the platform call as its first
 * suspension, and the text and the zip are ready before the click (R-B-302). A copy that worked
 * turns its button into Copied for [COPIED_MILLIS]. One the browser refused opens a dialog to copy
 * from by hand, and never says Copied. On a touch screen whose share sheet takes the zip, the zip
 * goes to the share sheet, and anywhere else it downloads. A share sheet someone closes counts as
 * done.
 *
 * @param[visible] Whether the sheet is open.
 * @param[state] The export model's state.
 * @param[capabilities] How each option shows up for the document's target.
 * @param[outcomeOf] The export of a state, memoized by the model. It is only asked while the sheet
 * shows.
 * @param[clipboard] Where Copy and Copy all write.
 * @param[files] Where the zip goes.
 * @param[dispatcher] Takes the option changes.
 * @param[workspace] Takes the target switch, the theme name, closing and toasts.
 */
@Composable
internal fun ExportSheet(
    visible: Boolean,
    state: ExportModel.State,
    capabilities: Capabilities,
    outcomeOf: (ExportModel.State) -> ExportOutcome,
    clipboard: Clipboard,
    files: FileSaver,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    BuilderSheet(
        visible = visible,
        onDismissRequest = { workspace.dispatch(WorkspaceAction.ClosePanel) },
        title = stringResource(Res.string.export_title),
        presentation = SheetPresentation.of(LocalLayout.current),
        modifier = modifier,
    ) {
        val scope = rememberCoroutineScope()
        var copied by remember { mutableStateOf<CopyKind?>(null) }
        var copies by remember { mutableIntStateOf(0) }
        var manualText by remember { mutableStateOf("") }
        var manualOpen by remember { mutableStateOf(false) }
        LaunchedEffect(copies) {
            if (copied == null) return@LaunchedEffect
            delay(COPIED_MILLIS)
            copied = null
        }

        // The write is the first suspension, so the browser still sees the click (R-B-302).
        fun copy(
            kind: CopyKind,
            text: String,
        ) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                val result = clipboard.writeText(text)
                if (result.isSuccess) {
                    copied = kind
                    copies++
                    dispatcher.dispatch(ExportAction.Exported)
                } else {
                    copied = null
                    manualText = text
                    manualOpen = true
                }
            }
        }

        val outcome = outcomeOf(state)
        val ready = outcome as? ExportOutcome.Ready
        val spacing = LocalBuilderTokens.current.spacing
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            BuilderScrollArea(Modifier.weight(1f, fill = false).fillMaxWidth()) {
                ExportHeader(state, capabilities, dispatcher, workspace)
            }
            when (outcome) {
                is ExportOutcome.Ready -> {
                    ExportFiles(outcome, state.selectedPath, dispatcher, onCopy = { text -> copy(CopyKind.File, text) })
                }
                is ExportOutcome.Blocked -> {
                    outcome.problems.forEach { problem ->
                        Notice(text = problemText(problem), icon = IconId.Error, emphasis = Emphasis.Danger)
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                val file = ready?.let { export -> export.fileAt(state.selectedPath) }
                CopyButton(
                    copied = copied == CopyKind.File,
                    label = stringResource(Res.string.export_copy_file),
                    enabled = file != null,
                    onClick = { if (file != null) copy(CopyKind.File, file.text) },
                )
                CopyButton(
                    copied = copied == CopyKind.All,
                    label = stringResource(Res.string.export_copy_all),
                    enabled = ready != null,
                    onClick = { if (ready != null) copy(CopyKind.All, ready.allText) },
                )
                ZipButton(ready, files, dispatcher, workspace)
            }
        }
        ManualCopyDialog(visible = manualOpen, text = manualText, onDismissRequest = { manualOpen = false })
    }
}

/** The target, the mode, the Expressive warning and the options. */
@Composable
private fun ExportHeader(
    state: ExportModel.State,
    capabilities: Capabilities,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val modes = mapOf(
        ExportMode.Dynamic to stringResource(Res.string.export_mode_dynamic),
        ExportMode.Frozen to stringResource(Res.string.export_mode_frozen),
    )
    val wide = LocalLayout.current.windowClass == WindowClass.Expanded
    var optionsOpen by rememberSaveable { mutableStateOf(wide) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            // Walking the arrows past a library would re-skin the app at every step, so only a pick switches.
            LibrarySwitcher(
                document = state.document,
                onSwitch = { choice, origin ->
                    workspace.dispatch(WorkspaceAction.EditWithReveal(choice.change, origin))
                },
                selectOnFocus = false,
            )
            BuilderSegmented(
                options = ExportMode.entries,
                selected = state.prefs.mode,
                onSelect = { mode -> dispatcher.dispatch(ExportAction.SetMode(mode)) },
                label = stringResource(Res.string.export_mode),
                optionLabel = { mode -> modes.getValue(mode) },
            )
        }
        if (state.expressiveOn2021) {
            Notice(text = stringResource(Res.string.export_expressive_2021), icon = IconId.Warning)
        }
        BuilderDisclosure(
            expanded = optionsOpen,
            onExpandedChange = { open -> optionsOpen = open },
            title = stringResource(Res.string.export_options),
            summary = stringResource(Res.string.export_options_summary, state.prefs.packageName),
        ) {
            ExportOptionsForm(state, capabilities, dispatcher, workspace)
        }
    }
}

/** The file tabs and the code of the file picked. */
@Composable
private fun ColumnScope.ExportFiles(
    export: ExportOutcome.Ready,
    selectedPath: String?,
    dispatcher: Dispatcher<ExportAction>,
    onCopy: (String) -> Unit,
) {
    val file = export.fileAt(selectedPath) ?: return
    BuilderTabs(
        tabs = export.files.map { each -> each.path },
        selected = file.path,
        onSelect = { path -> dispatcher.dispatch(ExportAction.SelectFile(path)) },
        label = { path -> path.fileName() },
        modifier = Modifier.fillMaxWidth(),
    )
    CodeView(
        lines = file.lines,
        onCopy = { onCopy(file.text) },
        label = file.path.fileName(),
        modifier = Modifier.weight(1f).fillMaxWidth(),
    )
}

/** A copy button that shows Copied, with a check, while [copied]. */
@Composable
private fun CopyButton(
    copied: Boolean,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    BuilderButton(
        onClick = onClick,
        label = if (copied) stringResource(Res.string.export_copied) else label,
        icon = if (copied) IconId.Check else IconId.Copy,
        enabled = enabled,
    )
}

/**
 * Download zip, or Share files on a touch screen whose share sheet takes the zip. Which one is
 * settled before the click, so the click never falls back to a download once the share is spent.
 */
@Composable
private fun ZipButton(
    ready: ExportOutcome.Ready?,
    files: FileSaver,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
) {
    val scope = rememberCoroutineScope()
    val coarse = LocalLayout.current.coarsePointer
    val shareable = remember(ready, files) { ready != null && files.canShare(listOf(ready.zip)) }
    val share = coarse && shareable
    BuilderButton(
        onClick = {
            if (ready == null) return@BuilderButton
            val zip = ready.zip
            // The save or the share is the first suspension, so the browser still sees the click (R-B-302).
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                val result = if (share) {
                    files.shareFiles(listOf(zip))
                } else {
                    files.save(zip.name, zip.bytes, zip.mime)
                }
                if (result.isSuccess) {
                    dispatcher.dispatch(ExportAction.Exported)
                } else {
                    val failed = if (share) Res.string.export_share_failed else Res.string.export_save_failed
                    workspace.dispatch(WorkspaceAction.ShowToast(getString(failed)))
                }
            }
        },
        label = stringResource(if (share) Res.string.export_share else Res.string.export_download),
        emphasis = Emphasis.Primary,
        icon = if (share) IconId.Share else IconId.Download,
        enabled = ready != null,
    )
}

/** The text a refused copy was for, to select and copy by hand. */
@Composable
private fun ManualCopyDialog(
    visible: Boolean,
    text: String,
    onDismissRequest: () -> Unit,
) {
    val layout = LocalLayout.current
    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.export_manual_title),
        actions = {
            BuilderButton(
                onClick = onDismissRequest,
                label = stringResource(Res.string.export_manual_done),
                emphasis = Emphasis.Primary,
            )
        },
    ) {
        BuilderText(text = stringResource(Res.string.export_manual_hint), emphasis = Emphasis.Secondary)
        BuilderScrollArea(Modifier.heightIn(max = layout.heightDp * MANUAL_COPY_HEIGHT_FRACTION)) {
            SelectionContainer {
                BuilderText(text = text, style = BuilderTextStyle.Value)
            }
        }
    }
}

/** One line that needs attention, with its icon. */
@Composable
private fun Notice(
    text: String,
    icon: IconId,
    emphasis: Emphasis = Emphasis.Secondary,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        BuilderIcon(id = icon, contentDescription = null, emphasis = emphasis)
        BuilderText(text = text, emphasis = emphasis)
    }
}

@Composable
private fun problemText(problem: ExportProblem): String =
    when (problem) {
        is ExportProblem.PackageName -> stringResource(Res.string.export_blocked_package, problem.packageName)
        is ExportProblem.ThemeName -> stringResource(Res.string.export_blocked_theme_name, problem.themeName)
        ExportProblem.ExtraColors -> stringResource(Res.string.export_blocked_extra_colors)
        is ExportProblem.NameTaken -> stringResource(Res.string.export_blocked_taken, problem.name)
    }

/** The file at [path], or the first one when [path] is null or the export no longer has it. */
private fun ExportOutcome.Ready.fileAt(path: String?): GeneratedFile? =
    files.firstOrNull { file -> file.path == path } ?: files.firstOrNull()

/** The last part of a path, the name a tab shows. */
private fun String.fileName(): String = substringAfterLast('/')
