package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.audit.AuditRow
import com.materialkolor.builder.engine.audit.ContrastAudit
import com.materialkolor.builder.engine.audit.ContrastBadge
import com.materialkolor.builder.engine.audit.rate
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.generated.resources.Res
import com.materialkolor.builder.preview.generated.resources.inspect_badge_aa
import com.materialkolor.builder.preview.generated.resources.inspect_badge_aa_large
import com.materialkolor.builder.preview.generated.resources.inspect_badge_aaa
import com.materialkolor.builder.preview.generated.resources.inspect_badge_fail
import com.materialkolor.builder.preview.generated.resources.inspect_jump_to_key_color
import com.materialkolor.builder.preview.generated.resources.inspect_mode_dark
import com.materialkolor.builder.preview.generated.resources.inspect_mode_light
import com.materialkolor.builder.preview.generated.resources.inspect_pin_role
import com.materialkolor.builder.preview.generated.resources.inspect_ratio
import com.materialkolor.builder.preview.generated.resources.inspect_show_on_ramp
import com.materialkolor.builder.preview.generated.resources.inspect_tone
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The Inspect card for [target]. It names the mode, then each color the element declared with its
 * hex and tone, then the ratio and badge of the first pair the audit rates, or of its first two
 * colors when the audit rates none. Pinned, it adds the
 * actions for the first color.
 */
@Composable
internal fun InspectCard(
    target: InspectTarget,
    pinned: Boolean,
    result: ThemeResult,
    actions: InspectActions,
    modifier: Modifier = Modifier,
) {
    val refs = target.entry.roles
    val isDark = target.isDark
    val colors = remember(result, refs, isDark) { refs.map { ref -> result.inspectColor(ref, isDark) } }
    // b-308b
    val rated = remember(result, refs, isDark) {
        result.audit.firstRated(refs, isDark)
            ?: result.rateFirstTwo(refs, isDark)
    }
    BuilderCard(modifier.testTag(INSPECT_CARD_TAG)) {
        BuilderText(
            text = stringResource(if (isDark) Res.string.inspect_mode_dark else Res.string.inspect_mode_light),
            style = BuilderTextStyle.SectionLabel,
        )
        for (color in colors) ColorLine(color)
        if (rated != null) RatingLine(rated)
        val first = refs.firstOrNull()
        if (pinned && first != null) CardActions(first, isDark, result, actions)
    }
}

/**
 * One color on the card as the audit names it.
 *
 * @property[name] What the color is called in code.
 * @property[argb] The color, or null when this theme has none by that name.
 * @property[tone] How light it is, rounded, or null along with [argb].
 */
@Immutable
internal class InspectColor(
    val name: String,
    val argb: Argb?,
    val tone: Int?,
)

/** A swatch of the color, then its name, hex and tone, read out as one. */
@Composable
private fun ColorLine(color: InspectColor) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val argb = color.argb
        if (argb != null) {
            Box(
                Modifier
                    .size(tokens.iconSize)
                    .clip(CircleShape)
                    .background(argb.toColor())
                    .border(tokens.outlineWidth, tokens.border, CircleShape),
            )
        }
        val tone = color.tone
        BuilderText(color.name, style = BuilderTextStyle.Label)
        if (argb != null) BuilderText(argb.toHex(), style = BuilderTextStyle.Value)
        if (tone != null) BuilderText(stringResource(Res.string.inspect_tone, tone), style = BuilderTextStyle.Value)
    }
}

/** The pair's ratio and the badge it earns, in icon and words. */
@Composable
private fun RatingLine(row: AuditRow) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(stringResource(Res.string.inspect_ratio, ratioText(row.ratio)), style = BuilderTextStyle.Value)
        BuilderBadge(label = stringResource(row.badge.label), status = row.badge.status, icon = row.badge.icon)
    }
}

/** Pin this role, Show on ramp and Jump to key color, all for [first]. Only a role pins. */
@Composable
private fun CardActions(
    first: ColorRef,
    isDark: Boolean,
    result: ThemeResult,
    actions: InspectActions,
) {
    val role = (first as? ColorRef.OfRole)?.role
    BuilderButton(
        onClick = { if (role != null) actions.onPin(role, isDark, result.roles[role, isDark].argb) },
        label = stringResource(Res.string.inspect_pin_role),
        icon = IconId.Pin,
        enabled = role != null && actions.pinEnabled,
    )
    BuilderButton(
        onClick = { actions.onShowOnRamp(first, isDark) },
        label = stringResource(Res.string.inspect_show_on_ramp),
    )
    BuilderButton(
        onClick = { actions.onJumpToKeyColor(first) },
        label = stringResource(Res.string.inspect_jump_to_key_color),
    )
}

/**
 * [ref] in the mode [isDark] picks. A role reports the tone its table gives it, which for a pinned
 * role is the pin's, and any other color the tone it measures at.
 */
internal fun ThemeResult.inspectColor(
    ref: ColorRef,
    isDark: Boolean,
): InspectColor {
    val name = ref.inspectName(document)
    return when (ref) {
        is ColorRef.OfRole -> {
            val entry = roles[ref.role, isDark]
            InspectColor(name, entry.argb, entry.tone.roundToInt())
        }
        is ColorRef.OfAccent -> {
            measured(name, accents.families.getOrNull(ref.slot.index)?.get(ref.slot.part, isDark))
        }
        is ColorRef.OfSlot -> {
            measured(name, customSlots[ref.slot, isDark])
        }
        is ColorRef.OfFluentText -> {
            // Fluent's text colors are fixed and see through, so show them as the audit lays them over the fill.
            val onFill = audit.rows.firstOrNull { row -> row.isDark == isDark && row.pair.foreground == ref }
            measured(name, onFill?.foreground)
        }
        is ColorRef.OfFluentShade -> {
            val tone = ref.shade.tone
            InspectColor(name, Argb(scheme(isDark).primaryPalette.tone(tone)), tone)
        }
    }
}

private fun measured(
    name: String,
    argb: Argb?,
): InspectColor = InspectColor(name, argb, argb?.let { color -> HctReadout.of(color).tone.roundToInt() })

/**
 * What the card calls the color [this] names, the way the code names it. Roles and Custom slots
 * go in lower camel case, as do Fluent's shades and text colors, and an accent is its name and part.
 */
internal fun ColorRef.inspectName(document: ThemeDocument): String =
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

/**
 * The row of the first pair of [refs] the audit rates in the mode [isDark] picks, pairs taken in the
 * order the colors were declared, or null when it rates none of them.
 */
internal fun ContrastAudit.firstRated(
    refs: List<ColorRef>,
    isDark: Boolean,
): AuditRow? {
    val mode = rows.filter { row -> row.isDark == isDark }
    for (first in refs.indices) {
        for (second in first + 1 until refs.size) {
            val pair = setOf(refs[first], refs[second])
            val row = mode.firstOrNull { row -> setOf(row.pair.foreground, row.pair.background) == pair }
            if (row != null) return row
        }
    }
    return null
}

// b-308b

/**
 * The second of [refs] as text over the first, rated in the mode [isDark] picks, or null when there
 * are fewer than two. It stands in when the audit rates none of the pairs, such as a role pair on the
 * Custom target, so the card still shows a ratio.
 */
internal fun ThemeResult.rateFirstTwo(
    refs: List<ColorRef>,
    isDark: Boolean,
): AuditRow? {
    if (refs.size < 2) return null
    return rate(ContrastPair(foreground = refs[1], background = refs[0], kind = PairKind.Text), isDark)
}

/** A ratio to one decimal, cut rather than rounded so a pair just under a line never reads as on it. */
internal fun ratioText(ratio: Double): String {
    val tenths = floor(ratio * TENTHS).toInt()
    return "${tenths / TENTHS}.${tenths % TENTHS}"
}

private const val TENTHS = 10

/** The primary palette tone Fluent cuts each shade at, the same cut its export and the audit take. */
private val FluentShade.tone: Int
    get() = when (this) {
        FluentShade.Dark3 -> 15
        FluentShade.Dark2 -> 30
        FluentShade.Dark1 -> 40
        FluentShade.Base -> 50
        FluentShade.Light1 -> 60
        FluentShade.Light2 -> 80
        FluentShade.Light3 -> 90
    }

private fun String.lowerFirst(): String = replaceFirstChar { char -> char.lowercaseChar() }

private val ContrastBadge.label: StringResource
    get() = when (this) {
        ContrastBadge.Aaa -> Res.string.inspect_badge_aaa
        ContrastBadge.Aa -> Res.string.inspect_badge_aa
        ContrastBadge.AaLarge -> Res.string.inspect_badge_aa_large
        ContrastBadge.Fail -> Res.string.inspect_badge_fail
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
