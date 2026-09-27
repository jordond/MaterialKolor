package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.feature.command.Shortcut
import com.materialkolor.builder.feature.command.rememberPanelShortcuts
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
import com.materialkolor.builder.generated.resources.export_options
import com.materialkolor.builder.generated.resources.export_save_failed
import com.materialkolor.builder.generated.resources.export_share
import com.materialkolor.builder.generated.resources.export_share_failed
import com.materialkolor.builder.generated.resources.export_title
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDisclosure
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSheet
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.SheetPresentation
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * How long Copy shows as Copied after it worked.
 */
internal const val COPIED_MILLIS = 1_200L

/**
 * What a copy button copies.
 */
private enum class CopyKind {
    File,
    All,
}

/**
 * The export sheet, over the whole screen at every size. A header that reads "Export AppTheme",
 * with the theme name and the package set in it as fields, the options beside the files, and Copy
 * all and Download zip at the bottom.
 *
 * The sheet picks its layout from its width, as [BodyLayout] has it. Wide, the options sit in a
 * column beside the workbench, the tree of files beside the code. Medium, the files are tabs over
 * the code instead. Narrow, the header only names the theme, and everything else is one scrolling
 * column, with the names and the options folded into a disclosure over the file tabs and the code.
 * Copy file sits at the end of the path over the code, and in the footer on the narrow layout,
 * where the path bar has no room for it. Switching the library is the same edit the top bar makes,
 * so the previews re-theme behind the sheet.
 *
 * Every copy, download and share starts inside the click, with the platform call as its first
 * suspension, and the text and the zip are ready before the click. A copy that worked
 * turns its button into Copied for [COPIED_MILLIS] and reads Copied out, since a screen reader never
 * hears a label change. One the browser refused opens a dialog to copy from by hand, and
 * never says Copied. On a touch screen whose share sheet takes the zip, the zip goes to the share
 * sheet, and anywhere else it downloads. A share sheet someone closes counts as done.
 *
 * C and Shift+C copy here as Copy file and Copy all do, from inside the key press, while no text
 * field in the sheet takes input.
 *
 * While the package or the theme name field holds a draft that is not valid, the sheet says what is
 * wrong in place of the files and Copy file, Copy all and Download wait for it.
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
 * @param[returnFocusTo] The button that opened the sheet, which gets focus back once it closes.
 * @param[glyph] The colours of the scheme glyph in the header, or null for no glyph.
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
    materialKolorVersion: String? = null,
    glyph: SchemeGlyph? = null,
) {
    val keys = rememberPanelShortcuts()
    // The footer and the body share what the copies did, so it lives here. Each opening starts it
    // over, a draft problem from the last time included.
    val sheet = remember(visible) { SheetCopies() }
    val scope = rememberCoroutineScope()
    val announcer = LocalAnnouncer.current
    val copiedWords = stringResource(Res.string.export_copied)
    LaunchedEffect(sheet, sheet.copies) {
        if (sheet.copied == null) return@LaunchedEffect
        delay(COPIED_MILLIS)
        sheet.copied = null
    }

    fun copy(
        kind: CopyKind,
        text: String,
    ) {
        scope.launchCopy(clipboard, text) { result ->
            if (result.isSuccess) {
                sheet.copied = kind
                sheet.copies++
                announcer.announce(copiedWords)
                dispatcher.dispatch(ExportAction.Exported)
            } else {
                sheet.copied = null
                sheet.manualText = text
                sheet.manualOpen = true
            }
        }
    }

    // The keys only show where there is a keyboard to press them on.
    val keysShown = !LocalLayout.current.coarsePointer
    val copyFileLabel = stringResource(Res.string.export_copy_file)
    val copiedLabel = stringResource(Res.string.export_copied)
    val copyFile: @Composable (picked: GeneratedFile?, onCode: Boolean) -> Unit = { picked, onCode ->
        val copied = sheet.copied == CopyKind.File
        val onClick = { if (picked != null) copy(CopyKind.File, picked.text) }
        if (onCode) {
            CodeCopyButton(
                label = if (copied) copiedLabel else copyFileLabel,
                copied = copied,
                enabled = picked != null,
                hint = if (keysShown) Shortcut.CopySeed.text(apple = false) else null,
                onClick = onClick,
            )
        } else {
            CopyButton(copied = copied, label = copyFileLabel, enabled = picked != null, onClick = onClick)
        }
    }
    val layout = BodyLayout.ofSheet(LocalLayout.current.widthDp)
    val narrow = layout == BodyLayout.Narrow

    BuilderSheet(
        visible = visible,
        onDismissRequest = { workspace.dispatch(WorkspaceAction.ClosePanel) },
        title = stringResource(Res.string.export_title),
        presentation = SheetPresentation.FullScreen,
        modifier = modifier.then(keys.modifier),
        returnFocusTo = returnFocusTo,
        header = {
            ExportHeader(state, glyph, dispatcher, workspace, sheet.drafts, compact = narrow)
        },
        footer = {
            val export = sheetExport(state, outcomeOf, sheet.drafts)
            val ready = export.ready
            ExportFooter(materialKolorVersion, ready) {
                if (narrow) copyFile(export.picked, false)
                CopyButton(
                    copied = sheet.copied == CopyKind.All,
                    label = stringResource(Res.string.export_copy_all),
                    enabled = ready != null,
                    tonal = true,
                    hint = if (keysShown) Shortcut.CopyAll.text(apple = false) else null,
                    onClick = { if (ready != null) copy(CopyKind.All, ready.allText) },
                )
                ZipButton(ready, files, dispatcher, workspace)
            }
        },
    ) {
        val export = sheetExport(state, outcomeOf, sheet.drafts)
        val ready = export.ready
        val picked = export.picked
        // C copies the file picked and Shift+C every file, as Copy file and Copy all do.
        SideEffect {
            keys.onShortcut = { shortcut ->
                when {
                    shortcut == Shortcut.CopySeed && picked != null -> {
                        copy(CopyKind.File, picked.text)
                        true
                    }
                    shortcut == Shortcut.CopyAll && ready != null -> {
                        copy(CopyKind.All, ready.allText)
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
        }
        val parts = BodyParts(
            names = { ExportNames(state, dispatcher, workspace, sheet.drafts) },
            notices = {
                export.problems.forEach { problem ->
                    Notice(text = problemText(problem), icon = IconId.Error, emphasis = Emphasis.Danger)
                }
            },
            options = { optionsModifier ->
                ExportOptionsForm(state, capabilities, dispatcher, workspace, optionsModifier)
            },
            onSelect = { path -> dispatcher.dispatch(ExportAction.SelectFile(path)) },
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val optionsWidth = layout.optionsWidth
            if (optionsWidth == null) {
                NarrowBody(export, parts, optionsSummary(state), viewport = constraints.maxHeight)
            } else {
                SplitBody(export, parts, tree = layout == BodyLayout.Wide, optionsWidth) {
                    copyFile(picked, true)
                }
            }
        }
        val zipLabel = if (zipShares(ready, files)) Res.string.export_share else Res.string.export_download
        ManualCopyDialog(
            visible = sheet.manualOpen,
            text = sheet.manualText,
            onDismissRequest = { sheet.manualOpen = false },
            saveLabel = stringResource(zipLabel),
        )
    }
}

/**
 * The parts every layout of the body places, each in its own spot.
 *
 * @property[names] The package and theme name fields, which only the narrow body shows, since the
 * header holds them everywhere else.
 * @property[notices] One notice per problem holding the export back.
 * @property[options] The options, given the modifier that places them.
 * @property[onSelect] Called with the path of the file picked.
 */
private class BodyParts(
    val names: @Composable () -> Unit,
    val notices: @Composable () -> Unit,
    val options: @Composable (Modifier) -> Unit,
    val onSelect: (String) -> Unit,
)

/**
 * The wide and medium body. The notices across the top, then the options in a column that scrolls
 * on its own beside the workbench, which takes the rest of the width and height. With [tree] the
 * files are a tree on the workbench, and without it tabs over it.
 *
 * @param[copyFile] The Copy file button for the path bar.
 */
@Composable
private fun SplitBody(
    export: SheetExport,
    parts: BodyParts,
    tree: Boolean,
    optionsWidth: Dp,
    copyFile: @Composable () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val ready = export.ready
    val picked = export.picked
    // The body keeps a focus ring's room along its edges, where the sheet would clip it.
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.extraSmall),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        parts.notices()
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.large)) {
            // Its fields and buttons take focus themselves, so the column is no stop of its own.
            BuilderScrollArea(Modifier.width(optionsWidth).fillMaxHeight(), tabStop = false) {
                parts.options(
                    Modifier.padding(
                        start = spacing.extraSmall,
                        end = spacing.medium,
                        top = spacing.extraSmall,
                        bottom = spacing.large,
                    ),
                )
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                if (!tree && ready != null && picked != null) ExportFileTabs(ready, picked, parts.onSelect)
                ExportWorkbench(
                    ready = ready,
                    picked = picked,
                    tree = tree,
                    onSelect = parts.onSelect,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    copyFile = copyFile,
                )
            }
        }
    }
}

/**
 * The narrow body, one column that scrolls as one. The notices, then the names and the options
 * folded into a disclosure, closed on open, whose summary says what is picked. Then the file tabs
 * and the code, the code taking the height the rest leaves, so nothing is cut off mid row where the
 * sheet runs short. Its fields and buttons take focus themselves, so the area is no stop of its
 * own, and its scrollbar sits in the sheet's padding.
 *
 * @param[summary] What the closed disclosure says is picked, "M3, Live, Multiplatform".
 * @param[viewport] The height the scroll shows, in pixels.
 */
@Composable
private fun NarrowBody(
    export: SheetExport,
    parts: BodyParts,
    summary: String,
    viewport: Int,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val ready = export.ready
    val picked = export.picked
    var optionsOpen by rememberSaveable { mutableStateOf(false) }
    BuilderScrollArea(Modifier.fillMaxSize(), tabStop = false, scrollbarInGutter = true) {
        FillLastColumn(viewport = viewport, fillLast = picked != null) {
            parts.notices()
            BuilderDisclosure(
                expanded = optionsOpen,
                onExpandedChange = { open -> optionsOpen = open },
                title = stringResource(Res.string.export_options),
                summary = summary,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
                    parts.names()
                    parts.options(Modifier)
                }
            }
            if (ready != null && picked != null) {
                ExportFileTabs(ready, picked, parts.onSelect)
                ExportWorkbench(ready = ready, picked = picked, tree = false, onSelect = parts.onSelect)
            }
        }
    }
}

/**
 * What the sheet hands out right now. [ready] is the export once nothing holds it back, [problems]
 * what does, and [picked] the file whose tab is open.
 */
private class SheetExport(
    val ready: ExportOutcome.Ready?,
    val problems: List<ExportProblem>,
    val picked: GeneratedFile?,
)

/**
 * The export of [state], held back while a draft in [drafts] is not valid. Only the parts
 * of the sheet that show ask for it, so a closed sheet generates nothing.
 */
private fun sheetExport(
    state: ExportModel.State,
    outcomeOf: (ExportModel.State) -> ExportOutcome,
    drafts: DraftProblems,
): SheetExport {
    val outcome = outcomeOf(state)
    val draftProblems = drafts.all
    val ready = (outcome as? ExportOutcome.Ready)?.takeIf { draftProblems.isEmpty() }
    val problems = (draftProblems + (outcome as? ExportOutcome.Blocked)?.problems.orEmpty()).distinct()
    return SheetExport(ready, problems, ready?.fileAt(state.selectedPath))
}

/**
 * What the copies did while the sheet is open, and what the option fields say is wrong.
 */
@Stable
private class SheetCopies {
    val drafts = DraftProblems()
    var copied: CopyKind? by mutableStateOf(null)
    var copies: Int by mutableIntStateOf(0)
    var manualText: String by mutableStateOf("")
    var manualOpen: Boolean by mutableStateOf(false)
}

/**
 * A copy button that shows Copied, with a check, while [copied].
 *
 * @param[tonal] Whether it carries its own fill.
 * @param[hint] The key that does the same, drawn as a keycap, or null for none.
 */
@Composable
private fun CopyButton(
    copied: Boolean,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tonal: Boolean = false,
    hint: String? = null,
) {
    BuilderButton(
        onClick = onClick,
        label = if (copied) stringResource(Res.string.export_copied) else label,
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
private fun ZipButton(
    ready: ExportOutcome.Ready?,
    files: FileSaver,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
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
private fun zipShares(
    ready: ExportOutcome.Ready?,
    files: FileSaver,
): Boolean {
    val coarse = LocalLayout.current.coarsePointer
    val shareable = remember(ready, files) { ready != null && files.canShare(listOf(ready.zip)) }
    return coarse && shareable
}

@Composable
private fun problemText(problem: ExportProblem): String =
    when (problem) {
        is ExportProblem.PackageName -> stringResource(Res.string.export_blocked_package, problem.packageName)
        is ExportProblem.ThemeName -> stringResource(Res.string.export_blocked_theme_name, problem.themeName)
        ExportProblem.ExtraColors -> stringResource(Res.string.export_blocked_extra_colors)
        is ExportProblem.NameTaken -> stringResource(Res.string.export_blocked_taken, problem.name)
    }

/**
 * The file at [path], or the first one when [path] is null or the export no longer has it.
 */
private fun ExportOutcome.Ready.fileAt(path: String?): GeneratedFile? =
    files.firstOrNull { file -> file.path == path } ?: files.firstOrNull()
