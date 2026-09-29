package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixture
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.target.expectedVariants
import com.materialkolor.builder.codegen.target.frozenPrefs
import com.materialkolor.builder.codegen.target.lintFailures
import com.materialkolor.builder.codegen.target.materialKolorImports
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.FrozenVariants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The golden cases of the Material 3 frozen export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object Material3FrozenCases {
    private val ExpressiveDocument: ThemeDocument =
        Fixtures.Base.copy(expressive = true, style = Style.Expressive, spec = SpecVersion.Spec2025)

    val AccentsPinsAmoled: Fixture = Fixtures.Pins.with(
        document = Fixtures.Pins.input.document.copy(
            accents = Fixtures.ThreeAccents.input.document.accents,
            amoled = true,
        ),
        prefs = frozenPrefs(),
    )

    private val AndroidDynamicColor: ExportPrefs = frozenPrefs().copy(multiplatform = false, androidDynamicColor = true)

    /**
     * Accents with the wallpaper colors, which no golden covers.
     */
    val AccentsAndroidDynamicColor: ExportInput = AccentsPinsAmoled.with(prefs = AndroidDynamicColor).input

    /**
     * The same, expressive.
     */
    val ExpressiveAccentsAndroidDynamicColor: ExportInput = AccentsPinsAmoled
        .with(
            document = AccentsPinsAmoled.input.document.copy(
                expressive = true,
                style = Style.Expressive,
                spec = SpecVersion.Spec2025,
            ),
            prefs = AndroidDynamicColor,
        ).input

    val all: Map<String, ExportInput> = mapOf(
        "material3-frozen-default" to Fixtures.Default.with(prefs = frozenPrefs()).input,
        "material3-frozen-accents-pins-amoled" to AccentsPinsAmoled.input,
        "material3-frozen-all-contrasts" to
            Fixtures.Default.with(prefs = frozenPrefs(FrozenVariants.AllContrasts)).input,
        "expressive-frozen-default" to
            Fixtures.Default.with(document = ExpressiveDocument, prefs = frozenPrefs()).input,
        "expressive-frozen-all-contrasts" to Fixtures.Default
            .with(
                document = ExpressiveDocument.copy(motionScheme = MotionSchemeChoice.Standard),
                prefs = frozenPrefs(FrozenVariants.AllContrasts),
            ).input,
        "material3-frozen-android-dynamic-color" to Fixtures.Default.with(prefs = AndroidDynamicColor).input,
        "inklet-frozen-default" to Fixtures.Default
            .with(document = Fixtures.Base.copy(library = Library.Inklet), prefs = frozenPrefs())
            .input,
    )

    fun files(case: String): List<GeneratedFile> = Material3Frozen.files(all.getValue(case))
}

class Material3FrozenTest {
    @Test
    fun material3Frozen_everyCase_matchesTheGoldenHash() {
        Material3FrozenCases.all.keys.forEach { case ->
            assertEquals(GoldenHashes.cases[case], GoldenDigest.of(Material3FrozenCases.files(case)), case)
        }
    }

    @Test
    fun material3Frozen_everyCodeLine_passesTheLintLimits() {
        Material3FrozenCases.all.keys.forEach { case ->
            assertEquals(emptyList(), lintFailures(Material3FrozenCases.files(case)), case)
        }
    }

    @Test
    fun material3Frozen_everyCase_importsNothingFromMaterialKolor() {
        Material3FrozenCases.all.keys.forEach { case ->
            assertEquals(emptyList(), materialKolorImports(Material3FrozenCases.files(case)), case)
        }
    }

    @Test
    fun material3Frozen_everyRole_isWrittenForBothModesAtEveryVariant() {
        Material3FrozenCases.all.forEach { (case, input) ->
            val files = Material3Frozen.files(input)
            val colors = files.single { it.path.endsWith("/Color.kt") }.text
            val theme = files.single { it.path.endsWith("/Theme.kt") }.text

            assertEquals(input.resolved.roles.keys, input.prefs.frozenVariants.expectedVariants(), case)
            input.resolved.roles.forEach { (variant, table) ->
                Role.entries.forEach { role ->
                    val parameter = role.name.replaceFirstChar { it.lowercaseChar() }
                    val suffix = variant.suffix
                    listOf("Light" to table.light, "Dark" to table.dark).forEach { (mode, values) ->
                        val name = "$parameter$mode$suffix"
                        val literal = Literals.hexText(values.getValue(role).value)
                        assertTrue("val $name = Color($literal)" in colors, "$name in $case")
                        assertTrue("    $parameter = $name,\n" in theme, "$name in $case")
                    }
                }
            }
        }
    }

    @Test
    fun material3Frozen_allContrasts_namesEverySchemeAndUsesTheStandardPair() {
        val theme = theme(Material3FrozenCases.all.getValue("material3-frozen-all-contrasts"))

        listOf(
            "val lightScheme = lightColorScheme(",
            "val darkScheme = darkColorScheme(",
            "val mediumContrastLightColorScheme = lightColorScheme(",
            "val mediumContrastDarkColorScheme = darkColorScheme(",
            "val highContrastLightColorScheme = lightColorScheme(",
            "val highContrastDarkColorScheme = darkColorScheme(",
            "colorScheme = if (isDark) darkScheme else lightScheme,",
        ).forEach { expected -> assertTrue(expected in theme, expected) }
    }

    @Test
    fun material3Frozen_expressive_writesTheMotionSchemeWithoutOptingIn() {
        val default = theme(Material3FrozenCases.all.getValue("expressive-frozen-default"))
        val standard = theme(Material3FrozenCases.all.getValue("expressive-frozen-all-contrasts"))

        assertTrue("MaterialExpressiveTheme(" in default, default)
        assertTrue("motionScheme = MotionScheme.expressive()," in default, default)
        assertTrue("motionScheme = MotionScheme.standard()," in standard, standard)
        assertFalse("@OptIn" in default, default)
    }

    @Test
    fun material3Frozen_accents_writeLiteralFamiliesOnlyWhenThereAreAny() {
        val withAccents = Material3Frozen.files(Material3FrozenCases.AccentsPinsAmoled.input)
        val without = Material3FrozenCases.files("material3-frozen-default")
        val extended = withAccents.last().text
        val brand = Material3FrozenCases.AccentsPinsAmoled.input.resolved.accents
            .first()
            .light

        assertEquals(listOf("Color.kt", "Theme.kt", "ExtendedColors.kt"), withAccents.map { it.fileName })
        assertEquals(listOf("Color.kt", "Theme.kt"), without.map { it.fileName })
        assertTrue("val extendedLight = ExtendedColors(" in extended, extended)
        assertTrue("val extendedDark = ExtendedColors(" in extended, extended)
        assertTrue("colorContainer = Color(${Literals.hexText(brand.container.value)})," in extended, extended)
        assertTrue("onColorContainer = Color(${Literals.hexText(brand.onContainer.value)})," in extended, extended)
        assertTrue("LocalExtendedColors provides extendedColors" in withAccents[1].text)
    }

    @Test
    fun material3Frozen_androidDynamicColor_fallsBackToTheLiteralScheme() {
        val theme = theme(Material3FrozenCases.all.getValue("material3-frozen-android-dynamic-color"))
        val expected =
            """
            |    val context = LocalContext.current
            |
            |    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            |        MaterialTheme(
            |            colorScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
            |            content = content,
            |        )
            |    } else {
            |        MaterialTheme(
            |            colorScheme = if (isDark) darkScheme else lightScheme,
            |            content = content,
            |        )
            |    }
            """.trimMargin()

        assertTrue("    dynamicColor: Boolean = true,\n" in theme, theme)
        assertTrue(expected in theme, theme)
    }

    @Test
    fun material3Frozen_androidDynamicColor_changesNothingInAMultiplatformExport() {
        Material3FrozenCases.all.values.filter { input -> input.prefs.multiplatform }.forEach { input ->
            val asked = input.copy(prefs = input.prefs.copy(androidDynamicColor = true))

            assertEquals(Material3Frozen.files(input).texts(), Material3Frozen.files(asked).texts())
        }
    }

    @Test
    fun material3Frozen_accentsWithAndroidDynamicColor_provideAroundTheBranch() {
        assertProvidesAroundTheBranch(Material3FrozenCases.AccentsAndroidDynamicColor, themeCall = "MaterialTheme(")
    }

    @Test
    fun material3Frozen_expressiveAccentsWithAndroidDynamicColor_provideAroundTheBranch() {
        assertProvidesAroundTheBranch(
            input = Material3FrozenCases.ExpressiveAccentsAndroidDynamicColor,
            themeCall = "MaterialExpressiveTheme(",
        )
    }

    @Test
    fun material3Frozen_inklet_wrapsTheContentInInkletTheme() {
        val theme = theme(Material3FrozenCases.all.getValue("inklet-frozen-default"))

        assertTrue("    MaterialTheme(\n" in theme, theme)
        assertTrue("        content = { InkletTheme(content = content) },\n" in theme, theme)
        assertTrue("import dev.ggoggam.inklet.InkletTheme" in theme, theme)
    }

    @Test
    fun material3Frozen_otherLibrary_isRefused() {
        val fluent = Fixtures.input(
            document = Fixtures.Base.copy(library = Library.Fluent),
            prefs = frozenPrefs(),
        )

        assertFailsWith<IllegalArgumentException> { Material3Frozen.files(fluent) }
    }

    /**
     * The accents are provided around the whole `if`, so the wallpaper branch reads them too, and the
     * `else` still calls [themeCall] on the literal standard pair.
     */
    private fun assertProvidesAroundTheBranch(
        input: ExportInput,
        themeCall: String,
    ) {
        val theme = theme(input)
        val provider = theme.indexOf("    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {\n")
        val branch = theme.indexOf("        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {\n")
        val literal = "        } else {\n            $themeCall\n" +
            "                colorScheme = if (isDark) darkScheme else lightScheme,\n"

        assertTrue(provider >= 0, theme)
        assertTrue(branch > provider, theme)
        assertTrue(literal in theme, theme)
    }

    private fun theme(input: ExportInput): String =
        Material3Frozen.files(input).single { it.fileName == "Theme.kt" }.text

    private val GeneratedFile.fileName: String
        get() = path.substringAfterLast('/')

    private fun List<GeneratedFile>.texts(): List<Pair<String, String>> = map { file -> file.path to file.text }

    private val ContrastVariant.suffix: String
        get() = when (this) {
            ContrastVariant.Standard -> ""
            ContrastVariant.Medium -> "MediumContrast"
            ContrastVariant.High -> "HighContrast"
        }
}
