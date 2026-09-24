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
    fun contrastScale_stops_areTheFourNamedLevelsInSliderOrder() {
        ContrastScale.Stops shouldBe listOf(-1f, 0f, 0.5f, 1f)
        ContrastScale.SNAP_DISTANCE shouldBe 0.04f
        ContrastScale.Stops.map { stop -> ContrastStop.of(ContrastScale.levelOf(stop)) } shouldBe ContrastStop.entries
    }

    @Test
    fun contrastScale_snap_neverReachesTwoStopsAtOnce() {
        ContrastScale.Stops.zipWithNext().forEach { (lower, upper) ->
            (upper - lower > 2 * ContrastScale.SNAP_DISTANCE) shouldBe true
        }
    }

    @Test
    fun contrastScale_levelOf_roundsToHundredthsAndStaysInRange() {
        ContrastScale.levelOf(0.537f) shouldBe ContrastLevel(54)
        ContrastScale.levelOf(-0.004f) shouldBe ContrastLevel.Standard
        ContrastScale.levelOf(1.2f) shouldBe ContrastLevel.High
        ContrastScale.levelOf(-1.2f) shouldBe ContrastLevel.Reduced
    }

    @Test
    fun contrastScale_parse_takesAnyLevelFromMinusOneToOne() {
        ContrastScale.parse("0.3") shouldBe ContrastLevel(30)
        ContrastScale.parse(" -0,75 ") shouldBe ContrastLevel(-75)
        ContrastScale.parse("1") shouldBe ContrastLevel.High
        ContrastScale.parse("0.333") shouldBe ContrastLevel(33)
        ContrastScale.parse("1.5") shouldBe null
        ContrastScale.parse("high") shouldBe null
        ContrastScale.parse("") shouldBe null
    }

    @Test
    fun contrastScale_format_showsTwoDecimals() {
        ContrastScale.format(ContrastLevel.Reduced) shouldBe "-1.00"
        ContrastScale.format(ContrastLevel.Standard) shouldBe "0.00"
        ContrastScale.format(ContrastLevel(-5)) shouldBe "-0.05"
        ContrastScale.format(ContrastLevel.Medium) shouldBe "0.50"
    }

    @Test
    fun contrastStop_of_namesOnlyALevelRightOnAStop() {
        ContrastStop.of(ContrastLevel.Medium) shouldBe ContrastStop.Medium
        ContrastStop.of(ContrastLevel(49)) shouldBe null
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
