package com.materialkolor.builder.preview.inspect

import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.audit.ContrastBadge
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.preview.material.MaterialComponent
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InspectRateTest {
    private val primary = ColorRef.OfRole(Role.Primary)
    private val onPrimary = ColorRef.OfRole(Role.OnPrimary)

    @Test
    fun rateOnPair_aRolePairTheCustomAuditLeavesOut_ratesTheOnRoleOverItsRole() {
        val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4), library = Library.Custom))
        val refs = listOf(primary, onPrimary)
        result.audit.firstRated(refs, isDark = true) shouldBe null

        val row = checkNotNull(result.rateOnPair(refs, isDark = true))
        row.pair shouldBe ContrastPair(foreground = onPrimary, background = primary, kind = PairKind.Text)
        row.isDark shouldBe true
        row.ratio shouldBe (6.12 plusOrMinus 0.01)
        row.badge shouldBe ContrastBadge.Aa
    }

    @Test
    fun rateOnPair_onRoleDeclaredFirst_stillRatesItOverItsRole() {
        val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4), library = Library.Custom))

        val row = checkNotNull(result.rateOnPair(listOf(onPrimary, primary), isDark = false))
        row.pair shouldBe ContrastPair(foreground = onPrimary, background = primary, kind = PairKind.Text)
        row.isDark shouldBe false
        row.ratio shouldBe (6.08 plusOrMinus 0.01)
        row.badge shouldBe ContrastBadge.Aa
    }

    @Test
    fun rateOnPair_outlinedTextField_ratesNothing() {
        val refs = MaterialComponent.OutlinedTextField.refs
        for (library in listOf(Library.Material3, Library.Custom)) {
            val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4), library = library))
            for (isDark in listOf(false, true)) {
                result.rateOnPair(refs, isDark) shouldBe null
                result.audit.firstRated(refs, isDark) shouldBe null
            }
        }
    }

    @Test
    fun rateOnPair_oneColor_ratesNothing() {
        val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4)))

        result.rateOnPair(listOf(primary), isDark = false) shouldBe null
    }
}
