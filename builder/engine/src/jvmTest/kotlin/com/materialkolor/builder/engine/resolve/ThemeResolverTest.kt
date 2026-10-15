package com.materialkolor.builder.engine.resolve

import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ThemeResolverTest {
    private val document = ThemeDocument(seed = Argb(0x6750A4))

    @Test
    fun resolve_sameDocument_returnsSameResult() {
        val resolver = ThemeResolver()

        assertSame(resolver.resolve(document), resolver.resolve(document))
    }

    @Test
    fun resolve_bothModes_areAlwaysBuilt() {
        val result = ThemeResolver().resolve(document)

        assertFalse(result.light.isDark)
        assertTrue(result.dark.isDark)
        assertSame(result.light, result.scheme(isDark = false))
        assertSame(result.dark, result.scheme(isDark = true))
    }

    @Test
    fun resolve_libraryOrThemeNameChange_isASchemeCacheHit() {
        val resolver = ThemeResolver()
        val first = resolver.resolve(document)
        val renamed = resolver.resolve(document.copy(library = Library.Fluent, themeName = "BrandTheme"))

        assertNotSame(first, renamed)
        assertSame(first.light, renamed.light)
        assertSame(first.dark, renamed.dark)
    }

    @Test
    fun resolve_changesAppliedAfterGeneration_reuseTheSchemes() {
        val resolver = ThemeResolver()
        val first = resolver.resolve(document)
        val skinned = resolver.resolve(
            document.copy(
                seedSource = SeedSource.Picked,
                amoled = true,
                pins = mapOf(Role.Primary to RolePin(light = Argb(0x123456))),
                expressive = true,
                motionScheme = MotionSchemeChoice.Standard,
            ),
        )

        assertSame(first.light, skinned.light)
        assertSame(first.dark, skinned.dark)
    }

    @Test
    fun resolve_tertiarySeedOnAClassicStyle_isASchemeCacheHit() {
        val resolver = ThemeResolver()
        val first = resolver.resolve(document)
        val seeded = resolver.resolve(document.copy(cmfTertiarySeed = Argb(0x00FF00)))

        assertSame(first.light, seeded.light)
    }

    @Test
    fun resolve_moreThanEightDocuments_evictsTheOldestResultButKeepsItsSchemes() {
        val resolver = ThemeResolver()
        val first = resolver.resolve(document)
        repeat(8) { index -> resolver.resolve(document.copy(themeName = "Theme$index")) }
        val again = resolver.resolve(document)

        assertNotSame(first, again)
        assertSame(first.light, again.light)
    }

    @Test
    fun scheme_documentInputs_isTheResolvedScheme() {
        val resolver = ThemeResolver()
        val result = resolver.resolve(document)
        val inputs = SchemeInputs.from(document)

        assertSame(result.light, resolver.scheme(inputs, isDark = false))
        assertSame(result.dark, resolver.scheme(inputs, isDark = true))
    }

    @Test
    fun scheme_sameInputs_returnsSameInstance() {
        val resolver = ThemeResolver()
        val inputs = SchemeInputs(seed = Argb(0x00AA55), style = Style.Vibrant, spec = SpecVersion.Spec2025)

        assertSame(resolver.scheme(inputs, isDark = true), resolver.scheme(inputs.copy(), isDark = true))
        assertNotSame(resolver.scheme(inputs, isDark = true), resolver.scheme(inputs, isDark = false))
    }

    @Test
    fun chrome_contrastAtOrAboveStandard_isTheDocumentScheme() {
        val resolver = ThemeResolver()
        for (hundredths in listOf(0, 25, 50, 100)) {
            val result = resolver.resolve(document.copy(contrast = ContrastLevel(hundredths)))

            assertSame(result.light, result.chrome(isDark = false), "light at $hundredths")
            assertSame(result.dark, result.chrome(isDark = true), "dark at $hundredths")
        }
    }

    @Test
    fun chrome_contrastBelowStandard_floorsAtStandard() {
        val resolver = ThemeResolver()
        val standard = resolver.resolve(document)
        for (hundredths in listOf(-100, -50, -1)) {
            val result = resolver.resolve(document.copy(contrast = ContrastLevel(hundredths)))

            assertEquals(hundredths / 100.0, result.light.contrastLevel)
            assertEquals(0.0, result.chrome(isDark = false).contrastLevel)
            assertSame(standard.light, result.chrome(isDark = false), "light at $hundredths")
            assertSame(standard.dark, result.chrome(isDark = true), "dark at $hundredths")
        }
    }

    @Test
    fun effectiveSpec_everyStyleAndRequest_matchesTheDomainTable() {
        val resolver = ThemeResolver()
        for (style in Style.entries) {
            for (requested in SpecVersion.entries) {
                val plain = document.copy(style = style, spec = requested)
                val keyed = plain.copy(keyColors = KeyColors(primary = Argb(0xB3261E), neutral = Argb(0x777777)))
                val expected = EffectiveSpec.of(style, requested)

                for (candidate in listOf(plain, keyed)) {
                    val result = resolver.resolve(candidate)
                    assertEquals(expected, result.effectiveSpec, "$style when $requested is requested")
                    assertEquals(result.light.specVersion, result.dark.specVersion, "$style when $requested")
                }
            }
        }
    }

    @Test
    fun resolve_primaryKeyColor_pinsOnlyThePrimaryPalette() {
        val resolver = ThemeResolver()
        val primary = Argb(0x00897B)
        val plain = resolver.resolve(document)
        val keyed = resolver.resolve(document.copy(keyColors = KeyColors(primary = primary)))

        assertEquals(TonalPalette.fromInt(primary.value).tones(), keyed.light.primaryPalette.tones())
        assertNotEquals(plain.light.primaryPalette.tones(), keyed.light.primaryPalette.tones())
        assertEquals(plain.light.secondaryPalette.tones(), keyed.light.secondaryPalette.tones())
        assertEquals(plain.light.tertiaryPalette.tones(), keyed.light.tertiaryPalette.tones())
        assertEquals(plain.light.neutralPalette.tones(), keyed.light.neutralPalette.tones())
        assertEquals(plain.light.errorPalette.tones(), keyed.light.errorPalette.tones())
        assertEquals(document.seed.value, keyed.light.sourceColorArgb)
    }

    @Test
    fun from_classicStyle_dropsTheTertiarySeed() {
        val seed = Argb(0x00FF00)

        assertNull(SchemeInputs.from(document.copy(cmfTertiarySeed = seed)).cmfTertiarySeed)
        assertEquals(seed, SchemeInputs.from(document.copy(style = Style.Cmf, cmfTertiarySeed = seed)).cmfTertiarySeed)
    }

    @Test
    fun forChrome_belowStandard_raisesOnlyTheContrast() {
        val inputs = SchemeInputs.from(document.copy(contrast = ContrastLevel.Reduced, style = Style.Rainbow))

        val chrome = inputs.forChrome()

        assertEquals(inputs.copy(contrast = ContrastLevel.Standard), chrome)
        assertSame(chrome, chrome.forChrome())
    }

    private fun TonalPalette.tones(): List<Int> = RampSet.Tones.map { tone -> tone(tone) }
}
