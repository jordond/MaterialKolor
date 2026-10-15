package com.materialkolor.builder.domain.link

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegacyQueryTest {
    /**
     * What an old link starts from, the default theme on the old builder's 2021 spec.
     */
    private val default = ThemeDocument.Default.copy(spec = SpecVersion.Spec2021)

    @Test
    fun parse_emptyQuery_isTheDefaultDocument() {
        assertEquals(LegacyImport(default, previewMode = null, packageName = null), LegacyQuery.parse(""))
        assertEquals(LegacyImport(default, previewMode = null, packageName = null), LegacyQuery.parse("?"))
    }

    @Test
    fun parse_colorSeed_readsEveryHexSpelling() {
        val expected = Argb(0xFF6750A4.toInt())
        listOf("FF6750A4", "ff6750a4", "6750A4", "%236750A4", "#6750A4", "006750A4").forEach { value ->
            assertEquals(expected, LegacyQuery.parse("?color_seed=$value").document.seed, value)
        }
    }

    @Test
    fun parse_colorSeedThatIsNotHex_isSkipped() {
        listOf("", "6750A", "6750A4F", "GG6750A4", "FF6750A4FF", "red", "-6750A4").forEach { value ->
            assertEquals(default.seed, LegacyQuery.parse("?color_seed=$value").document.seed, value)
        }
    }

    @Test
    fun parse_keyColors_setTheirOwnPaletteEach() {
        val query =
            "color_primary=FF0000FF&color_secondary=FF00FF00&color_tertiary=FFFF0000&color_error=FF111111" +
                "&color_neutral=FF222222&color_neutralvariant=FF333333"
        val expected =
            KeyColors(
                primary = Argb(0x0000FF),
                secondary = Argb(0x00FF00),
                tertiary = Argb(0xFF0000),
                error = Argb(0x111111),
                neutral = Argb(0x222222),
                neutralVariant = Argb(0x333333),
            )
        assertEquals(expected, LegacyQuery.parse(query).document.keyColors)
    }

    @Test
    fun parse_style_readsEveryName() {
        Style.entries.forEach { style ->
            assertEquals(style, LegacyQuery.parse("style=${style.name}").document.style)
        }
    }

    @Test
    fun parse_cmfStyleWithASeed_setsTheTertiarySeed() {
        val document = LegacyQuery.parse("?style=Cmf%3AFF7D5260").document
        assertEquals(Style.Cmf, document.style)
        assertEquals(Argb(0x7D5260), document.cmfTertiarySeed)
        assertEquals(document, LegacyQuery.parse("?style=Cmf:ff7d5260").document)
    }

    @Test
    fun parse_styleItDoesNotKnow_isSkipped() {
        listOf("tonalspot", "Cmf:", "Cmf:XYZ", "Cmf:FF7D5", "Pastel", "").forEach { value ->
            val document = LegacyQuery.parse("style=$value").document
            assertEquals(Style.TonalSpot, document.style, value)
            assertNull(document.cmfTertiarySeed, value)
        }
        assertEquals(Style.Cmf, LegacyQuery.parse("style=Cmf:7D5260").document.style)
    }

    @Test
    fun parse_contrast_readsAnyDoubleAtTheNearestNamedLevel() {
        val cases =
            mapOf(
                "-1.0" to ContrastLevel.Reduced,
                "0.5" to ContrastLevel.Medium,
                "1" to ContrastLevel.High,
                "0" to ContrastLevel.Standard,
                "0.333" to ContrastLevel.Medium,
                "0.25" to ContrastLevel.Standard,
                "2.5" to ContrastLevel.High,
                "-7" to ContrastLevel.Reduced,
            )
        cases.forEach { (value, level) ->
            assertEquals(level, LegacyQuery.parse("contrast=$value").document.contrast, value)
        }
    }

    @Test
    fun parse_contrastThatIsNotANumber_isSkipped() {
        listOf("NaN", "high", "", "0.5.1").forEach { value ->
            assertEquals(ContrastLevel.Standard, LegacyQuery.parse("contrast=$value").document.contrast, value)
        }
    }

    @Test
    fun parse_colorSpec_readsTheLibraryNames() {
        assertEquals(SpecVersion.Spec2021, LegacyQuery.parse("color_spec=SPEC_2021").document.spec)
        assertEquals(SpecVersion.Spec2025, LegacyQuery.parse("color_spec=SPEC_2025").document.spec)
        assertEquals(SpecVersion.Spec2026, LegacyQuery.parse("color_spec=SPEC_2026").document.spec)
        assertEquals(SpecVersion.Spec2021, LegacyQuery.parse("color_spec=Spec2025").document.spec)
    }

    @Test
    fun parse_noColorSpec_staysOn2021ThoughANewThemeAsksForTheNewest() {
        assertEquals(SpecVersion.Spec2026, ThemeDocument.Default.spec)
        assertEquals(SpecVersion.Spec2021, LegacyQuery.parse("color_seed=FF6750A4").document.spec)
    }

    @Test
    fun parse_darkMode_setsThePreviewModeAndNotTheDocument() {
        val dark = LegacyQuery.parse("dark_mode=true")
        assertEquals(LegacyPreviewMode.Dark, dark.previewMode)
        assertEquals(default, dark.document)
        assertNull(LegacyQuery.parse("dark_mode=yes").previewMode)
        assertNull(LegacyQuery.parse("color_seed=FF6750A4").previewMode)
    }

    @Test
    fun parse_darkModeFalse_leavesThePreviewModeUnset() {
        val light = LegacyQuery.parse("dark_mode=false")
        assertNull(light.previewMode)
        assertEquals(default, light.document)
        assertNull(LegacyQuery.parse("dark_mode=true&dark_mode=false").previewMode)
        assertEquals(LegacyPreviewMode.Dark, LegacyQuery.parse("dark_mode=false&dark_mode=true").previewMode)
    }

    @Test
    fun parse_isAmoled_setsAmoled() {
        assertTrue(LegacyQuery.parse("is_amoled=true").document.amoled)
        assertFalse(LegacyQuery.parse("is_amoled=false").document.amoled)
        assertFalse(LegacyQuery.parse("is_amoled=1").document.amoled)
    }

    @Test
    fun parse_expressive_meansMaterial3WithTheExpressiveExtras() {
        val expressive = LegacyQuery.parse("expressive=true").document
        assertEquals(Library.Material3, expressive.library)
        assertTrue(expressive.expressive)
        assertFalse(LegacyQuery.parse("expressive=false").document.expressive)
    }

    @Test
    fun parse_packageName_goesBesideTheDocument() {
        val legacy = LegacyQuery.parse("?package_name=com.example.theme&color_seed=FF6750A4")
        assertEquals("com.example.theme", legacy.packageName)
        assertEquals(default.copy(seed = Argb(0x6750A4)), legacy.document)
        assertNull(LegacyQuery.parse("package_name=").packageName)
        assertEquals("com.ex ample", LegacyQuery.parse("package_name=com.ex%20ample").packageName)
    }

    @Test
    fun parse_selectedPresetId_becomesAPresetSeedSource() {
        (0..4).forEach { index ->
            val id = "res-$index"
            assertEquals(SeedSource.Preset(id), LegacyQuery.parse("selected_preset_id=$id").document.seedSource)
        }
        listOf("res-5", "preset", "").forEach { value ->
            assertEquals(SeedSource.Typed, LegacyQuery.parse("selected_preset_id=$value").document.seedSource, value)
        }
    }

    @Test
    fun parse_miscAndDestination_areReadAndIgnored() {
        val legacy = LegacyQuery.parse("misc=true&destination=Export")
        assertEquals(LegacyImport(default, previewMode = null, packageName = null), legacy)
    }

    @Test
    fun parse_malformedSegments_areSkippedOneByOne() {
        val query = "?ref&color_seed=FF6750A4&&=true&is_amoled&contrast=%ZZ&style=%&unknown=1&dark_mode=true&"
        val legacy = LegacyQuery.parse(query)
        assertEquals(default.copy(seed = Argb(0x6750A4)), legacy.document)
        assertEquals(LegacyPreviewMode.Dark, legacy.previewMode)
    }

    @Test
    fun parse_repeatedKey_keepsTheLastOne() {
        assertEquals(Argb(0x00FF00), LegacyQuery.parse("color_seed=FF0000FF&color_seed=FF00FF00").document.seed)
    }

    @Test
    fun parse_linkTheOldBuilderWrote_readsEveryField() {
        val query =
            "?color_seed=FF6750A4&color_primary=FF00658E&dark_mode=true&style=Cmf%3AFF7D5260" +
                "&selected_preset_id=res-2&contrast=0.5&is_amoled=true&color_spec=SPEC_2025" +
                "&package_name=com.example.theme&misc=true&expressive=true"
        val expected =
            LegacyImport(
                document =
                    default.copy(
                        seed = Argb(0x6750A4),
                        seedSource = SeedSource.Preset("res-2"),
                        keyColors = KeyColors().with(KeyColor.Primary, Argb(0x00658E)),
                        style = Style.Cmf,
                        cmfTertiarySeed = Argb(0x7D5260),
                        contrast = ContrastLevel.Medium,
                        spec = SpecVersion.Spec2025,
                        amoled = true,
                        library = Library.Material3,
                        expressive = true,
                    ),
                previewMode = LegacyPreviewMode.Dark,
                packageName = "com.example.theme",
            )
        assertEquals(expected, LegacyQuery.parse(query))
    }

    @Test
    fun parse_randomQueries_neverThrow() {
        val random = Random(20260922)
        val keys = LEGACY_KEYS + listOf("", "ref", "color", "style%3D", "%", "=")
        val values = listOf("", "true", "false", "FF6750A4", "#6750A4", "Cmf:FF7D5260", "-1.0", "NaN", "1e999")
        val alphabet = "abcdefABCDEF0123456789%#&=?+-._:é☕ "
        repeat(10_000) {
            val query =
                List(random.nextInt(until = 8)) {
                    when (random.nextInt(until = 3)) {
                        0 -> "${keys.random(random)}=${values.random(random)}"
                        1 -> "${keys.random(random)}=${noise(random, alphabet)}"
                        else -> noise(random, alphabet)
                    }
                }.joinToString(separator = "&", prefix = if (random.nextBoolean()) "?" else "")
            LegacyQuery.parse(query)
            LegacyQuery.recognizes(query)
        }
    }

    private fun noise(
        random: Random,
        alphabet: String,
    ): String = CharArray(random.nextInt(until = 16)) { alphabet.random(random) }.concatToString()

    private companion object {
        val LEGACY_KEYS: List<String> =
            listOf(
                "color_seed",
                "color_primary",
                "color_secondary",
                "color_tertiary",
                "color_error",
                "color_neutral",
                "color_neutralvariant",
                "style",
                "contrast",
                "color_spec",
                "dark_mode",
                "is_amoled",
                "expressive",
                "package_name",
                "misc",
                "selected_preset_id",
                "destination",
            )
    }
}
