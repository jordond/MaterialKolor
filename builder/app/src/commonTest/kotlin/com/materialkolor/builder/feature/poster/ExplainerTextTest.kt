package com.materialkolor.builder.feature.poster

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.engine.resolve.ThemeResolver
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt
import kotlin.test.Test

class ExplainerTextTest {
    private val resolver = ThemeResolver()

    /** A red loud enough that every style has to cap or move it. */
    private val seed = Argb(0xE53935)

    @Test
    fun sentences_everyStyleSpecAndMode_areWorkedOutFromTheScheme() {
        val seedHct = HctReadout.of(seed)
        Style.entries.forEach { style ->
            SpecVersion.entries.forEach { spec ->
                listOf(false, true).forEach { isDark ->
                    val result = resolver.resolve(ThemeDocument(seed = seed, style = style, spec = spec))
                    val scheme = result.scheme(isDark)
                    val sentences = ExplainerText.sentences(PrimaryFacts.of(result, isDark))
                    withClue("$style $spec dark $isDark") {
                        sentences.shouldNotBeEmpty()
                        val chroma = sentences.first()
                        when (chroma.key) {
                            ExplainerKey.ChromaCapped, ExplainerKey.ChromaLifted -> {
                                chroma.values shouldBe
                                    listOf(scheme.primaryPalette.chroma.roundToInt(), seedHct.chroma.roundToInt())
                            }
                            ExplainerKey.ChromaKept, ExplainerKey.ChromaNone -> {
                                chroma.values shouldBe listOf(seedHct.chroma.roundToInt())
                            }
                            else -> {
                                chroma.key shouldBe ExplainerKey.ChromaCapped
                            }
                        }
                        sentences.last() shouldBe
                            ExplainerSentence(
                                if (isDark) ExplainerKey.ToneDark else ExplainerKey.ToneLight,
                                listOf(
                                    result.effectiveSpec.year,
                                    HctReadout.of(Argb(scheme.primary)).tone.roundToInt(),
                                    seedHct.tone.roundToInt(),
                                ),
                            )
                        sentences.forEach { sentence ->
                            sentence.values.shouldNotBeEmpty()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun sentences_tonalSpot_capsChromaAtWhatThePaletteAskedFor() {
        val result = resolver.resolve(ThemeDocument(seed = seed, style = Style.TonalSpot))

        val chroma = ExplainerText.sentences(PrimaryFacts.of(result, isDark = false)).first()

        chroma.key shouldBe ExplainerKey.ChromaCapped
        chroma.values.first() shouldBe result.light.primaryPalette.chroma
            .roundToInt()
    }

    @Test
    fun sentences_monochrome_saysPrimaryHasNoChroma() {
        val result = resolver.resolve(ThemeDocument(seed = seed, style = Style.Monochrome))

        ExplainerText.sentences(PrimaryFacts.of(result, isDark = false)).first().key shouldBe ExplainerKey.ChromaNone
    }

    @Test
    fun sentences_fidelity_keepsTheSeedsChroma() {
        val result = resolver.resolve(ThemeDocument(seed = seed, style = Style.Fidelity))

        ExplainerText.sentences(PrimaryFacts.of(result, isDark = false)).first().key shouldBe ExplainerKey.ChromaKept
    }

    @Test
    fun sentences_expressive_turnsTheHue() {
        val result = resolver.resolve(ThemeDocument(seed = seed, style = Style.Expressive))

        val keys = ExplainerText.sentences(PrimaryFacts.of(result, isDark = false)).map { sentence -> sentence.key }

        keys shouldContain ExplainerKey.HueTurned
    }

    @Test
    fun take_primaryWithinTwoOfTheSeed_saysNothing() {
        val seed = HctReadout(hue = 20.0, chroma = 60.0, tone = 50.0)

        ExplainerText.take(seed, seed) shouldBe null
        ExplainerText.take(seed, HctReadout(hue = 21.5, chroma = 58.5, tone = 51.9)) shouldBe null
    }

    @Test
    fun take_theAxisThatMovedFurthest_namesTheTake() {
        val seed = HctReadout(hue = 20.0, chroma = 60.0, tone = 50.0)

        ExplainerText.take(seed, HctReadout(hue = 20.0, chroma = 36.0, tone = 40.0)) shouldBe PrimaryTake.Calmer
        ExplainerText.take(seed, HctReadout(hue = 20.0, chroma = 90.0, tone = 40.0)) shouldBe PrimaryTake.Bolder
        ExplainerText.take(seed, HctReadout(hue = 20.0, chroma = 60.0, tone = 30.0)) shouldBe PrimaryTake.Deeper
        ExplainerText.take(seed, HctReadout(hue = 20.0, chroma = 60.0, tone = 80.0)) shouldBe PrimaryTake.Lighter
        ExplainerText.take(seed, HctReadout(hue = 350.0, chroma = 60.0, tone = 50.0)) shouldBe PrimaryTake.Turned
    }

    @Test
    fun take_aGraySeed_ignoresItsHue() {
        val gray = HctReadout(hue = 20.0, chroma = 1.0, tone = 50.0)

        ExplainerText.take(gray, HctReadout(hue = 200.0, chroma = 2.0, tone = 50.0)) shouldBe null
    }

    @Test
    fun keepChroma_readsTheFidelityScheme() {
        val fidelity = resolver.scheme(SchemeInputs(seed = seed, style = Style.Fidelity), isDark = false)

        val sentence = ExplainerText.keepChroma(seed, fidelity)

        sentence.key shouldBe ExplainerKey.KeepChroma
        sentence.values shouldBe listOf(
            fidelity.primaryPalette.chroma.roundToInt(),
            HctReadout.of(Argb(fidelity.primaryContainer)).tone.roundToInt(),
            HctReadout.of(seed).tone.roundToInt(),
        )
    }

    @Test
    fun matchExactly_pinsPrimaryToTheSeedAndOnPrimaryToTheRampsOnTone() {
        val document = ThemeDocument(seed = seed)
        val match = MatchExactly.of(resolver.resolve(document), seed, pinDark = true)

        val matched = match.applyTo(document)

        matched.pins[Role.Primary] shouldBe RolePin(light = seed, dark = seed)
        matched.pins[Role.OnPrimary] shouldBe RolePin(light = match.onPrimaryLight, dark = match.onPrimaryDark)
    }

    @Test
    fun matchExactly_lightOnly_leavesDarkAlone() {
        val match = MatchExactly.of(resolver.resolve(ThemeDocument(seed = seed)), seed, pinDark = false)

        match.onPrimaryDark shouldBe null
        match.pins.map { pin -> pin.mode }.toSet() shouldBe setOf(PinMode.Light)
        match.pins shouldContain DocumentChange.SetPin(Role.Primary, PinMode.Light, seed)
    }

    @Test
    fun matchExactly_warns_onlyBelowAa() {
        val onWhite = MatchExactly(seed, before = 5.0, after = 4.49, Argb(0xFFFFFF), onPrimaryDark = null)

        onWhite.warns shouldBe true
        onWhite.copy(after = 4.5).warns shouldBe false
    }

    @Test
    fun explainerKeys_eachHaveTheirOwnWords() {
        ExplainerKey.entries.map { key -> key.resource }.shouldBeUnique()
        PrimaryTake.entries.map { take -> take.line }.shouldBeUnique()
    }
}
