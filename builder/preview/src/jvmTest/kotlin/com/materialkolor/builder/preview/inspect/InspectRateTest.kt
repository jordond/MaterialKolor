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
import com.materialkolor.builder.preview.material.MaterialComponent
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

        val pair = ContrastPair(foreground = onPrimary, background = primary, kind = PairKind.Text)
        result.rateOnPair(refs, isDark = true) shouldBe result.rate(pair, isDark = true)
    }

    // b-308ba
    @Test
    fun rateOnPair_onRoleDeclaredFirst_stillRatesItOverItsRole() {
        val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4), library = Library.Custom))

        val pair = ContrastPair(foreground = onPrimary, background = primary, kind = PairKind.Text)
        result.rateOnPair(listOf(onPrimary, primary), isDark = false) shouldBe result.rate(pair, isDark = false)
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
