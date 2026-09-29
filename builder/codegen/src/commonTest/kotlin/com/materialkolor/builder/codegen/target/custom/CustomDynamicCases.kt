package com.materialkolor.builder.codegen.target.custom

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixture
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.target.frozenPrefs
import com.materialkolor.builder.codegen.target.lintFailures
import com.materialkolor.builder.codegen.validate.ReservedNames
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.domain.model.TonalRamp
import com.materialkolor.builder.domain.persist.ExportTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The golden cases of the Custom dynamic export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object CustomDynamicCases {
    val PinsAmoled: Fixture = Fixtures.Pins.with(
        document = Fixtures.Pins.input.document
            .copy(library = Library.Custom, amoled = true),
    )

    /**
     * A tone on two ramp slots, one moved in light mode only and one moved to the same tone in both,
     * and a tone on a role slot, which the export ignores.
     */
    val CustomTones: Fixture = Fixtures.Default.with(
        document = Fixtures.Base.copy(
            library = Library.Custom,
            customTones = mapOf(
                CustomSlot.PrimaryPressed to CustomTone(light = 36),
                CustomSlot.BorderStrong to CustomTone(light = 60, dark = 60),
                CustomSlot.Primary to CustomTone(light = 50, dark = 50),
            ),
        ),
    )

    private val fixtures: List<Pair<String, Fixture>> = listOf(
        "default" to Fixtures.Default.custom(),
        "pins-amoled" to PinsAmoled,
        "custom-tones" to CustomTones,
        "accents" to Fixtures.ThreeAccents.custom(),
        "cmf" to Fixtures.Cmf.custom(),
    )

    val all: Map<String, ExportInput> =
        fixtures.associate { (name, fixture) -> "custom-dynamic-$name" to fixture.input }

    fun files(case: String): List<GeneratedFile> = CustomDynamic.files(all.getValue(case))
}

/**
 * This fixture exported for Custom.
 */
internal fun Fixture.custom(): Fixture = with(document = input.document.copy(library = Library.Custom))

class CustomDynamicTest {
    @Test
    fun customDynamic_everyCase_matchesTheGoldenHash() {
        CustomDynamicCases.all.keys.forEach { case ->
            assertEquals(GoldenHashes.cases[case], GoldenDigest.of(CustomDynamicCases.files(case)), case)
        }
    }

    @Test
    fun customDynamic_everyCodeLine_passesTheLintLimits() {
        CustomDynamicCases.all.keys.forEach { case ->
            assertEquals(emptyList(), lintFailures(CustomDynamicCases.files(case)), case)
        }
    }

    @Test
    fun customDynamic_files_areSeedsColorsAndTheme() {
        CustomDynamicCases.all.forEach { (case, input) ->
            val names = CustomDynamic.files(input).map { file -> file.path.substringAfterLast('/') }

            assertEquals(listOf("ThemeSeeds.kt", "ThemeColors.kt", "Theme.kt"), names, case)
        }
    }

    @Test
    fun customDynamic_themeColorsClass_isTheOneTheFrozenExportDeclares() {
        listOf(Fixtures.Default.custom(), Fixtures.ThreeAccents.custom()).forEach { fixture ->
            val dynamic = themeColors(fixture.input).declaration("data class ThemeColors(")
            val frozen = CustomFrozen
                .files(fixture.with(prefs = frozenPrefs()).input)
                .single { it.path.endsWith("/ThemeColors.kt") }
                .text
                .declaration("data class ThemeColors(")

            assertEquals(frozen, dynamic, fixture.name)
        }
    }

    @Test
    fun customDynamic_everySlot_isFilledOnceInEnumOrder() {
        CustomDynamicCases.all.forEach { (case, input) ->
            val colors = themeColors(input).substringAfter("        ThemeColors(")
            val filled = CustomSlot.entries.map { slot -> colors.indexOf("\n            ${slot.propertyName} = ") }

            assertTrue(filled.none { index -> index < 0 }, "$case misses a slot")
            assertEquals(filled.sorted(), filled, case)
        }
    }

    @Test
    fun customDynamic_scheme_takesTheSeedParameterAndEveryOverride() {
        val colors = themeColors(Fixtures.PrimaryOverride.custom().input)
        val seeds = file(Fixtures.PrimaryOverride.custom().input, "ThemeSeeds.kt")

        assertTrue("val scheme = rememberDynamicScheme(\n        seedColor = seedColor,\n" in colors, colors)
        val overrides = "        isDark = isDark,\n" +
            "        primary = Primary,\n" +
            "        specVersion = ColorSpec.SpecVersion.SPEC_2021,\n" +
            "    )"
        assertTrue(overrides in colors, colors)
        assertTrue("val SeedColor = Color(0xFFD9653B)" in seeds, seeds)
        assertTrue("val Primary = Color(0xFF6750A4)" in seeds, seeds)
    }

    @Test
    fun customDynamic_default_readsRolesAndCutsRamps() {
        val colors = themeColors(Fixtures.Default.custom().input)

        listOf(
            "val kolors = MaterialKolors(scheme)\n",
            "primary = kolors.primary(),",
            "surfaceInverse = kolors.inverseSurface(),",
            "scrim = kolors.scrim(),",
            "primaryPressed = scheme.primaryPalette.toneColor(if (isDark) 70 else 32),",
            "textStrong = scheme.neutralVariantPalette.toneColor(if (isDark) 90 else 10),",
            "focusRing = scheme.primaryPalette.toneColor(if (isDark) 60 else 50),",
            "shadow = scheme.neutralPalette.toneColor(0),",
            "return remember(scheme) {",
        ).forEach { line -> assertTrue(line in colors, "$line in $colors") }
        assertFalse("Amoled" in colors, colors)
    }

    @Test
    fun customDynamic_pinsAndAmoled_landInTheirOwnMode() {
        val colors = themeColors(CustomDynamicCases.PinsAmoled.input)

        listOf(
            "val kolors = MaterialKolors(scheme, isAmoled = true)",
            "primary = if (isDark) Color(0xFFFFB4A8) else Color(0xFF8B1A10),",
            "surface = if (isDark) kolors.surface() else Color(0xFFFFFBFF),",
            "onSurface = kolors.onSurface(),",
        ).forEach { line -> assertTrue(line in colors, "$line in $colors") }
    }

    @Test
    fun customDynamic_customTones_moveRampSlotsAndLeaveRoleSlots() {
        val colors = themeColors(CustomDynamicCases.CustomTones.input)

        assertTrue("primaryPressed = scheme.primaryPalette.toneColor(if (isDark) 70 else 36)," in colors, colors)
        assertTrue("borderStrong = scheme.neutralVariantPalette.toneColor(60)," in colors, colors)
        assertTrue("primary = kolors.primary()," in colors, colors)
    }

    @Test
    fun customDynamic_onRamp_usesOnToneUnlessMovedByATone() {
        val resolution = SlotResolution.OnRamp(TonalRamp.Tertiary, light = 40, dark = 80)

        assertEquals(
            "scheme.tertiaryPalette.onTone(if (isDark) 80 else 40)",
            render(slotValue(resolution, tone = null, pins = emptyMap())),
        )
        assertEquals(
            "if (isDark) scheme.tertiaryPalette.onTone(80) else scheme.tertiaryPalette.toneColor(20)",
            render(slotValue(resolution, tone = CustomTone(light = 20), pins = emptyMap())),
        )
        assertEquals(
            "scheme.tertiaryPalette.toneColor(if (isDark) 90 else 20)",
            render(slotValue(resolution, tone = CustomTone(light = 20, dark = 90), pins = emptyMap())),
        )
    }

    @Test
    fun customDynamic_rolePin_winsOverAToneOnTheSameSlot() {
        val resolution = SlotResolution.FromRole(Role.Primary)
        val pins = mapOf(Role.Primary to RolePin(dark = Fixtures.Default.input.document.seed))

        assertEquals(
            "if (isDark) Color(0xFFD9653B) else kolors.primary()",
            render(slotValue(resolution, tone = CustomTone(light = 10, dark = 90), pins = pins)),
        )
    }

    @Test
    fun customDynamic_accents_followTheMaterial3Chain() {
        val colors = themeColors(Fixtures.ThreeAccents.custom().input)
        val harmonized = "val brandPalette = rememberTonalPalette(\n" +
            "        seed = BrandSeed,\n        harmonizeWith = seedColor,\n    )"

        assertTrue(harmonized in colors, colors)
        assertTrue("val successPalette = rememberTonalPalette(seed = SuccessSeed)" in colors, colors)
        assertTrue("return remember(scheme, brandPalette, successPalette, warningPalette) {" in colors, colors)
        assertTrue("    val brand: ColorFamily,\n" in colors, colors)
        assertTrue("brand = brandPalette.colorFamily(\n" in colors, colors)
        assertTrue("threshold = ContrastThreshold.WCAG_AAA_NORMAL_TEXT," in colors, colors)
        assertTrue("private fun TonalPalette.colorFamily(" in colors, colors)
        assertFalse("ColorFamily" in themeColors(Fixtures.Default.custom().input))
    }

    @Test
    fun customDynamic_cmf_passesItsTertiarySeed() {
        val input = Fixtures.Cmf.custom().input

        assertTrue("style = PaletteStyle.Cmf(tertiarySeedColor = TertiarySeedColor)," in themeColors(input))
        assertTrue("val TertiarySeedColor = Color(0xFF2E7D32)" in file(input, "ThemeSeeds.kt"))
    }

    @Test
    fun customDynamic_theme_providesTheRememberedColors() {
        val theme = file(Fixtures.Default.custom().input, "Theme.kt")
        val colors = "val colors = rememberThemeColors(\n        seedColor = SeedColor,\n" +
            "        isDark = isDark,\n    )"

        assertTrue("val LocalThemeColors = staticCompositionLocalOf<ThemeColors> {" in theme, theme)
        assertTrue(colors in theme, theme)
        assertTrue("CompositionLocalProvider(LocalThemeColors provides colors, content = content)" in theme, theme)
        assertFalse("animate" in theme, theme)
    }

    @Test
    fun customDynamic_reservedNames_coverEveryImport() {
        val reserved = ReservedNames.of(ExportTarget.Custom)

        CustomDynamicCases.all.forEach { (case, input) ->
            val imported = CustomDynamic
                .files(input)
                .flatMap { file -> file.text.lines() }
                .filter { line -> line.startsWith("import ") }
                .map { line -> line.substringAfterLast('.') }

            assertTrue(reserved.containsAll(imported), "$case imports ${imported - reserved}")
        }
        assertTrue(REMEMBER_THEME_COLORS in reserved)
    }

    @Test
    fun customDynamic_otherLibrary_isRefused() {
        val material3 = Fixtures.input(document = Fixtures.Base)

        assertFailsWith<IllegalArgumentException> { CustomDynamic.files(material3) }
    }

    private fun themeColors(input: ExportInput): String = file(input, "ThemeColors.kt")

    private fun file(
        input: ExportInput,
        name: String,
    ): String = CustomDynamic.files(input).single { it.path.endsWith("/$name") }.text

    /**
     * The declaration that opens with [start], up to its closing parenthesis.
     */
    private fun String.declaration(start: String): String = substring(indexOf(start)).substringBefore("\n)\n")

    private fun render(value: Expression): String =
        kotlinFile(path = "Value.kt", packageName = "value") { property("value", value) }
            .text
            .substringAfter("val value = ")
            .trimEnd()
}
