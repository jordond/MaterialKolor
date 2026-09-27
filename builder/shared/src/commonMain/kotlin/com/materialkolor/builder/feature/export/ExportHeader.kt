package com.materialkolor.builder.feature.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.poster.specName
import com.materialkolor.builder.feature.poster.styleDisplayName
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_from
import com.materialkolor.builder.generated.resources.export_from_unnamed
import com.materialkolor.builder.generated.resources.export_heading
import com.materialkolor.builder.generated.resources.export_heading_named
import com.materialkolor.builder.generated.resources.export_package_prefix
import com.materialkolor.builder.generated.resources.export_style_spec
import com.materialkolor.builder.generated.resources.finetune_summary_spec
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import com.materialkolor.builder.kit.widget.SchemeChip
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * The three colours of the header's scheme glyph, as a scheme chip draws them.
 */
@Immutable
internal class SchemeGlyph(
    val primary: Color,
    val secondaryContainer: Color,
    val tertiaryContainer: Color,
)

/**
 * The sheet's header, the scheme glyph beside what is exported and where it comes from.
 *
 * Wide enough, it reads as a sentence to edit. "Export" and the theme name field make the heading,
 * the package field sits on the line under it with the seed, the style and the spec. [compact], on a phone, the heading is "Export AppTheme"
 * with where it comes from under it, and the two fields fold into Options instead.
 *
 * The sheet's title names it to assistive tech, so "Export" stays out of the semantics.
 *
 * @param[glyph] The scheme glyph's colours, or null for no glyph.
 * @param[drafts] Where the fields say what is wrong with their drafts.
 * @param[compact] Whether the fields fold into Options, as the phone's layout has it.
 */
@Composable
internal fun RowScope.ExportHeader(
    state: ExportModel.State,
    glyph: SchemeGlyph?,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
    drafts: DraftProblems,
    compact: Boolean,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            SchemeChip(
                primary = glyph.primary,
                secondaryContainer = glyph.secondaryContainer,
                tertiaryContainer = glyph.tertiaryContainer,
            )
        }
        if (compact) {
            CompactHeading(state, Modifier.weight(1f))
        } else {
            WideHeading(state, dispatcher, workspace, drafts, Modifier.weight(1f))
        }
    }
}

/**
 * "Export" and the theme name field, then the package field and where the theme comes from.
 */
@Composable
private fun WideHeading(
    state: ExportModel.State,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
    drafts: DraftProblems,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val type = LocalBuilderType.current
    val heading = headingType(HeadingSize, HeadingLine, HeadingTracking)
    val code = type.code.merge(fontSize = 13.sp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = stringResource(Res.string.export_heading),
                modifier = Modifier.clearAndSetSemantics {},
                style = heading.merge(color = tokens.textStrong),
                maxLines = 1,
            )
            ThemeNameField(
                state = state,
                workspace = workspace,
                drafts = drafts,
                look = FieldLook.Inline(heading, tokens.accent),
                modifier = Modifier.widthIn(max = ThemeNameMaxWidth),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = stringResource(Res.string.export_package_prefix),
                    modifier = Modifier.clearAndSetSemantics {},
                    style = code.merge(color = tokens.textMuted),
                )
                PackageField(
                    state = state,
                    dispatcher = dispatcher,
                    drafts = drafts,
                    look = FieldLook.Inline(code),
                    modifier = Modifier.widthIn(max = PackageMaxWidth),
                )
            }
            Dot()
            Source(state)
            Dot()
            BuilderText(text = styleAndSpec(state), emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * "Export AppTheme" over where it comes from, for a phone.
 */
@Composable
private fun CompactHeading(
    state: ExportModel.State,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    Column(modifier) {
        BasicText(
            text = stringResource(Res.string.export_heading_named, state.document.themeName),
            modifier = Modifier.clearAndSetSemantics {},
            style = headingType(CompactSize, CompactLine, CompactTracking).merge(color = tokens.textStrong),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Source(state)
    }
}

/**
 * Where the theme comes from, "from Terracotta", the seed's swatch and its hex.
 */
@Composable
private fun Source(state: ExportModel.State) {
    val tokens = LocalBuilderTokens.current
    val seed = state.document.seed
    val project = state.projectName
    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small - SwatchGapTrim),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(
            text = if (project.isBlank()) {
                stringResource(Res.string.export_from_unnamed)
            } else {
                stringResource(Res.string.export_from, project)
            },
            emphasis = Emphasis.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .size(SwatchSize)
                .background(seed.toColor(), RoundedCornerShape(SwatchRadius)),
        )
        BuilderText(text = seed.toHex(), style = BuilderTextStyle.Value)
    }
}

/**
 * The style and the spec the export builds its scheme with, "Tonal Spot, 2026 spec".
 */
@Composable
private fun styleAndSpec(state: ExportModel.State): String {
    val targeted = state.document.forTarget(state.target)
    val style = stringResource(styleDisplayName(targeted.style))
    val spec = stringResource(
        Res.string.finetune_summary_spec,
        stringResource(specName(EffectiveSpec.of(targeted.style, targeted.spec))),
    )
    return stringResource(Res.string.export_style_spec, style, spec)
}

/**
 * The quiet dot between the parts of a line, such as the header's second line.
 */
@Composable
internal fun Dot() {
    BuilderText(
        text = DOT,
        modifier = Modifier.clearAndSetSemantics {},
        emphasis = Emphasis.Subtle,
    )
}

/**
 * The header's heading type, the title's face at [size] and heavier.
 */
@Composable
private fun headingType(
    size: Float,
    line: Float,
    tracking: Float,
): TextStyle =
    LocalBuilderType.current.title.merge(
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = HeadingWeight,
        letterSpacing = tracking.sp,
    )

private const val DOT = "·"
private const val HeadingSize = 28f
private const val HeadingLine = 36f
private const val HeadingTracking = -0.6f
private const val CompactSize = 20f
private const val CompactLine = 24f
private const val CompactTracking = -0.3f
private val HeadingWeight = FontWeight(750)

/**
 * The widest the theme name and the package fields get, so a long name leaves the rest of the
 * header its room.
 */
private val ThemeNameMaxWidth: Dp = 360.dp
private val PackageMaxWidth: Dp = 320.dp

/**
 * The seed's swatch beside its hex.
 */
private val SwatchSize: Dp = 14.dp
private val SwatchRadius: Dp = 4.dp

/**
 * How much closer the swatch sits to the words beside it than the parts of the line sit.
 */
private val SwatchGapTrim: Dp = 2.dp
