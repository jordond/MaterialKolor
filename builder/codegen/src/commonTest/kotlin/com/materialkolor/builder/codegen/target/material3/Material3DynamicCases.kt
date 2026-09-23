package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixture
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.MAX_LINE_LENGTH
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportPrefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The golden cases of the Material 3 dynamic export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object Material3DynamicCases {
    private val ExpressiveDefault: Fixture = Fixtures.Default.with(
        document = ThemeDocument.Default.copy(expressive = true, style = Style.Expressive, spec = SpecVersion.Spec2025),
    )

    private val ExpressivePins: Fixture = Fixtures.Pins.with(
        document = Fixtures.Pins.input.document.copy(
            expressive = true,
            style = Style.Expressive,
            spec = SpecVersion.Spec2025,
            motionScheme = MotionSchemeChoice.Standard,
        ),
    )

    private val plain: List<Fixture> = listOf(
        Fixtures.Default,
        Fixtures.PrimaryOverride,
        Fixtures.AllOverrides,
        Fixtures.Cmf,
        Fixtures.ReducedContrast,
        Fixtures.HighContrast,
        Fixtures.Amoled,
        Fixtures.Watch2025,
        Fixtures.ThreeAccents,
        Fixtures.Pins,
        Fixtures.Animated,
        Fixtures.AndroidOnly,
    )

    val all: Map<String, ExportInput> =
        plain.associate { fixture -> "material3-dynamic-${fixture.name}" to fixture.input } +
            mapOf(
                "expressive-dynamic-default" to ExpressiveDefault.input,
                "expressive-dynamic-tonal-spot-2021" to Fixtures.ExpressiveOnTonalSpot2021.input,
                "expressive-dynamic-pins" to ExpressivePins.input,
            )

    fun files(case: String): List<GeneratedFile> = Material3Dynamic.files(all.getValue(case))
}

class Material3DynamicTest {
    @Test
    fun material3Dynamic_everyCase_matchesTheGoldenHash() {
        Material3DynamicCases.all.keys.forEach { case ->
            assertEquals(GoldenHashes.cases[case], GoldenDigest.of(Material3DynamicCases.files(case)), case)
        }
    }

    @Test
    fun material3Dynamic_everyCodeLine_fitsTheColumnLimit() {
        Material3DynamicCases.all.keys.forEach { case ->
            Material3DynamicCases.files(case).forEach { file ->
                val lines = file.text.lines()
                // The header's share link grows with the theme. ktlint leaves a line that is only a comment alone.
                val code = lines.filterNot { it.startsWith("//") }
                assertTrue(code.all { it.length <= MAX_LINE_LENGTH }, "${file.path} in $case")
                assertTrue(lines.none { it.endsWith(" ") }, "${file.path} in $case")
            }
        }
    }

    @Test
    fun material3Dynamic_primaryOverride_writesTheSeedAndThePrimary() {
        val theme = theme(Fixtures.PrimaryOverride.input)

        assertTrue("seedColor = SeedColor," in theme, theme)
        assertTrue("primary = Primary," in theme, theme)
    }

    @Test
    fun material3Dynamic_stateForm_onlyWithPins() {
        Material3DynamicCases.all.forEach { (case, input) ->
            val theme = theme(input)
            val pinned = input.document.pins.isNotEmpty()

            assertEquals(pinned, "rememberDynamicMaterialThemeState(" in theme, case)
            assertEquals(pinned, "state = state," in theme, case)
        }
    }

    @Test
    fun material3Dynamic_expressiveOnTonalSpot2021_writesStyleAndSpec() {
        val theme = theme(Fixtures.ExpressiveOnTonalSpot2021.input)

        assertTrue("style = PaletteStyle.TonalSpot," in theme, theme)
        assertTrue("specVersion = ColorSpec.SpecVersion.SPEC_2021," in theme, theme)
        assertTrue("@OptIn(ExperimentalMaterial3ExpressiveApi::class)" in theme, theme)
        assertTrue("motionScheme = MotionScheme.expressive()," in theme, theme)
    }

    @Test
    fun material3Dynamic_defaults_areLeftOut() {
        val theme = theme(Fixtures.Default.input)

        listOf("style", "contrastLevel", "specVersion", "platform", "isAmoled", "animate").forEach { name ->
            assertFalse("$name =" in theme, "$name in $theme")
        }
    }

    @Test
    fun material3Dynamic_contrastLevel_isWrittenWithoutDoubleToString() {
        assertTrue("contrastLevel = -1.0," in theme(Fixtures.ReducedContrast.input))
        assertTrue("contrastLevel = 1.0," in theme(Fixtures.HighContrast.input))
    }

    @Test
    fun material3Dynamic_animate_alwaysWritesTheDuration() {
        val theme = theme(Fixtures.Animated.input)

        assertTrue("animate = true," in theme, theme)
        assertTrue("animationSpec = tween(durationMillis = 500)," in theme, theme)
    }

    @Test
    fun material3Dynamic_accents_writeExtendedColorsOnlyWhenThereAreAny() {
        val withAccents = Material3Dynamic.files(Fixtures.ThreeAccents.input).map { it.path.substringAfterLast('/') }
        val without = Material3Dynamic.files(Fixtures.Default.input).map { it.path.substringAfterLast('/') }

        assertEquals(listOf("Color.kt", "Theme.kt", "ExtendedColors.kt"), withAccents)
        assertEquals(listOf("Color.kt", "Theme.kt"), without)
    }

    @Test
    fun material3Dynamic_accents_harmonizeOnlyWhenAsked() {
        val extended = Material3Dynamic.files(Fixtures.ThreeAccents.input).last().text

        // b-110a
        val harmonized = "rememberTonalPalette(\n        seed = BrandSeed,\n        harmonizeWith = seedColor,\n    )"
        assertTrue(harmonized in extended, extended)
        assertTrue("rememberTonalPalette(seed = SuccessSeed)" in extended, extended)
        assertTrue("threshold = ContrastThreshold.WCAG_AAA_NORMAL_TEXT" in extended, extended)
    }

    @Test
    fun material3Dynamic_otherLibrary_isRefused() {
        val fluent = Fixtures.input(
            document = ThemeDocument.Default.copy(library = Library.Fluent),
            prefs = ExportPrefs(),
        )

        assertFailsWith<IllegalArgumentException> { Material3Dynamic.files(fluent) }
    }

    private fun theme(input: ExportInput): String =
        Material3Dynamic.files(input).single { it.path.endsWith("/Theme.kt") }.text
}
