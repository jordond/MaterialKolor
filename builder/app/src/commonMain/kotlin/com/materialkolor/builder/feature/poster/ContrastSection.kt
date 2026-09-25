package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.capability.Control
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
import com.materialkolor.builder.generated.resources.contrast_lowest
import com.materialkolor.builder.generated.resources.contrast_medium
import com.materialkolor.builder.generated.resources.contrast_pair_dark
import com.materialkolor.builder.generated.resources.contrast_pair_light
import com.materialkolor.builder.generated.resources.contrast_ratio
import com.materialkolor.builder.generated.resources.contrast_reduced
import com.materialkolor.builder.generated.resources.contrast_standard
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor

/**
 * The contrast level with the lowest text pair it leaves (F-12).
 *
 * Contrast is one of the four levels the library names, offered as one choice (D53) on a single
 * row that fills the poster's width. A pick is one discrete edit. The arrow keys only move the
 * focus, and Enter or Space picks, since each pick is a new scheme. The lowest ratio and its badge
 * sit on the right of the label, and the pair it belongs to on a line under the levels. The readout
 * rates the target's own pairs in the modes the preview shows, and its badge carries an icon as
 * well as its words. A target that ignores contrast says why and takes no pick.
 */
@Composable
internal fun ContrastSection(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    details: Boolean = true, // b-510
) {
    val spacing = LocalBuilderTokens.current.spacing
    val selected = ContrastStop.of(context.document.contrast)
    val state = context.capabilities[Control.Contrast]
    val labels = ContrastStop.entries.associateWith { stop -> stringResource(stop.label) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        // b-510
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
        if (details) ContrastNotes(context)
    }
}

// b-510

/**
 * What the contrast levels leave, for the phone sheet, which shows the levels alone at its peek and
 * this under them once it is dragged up. The label with its info button and the lowest ratio, the
 * pair behind it, and why a target ignores contrast if it does.
 */
@Composable
internal fun ContrastDetails(
    context: PosterContext,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        ContrastHeader(context)
        ContrastNotes(context)
    }
}

/**
 * The Contrast label with its info button, and the lowest ratio on the right in line with it.
 */
@Composable
private fun ContrastHeader(context: PosterContext) {
    val spacing = LocalBuilderTokens.current.spacing
    val row = rememberLowestPair(context)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        InfoLabel(
            label = stringResource(Res.string.contrast_label),
            topic = InfoTopic.Contrast,
            modifier = Modifier.alignByBaseline(),
        )
        LowestRatio(row, Modifier.weight(1f).alignByBaseline())
    }
}

/**
 * The pair behind the lowest ratio, and why the target ignores contrast if it does.
 */
@Composable
private fun ContrastNotes(context: PosterContext) {
    LowestPair(rememberLowestPair(context), context.result.document)
    context.capabilities[Control.Contrast].explanation?.let { reason -> ReasonLine(reason) }
}

/**
 * The text pair with the lowest ratio in the modes the preview shows.
 */
@Composable
private fun rememberLowestPair(context: PosterContext): AuditRow =
    remember(context.result, context.visibleModes) { context.result.audit.lowestPair(context.visibleModes) }

/**
 * The lowest ratio any text pair has in the modes the preview shows, with the badge it earns, as
 * one line on the right of the Contrast label. Where the line runs short its words give way first,
 * and the ratio and the badge always show.
 */
@Composable
private fun LowestRatio(
    row: AuditRow,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(
            text = stringResource(Res.string.contrast_lowest),
            modifier = Modifier.weight(1f, fill = false),
            style = BuilderTextStyle.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BuilderText(
            text = stringResource(Res.string.contrast_ratio, ratioText(row.ratio)),
            style = BuilderTextStyle.Value,
            maxLines = 1,
        )
        BuilderBadge(
            label = stringResource(row.badge.label),
            status = row.badge.status,
            icon = row.badge.icon,
        )
    }
}

/**
 * The pair behind the lowest ratio and the mode it is lowest in, one small line.
 */
@Composable
private fun LowestPair(
    row: AuditRow,
    document: ThemeDocument,
) {
    BuilderText(
        text = stringResource(
            if (row.isDark) Res.string.contrast_pair_dark else Res.string.contrast_pair_light,
            row.pair.foreground.readoutName(document),
            row.pair.background.readoutName(document),
        ),
        style = BuilderTextStyle.Label,
        emphasis = Emphasis.Secondary,
    )
}

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
         * The named level nearest [level], which is [level] itself in any document (D53).
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

private const val TENTHS = 10
