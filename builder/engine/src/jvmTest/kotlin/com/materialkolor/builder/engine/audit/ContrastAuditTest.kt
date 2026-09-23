package com.materialkolor.builder.engine.audit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.ContrastPairs
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.audit.FluentText
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContrastAuditTest {
    private val document = ThemeDocument(
        seed = Argb(0x6750A4),
        accents = listOf(
            Accent(name = "Brand", seed = Argb(0xB3261E)),
            Accent(name = "Status", seed = Argb(0x2E7D32), harmonize = false),
        ),
        pins = mapOf(Role.Tertiary to RolePin(light = Argb(0x7D5260))),
    )

    @Test
    fun from_everyTarget_ratesEveryPairInBothModes() {
        val resolver = ThemeResolver()
        for (library in Library.entries) {
            val target = document.copy(library = library)
            val pairs = ContrastPairs.forTarget(library, target.accents.size, target.pins.keys)
            val audit = resolver.resolve(target).audit

            assertEquals(pairs.size * 2, audit.rows.size, "$library")
            for (isDark in listOf(false, true)) {
                val rated = audit.rows.filter { row -> row.isDark == isDark }.map { row -> row.pair }
                assertEquals(pairs, rated, "$library, dark $isDark")
            }
        }
    }

    @Test
    fun from_outlinePairs_areRatedAsShapes() {
        val rows = ThemeResolver().resolve(document).audit.rows
        val outlines = rows.filter { row -> row.pair.foreground == ColorRef.OfRole(Role.Outline) }

        assertTrue(outlines.isNotEmpty())
        for (row in outlines) {
            assertEquals(PairKind.NonText, row.pair.kind)
            assertEquals(if (row.ratio >= 3.0) ContrastBadge.Aa else ContrastBadge.Fail, row.badge, "$row")
        }
    }

    @Test
    fun from_textRatios_earnTheirBadges() {
        val rows = ThemeResolver()
            .resolve(document)
            .audit.rows
            .filter { row -> row.pair.kind == PairKind.Text }

        for (row in rows) {
            val expected = when {
                row.ratio >= 7.0 -> ContrastBadge.Aaa
                row.ratio >= 4.5 -> ContrastBadge.Aa
                row.ratio >= 3.0 -> ContrastBadge.AaLarge
                else -> ContrastBadge.Fail
            }
            assertEquals(expected, row.badge, "$row")
            assertEquals(row.ratio >= 4.5, row.passes, "$row")
        }
    }

    @Test
    fun lowestPair_visibleModes_returnsTheMinimumTextRow() {
        val audit = ThemeResolver().resolve(document).audit
        val text = audit.rows.filter { row -> row.pair.kind == PairKind.Text }

        assertEquals(text.minBy { row -> row.ratio }, audit.lowestPair(PreviewMode.Split))
        assertEquals(text.filter { row -> !row.isDark }.minBy { row -> row.ratio }, audit.lowestPair(PreviewMode.Light))
        assertEquals(text.filter { row -> row.isDark }.minBy { row -> row.ratio }, audit.lowestPair(PreviewMode.Dark))
    }

    @Test
    fun from_fluent_measuresAgainstTheToneFortyAndEightyFill() {
        val fluent = document.copy(
            library = Library.Fluent,
            contrast = ContrastLevel.High,
            pins = mapOf(Role.Primary to RolePin(light = Argb(0x00FF00), dark = Argb(0x00FF00))),
        )
        val result = ThemeResolver().resolve(fluent)
        val rows = result.audit.rows

        assertEquals(FluentText.entries.size * 2, rows.size)
        for (row in rows) {
            val scheme = result.scheme(row.isDark)
            val fill = scheme.primaryPalette.tone(if (row.isDark) 80 else 40)
            val text = (row.pair.foreground as ColorRef.OfFluentText).text
            val expected = when (text) {
                FluentText.OnAccentPrimary -> if (row.isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
                FluentText.OnAccentSecondary -> if (row.isDark) Color(0x80000000) else Color(0xB3FFFFFF)
            }.compositeOver(Color(fill))

            assertEquals(Argb(fill), row.background, "$row")
            assertNotEquals(result.roles[Role.Primary, row.isDark].argb, row.background, "$row")
            assertEquals(Argb(expected.toArgb()), row.foreground, "$row")
        }
    }

    @Test
    fun rate_fluentShade_readsTheShadeOfThatModeAndSuggestsTheSeed() {
        val result = ThemeResolver().resolve(document.copy(library = Library.Fluent))
        val tones = mapOf(
            FluentShade.Dark3 to 15,
            FluentShade.Dark2 to 30,
            FluentShade.Dark1 to 40,
            FluentShade.Base to 50,
            FluentShade.Light1 to 60,
            FluentShade.Light2 to 80,
            FluentShade.Light3 to 90,
        )
        for ((shade, tone) in tones) {
            for (isDark in listOf(false, true)) {
                val pair = ContrastPair(
                    foreground = ColorRef.OfFluentText(FluentText.OnAccentPrimary),
                    background = ColorRef.OfFluentShade(shade),
                    kind = PairKind.Text,
                )
                val row = ContrastAudit.rate(result, pair, isDark)
                val expected = Argb(result.scheme(isDark).primaryPalette.tone(tone))

                assertEquals(expected, row.background, "$shade, dark $isDark")
            }
        }

        val pale = ContrastPair(
            foreground = ColorRef.OfFluentText(FluentText.OnAccentPrimary),
            background = ColorRef.OfFluentShade(FluentShade.Light3),
            kind = PairKind.Text,
        )
        val light = ContrastAudit.rate(result, pale, isDark = false)

        assertEquals(AuditReason.TextUnreadable, light.reason)
        assertEquals(AuditSuggestion.ChangeSeed, light.suggestion)
    }

    @Test
    fun from_pinnedPairThatFails_saysWhyAndSuggestsThePin() {
        val pinned = document.copy(
            pins = mapOf(
                Role.Primary to RolePin(light = Argb(0x777777)),
                Role.OnPrimary to RolePin(light = Argb(0x787878)),
            ),
        )
        val rows = ThemeResolver().resolve(pinned).audit.rows
        val onPrimary = rows.filter { row ->
            row.pair.foreground == ColorRef.OfRole(Role.OnPrimary) &&
                row.pair.background == ColorRef.OfRole(Role.Primary)
        }
        val light = onPrimary.single { row -> !row.isDark }
        val dark = onPrimary.single { row -> row.isDark }

        assertEquals(ContrastBadge.Fail, light.badge)
        assertEquals(AuditReason.TextUnreadable, light.reason)
        assertEquals(AuditSuggestion.ChangePin, light.suggestion)
        assertNull(dark.reason)
        assertNull(dark.suggestion)
    }

    @Test
    fun from_looseAccentThreshold_readsOnlyLargeAndSuggestsTheAccentTones() {
        val accent = Accent(name = "Brand", seed = Argb(0xB3261E), threshold = OnColorThreshold.AaLarge)
        val loose = document.copy(accents = listOf(accent))
        val rows = ThemeResolver().resolve(loose).audit.rows
        val failing = rows.filter { row -> row.pair.foreground is ColorRef.OfAccent && !row.passes }

        assertTrue(failing.isNotEmpty())
        for (row in failing) {
            assertEquals(AuditReason.TextOnlyLarge, row.reason, "$row")
            assertEquals(AuditSuggestion.MoveAccentTones, row.suggestion, "$row")
        }
    }

    @Test
    fun from_movedCustomToneThatFails_suggestsMovingTheSlotTone() {
        val custom = document.copy(
            library = Library.Custom,
            customTones = mapOf(CustomSlot.PrimaryPressed to CustomTone(light = 90)),
        )
        val rows = ThemeResolver().resolve(custom).audit.rows
        val pressed = rows.single { row ->
            !row.isDark && row.pair.background == ColorRef.OfSlot(CustomSlot.PrimaryPressed)
        }

        assertEquals(AuditReason.TextUnreadable, pressed.reason)
        assertEquals(AuditSuggestion.MoveSlotTone, pressed.suggestion)
    }
}
