package com.materialkolor.builder.codegen.target.fluent

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixture
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.target.lintFailures
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportPrefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The golden cases of the Fluent dynamic export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object FluentDynamicCases {
    val VibrantPrimaryOverride: Fixture = Fixtures.PrimaryOverride.fluent().let { fixture ->
        fixture.with(document = fixture.input.document.copy(style = Style.Vibrant))
    }

    val all: Map<String, ExportInput> = mapOf(
        "fluent-dynamic-default" to Fixtures.Default.fluent().input,
        "fluent-dynamic-vibrant-primary-override" to VibrantPrimaryOverride.input,
        "fluent-dynamic-animated" to Fixtures.Animated.fluent().input,
        "fluent-dynamic-inline" to Fixtures.Default.fluentInline(),
        "fluent-dynamic-inline-vibrant-primary-override" to VibrantPrimaryOverride.input.inline(),
        "fluent-dynamic-inline-animated" to Fixtures.Animated.fluentInline(),
    )

    fun files(case: String): List<GeneratedFile> = FluentDynamic.files(all.getValue(case))
}

/**
 * This fixture exported for Fluent.
 */
internal fun Fixture.fluent(): Fixture = with(document = input.document.copy(library = Library.Fluent))

/**
 * The same export when `material-kolor-fluent` is not published, so the shades are mapped inline.
 */
internal fun ExportInput.inline(): ExportInput = copy(versions = versions.copy(fluentModuleAvailable = false))

/**
 * This fixture exported for Fluent without the module.
 */
internal fun Fixture.fluentInline(): ExportInput = fluent().input.inline()

class FluentDynamicTest {
    @Test
    fun fluentDynamic_everyCase_matchesTheGoldenHash() {
        FluentDynamicCases.all.keys.forEach { case ->
            assertEquals(GoldenHashes.cases[case], GoldenDigest.of(FluentDynamicCases.files(case)), case)
        }
    }

    @Test
    fun fluentDynamic_everyCodeLine_passesTheLintLimits() {
        FluentDynamicCases.all.keys.forEach { case ->
            assertEquals(emptyList(), lintFailures(FluentDynamicCases.files(case)), case)
        }
    }

    @Test
    fun fluentDynamic_primaryOverride_writesTheSeedThePrimaryAndTheStyle() {
        val input = FluentDynamicCases.VibrantPrimaryOverride.input

        listOf(input, input.inline()).forEach { candidate ->
            val theme = theme(candidate)

            assertTrue("seedColor = SeedColor," in theme, theme)
            assertTrue("isDark = isDark," in theme, theme)
            assertTrue("primary = Primary," in theme, theme)
            assertTrue("style = PaletteStyle.Vibrant," in theme, theme)
        }
    }

    @Test
    fun fluentDynamic_module_callsRememberFluentColors() {
        val theme = theme(Fixtures.Default.fluent().input)

        assertTrue("val colors = rememberFluentColors(" in theme, theme)
        assertFalse("rememberDynamicScheme" in theme, theme)
        assertFalse(SWAP_TO_MODULE_NOTE in theme, theme)
        assertTrue("FluentTheme(\n        colors = colors,\n        content = content,\n    )" in theme, theme)
    }

    @Test
    fun fluentDynamic_inline_mapsTheShadesFromCore() {
        val theme = theme(Fixtures.Default.fluentInline())

        assertTrue("// $SWAP_TO_MODULE_NOTE" in theme, theme)
        assertTrue("val scheme = rememberDynamicScheme(" in theme, theme)
        assertTrue("shades = scheme.primaryPalette.toShades()," in theme, theme)
        assertTrue("private fun TonalPalette.toShades(): Shades {" in theme, theme)
        assertFalse("com.materialkolor.fluent" in theme, theme)
        val tones = listOf("base" to 50, "light1" to 60, "light2" to 80, "light3" to 90)
        val darkTones = listOf("dark1" to 40, "dark2" to 30, "dark3" to 15)
        (tones + darkTones).forEach { (shade, tone) -> assertTrue("val $shade = toneColor($tone)" in theme, theme) }
    }

    @Test
    fun fluentDynamic_animate_wrapsTheColorsOnlyWithTheModule() {
        val module = theme(Fixtures.Animated.fluent().input)
        val inline = theme(Fixtures.Animated.fluentInline())
        val wrapped = "val colors = animateFluentColors(targetColors, animationSpec = { tween(durationMillis = 500) })"

        assertTrue(wrapped in module, module)
        assertFalse("animateFluentColors" in inline, inline)
        assertFalse("tween" in inline, inline)
    }

    @Test
    fun fluentDynamic_inlineAnimate_saysTheModuleAnimates() {
        val animated = theme(Fixtures.Animated.fluentInline())
        val still = theme(Fixtures.Default.fluentInline())
        val module = theme(Fixtures.Animated.fluent().input)

        assertTrue("// $SWAP_TO_MODULE_ANIMATED_NOTE\n" in animated, animated)
        assertFalse("// $SWAP_TO_MODULE_NOTE\n" in animated, animated)
        assertTrue("// $SWAP_TO_MODULE_NOTE\n" in still, still)
        assertFalse(SWAP_TO_MODULE_ANIMATED_NOTE in module, module)
    }

    @Test
    fun fluentDynamic_hiddenKeyColors_areNeverWritten() {
        val input = Fixtures.AllOverrides.fluent().input

        listOf(input, input.inline()).forEach { candidate ->
            val text = FluentDynamic.files(candidate).joinToString("\n") { it.text }

            assertTrue("val Primary = Color(0xFF6750A4)" in text, text)
            assertTrue("primary = Primary," in text, text)
            listOf("Secondary", "Tertiary", "Neutral", "Error").forEach { hidden ->
                assertFalse(hidden in text, "$hidden in $text")
            }
        }
    }

    @Test
    fun fluentDynamic_contrastPinsAccentsAndAmoled_areNeverWritten() {
        val document = Fixtures.ThreeAccents.input.document.copy(
            library = Library.Fluent,
            contrast = Fixtures.HighContrast.input.document.contrast,
            pins = Fixtures.Pins.input.document.pins,
            amoled = true,
        )
        val input = Fixtures.input(document, ExportPrefs())

        listOf(input, input.inline()).forEach { candidate ->
            val text = FluentDynamic.files(candidate).joinToString("\n") { it.text }

            listOf("contrastLevel", "BrandSeed", "Amoled", "0xFF8B1A10").forEach { absent ->
                assertFalse(absent in text, "$absent in $text")
            }
        }
    }

    @Test
    fun fluentDynamic_otherLibrary_isRefused() {
        val unstyled = Fixtures.input(document = ThemeDocument.Default.copy(library = Library.Unstyled))

        assertFailsWith<IllegalArgumentException> { FluentDynamic.files(unstyled) }
    }

    private fun theme(input: ExportInput): String =
        FluentDynamic.files(input).single { it.path.endsWith("/Theme.kt") }.text
}
