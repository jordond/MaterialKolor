package com.materialkolor.builder.feature.poster

import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.style_spec_classic
import com.materialkolor.builder.generated.resources.style_spec_cmf
import com.materialkolor.builder.generated.resources.style_spec_revised
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StyleContrastLogicTest {
    @Test
    fun styleWords_tenStyles_eachHaveTheirOwnNameTooltipAndDescription() {
        val names = Style.entries.map(::styleName)
        val tooltips = Style.entries.map(::styleTooltip)
        val descriptions = Style.entries.map(::styleDescription)

        names shouldHaveSize 10
        names.shouldBeUnique()
        tooltips.shouldBeUnique()
        descriptions.shouldBeUnique()
    }

    @Test
    fun specSupport_eachStyle_namesTheSpecsItRunsIn() {
        specSupport(Style.TonalSpot) shouldBe Res.string.style_spec_revised
        specSupport(Style.Expressive) shouldBe Res.string.style_spec_revised
        specSupport(Style.Fidelity) shouldBe Res.string.style_spec_classic
        specSupport(Style.Monochrome) shouldBe Res.string.style_spec_classic
        specSupport(Style.Cmf) shouldBe Res.string.style_spec_cmf
    }

    @Test
    fun contrastStop_of_namesTheNearestLevel() {
        ContrastStop.entries.map { stop -> stop.level } shouldBe ContrastLevel.Stops
        ContrastStop.of(ContrastLevel.Medium) shouldBe ContrastStop.Medium
        ContrastStop.of(ContrastLevel(49)) shouldBe ContrastStop.Medium
        ContrastStop.of(ContrastLevel(25)) shouldBe ContrastStop.Standard
    }

    @Test
    fun ratioText_cutsToOneDecimal() {
        ratioText(4.49) shouldBe "4.4"
        ratioText(4.5) shouldBe "4.5"
        ratioText(21.0) shouldBe "21.0"
    }

    @Test
    fun readoutName_aRole_readsAsItsNameInCode() {
        val document = ThemeDocument(seed = Argb(0x6750A4))

        ColorRef.OfRole(Role.OnPrimaryContainer).readoutName(document) shouldBe "onPrimaryContainer"
        ColorRef.OfRole(Role.Primary).readoutName(document) shouldBe "primary"
    }
}
