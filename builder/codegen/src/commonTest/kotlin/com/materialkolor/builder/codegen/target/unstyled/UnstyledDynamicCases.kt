package com.materialkolor.builder.codegen.target.unstyled

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixture
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.target.lintFailures
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The golden cases of the Unstyled dynamic export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object UnstyledDynamicCases {
    private val fixtures: List<Fixture> = listOf(
        Fixtures.Default,
        Fixtures.PrimaryOverride,
        Fixtures.Pins,
        Fixtures.ThreeAccents,
        Fixtures.Animated,
    ).map { fixture -> fixture.unstyled() }

    val all: Map<String, ExportInput> =
        fixtures.associate { fixture -> "unstyled-dynamic-${fixture.name}" to fixture.input }

    fun files(case: String): List<GeneratedFile> = UnstyledDynamic.files(all.getValue(case))
}

/** This fixture exported for Unstyled. */
internal fun Fixture.unstyled(): Fixture = with(document = input.document.copy(library = Library.Unstyled))

class UnstyledDynamicTest {
    @Test
    fun unstyledDynamic_everyCase_matchesTheGoldenHash() {
        UnstyledDynamicCases.all.keys.forEach { case ->
            assertEquals(GoldenHashes.cases[case], GoldenDigest.of(UnstyledDynamicCases.files(case)), case)
        }
    }

    @Test
    fun unstyledDynamic_everyCodeLine_passesTheLintLimits() {
        UnstyledDynamicCases.all.keys.forEach { case ->
            assertEquals(emptyList(), lintFailures(UnstyledDynamicCases.files(case)), case)
        }
    }

    @Test
    fun unstyledDynamic_primaryOverride_writesTheSeedAndThePrimary() {
        val theme = theme(Fixtures.PrimaryOverride.unstyled().input)

        assertTrue("seedColor = SeedColor," in theme, theme)
        assertTrue("primary = Primary," in theme, theme)
    }

    @Test
    fun unstyledDynamic_default_writesOnlyTheSeed() {
        val theme = theme(Fixtures.Default.unstyled().input)

        assertTrue("val AppTheme = buildThemeV2 { dynamicColorSchemes(seedColor = SeedColor) }" in theme, theme)
        assertFalse(TRANSITION in theme, theme)
    }

    @Test
    fun unstyledDynamic_explicitSchemes_onlyWithPinsOrAccents() {
        UnstyledDynamicCases.all.forEach { (case, input) ->
            val theme = theme(input)
            val explicit = input.document.pins.isNotEmpty() || input.document.accents.isNotEmpty()

            assertEquals(!explicit, "dynamicColorSchemes(" in theme, case)
            assertEquals(if (explicit) 2 else 0, theme.occurrences("rememberDynamicScheme("), case)
            assertEquals(if (explicit) 1 else 0, theme.occurrences("colorScheme(ColorScheme.Dark)"), case)
        }
    }

    @Test
    fun unstyledDynamic_pins_landInTheirOwnModeOnly() {
        val theme = theme(Fixtures.Pins.unstyled().input)
        val (light, dark) = theme.split("colorScheme(ColorScheme.Dark)")

        assertTrue("MaterialKolorTokens.primary to Color(0xFF8B1A10)," in light, light)
        assertTrue("MaterialKolorTokens.surface to Color(0xFFFFFBFF)," in light, light)
        assertFalse("MaterialKolorTokens.outline" in light, light)
        assertTrue("MaterialKolorTokens.primary to Color(0xFFFFB4A8)," in dark, dark)
        assertTrue("MaterialKolorTokens.outline to Color(0xFF9A8C89)," in dark, dark)
        assertFalse("MaterialKolorTokens.surface" in dark, dark)
    }

    @Test
    fun unstyledDynamic_accents_followTheMaterial3Chain() {
        val theme = theme(Fixtures.ThreeAccents.unstyled().input)
        val harmonized = "rememberTonalPalette(\n        seed = BrandSeed,\n        harmonizeWith = SeedColor,\n    )"

        assertTrue(harmonized in theme, theme)
        assertTrue("rememberTonalPalette(seed = SuccessSeed)" in theme, theme)
        assertTrue("ThemeTokens.brand to brandPalette.toneColor(40)," in theme, theme)
        assertTrue("ThemeTokens.onBrandContainer to brandPalette.onTone(30)," in theme, theme)
        assertTrue("warningPalette.onTone(50, ContrastThreshold.WCAG_AAA_NORMAL_TEXT)" in theme, theme)
    }

    @Test
    fun unstyledDynamic_tokens_onlyWithAccents() {
        val withAccents = UnstyledDynamic.files(Fixtures.ThreeAccents.unstyled().input)
        val without = UnstyledDynamic.files(Fixtures.Default.unstyled().input)
        val tokens = withAccents.single { it.path.endsWith("/Tokens.kt") }.text

        assertEquals(listOf("Color.kt", "Theme.kt", "Tokens.kt"), withAccents.map { it.path.substringAfterLast('/') })
        assertEquals(listOf("Color.kt", "Theme.kt"), without.map { it.path.substringAfterLast('/') })
        assertEquals(12, tokens.occurrences("ThemeToken<Color>("), tokens)
        assertTrue("val onWarningContainer = ThemeToken<Color>(\"onWarningContainer\")" in tokens, tokens)
    }

    @Test
    fun unstyledDynamic_animate_setsTheTransitionSpec() {
        val theme = theme(Fixtures.Animated.unstyled().input)

        assertTrue("$TRANSITION = tween(durationMillis = 500)" in theme, theme)
    }

    @Test
    fun unstyledDynamic_amoled_isNeverWritten() {
        val theme = theme(Fixtures.Amoled.unstyled().input)

        assertFalse("Amoled" in theme, theme)
    }

    @Test
    fun unstyledDynamic_otherLibrary_isRefused() {
        val fluent = Fixtures.input(document = ThemeDocument.Default.copy(library = Library.Fluent))

        assertFailsWith<IllegalArgumentException> { UnstyledDynamic.files(fluent) }
    }

    private fun theme(input: ExportInput): String =
        UnstyledDynamic.files(input).single { it.path.endsWith("/Theme.kt") }.text

    private fun String.occurrences(text: String): Int = windowed(text.length).count { window -> window == text }

    private companion object {
        const val TRANSITION = "colorSchemeTransitionSpec"
    }
}
