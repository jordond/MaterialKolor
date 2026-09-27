package com.materialkolor.builder.feature.export

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_tree
import com.materialkolor.builder.kit.control.BuilderChoiceGroup
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderTabs
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.foldedTabName
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import com.materialkolor.builder.kit.widget.CodeView
import org.jetbrains.compose.resources.stringResource

/**
 * How wide the file tree is beside the code.
 */
private val TreeWidth = 264.dp

/**
 * The files on the code ground, the tree of them as the zip lays them out beside a path bar over
 * the code of the file picked. The ground is the skin's code ground in both modes, so everything on
 * it takes the code palette's colours. The tree sits on a lighter band of it, so the files read apart
 * from the code.
 *
 * Without [tree] the files are the caller's tabs above it, and the ground holds the path bar and the
 * code alone. While nothing can be exported the ground stays, with no path and no code.
 *
 * @param[ready] The export, or null while something holds it back.
 * @param[picked] The file whose code shows.
 * @param[tree] Whether the tree of files shows beside the code.
 * @param[onSelect] Called with the path of the file picked in the tree.
 * @param[copyFile] The Copy file button at the end of the path bar, or null for none.
 */
@Composable
internal fun ExportWorkbench(
    ready: ExportOutcome.Ready?,
    picked: GeneratedFile?,
    tree: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    copyFile: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.large)
    Row(modifier.clip(shape).background(tokens.codeBackground)) {
        if (tree && ready != null && picked != null) {
            FileTree(
                ready = ready,
                selectedPath = picked.path,
                onSelect = onSelect,
                modifier = Modifier
                    .width(TreeWidth)
                    .fillMaxHeight()
                    .background(tokens.codePalette.plain.copy(alpha = TREE_BAND_ALPHA)),
            )
            Box(Modifier.width(tokens.outlineWidth).fillMaxHeight().background(ruleColor()))
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            PathBar(picked?.path, copyFile)
            Box(Modifier.fillMaxWidth().height(tokens.outlineWidth).background(ruleColor()))
            if (picked != null) {
                // The code keeps off the ground's rounded corners, so its square ones never show.
                CodeView(
                    lines = picked.lines,
                    onCopy = null,
                    label = picked.path.fileName(),
                    framed = false,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            start = tokens.spacing.small,
                            end = tokens.spacing.medium,
                            bottom = tokens.spacing.medium,
                        ),
                )
            }
        }
    }
}

/**
 * The files as tabs, for the widths with no room for the tree.
 */
@Composable
internal fun ExportFileTabs(
    ready: ExportOutcome.Ready,
    picked: GeneratedFile,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BuilderTabs(
        tabs = ready.files.map { each -> each.path },
        selected = picked.path,
        onSelect = onSelect,
        label = { path -> path.fileName() },
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The hairlines between the parts of the ground.
 */
@Composable
private fun ruleColor(): Color {
    val muted = LocalBuilderTokens.current.codePalette.muted
    return muted.copy(alpha = RULE_ALPHA)
}

private const val RULE_ALPHA = 0.3f

/**
 * How strongly the tree's band is washed with the code's ink, a step up from the ground.
 */
private const val TREE_BAND_ALPHA = 0.04f

/**
 * The zip's name over its folders and files. The files are one choice with a single Tab stop, each
 * a tab, "Color.kt, tab, selected", and the arrows move between them. A folder's name sits over the
 * first file in it, outside the file's own focus.
 */
@Composable
private fun FileTree(
    ready: ExportOutcome.Ready,
    selectedPath: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val palette = tokens.codePalette
    val entries = remember(ready.files) { treeEntries(exportTree(ready.files)) }
    val byPath = remember(entries) { entries.associateBy { entry -> entry.file.path } }
    val target = LocalLayout.current.primaryTouchTarget
    Column(modifier.padding(tokens.spacing.small)) {
        Row(
            modifier = Modifier.heightIn(min = target).padding(horizontal = tokens.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderIcon(id = IconId.Archive, contentDescription = null, tint = palette.muted, size = treeIconSize())
            BuilderText(
                text = ready.zip.name,
                style = BuilderTextStyle.Code,
                color = palette.plain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BuilderScrollArea(Modifier.weight(1f).fillMaxWidth(), tabStop = false) {
            BuilderChoiceGroup(
                options = entries.map { entry -> entry.file.path },
                selected = selectedPath,
                onSelect = onSelect,
                label = stringResource(Res.string.export_tree),
                modifier = Modifier.padding(tokens.spacing.extraSmall),
                columns = 1,
            ) { path, isSelected, optionModifier ->
                val entry = byPath.getValue(path)
                Column(Modifier.fillMaxWidth()) {
                    entry.folders.forEach { folder -> FolderLabel(folder) }
                    FileRow(entry.file, isSelected, onClick = { onSelect(path) }, modifier = optionModifier)
                }
            }
        }
    }
}

/**
 * A file of the tree with the folder rows that go just above it.
 */
private class TreeEntry(
    val folders: List<TreeRow.Folder>,
    val file: TreeRow.File,
)

/**
 * [rows] gathered under the file each folder row comes before.
 */
private fun treeEntries(rows: List<TreeRow>): List<TreeEntry> {
    val entries = mutableListOf<TreeEntry>()
    val folders = mutableListOf<TreeRow.Folder>()
    for (row in rows) {
        when (row) {
            is TreeRow.Folder -> {
                folders += row
            }
            is TreeRow.File -> {
                entries += TreeEntry(folders.toList(), row)
                folders.clear()
            }
        }
    }
    return entries
}

/**
 * The glyphs of the tree, a little under the kit's icon size so they sit with the code's type.
 */
@Composable
private fun treeIconSize() = LocalBuilderTokens.current.iconSize * TREE_ICON_SCALE

private const val TREE_ICON_SCALE = 0.8f

/**
 * How far in a row of [depth] starts.
 */
@Composable
private fun indentOf(depth: Int): Dp {
    val spacing = LocalBuilderTokens.current.spacing
    return spacing.small + spacing.large * depth
}

@Composable
private fun FolderLabel(folder: TreeRow.Folder) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = Modifier.padding(
            start = indentOf(folder.depth),
            end = tokens.spacing.small,
            top = tokens.spacing.small,
            bottom = tokens.spacing.extraSmall,
        ),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderIcon(
            id = IconId.FolderOutline,
            contentDescription = null,
            tint = tokens.codePalette.muted,
            size = treeIconSize(),
        )
        BuilderText(text = folder.label, style = BuilderTextStyle.Code, color = tokens.codePalette.muted)
    }
}

/**
 * One file of the tree, its name and its line count. The one picked takes a wash of the code's
 * keyword ink and sets its name in bold, so it never rests on the wash alone.
 */
@Composable
private fun FileRow(
    file: TreeRow.File,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val palette = tokens.codePalette
    val ink = palette[TokenKind.Keyword]
    val shape = RoundedCornerShape(tokens.radius.small)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .focusOutline(focused, ink, tokens.highlightWidth, shape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ).foldedTabName(file.name, selected)
            .clip(shape)
            .background(if (selected) ink.copy(alpha = PICKED_FILE_ALPHA) else Color.Transparent)
            .heightIn(min = maxOf(FileRowHeight, LocalLayout.current.primaryTouchTarget))
            .padding(start = indentOf(file.depth), end = tokens.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The row reads as its tab name alone, so the name and the count drawn here stay out of it.
        BuilderIcon(
            id = IconId.File,
            contentDescription = null,
            tint = if (selected) palette.plain else palette.muted,
            size = treeIconSize(),
        )
        BasicText(
            text = file.name,
            modifier = Modifier.weight(1f).clearAndSetSemantics {},
            style = LocalBuilderType.current.code.merge(
                color = palette.plain,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BuilderText(
            text = file.lines.toString(),
            modifier = Modifier.clearAndSetSemantics {},
            style = BuilderTextStyle.Code,
            color = if (selected) palette.plain else palette.muted,
            maxLines = 1,
        )
    }
}

/**
 * How strongly the picked file is washed with the code's keyword ink, enough to stand off the
 * tree's band in both modes.
 */
private const val PICKED_FILE_ALPHA = 0.22f

/**
 * The height of a file's row where the pointer allows a shorter one.
 */
private val FileRowHeight: Dp = 34.dp

/**
 * The picked file's path, its folder quiet and its name in full ink and bold, cut from the start
 * when short of room so the name always shows, then [copyFile]. The bar is tall enough that the
 * button keeps clear of the ground's rounded corner.
 */
@Composable
private fun PathBar(
    path: String?,
    copyFile: (@Composable () -> Unit)?,
) {
    val tokens = LocalBuilderTokens.current
    val palette = tokens.codePalette
    val name = path?.fileName().orEmpty()
    val folder = path?.removeSuffix(name).orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = maxOf(PathBarHeight, LocalLayout.current.primaryTouchTarget + tokens.spacing.medium))
            .padding(start = tokens.spacing.large, end = tokens.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The type is monospaced, so the characters that fit follow from the width of one.
        val style = LocalBuilderType.current.code
        val measurer = rememberTextMeasurer()
        val charWidth = remember(style, measurer) { measurer.measure("0", style).size.width }
        BoxWithConstraints(Modifier.weight(1f)) {
            val fits = if (charWidth > 0) constraints.maxWidth / charWidth else Int.MAX_VALUE
            val shown = startCut(folder + name, fits)
            val quiet = (shown.length - name.length).coerceAtLeast(0)
            BasicText(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = palette.muted)) { append(shown.take(quiet)) }
                    withStyle(SpanStyle(color = palette.plain, fontWeight = FontWeight.SemiBold)) {
                        append(shown.drop(quiet))
                    }
                },
                style = style,
                maxLines = 1,
                softWrap = false,
            )
        }
        copyFile?.invoke()
    }
}

/**
 * [text] as it fits [fits] characters, its start given up to an ellipsis when it is longer.
 */
private fun startCut(
    text: String,
    fits: Int,
): String =
    when {
        text.length <= fits -> text
        fits <= 1 -> ELLIPSIS
        else -> ELLIPSIS + text.takeLast(fits - 1)
    }

private const val ELLIPSIS = "…"

/**
 * How tall the path bar is where the pointer allows, room for Copy file with a margin round it.
 */
private val PathBarHeight: Dp = 52.dp

/**
 * Copy file on the code ground, a pill in the code's own inks so it reads on the dark ground in
 * both modes, with the key that does the same as a keycap after its label.
 *
 * @param[label] What it does, "Copy file", or "Copied" for a moment after it worked.
 * @param[copied] Whether the copy just worked, which swaps the glyph for a check.
 * @param[hint] The key that does the same, or null where there is no keyboard to press it on.
 */
@Composable
internal fun CodeCopyButton(
    label: String,
    copied: Boolean,
    enabled: Boolean,
    hint: String?,
    onClick: () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val palette = tokens.codePalette
    val pill = RoundedCornerShape(percent = 50)
    BuilderPressable(onClick = onClick, label = label, enabled = enabled, shape = pill) {
        Row(
            modifier = Modifier
                .background(palette.plain.copy(alpha = COPY_FILL_ALPHA), pill)
                .heightIn(min = CopyHeight)
                .padding(
                    start = tokens.spacing.medium,
                    end = if (hint ==
                        null
                    ) {
                        tokens.spacing.large
                    } else {
                        tokens.spacing.small
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderIcon(
                id = if (copied) IconId.Check else IconId.Copy,
                contentDescription = null,
                tint = palette.plain,
                size = CopyIconSize,
            )
            BasicText(
                text = label,
                style = LocalBuilderType.current.label.merge(color = palette.plain, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (hint != null) {
                BasicText(
                    text = hint,
                    modifier = Modifier
                        .background(tokens.codeBackground, RoundedCornerShape(tokens.radius.small / 2))
                        .padding(horizontal = tokens.spacing.small - KeycapTrim, vertical = KeycapTrim),
                    style = LocalBuilderType.current.value.merge(color = palette.muted, fontSize = KeycapSize),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * How strongly Copy file's pill is washed with the code's ink.
 */
private const val COPY_FILL_ALPHA = 0.1f

/**
 * Copy file's pill, its glyph and its keycap.
 */
private val CopyHeight: Dp = 36.dp
private val CopyIconSize: Dp = 16.dp
private val KeycapSize = 11.sp
private val KeycapTrim: Dp = 2.dp

/**
 * The last part of a path, the name a tab shows.
 */
internal fun String.fileName(): String = substringAfterLast('/')
