package com.materialkolor.builder.engine.audit

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.ContrastPairs
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.audit.FluentText
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.export.fluentShades
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.engine.resolve.toDomain
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.ktx.toneColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * The badge a rated pair earns under WCAG 2.2.
 *
 * Text earns [Aaa] at 7 to 1, [Aa] at 4.5 to 1 and [AaLarge] at 3 to 1. A shape only has the one
 * rule, 3 to 1, so it earns [Aa] or [Fail].
 */
public enum class ContrastBadge {
    Aaa,
    Aa,
    AaLarge,
    Fail,
}

/**
 * Why a pair misses the ratio its kind has to clear.
 *
 * @property[key] The string resource the UI shows for this reason.
 */
public enum class AuditReason(
    public val key: String,
) {
    /** Text between 3 and 4.5 to 1, readable only when it is set large. */
    TextOnlyLarge(key = "audit_reason_text_only_large"),

    /** Text under 3 to 1, hard to read at any size. */
    TextUnreadable(key = "audit_reason_text_unreadable"),

    /** A border or other shape under 3 to 1, hard to see against what it sits on. */
    ShapeFaint(key = "audit_reason_shape_faint"),
}

/**
 * What to try on a pair that misses, most specific first.
 *
 * When the two colors of a pair point at different fixes, the one listed first here wins.
 *
 * @property[key] The string resource the UI shows for this suggestion.
 */
public enum class AuditSuggestion(
    public val key: String,
) {
    /** One of the colors is pinned, so move the pin or clear it. */
    ChangePin(key = "audit_suggest_change_pin"),

    /** An accent's tones sit too close, so pull them apart or raise its threshold. */
    MoveAccentTones(key = "audit_suggest_move_accent_tones"),

    /** A Custom slot is cut at a fixed tone, so move that tone. */
    MoveSlotTone(key = "audit_suggest_move_slot_tone"),

    /** Fluent's text colors are fixed, so only a lighter or darker seed moves the fill under them. */
    ChangeSeed(key = "audit_suggest_change_seed"),

    /** The colors come from the scheme, so a higher contrast level pulls them apart. */
    RaiseContrast(key = "audit_suggest_raise_contrast"),
}

/**
 * One pair rated in one mode.
 *
 * @property[pair] The pair, as the domain names it.
 * @property[isDark] Whether this is the dark mode rating.
 * @property[foreground] The color drawn on top, composited over [background] when it carries alpha.
 * @property[background] The color it sits on.
 * @property[ratio] The WCAG 2.2 contrast ratio of the two.
 * @property[badge] The badge [ratio] earns for the pair's kind.
 * @property[reason] Why the pair misses its ratio, or null when it passes.
 * @property[suggestion] What to try, or null when it passes.
 */
@Immutable
public data class AuditRow(
    public val pair: ContrastPair,
    public val isDark: Boolean,
    public val foreground: Argb,
    public val background: Argb,
    public val ratio: Double,
    public val badge: ContrastBadge,
    public val reason: AuditReason?,
    public val suggestion: AuditSuggestion?,
) {
    /** Whether the pair clears 4.5 to 1 as text or 3 to 1 as a shape. */
    public val passes: Boolean
        get() = reason == null
}

/**
 * Every contrast pair of the document's target, rated in both modes.
 *
 * The pairs are the ones `ContrastPairs.forTarget` names for the target, the document's accents
 * and its pins. Roles are read through the role tables, so pins and AMOLED are rated as they
 * render. A Fluent pair rates Fluent's fixed text against its accent fill, which is the primary
 * ramp at tone 40 in light mode and tone 80 in dark mode rather than the scheme's primary role.
 *
 * @property[rows] Every pair of the target in both modes, in pair order with light first.
 */
@Immutable
public class ContrastAudit internal constructor(
    public val rows: ImmutableList<AuditRow>,
) {
    /** The rows of the modes [visibleModes] shows. */
    public fun rows(visibleModes: PreviewMode): List<AuditRow> = rows.filter { row -> visibleModes.shows(row.isDark) }

    /**
     * The text pair with the lowest ratio across the modes [visibleModes] shows, for the contrast
     * readout.
     *
     * Shapes are left out, since the readout names an on color over its background. Every target
     * rates text in both modes, so there is always one.
     */
    public fun lowestPair(visibleModes: PreviewMode): AuditRow =
        rows(visibleModes)
            .filter { row -> row.pair.kind == PairKind.Text }
            .minBy { row -> row.ratio }

    internal companion object {
        /** Rate every pair of [result]'s target in both modes. */
        fun from(result: ThemeResult): ContrastAudit {
            val document = result.document
            val pairs = ContrastPairs.forTarget(
                library = document.library,
                accentCount = document.accents.size,
                pinned = document.pins.keys,
            )
            return ContrastAudit(
                rows = pairs
                    .flatMap { pair -> listOf(rate(result, pair, isDark = false), rate(result, pair, isDark = true)) }
                    .toImmutableList(),
            )
        }

        /** Rate [pair] in the mode [isDark] picks, the one place a pair's colors are looked up. */
        internal fun rate(
            result: ThemeResult,
            pair: ContrastPair,
            isDark: Boolean,
        ): AuditRow {
            val fluent = pair.isFluent
            val background = result.color(pair.background, isDark, fluent)
            val foreground = result.color(pair.foreground, isDark, fluent).compositeOver(background)
            val ratio = foreground.contrastRatio(background)
            val reason = reason(ratio, pair.kind)
            return AuditRow(
                pair = pair,
                isDark = isDark,
                foreground = foreground.toDomain(),
                background = background.toDomain(),
                ratio = ratio,
                badge = badge(ratio, pair.kind),
                reason = reason,
                suggestion = reason?.let { suggestion(pair, result.pinnedIn(isDark)) },
            )
        }

        /**
         * The color [ref] names in the mode [isDark] picks.
         *
         * In a Fluent pair the primary role stands for Fluent's accent fill, which is cut from the
         * scheme's own primary palette. A Fluent shade is the one the export writes for that mode.
         * Fluent's text colors can carry alpha, and the caller lays them over the background before
         * measuring.
         */
        private fun ThemeResult.color(
            ref: ColorRef,
            isDark: Boolean,
            fluent: Boolean,
        ): Color =
            when (ref) {
                is ColorRef.OfRole -> {
                    if (fluent && ref.role == Role.Primary) {
                        scheme(isDark).primaryPalette.toneColor(if (isDark) FLUENT_FILL_DARK else FLUENT_FILL_LIGHT)
                    } else {
                        roles[ref.role, isDark].argb.toColor()
                    }
                }
                is ColorRef.OfAccent -> {
                    accents[ref.slot, isDark].toColor()
                }
                is ColorRef.OfSlot -> {
                    customSlots[ref.slot, isDark].toColor()
                }
                is ColorRef.OfFluentText -> {
                    ref.text.color(isDark)
                }
                is ColorRef.OfFluentShade -> {
                    val shades = fluentShades()
                    (if (isDark) shades.dark else shades.light)[ref.shade].toColor()
                }
            }

        private val ContrastPair.isFluent: Boolean
            get() = foreground.isFluent || background.isFluent

        private val ColorRef.isFluent: Boolean
            get() = this is ColorRef.OfFluentText || this is ColorRef.OfFluentShade

        /** The roles the document pins in the mode [isDark] picks. */
        private fun ThemeResult.pinnedIn(isDark: Boolean): Set<Role> =
            document.pins.filterValues { pin -> (if (isDark) pin.dark else pin.light) != null }.keys

        private fun badge(
            ratio: Double,
            kind: PairKind,
        ): ContrastBadge =
            when (kind) {
                PairKind.Text -> {
                    if (ratio >= AAA_TEXT) {
                        ContrastBadge.Aaa
                    } else if (ratio >= AA_TEXT) {
                        ContrastBadge.Aa
                    } else if (ratio >= AA_LARGE) {
                        ContrastBadge.AaLarge
                    } else {
                        ContrastBadge.Fail
                    }
                }
                PairKind.NonText -> {
                    if (ratio >= AA_LARGE) ContrastBadge.Aa else ContrastBadge.Fail
                }
            }

        private fun reason(
            ratio: Double,
            kind: PairKind,
        ): AuditReason? =
            when (kind) {
                PairKind.Text -> {
                    if (ratio >= AA_TEXT) {
                        null
                    } else if (ratio >= AA_LARGE) {
                        AuditReason.TextOnlyLarge
                    } else {
                        AuditReason.TextUnreadable
                    }
                }
                PairKind.NonText -> {
                    if (ratio >= AA_LARGE) null else AuditReason.ShapeFaint
                }
            }

        private fun suggestion(
            pair: ContrastPair,
            pinned: Set<Role>,
        ): AuditSuggestion {
            if (pair.isFluent) return AuditSuggestion.ChangeSeed
            return listOf(pair.foreground, pair.background)
                .map { ref -> ref.suggestion(pinned) }
                .minBy { suggestion -> suggestion.ordinal }
        }

        private fun ColorRef.suggestion(pinned: Set<Role>): AuditSuggestion =
            when (this) {
                is ColorRef.OfRole -> {
                    role.suggestion(pinned)
                }
                is ColorRef.OfAccent -> {
                    AuditSuggestion.MoveAccentTones
                }
                is ColorRef.OfSlot -> {
                    when (val resolution = slot.resolution) {
                        is SlotResolution.FromRole -> resolution.role.suggestion(pinned)
                        is SlotResolution.FromRamp,
                        is SlotResolution.OnRamp,
                        -> AuditSuggestion.MoveSlotTone
                    }
                }
                is ColorRef.OfFluentText,
                is ColorRef.OfFluentShade,
                -> {
                    AuditSuggestion.ChangeSeed
                }
            }

        private fun Role.suggestion(pinned: Set<Role>): AuditSuggestion =
            if (this in pinned) AuditSuggestion.ChangePin else AuditSuggestion.RaiseContrast

        /**
         * Fluent's text on accent colors, from `generateTextColors` in compose-fluent v0.1.0,
         * `io/github/composefluent/Colors.kt`.
         */
        private fun FluentText.color(isDark: Boolean): Color =
            when (this) {
                FluentText.OnAccentPrimary -> if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
                FluentText.OnAccentSecondary -> if (isDark) Color(0x80000000) else Color(0xB3FFFFFF)
            }

        private operator fun FluentShadeValues.get(shade: FluentShade): Argb =
            when (shade) {
                FluentShade.Dark3 -> dark3
                FluentShade.Dark2 -> dark2
                FluentShade.Dark1 -> dark1
                FluentShade.Base -> base
                FluentShade.Light1 -> light1
                FluentShade.Light2 -> light2
                FluentShade.Light3 -> light3
            }

        private fun PreviewMode.shows(isDark: Boolean): Boolean =
            when (this) {
                PreviewMode.Light -> !isDark
                PreviewMode.Split -> true
                PreviewMode.Dark -> isDark
            }
    }
}

// b-308

/**
 * Rate [pair] in the mode [isDark] picks, the same way the audit rates its own pairs.
 *
 * It works for a pair the audit leaves out too, such as a role pair on the Custom target that no
 * pin brought in, so a view that shows a ratio for any pair reads it from one place.
 */
public fun ThemeResult.rate(
    pair: ContrastPair,
    isDark: Boolean,
): AuditRow = ContrastAudit.rate(this, pair, isDark)

/** Fluent's default accent fill in light mode, the `dark1` shade, tone 40 of the primary ramp. */
private const val FLUENT_FILL_LIGHT = 40

/** Fluent's default accent fill in dark mode, the `light2` shade, tone 80 of the primary ramp. */
private const val FLUENT_FILL_DARK = 80

private const val AAA_TEXT = 7.0
private const val AA_TEXT = 4.5
private const val AA_LARGE = 3.0
