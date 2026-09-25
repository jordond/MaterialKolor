package com.materialkolor.builder.feature.poster

import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SpecVersion
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

    // b-510
    @Test
    fun specTag_eachTheme_tagsOnlyTheChipsThatMoveItToAnotherSpec() {
        val classic = Style.entries.filter { style -> EffectiveSpec.offered(style) == setOf(SpecVersion.Spec2021) }
        val revised = Style.entries.filter { style -> SpecVersion.Spec2025 in EffectiveSpec.offered(style) }

        fun tags(document: ThemeDocument): Map<Style, SpecVersion> =
            Style.entries.mapNotNull { style -> specTag(style, document)?.let { spec -> style to spec } }.toMap()

        val on2021 = ThemeDocument(seed = Argb(0x6750A4), style = Style.TonalSpot, spec = SpecVersion.Spec2021)
        tags(on2021) shouldBe mapOf(Style.Cmf to SpecVersion.Spec2026)

        val on2025 = on2021.copy(spec = SpecVersion.Spec2025)
        tags(on2025) shouldBe classic.associateWith { SpecVersion.Spec2021 } + (Style.Cmf to SpecVersion.Spec2026)

        val onCmf = on2021.copy(style = Style.Cmf, spec = SpecVersion.Spec2025)
        tags(onCmf) shouldBe
            classic.associateWith { SpecVersion.Spec2021 } + revised.associateWith { SpecVersion.Spec2025 }
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
