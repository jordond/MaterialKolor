package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.Reason
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.audit.AuditRow
import com.materialkolor.builder.engine.audit.ContrastBadge
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.contrast_badge_aa
import com.materialkolor.builder.generated.resources.contrast_badge_aa_large
import com.materialkolor.builder.generated.resources.contrast_badge_aaa
import com.materialkolor.builder.generated.resources.contrast_badge_fail
import com.materialkolor.builder.generated.resources.contrast_high
import com.materialkolor.builder.generated.resources.contrast_label
import com.materialkolor.builder.generated.resources.contrast_level
import com.materialkolor.builder.generated.resources.contrast_medium
import com.materialkolor.builder.generated.resources.contrast_pair_dark
import com.materialkolor.builder.generated.resources.contrast_pair_light
import com.materialkolor.builder.generated.resources.contrast_readout
import com.materialkolor.builder.generated.resources.contrast_readout_spoken
import com.materialkolor.builder.generated.resources.contrast_readout_tooltip
import com.materialkolor.builder.generated.resources.contrast_reduced
import com.materialkolor.builder.generated.resources.contrast_standard
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor

/**
 * The contrast level with the lowest text pair it leaves.
 *
 * Contrast is one of the four levels the library names, offered as one choice on a single row that
 * fills the poster's width. A pick is one discrete edit. The arrow keys only move the focus, and
 * Enter or Space picks, since each pick is a new scheme. The lowest ratio and its grade sit on the
 * right of the label, and the pair they belong to is in the readout's tooltip and spoken name. The
 * readout rates the target's own pairs in the modes the preview shows, and a grade short of AA
 * carries a glyph as well as its words. A target that ignores contrast takes no pick, and says why
 * under the Contrast info button.
 */
@Composable
internal fun ContrastSection(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    details: Boolean = true,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val selected = ContrastStop.of(context.document.contrast)
    val state = context.capabilities[Control.Contrast]
    val labels = ContrastStop.entries.associateWith { stop -> stringResource(stop.label) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        if (details) ContrastHeader(context)
        BuilderSegmented(
            options = ContrastStop.entries,
            selected = selected,
            onSelect = { stop ->
                if (stop != selected) {
                    dispatcher.dispatch(
                        WorkspaceAction.Edit(DocumentChange.SetContrast(stop.level), EditPhase.Discrete),
                    )
                }
            },
            label = stringResource(Res.string.contrast_level),
            modifier = Modifier.fillMaxWidth(),
            enabled = state.usable,
            selectOnFocus = false,
            compact = true,
        ) { stop -> labels.getValue(stop) }
    }
}

/**
 * What the contrast levels leave, for the phone sheet, which shows the levels alone at its peek and
 * this under them once it is dragged up. The label with its info button and the lowest ratio.
 */
@Composable
internal fun ContrastDetails(
    context: PosterContext,
    modifier: Modifier = Modifier,
) {
    ContrastHeader(context, modifier)
}

/**
 * The Contrast label with its info button, and the lowest ratio with its grade on the right in line
 * with it. Why the target treats contrast differently, if it does, opens with the explanation.
 */
@Composable
private fun ContrastHeader(
    context: PosterContext,
    modifier: Modifier = Modifier,
) {
    val row = rememberLowestPair(context)
    ReasonInfoLabel(
        label = stringResource(Res.string.contrast_label),
        topic = InfoTopic.Contrast,
        reason = context.capabilities[Control.Contrast].explanation,
        modifier = modifier,
    ) {
        LowestRatio(row, context.result.document, Modifier.weight(1f))
    }
}

/**
 * [InfoLabel] for a section the target treats differently. The [reason] opens under the section's
 * explanation rather than standing under the section, so the skins that give one still fit the
 * poster down to its Fine-tune button. The info button still offers it, and it reads out with the
 * explanation once open. Without a reason this is [InfoLabel] itself.
 */
@Composable
internal fun ReasonInfoLabel(
    label: String,
    topic: InfoTopic,
    reason: Reason?,
    modifier: Modifier = Modifier,
    end: @Composable RowScope.() -> Unit = {},
) {
    if (reason == null) {
        InfoLabel(label = label, topic = topic, modifier = modifier, end = end)
        return
    }
    val spacing = LocalBuilderTokens.current.spacing
    var open by rememberSaveable(topic) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(label)
            InfoButton(topic = topic, expanded = open, onClick = { open = !open })
            end()
        }
        if (open) {
            InfoNote(topic)
            ReasonLine(reason)
        }
    }
}

/**
 * The text pair with the lowest ratio in the modes the preview shows.
 */
@Composable
private fun rememberLowestPair(context: PosterContext): AuditRow =
    remember(context.result, context.visibleModes) { context.result.audit.lowestPair(context.visibleModes) }

/**
 * The lowest ratio any text pair has in the modes the preview shows, with the grade it earns, as
 * one plain line on the right of the Contrast label, "5.0:1 AA". A grade short of AA leads with a
 * warning glyph, so it never rests on its words alone. The pair behind the ratio shows in the
 * tooltip and is read out with it, as in "Lowest text pair 5.0:1, AA, inversePrimary on
 * inverseSurface, in dark".
 */
@Composable
private fun LowestRatio(
    row: AuditRow,
    document: ThemeDocument,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val ratio = ratioText(row.ratio)
    val grade = stringResource(row.badge.label)
    val pair = stringResource(
        if (row.isDark) Res.string.contrast_pair_dark else Res.string.contrast_pair_light,
        row.pair.foreground.readoutName(document),
        row.pair.background.readoutName(document),
    )
    val spoken = stringResource(Res.string.contrast_readout_spoken, ratio, grade, pair)
    Box(modifier, contentAlignment = Alignment.CenterEnd) {
        BuilderTooltip(text = stringResource(Res.string.contrast_readout_tooltip, pair)) {
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.badge.warning?.let { glyph ->
                    BuilderIcon(glyph, contentDescription = null, size = ReadoutGlyphSize)
                }
                BuilderText(
                    text = stringResource(Res.string.contrast_readout, ratio, grade),
                    style = BuilderTextStyle.Value,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The size of the glyph before a grade short of AA, the height of the readout's text.
 */
private val ReadoutGlyphSize = 14.dp

/**
 * The four named contrast levels, in the order the choice offers them.
 *
 * @property[level] Where the level sits.
 * @property[label] What the level is called.
 */
internal enum class ContrastStop(
    val level: ContrastLevel,
    val label: StringResource,
) {
    Reduced(ContrastLevel.Reduced, Res.string.contrast_reduced),
    Standard(ContrastLevel.Standard, Res.string.contrast_standard),
    Medium(ContrastLevel.Medium, Res.string.contrast_medium),
    High(ContrastLevel.High, Res.string.contrast_high),
    ;

    companion object {
        /**
         * The named level nearest [level], which is [level] itself in any document.
         */
        fun of(level: ContrastLevel): ContrastStop {
            val nearest = level.snapped()
            return entries.first { stop -> stop.level == nearest }
        }
    }
}

/**
 * A ratio to one decimal, cut rather than rounded so a pair just under a line never reads as on it.
 */
internal fun ratioText(ratio: Double): String {
    val tenths = floor(ratio * TENTHS).toInt()
    return "${tenths / TENTHS}.${tenths % TENTHS}"
}

/**
 * What the readout calls the color [this] names, the way the roles are named in code.
 */
internal fun ColorRef.readoutName(document: ThemeDocument): String =
    when (this) {
        is ColorRef.OfRole -> {
            role.name.lowerFirst()
        }
        is ColorRef.OfAccent -> {
            val accent = document.accents
                .getOrNull(slot.index)
                ?.name
                .orEmpty()
            "$accent ${slot.part.name.lowerFirst()}".trim()
        }
        is ColorRef.OfSlot -> {
            slot.name.lowerFirst()
        }
        is ColorRef.OfFluentText -> {
            text.name.lowerFirst()
        }
        is ColorRef.OfFluentShade -> {
            shade.name.lowerFirst()
        }
    }

private fun String.lowerFirst(): String = replaceFirstChar { char -> char.lowercaseChar() }

private val ContrastBadge.label: StringResource
    get() = when (this) {
        ContrastBadge.Aaa -> Res.string.contrast_badge_aaa
        ContrastBadge.Aa -> Res.string.contrast_badge_aa
        ContrastBadge.AaLarge -> Res.string.contrast_badge_aa_large
        ContrastBadge.Fail -> Res.string.contrast_badge_fail
    }

/**
 * The glyph a grade short of AA leads with, or null for a grade that passes.
 */
private val ContrastBadge.warning: IconId?
    get() = when (this) {
        ContrastBadge.Aaa, ContrastBadge.Aa -> null
        ContrastBadge.AaLarge -> IconId.Warning
        ContrastBadge.Fail -> IconId.Error
    }

private const val TENTHS = 10
