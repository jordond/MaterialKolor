package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_checked
import com.materialkolor.builder.generated.resources.export_checked_any
import com.materialkolor.builder.generated.resources.export_files
import com.materialkolor.builder.generated.resources.export_files_lines
import com.materialkolor.builder.generated.resources.export_lines
import com.materialkolor.builder.generated.resources.export_subtitle
import com.materialkolor.builder.generated.resources.export_subtitle_unnamed
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The theme's name, the project it belongs to and its seed, "AppTheme from Burnt Ember #D9653B".
 */
@Composable
internal fun exportSubtitle(state: ExportModel.State): String {
    val themeName = state.document.themeName
    val seed = state.document.seed.toHex()
    val project = state.projectName
    return if (project.isBlank()) {
        stringResource(Res.string.export_subtitle_unnamed, themeName, seed)
    } else {
        stringResource(Res.string.export_subtitle, themeName, project, seed)
    }
}

/**
 * How the export sheet lays its body out, picked from the body's own width, since the sheet takes
 * the whole screen at every size.
 */
internal enum class BodyLayout {
    /**
     * The options in a column beside the files, which show as a tree beside the code.
     */
    Wide,

    /**
     * The options in a column beside the files, which show as tabs over the code.
     */
    Medium,

    /**
     * One scrolling column, the options folded into a disclosure over the file tabs and the code.
     */
    Narrow,

    ;

    /**
     * How wide the options column is, or null where the options fold into the column.
     */
    val optionsWidth: Dp?
        get() = when (this) {
            Wide -> 344.dp
            Medium -> 320.dp
            Narrow -> null
        }

    companion object {
        /**
         * The layout for a body [width] wide.
         */
        fun of(width: Dp): BodyLayout =
            when {
                width >= 1100.dp -> Wide
                width >= 720.dp -> Medium
                else -> Narrow
            }
    }
}

/**
 * The note that every export is compile checked and how many files and lines it holds, with the
 * copy and download buttons after it. They share a row where the sheet is wide enough, and the
 * buttons move under the note where it is not.
 *
 * @param[ready] The export, whose files and lines the note counts, or null for no count.
 */
@Composable
internal fun ExportFooter(
    materialKolorVersion: String?,
    ready: ExportOutcome.Ready?,
    buttons: @Composable () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val note: @Composable (Modifier) -> Unit = { noteModifier ->
        Row(
            modifier = noteModifier,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderIcon(id = IconId.Check, contentDescription = null, emphasis = Emphasis.Secondary)
            // The count follows the note, or goes under it where the two do not fit a line.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderText(
                    text = if (materialKolorVersion == null) {
                        stringResource(Res.string.export_checked_any)
                    } else {
                        stringResource(Res.string.export_checked, materialKolorVersion)
                    },
                    emphasis = Emphasis.Secondary,
                )
                if (ready != null) BuilderText(text = filesAndLines(ready), emphasis = Emphasis.Subtle)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= FOOTER_ROW_MIN_WIDTH) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                note(Modifier.weight(1f))
                buttons()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                note(Modifier)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) { buttons() }
            }
        }
    }
}

/**
 * How many files and lines [ready] holds, "5 files, 94 lines".
 */
@Composable
private fun filesAndLines(ready: ExportOutcome.Ready): String {
    val files = ready.files.size
    val lines = ready.files.sumOf { file -> file.lines.size }
    return stringResource(
        Res.string.export_files_lines,
        pluralStringResource(Res.plurals.export_files, files, files),
        pluralStringResource(Res.plurals.export_lines, lines, lines),
    )
}

/**
 * One line that needs attention, with its icon.
 */
@Composable
internal fun Notice(
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

/**
 * The narrowest the footer gets while its note and its buttons still share a row.
 */
private val FOOTER_ROW_MIN_WIDTH = 600.dp

/**
 * The export sheet's body inside its scroll, top to bottom with a gap between. With [fillLast] the
 * last child, the code, takes the height of [viewport] the others leave, never under
 * [CodeMinHeight], so it fills the sheet while there is room and the body scrolls as one once there
 * is not. The body keeps a focus ring's room above and below, where the scroll would clip it.
 *
 * @param[viewport] The height the scroll shows, in pixels.
 * @param[fillLast] Whether the last child fills what is left, false while there is no code.
 */
@Composable
internal fun FillLastColumn(
    viewport: Int,
    fillLast: Boolean,
    content: @Composable () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Layout(content) { measurables, constraints ->
        val gap = spacing.medium.roundToPx()
        val edge = spacing.extraSmall.roundToPx()
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val fill = if (fillLast) measurables.lastOrNull() else null
        val placed = measurables.filter { each -> each !== fill }.map { each -> each.measure(loose) }
        val gaps = gap * (measurables.size - 1).coerceAtLeast(0)
        val left = viewport - edge * 2 - gaps - placed.sumOf { each -> each.height }
        val last = fill?.let { code ->
            val height = maxOf(CodeMinHeight.roundToPx(), left)
            code.measure(loose.copy(minHeight = height, maxHeight = height))
        }
        val all = placed + listOfNotNull(last)
        val height = edge * 2 + gaps + all.sumOf { each -> each.height }
        layout(constraints.maxWidth, height) {
            var y = edge
            all.forEach { each ->
                each.placeRelative(0, y)
                y += each.height + gap
            }
        }
    }
}

/**
 * The shortest the code gets before the body scrolls instead of squeezing it further.
 */
private val CodeMinHeight: Dp = 240.dp
