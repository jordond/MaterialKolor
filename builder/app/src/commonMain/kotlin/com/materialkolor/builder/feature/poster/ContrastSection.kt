package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.audit.ContrastBadge
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.contrast_badge_aa
import com.materialkolor.builder.generated.resources.contrast_badge_aa_large
import com.materialkolor.builder.generated.resources.contrast_badge_aaa
import com.materialkolor.builder.generated.resources.contrast_badge_fail
import com.materialkolor.builder.generated.resources.contrast_field
import com.materialkolor.builder.generated.resources.contrast_field_error
import com.materialkolor.builder.generated.resources.contrast_high
import com.materialkolor.builder.generated.resources.contrast_label
import com.materialkolor.builder.generated.resources.contrast_lowest_dark
import com.materialkolor.builder.generated.resources.contrast_lowest_light
import com.materialkolor.builder.generated.resources.contrast_medium
import com.materialkolor.builder.generated.resources.contrast_reduced
import com.materialkolor.builder.generated.resources.contrast_slider
import com.materialkolor.builder.generated.resources.contrast_standard
import com.materialkolor.builder.generated.resources.contrast_state
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderSlider
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The contrast level with the lowest text pair it leaves (F-12).
 *
 * The slider runs from -1 to 1 and a drag snaps onto a named stop it lands near. A drag moves the
 * document every frame and lets go as one undo entry, and so does each arrow press. The field takes
 * any level typed into it. The readout rates the target's own pairs in the modes the preview shows,
 * and its badge carries an icon as well as its words.
 */
@Composable
internal fun ContrastSection(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val level = context.document.contrast
    val drag = remember { PendingLevel() }
    val valueText = ContrastScale.format(level)
    val stopName = ContrastStop.of(level)?.let { stop -> stringResource(stop.label) }
    val fieldError = stringResource(Res.string.contrast_field_error)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        InfoLabel(label = stringResource(Res.string.contrast_label), topic = InfoTopic.Contrast)
        BuilderSlider(
            value = ContrastScale.valueOf(level),
            onValueChange = { value ->
                val next = ContrastScale.levelOf(value)
                drag.level = next
                dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetContrast(next), EditPhase.Dragging))
            },
            label = stringResource(Res.string.contrast_slider),
            modifier = Modifier.fillMaxWidth(),
            onValueChangeFinished = {
                drag.level?.let { last ->
                    drag.level = null
                    dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetContrast(last), EditPhase.Released))
                }
            },
            valueRange = ContrastScale.Range,
            stops = ContrastScale.Stops,
            snapDistance = ContrastScale.SNAP_DISTANCE,
            stateDescription =
                stopName?.let { name -> stringResource(Res.string.contrast_state, valueText, name) } ?: valueText,
        )
        BuilderTextField(
            value = valueText,
            onCommit = { text ->
                ContrastScale.parse(text)?.let { typed ->
                    dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetContrast(typed), EditPhase.Discrete))
                }
            },
            label = stringResource(Res.string.contrast_field),
            modifier = Modifier.fillMaxWidth(),
            error = { text -> if (ContrastScale.parse(text) == null) fieldError else null },
            supportingText = stopName,
        )
        LowestPairReadout(context)
    }
}

/**
 * The text pair with the lowest ratio in the modes the preview shows, with the badge it earns.
 */
@Composable
private fun LowestPairReadout(context: PosterContext) {
    val spacing = LocalBuilderTokens.current.spacing
    val row = remember(context.result, context.visibleModes) { context.result.audit.lowestPair(context.visibleModes) }
    val document = context.result.document
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        BuilderText(
            text = stringResource(
                if (row.isDark) Res.string.contrast_lowest_dark else Res.string.contrast_lowest_light,
                row.pair.foreground.readoutName(document),
                row.pair.background.readoutName(document),
                ratioText(row.ratio),
            ),
            style = BuilderTextStyle.Value,
        )
        BuilderBadge(
            label = stringResource(row.badge.label),
            status = row.badge.status,
            icon = row.badge.icon,
        )
    }
}

/** The last level a drag reported, which its release lands on. Only the release reads it. */
private class PendingLevel {
    var level: ContrastLevel? = null
}

/**
 * The four named contrast levels the slider snaps to.
 *
 * @property[level] Where the stop sits.
 * @property[label] What the stop is called.
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
        /** The stop [level] sits exactly on, or null between stops. */
        fun of(level: ContrastLevel): ContrastStop? = entries.firstOrNull { stop -> stop.level == level }
    }
}

/**
 * How the contrast control turns the slider's floats and typed text into a [ContrastLevel] and back.
 */
internal object ContrastScale {
    /** How close a drag has to land to a stop to snap onto it. */
    const val SNAP_DISTANCE: Float = 0.04f

    /** The values the slider covers. */
    val Range: ClosedFloatingPointRange<Float> = -1f..1f

    /** The named stops as slider values, in slider order. */
    val Stops: List<Float> = ContrastStop.entries.map { stop -> valueOf(stop.level) }

    /** [level] as a slider value. */
    fun valueOf(level: ContrastLevel): Float = level.hundredths / HUNDREDTHS

    /** The level nearest the slider [value], in whole hundredths. */
    fun levelOf(value: Float): ContrastLevel =
        ContrastLevel((value * HUNDREDTHS).roundToInt().coerceIn(-MAX_HUNDREDTHS, MAX_HUNDREDTHS))

    /** [level] as the field shows it, to two decimals. */
    fun format(level: ContrastLevel): String {
        val sign = if (level.hundredths < 0) "-" else ""
        val whole = abs(level.hundredths) / MAX_HUNDREDTHS
        val fraction = (abs(level.hundredths) % MAX_HUNDREDTHS).toString().padStart(2, '0')
        return "$sign$whole.$fraction"
    }

    /**
     * The level typed as [text], any number from -1 to 1 with a point or a comma, rounded to
     * hundredths. Null when it is not one.
     */
    fun parse(text: String): ContrastLevel? {
        val number = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (number.isNaN() || number < -1.0 || number > 1.0) return null
        return ContrastLevel((number * MAX_HUNDREDTHS).roundToInt())
    }

    private const val HUNDREDTHS = 100f
    private const val MAX_HUNDREDTHS = 100
}

/** A ratio to one decimal, cut rather than rounded so a pair just under a line never reads as on it. */
internal fun ratioText(ratio: Double): String {
    val tenths = floor(ratio * TENTHS).toInt()
    return "${tenths / TENTHS}.${tenths % TENTHS}"
}

/** What the readout calls the color [this] names, the way the roles are named in code. */
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
