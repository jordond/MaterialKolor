package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.feature.topbar.LibrarySwitcher
import com.materialkolor.builder.feature.workspace.ManualCopyDialog
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
import com.materialkolor.builder.generated.resources.export_mode
import com.materialkolor.builder.generated.resources.export_mode_dynamic
import com.materialkolor.builder.generated.resources.export_mode_frozen
import com.materialkolor.builder.generated.resources.export_options
import com.materialkolor.builder.generated.resources.export_options_summary
import com.materialkolor.builder.generated.resources.export_save_failed
import com.materialkolor.builder.generated.resources.export_share
import com.materialkolor.builder.generated.resources.export_share_failed
import com.materialkolor.builder.generated.resources.export_title
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDisclosure
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSheet
import com.materialkolor.builder.kit.control.BuilderTabs
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.SheetPresentation
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.CodeView
import dev.stateholder.dispatcher.Dispatcher
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** How long Copy shows as Copied after it worked (F-26). */
internal const val COPIED_MILLIS = 1_200L

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
 * turns its button into Copied for [COPIED_MILLIS] and reads Copied out, since a screen reader never
 * hears a label change (AR-06). One the browser refused opens a dialog to copy from by hand, and
 * never says Copied. On a touch screen whose share sheet takes the zip, the zip goes to the share
 * sheet, and anywhere else it downloads. A share sheet someone closes counts as done.
 *
 * While the package or the theme name field holds a draft that is not valid, the sheet says what is
 * wrong in place of the files and Copy file, Copy all and Download wait for it (R-B-309).
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
 * @param[returnFocusTo] The button that opened the sheet, which gets focus back once it closes (AR-09).
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
    returnFocusTo: FocusRequester? = null,
) {
    BuilderSheet(
        visible = visible,
        onDismissRequest = { workspace.dispatch(WorkspaceAction.ClosePanel) },
        title = stringResource(Res.string.export_title),
        presentation = SheetPresentation.of(LocalLayout.current),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
    ) {
        val scope = rememberCoroutineScope()
        val announcer = LocalAnnouncer.current
        val copiedWords = stringResource(Res.string.export_copied)
        val drafts = remember { DraftProblems() }
        var copied by remember { mutableStateOf<CopyKind?>(null) }
        var copies by remember { mutableIntStateOf(0) }
        var manualText by remember { mutableStateOf("") }
        var manualOpen by remember { mutableStateOf(false) }
        LaunchedEffect(copies) {
            if (copied == null) return@LaunchedEffect
            delay(COPIED_MILLIS)
            copied = null
        }

        fun copy(
            kind: CopyKind,
            text: String,
        ) {
            scope.launchCopy(clipboard, text) { result ->
                if (result.isSuccess) {
                    copied = kind
                    copies++
                    announcer.announce(copiedWords)
                    dispatcher.dispatch(ExportAction.Exported)
                } else {
                    copied = null
                    manualText = text
                    manualOpen = true
                }
            }
        }

        val outcome = outcomeOf(state)
        val draftProblems = drafts.all
        val ready = (outcome as? ExportOutcome.Ready)?.takeIf { draftProblems.isEmpty() }
        val problems = (draftProblems + (outcome as? ExportOutcome.Blocked)?.problems.orEmpty()).distinct()
        val spacing = LocalBuilderTokens.current.spacing
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            // Its fields and buttons take focus themselves, so the area is no stop of its own.
            BuilderScrollArea(Modifier.weight(1f, fill = false).fillMaxWidth(), tabStop = false) {
                ExportHeader(state, capabilities, dispatcher, workspace, drafts)
            }
            if (ready != null) {
                ExportFiles(ready, state.selectedPath, dispatcher, onCopy = { text -> copy(CopyKind.File, text) })
            }
            problems.forEach { problem ->
                Notice(text = problemText(problem), icon = IconId.Error, emphasis = Emphasis.Danger)
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
    drafts: DraftProblems,
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
            LibrarySwitcher(
                document = state.document,
                onSwitch = { choice, origin ->
                    workspace.dispatch(WorkspaceAction.EditWithReveal(choice.change, origin))
                },
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
            ExportOptionsForm(state, capabilities, dispatcher, workspace, drafts)
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
        emphasis = Emphasis.Primary,
        icon = if (share) IconId.Share else IconId.Download,
        enabled = ready != null,
    )
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
