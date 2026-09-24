package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.audit.AuditReason
import com.materialkolor.builder.engine.audit.AuditRow
import com.materialkolor.builder.engine.audit.AuditSuggestion
import com.materialkolor.builder.engine.audit.ContrastBadge
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.poster.ratioText
import com.materialkolor.builder.feature.poster.readoutName
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.audit_reason_shape_faint
import com.materialkolor.builder.generated.resources.audit_reason_text_only_large
import com.materialkolor.builder.generated.resources.audit_reason_text_unreadable
import com.materialkolor.builder.generated.resources.audit_suggest_change_pin
import com.materialkolor.builder.generated.resources.audit_suggest_change_seed
import com.materialkolor.builder.generated.resources.audit_suggest_move_accent_tones
import com.materialkolor.builder.generated.resources.audit_suggest_move_slot_tone
import com.materialkolor.builder.generated.resources.audit_suggest_raise_contrast
import com.materialkolor.builder.generated.resources.contrast_badge_aa
import com.materialkolor.builder.generated.resources.contrast_badge_aa_large
import com.materialkolor.builder.generated.resources.contrast_badge_aaa
import com.materialkolor.builder.generated.resources.contrast_badge_fail
import com.materialkolor.builder.generated.resources.tabs_all_pass
import com.materialkolor.builder.generated.resources.tabs_failures_only
import com.materialkolor.builder.generated.resources.tabs_pair
import com.materialkolor.builder.generated.resources.tabs_ratio
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderFilterChip
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Tags each pair row of the Contrast tab, for tests to count them. */
internal const val CONTRAST_ROW_TAG: String = "contrast-row"

/**
 * The Contrast tab, every pair the audit of [result] rates for its target in the modes [mode]
 * shows (F-24).
 *
 * A row shows both colors, the pair's names, its WCAG 2.2 ratio and its badge, and says why a
 * failing pair fails and what to try. Failures only hides the pairs that pass. While nothing
 * visible fails the tab says every pair passes, and with the filter on that note is all it shows.
 *
 * @param[result] The resolved theme the canvas shows.
 * @param[mode] Which modes to show, laid out like the Roles tab.
 * @param[filter] The vision filter the canvas is drawn through, or null for none.
 * @param[modifier] Applied to the tab.
 */
@Composable
internal fun ContrastTab(
    result: ThemeResult,
    mode: PreviewMode,
    filter: ColorMatrix?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    var failuresOnly by remember { mutableStateOf(false) }
    val rows = remember(result, mode) { result.audit.rows(mode) }
    val allPass = rows.all { row -> row.passes }
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.large, top = spacing.large, end = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            BuilderFilterChip(
                selected = failuresOnly,
                onSelectedChange = { on -> failuresOnly = on },
                label = stringResource(Res.string.tabs_failures_only),
            )
            if (allPass) BuilderText(stringResource(Res.string.tabs_all_pass))
        }
        if (!(allPass && failuresOnly)) {
            DataColumns(mode, filter, Modifier.weight(1f)) { isDark ->
                for (row in rows) {
                    if (row.isDark == isDark && (!failuresOnly || !row.passes)) PairRow(row, result.document)
                }
            }
        }
    }
}

/**
 * One rated pair as a single node for a screen reader, its colors as decorative swatches.
 */
@Composable
private fun PairRow(
    row: AuditRow,
    document: ThemeDocument,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val names = stringResource(
        Res.string.tabs_pair,
        row.pair.foreground.readoutName(document),
        row.pair.background.readoutName(document),
    )
    val ratio = stringResource(Res.string.tabs_ratio, ratioText(row.ratio))
    val badge = stringResource(row.badge.label)
    val reason = row.reason?.let { reason -> stringResource(reason.text) }
    val suggestion = row.suggestion?.let { suggestion -> stringResource(suggestion.text) }
    val description = listOfNotNull(
        listOf(names, ratio, badge).joinToString(", "),
        reason,
        suggestion,
    ).joinToString(" ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = description
                testTag = CONTRAST_ROW_TAG
            }.padding(vertical = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PairSwatch(row.foreground)
            PairSwatch(row.background)
            BuilderText(names, Modifier.weight(1f), style = BuilderTextStyle.Label)
            BuilderText(ratio, style = BuilderTextStyle.Value)
            BuilderBadge(label = badge, status = row.badge.status, icon = row.badge.icon)
        }
        if (reason != null) BuilderText(reason, emphasis = Emphasis.Secondary)
        if (suggestion != null) BuilderText(suggestion, emphasis = Emphasis.Secondary)
    }
}

/** A small square of [argb], only there to be seen. */
@Composable
private fun PairSwatch(argb: Argb) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small)
    Box(
        Modifier
            .size(tokens.iconSize)
            .clip(shape)
            .background(argb.toColor())
            .border(tokens.outlineWidth, tokens.border, shape),
    )
}

/** What the Contrast tab says for a reason, the string named by its key. */
internal val AuditReason.text: StringResource
    get() = when (this) {
        AuditReason.TextOnlyLarge -> Res.string.audit_reason_text_only_large
        AuditReason.TextUnreadable -> Res.string.audit_reason_text_unreadable
        AuditReason.ShapeFaint -> Res.string.audit_reason_shape_faint
    }

/** What the Contrast tab says for a suggestion, the string named by its key. */
internal val AuditSuggestion.text: StringResource
    get() = when (this) {
        AuditSuggestion.ChangePin -> Res.string.audit_suggest_change_pin
        AuditSuggestion.MoveAccentTones -> Res.string.audit_suggest_move_accent_tones
        AuditSuggestion.MoveSlotTone -> Res.string.audit_suggest_move_slot_tone
        AuditSuggestion.ChangeSeed -> Res.string.audit_suggest_change_seed
        AuditSuggestion.RaiseContrast -> Res.string.audit_suggest_raise_contrast
    }

private val ContrastBadge.label: StringResource
    get() = when (this) {
        ContrastBadge.Aaa -> Res.string.contrast_badge_aaa
        ContrastBadge.Aa -> Res.string.contrast_badge_aa
        ContrastBadge.AaLarge -> Res.string.contrast_badge_aa_large
        ContrastBadge.Fail -> Res.string.contrast_badge_fail
    }

private val ContrastBadge.status: BadgeStatus
    get() = when (this) {
        ContrastBadge.Aaa, ContrastBadge.Aa -> BadgeStatus.Success
        ContrastBadge.AaLarge -> BadgeStatus.Warning
        ContrastBadge.Fail -> BadgeStatus.Danger
    }

private val ContrastBadge.icon: IconId
    get() = when (this) {
        ContrastBadge.Aaa, ContrastBadge.Aa -> IconId.Check
        ContrastBadge.AaLarge -> IconId.Warning
        ContrastBadge.Fail -> IconId.Error
    }
