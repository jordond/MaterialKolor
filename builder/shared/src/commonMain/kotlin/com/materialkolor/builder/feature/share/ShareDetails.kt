package com.materialkolor.builder.feature.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.validate.ProjectNameProblem
import com.materialkolor.builder.domain.validate.projectNameProblem
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.poster.specName
import com.materialkolor.builder.feature.poster.styleName
import com.materialkolor.builder.feature.projects.targetLabel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.projects_name_empty
import com.materialkolor.builder.generated.resources.projects_name_long
import com.materialkolor.builder.generated.resources.share_copy
import com.materialkolor.builder.generated.resources.share_detail_accents
import com.materialkolor.builder.generated.resources.share_detail_extras
import com.materialkolor.builder.generated.resources.share_detail_extras_none
import com.materialkolor.builder.generated.resources.share_detail_for
import com.materialkolor.builder.generated.resources.share_detail_key_colors
import com.materialkolor.builder.generated.resources.share_detail_seed
import com.materialkolor.builder.generated.resources.share_detail_style
import com.materialkolor.builder.generated.resources.share_detail_style_value
import com.materialkolor.builder.generated.resources.share_link_label
import com.materialkolor.builder.generated.resources.share_link_name
import com.materialkolor.builder.generated.resources.share_name_help
import com.materialkolor.builder.generated.resources.share_name_label
import com.materialkolor.builder.generated.resources.share_name_not_saved
import com.materialkolor.builder.generated.resources.share_name_transient
import com.materialkolor.builder.generated.resources.share_send
import com.materialkolor.builder.generated.resources.share_transient_save
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import com.materialkolor.builder.kit.widget.SelectableText
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How wide the labels of the list of what the link carries are.
 */
private val DetailLabelWidth: Dp = 96.dp

/**
 * How big a color swatch in the list is.
 */
private val SwatchSize: Dp = 18.dp

/**
 * The most lines of the link that show before it scrolls inside its box.
 */
private const val LINK_MAX_LINES = 4

/**
 * The project name as the share dialog shows it, and what it can do with it.
 *
 * @property[value] The project's name as it is saved.
 * @property[transient] Whether the project came from a link and is not saved yet, so it cannot be
 *   renamed.
 * @property[notSaved] Whether the last rename did not land.
 * @property[onDraftChange] Called with the field's text each time someone changes it.
 * @property[onCommit] Called with a name the field commits.
 * @property[onSaveToProjects] Saves a project from a link to the drawer.
 */
internal class ShareName(
    val value: String,
    val transient: Boolean,
    val notSaved: Boolean,
    val onDraftChange: (String) -> Unit,
    val onCommit: (String) -> Unit,
    val onSaveToProjects: () -> Unit,
)

/**
 * The details side of the share dialog, top down, the sentence on what a link is, the project
 * name, what the link carries and the whole link.
 */
@Composable
internal fun ShareDetails(
    link: String,
    document: ThemeDocument,
    name: ShareName,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        BuilderText(text = stringResource(Res.string.share_body), emphasis = Emphasis.Secondary)
        NameSection(name)
        DetailList(document)
        LinkBox(link)
    }
}

/**
 * The project name, a field that renames the project, or for a project from a link its name with
 * a way to save it first.
 */
@Composable
private fun NameSection(name: ShareName) {
    val spacing = LocalBuilderTokens.current.spacing
    if (!name.transient) {
        val empty = stringResource(Res.string.projects_name_empty)
        val long = stringResource(Res.string.projects_name_long)
        BuilderTextField(
            value = name.value,
            onCommit = name.onCommit,
            label = stringResource(Res.string.share_name_label),
            modifier = Modifier.fillMaxWidth(),
            error = { draft ->
                when (projectNameProblem(draft)) {
                    ProjectNameProblem.Blank -> empty
                    ProjectNameProblem.TooLong -> long
                    null -> null
                }
            },
            supportingText = stringResource(
                if (name.notSaved) Res.string.share_name_not_saved else Res.string.share_name_help,
            ),
            onDraftChange = name.onDraftChange,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderText(text = stringResource(Res.string.share_name_label), style = BuilderTextStyle.Label)
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderText(text = name.value, modifier = Modifier.weight(1f), style = BuilderTextStyle.Value)
            BuilderButton(
                onClick = name.onSaveToProjects,
                label = stringResource(Res.string.share_transient_save),
                emphasis = Emphasis.Secondary,
                icon = IconId.Folder,
            )
        }
        BuilderText(text = stringResource(Res.string.share_name_transient), emphasis = Emphasis.Secondary)
    }
}

/**
 * What the link carries, a muted label on the left of each value.
 */
@Composable
private fun DetailList(document: ThemeDocument) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        DetailRow(stringResource(Res.string.share_detail_seed)) {
            Swatch(document.seed)
            BuilderText(text = document.seed.toHex(), style = BuilderTextStyle.Code)
        }
        DetailRow(stringResource(Res.string.share_detail_style)) {
            BuilderText(
                text = stringResource(
                    Res.string.share_detail_style_value,
                    stringResource(styleName(document.style)),
                    stringResource(specName(EffectiveSpec.of(document.style, document.spec))),
                ),
            )
        }
        DetailRow(stringResource(Res.string.share_detail_for)) {
            BuilderText(text = targetLabel(ExportTarget.of(document.library, document.expressive)))
        }
        DetailRow(stringResource(Res.string.share_detail_extras)) {
            val extras = extraColors(document)
            extras.forEach { color -> Swatch(color) }
            BuilderText(text = extrasText(document), emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * One row of the list, [label] in its column and [value] beside it.
 */
@Composable
private fun DetailRow(
    label: String,
    value: @Composable () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(text = label, modifier = Modifier.width(DetailLabelWidth), emphasis = Emphasis.Secondary)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) { value() }
    }
}

/**
 * A small rounded square of [color]. The text beside it says what it is.
 */
@Composable
private fun Swatch(color: Argb) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small / 2)
    Box(
        Modifier
            .size(SwatchSize)
            .background(color.toColor(), shape)
            .border(tokens.outlineWidth, tokens.border, shape),
    )
}

/**
 * The colors [document] carries past its seed, in the order the Worker draws them on the card,
 * the key colors set by hand, the tertiary seed of the Cmf style, then each accent's seed.
 */
internal fun extraColors(document: ThemeDocument): List<Argb> =
    KeyColor.entries.sortedBy { slot -> slot.code }.mapNotNull { slot -> document.keyColors[slot] } +
        listOfNotNull(document.cmfTertiarySeed) +
        document.accents.map { accent -> accent.seed }

/**
 * How many extra colors [document] carries, such as "2 key colors, 3 accents", or "None". The
 * tertiary seed of the Cmf style counts as a key color.
 */
@Composable
private fun extrasText(document: ThemeDocument): String {
    val keyColors = KeyColor.entries.count { slot -> document.keyColors[slot] != null } +
        (if (document.cmfTertiarySeed != null) 1 else 0)
    val accents = document.accents.size
    val parts = buildList {
        if (keyColors > 0) add(pluralStringResource(Res.plurals.share_detail_key_colors, keyColors, keyColors))
        if (accents > 0) add(pluralStringResource(Res.plurals.share_detail_accents, accents, accents))
    }
    return if (parts.isEmpty()) stringResource(Res.string.share_detail_extras_none) else parts.joinToString(", ")
}

/**
 * The whole link under its label, wrapped in an outlined box that grows to [LINK_MAX_LINES] lines
 * and scrolls inside past that.
 */
@Composable
private fun LinkBox(link: String) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small)
    val lineHeight = with(LocalDensity.current) { LocalBuilderType.current.code.lineHeight.toDp() }
    val padding = tokens.spacing.medium
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
        BuilderText(text = stringResource(Res.string.share_link_label), style = BuilderTextStyle.Label)
        BuilderScrollArea(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = lineHeight * LINK_MAX_LINES + padding * 2)
                .border(tokens.outlineWidth, tokens.borderStrong, shape),
            fitContent = true,
        ) {
            SelectableText(
                text = link,
                modifier = Modifier.fillMaxWidth().padding(padding),
                style = BuilderTextStyle.Code,
                label = stringResource(Res.string.share_link_name),
                singleLine = false,
            )
        }
    }
}

/**
 * Copy link, full width, or where the share sheet is Copy link and Share side by side at equal
 * widths with Share the primary one.
 *
 * @param[copyFocus] Given to Copy link, which takes focus as the dialog opens.
 */
@Composable
internal fun ShareButtons(
    sharesToSheet: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    copyFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val copyLabel = stringResource(Res.string.share_copy)
    if (!sharesToSheet) {
        BuilderButton(
            onClick = onCopy,
            label = copyLabel,
            modifier = modifier.fillMaxWidth().focusRequester(copyFocus),
            emphasis = Emphasis.Primary,
            icon = IconId.Copy,
        )
        return
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small)) {
        BuilderButton(
            onClick = onCopy,
            label = copyLabel,
            modifier = Modifier.weight(1f).focusRequester(copyFocus),
            emphasis = Emphasis.Secondary,
            icon = IconId.Copy,
        )
        BuilderButton(
            onClick = onShare,
            label = stringResource(Res.string.share_send),
            modifier = Modifier.weight(1f),
            emphasis = Emphasis.Primary,
            icon = IconId.Share,
        )
    }
}
