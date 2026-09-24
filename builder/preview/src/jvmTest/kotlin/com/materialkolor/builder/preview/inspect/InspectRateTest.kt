package com.materialkolor.builder.preview.inspect

import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.audit.rate
import com.materialkolor.builder.engine.resolve.ThemeResolver
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InspectRateTest {
    private val primary = ColorRef.OfRole(Role.Primary)
    private val onPrimary = ColorRef.OfRole(Role.OnPrimary)

    @Test
    fun rateFirstTwo_aRolePairTheCustomAuditLeavesOut_ratesTheSecondOverTheFirst() {
        val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4), library = Library.Custom))
        val refs = listOf(primary, onPrimary)
        result.audit.firstRated(refs, isDark = true) shouldBe null

        val pair = ContrastPair(foreground = onPrimary, background = primary, kind = PairKind.Text)
        result.rateFirstTwo(refs, isDark = true) shouldBe result.rate(pair, isDark = true)
    }

    @Test
    fun rateFirstTwo_oneColor_ratesNothing() {
        val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4)))

        result.rateFirstTwo(listOf(primary), isDark = false) shouldBe null
    }
}
