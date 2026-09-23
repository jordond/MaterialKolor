package com.materialkolor.builder.engine

import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.FluentShades
import com.materialkolor.builder.domain.export.ResolvedExport
import com.materialkolor.builder.domain.export.RoleTable
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.FrozenVariants
import com.materialkolor.builder.engine.export.ExportResolver
import com.materialkolor.builder.engine.resolve.RampSet
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.ktx.harmonize
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The gate between the engine and every library adapter an export calls.
 *
 * Each case resolves a document through [ExportResolver], which is what a frozen export writes,
 * and holds it to what the adapter builds at runtime for the same document. A dynamic export and a
 * frozen one of the same theme then show the same colors, and both match the builder's preview.
 */
class EngineParityTest {
    private val documents = ParityDocuments().documents(DOCUMENT_COUNT)
    private val overrides = ParityDocuments(seed = OVERRIDE_SEED).primaryOverrides(DOCUMENT_COUNT)
    private val exports = ExportResolver()

    @Test
    fun parityDocuments_fiftyDocuments_coverEveryInputAnExportReads() {
        val pairs = documents.map { document -> document.style to document.spec }.toSet()
        val accents = documents.flatMap { document -> document.accents }
        val tonedSlots = documents.flatMap { document -> document.customTones.keys }
        val pins = documents.flatMap { document -> document.pins.values }

        assertEquals(Style.entries.size * SpecVersion.entries.size, pairs.size, "every style and spec pair")
        assertTrue(documents.any { document -> document.amoled }, "an AMOLED document")
        assertTrue(pins.any { pin -> (pin.light == null) != (pin.dark == null) }, "a pin for one mode only")
        assertTrue(tonedSlots.any { slot -> slot.resolution is SlotResolution.FromRamp }, "a moved ramp slot")
        assertTrue(tonedSlots.any { slot -> slot.resolution is SlotResolution.FromRole }, "an ignored role slot")
        assertTrue(documents.any { document -> document.keyColors.primary != null }, "a primary override")
        assertTrue(
            documents.any { document -> document.accents.any { accent -> accent.harmonizeMoves(document) } },
            "a harmonized accent that harmonizing moves",
        )
        assertTrue(accents.any { accent -> !accent.harmonize }, "an accent left as it is")
    }

    @Test
    fun resolve_randomDocuments_rolesMatchMaterial3() {
        for (document in documents) {
            val roles = exports.resolve(document, StandardOnly).standardRoles()
            forEachMode { isDark ->
                assertSameColors(material3Roles(document, isDark), roles.mode(isDark), "Material 3", document, isDark)
            }
        }
    }

    @Test
    fun resolve_randomDocuments_rolesMatchExpressiveTheme() {
        val rendered = renderExpressive(documents, defaults = false)

        documents.forEachIndexed { index, document ->
            val roles = exports.resolve(document, StandardOnly).standardRoles()
            forEachMode { isDark ->
                val expected = expressiveRoles(document, rendered[index].mode(isDark), isDark)
                assertSameColors(expected, roles.mode(isDark), "Material 3 Expressive", document, isDark)
            }
        }
    }

    /**
     * An expressive export leaves out every argument that matches the theme's own defaults, so a
     * document on those defaults has to come out the same when the theme picks them itself.
     */
    @Test
    fun resolve_expressiveDefaults_rolesMatchExpressiveThemeLeftOnItsDefaults() {
        val onDefaults = documents.map { document ->
            document.copy(
                style = Style.Expressive,
                spec = SpecVersion.Spec2025,
                contrast = ContrastLevel.Standard,
                platform = SchemePlatform.Phone,
            )
        }
        val rendered = renderExpressive(onDefaults, defaults = true)

        onDefaults.forEachIndexed { index, document ->
            val roles = exports.resolve(document, StandardOnly).standardRoles()
            forEachMode { isDark ->
                val expected = expressiveRoles(document, rendered[index].mode(isDark), isDark)
                assertSameColors(expected, roles.mode(isDark), "expressive defaults", document, isDark)
            }
        }
    }

    /** Unstyled has no AMOLED switch and its export never turns it on, so these run with it off. */
    @Test
    fun resolve_randomDocuments_rolesMatchUnstyledThemeValues() {
        for (document in documents.map { document -> document.copy(amoled = false) }) {
            val roles = exports.resolve(document, StandardOnly).standardRoles()
            forEachMode { isDark ->
                assertSameColors(unstyledRoles(document, isDark), roles.mode(isDark), "Unstyled", document, isDark)
            }
        }
    }

    @Test
    fun resolve_randomDocuments_customSlotsMatchTheCustomDynamicExport() {
        for (document in documents) {
            val slots = exports.resolve(document, StandardOnly).customSlots.getValue(ContrastVariant.Standard)
            forEachMode { isDark ->
                val actual = if (isDark) slots.dark else slots.light
                assertSameColors(customDynamicSlots(document, isDark), actual, "Custom", document, isDark)
            }
        }
    }

    @Test
    fun resolve_randomDocuments_accentsMatchRememberTonalPalette() {
        for (document in documents) {
            assertEquals(accentFamilies(document), exports.resolve(document, StandardOnly).accents, "in $document")
        }
    }

    @Test
    fun resolve_randomDocuments_fluentShadesMatchToFluentShadesInEachMode() {
        for (document in documents) {
            assertEquals(fluentShadesOf(document), exports.resolve(document, StandardOnly).fluentShades, "in $document")
        }
    }

    /** `toFluentColors` reads each mode's own primary palette, and 2025 TonalSpot softens the dark one. */
    @Test
    fun resolve_tonalSpot2025_fluentShadesDifferBetweenModes() {
        val document = ThemeDocument(
            seed = Argb(0xFF6750A4.toInt()),
            style = Style.TonalSpot,
            spec = SpecVersion.Spec2025,
        )
        val shades = checkNotNull(exports.resolve(document, StandardOnly).fluentShades)

        assertEquals(fluentShadesOf(document), shades)
        assertNotEquals(shades.light, shades.dark, "light and dark shades")
    }

    @Test
    fun resolve_allContrasts_mediumAndHighMatchTheLibraryAtThoseLevels() {
        for (document in documents) {
            val resolved = exports.resolve(document, AllContrasts)
            assertEquals(ContrastVariant.entries.toSet(), resolved.roles.keys)
            assertEquals(ContrastVariant.entries.toSet(), resolved.customSlots.keys)
            assertEquals(exports.resolve(document, StandardOnly).standardRoles(), resolved.standardRoles())

            for ((variant, level) in listOf(ContrastVariant.Medium to 0.5, ContrastVariant.High to 1.0)) {
                val roles = resolved.roles.getValue(variant)
                val slots = resolved.customSlots.getValue(variant)
                forEachMode { isDark ->
                    val context = "$variant Material 3"
                    assertSameColors(
                        material3Roles(document, isDark, level),
                        roles.mode(isDark),
                        context,
                        document,
                        isDark,
                    )
                    val actualSlots = if (isDark) slots.dark else slots.light
                    val expectedSlots = customDynamicSlots(document, isDark, level)
                    assertSameColors(expectedSlots, actualSlots, "$variant Custom", document, isDark)
                }
            }
        }
    }

    /** Every contrast is a frozen option, so a dynamic export that still carries it gets standard alone. */
    @Test
    fun resolve_dynamicWithAllContrastsLeftOver_resolvesStandardOnly() {
        val leftover = ExportPrefs(mode = ExportMode.Dynamic, frozenVariants = FrozenVariants.AllContrasts)
        val resolved = exports.resolve(documents.first(), leftover)

        assertEquals(setOf(ContrastVariant.Standard), resolved.roles.keys)
        assertEquals(setOf(ContrastVariant.Standard), resolved.customSlots.keys)
        assertEquals(exports.resolve(documents.first(), StandardOnly), resolved)
    }

    @Test
    fun resolve_standardOnly_fillsEveryPartForTheStandardVariantAlone() {
        val resolved = exports.resolve(documents.first(), StandardOnly)

        assertEquals(setOf(ContrastVariant.Standard), resolved.roles.keys)
        assertEquals(setOf(ContrastVariant.Standard), resolved.customSlots.keys)
        assertTrue(resolved.fluentShades != null, "Fluent shades are filled for every target")
    }

    /** The chrome schemes floor contrast for the builder's own UI, and that must never reach an export. */
    @Test
    fun resolve_reducedContrast_readsTheDocumentSchemesNotTheChrome() {
        val document = ThemeDocument(seed = Argb(0x6750A4), contrast = ContrastLevel.Reduced)
        val theme = ThemeResolver().resolve(document)
        val roles = exports.resolve(document, StandardOnly).standardRoles()

        forEachMode { isDark ->
            assertSameColors(material3Roles(document, isDark), roles.mode(isDark), "reduced", document, isDark)
            assertNotEquals(coreRoles(theme.chrome(isDark)), roles.mode(isDark), "the chrome scheme leaked")
        }
    }

    @Test
    fun effectiveSpec_everyStyleAndRequestedSpec_isTheSpecTheSchemeRan() {
        for (style in Style.entries) {
            for (requested in SpecVersion.entries) {
                val document = ThemeDocument(seed = Argb(0x6750A4), style = style, spec = requested)
                forEachMode { isDark ->
                    val ran = referenceScheme(document, isDark).specVersion
                    val expected = EffectiveSpec.of(style, requested)

                    assertEquals(expected.name, "Spec" + ran.name.removePrefix("SPEC_"), "$style asking for $requested")
                }
            }
        }
    }

    @Test
    fun resolve_primaryOverride_everyAdapterMatchesTheCoreScheme() {
        for (document in overrides) {
            val resolved = exports.resolve(document, StandardOnly)
            val roles = resolved.standardRoles()
            val slots = resolved.customSlots.getValue(ContrastVariant.Standard)

            forEachMode { isDark ->
                val core = coreRoles(referenceScheme(document, isDark))
                assertSameColors(core, roles.mode(isDark), "engine", document, isDark)
                assertSameColors(core, material3Roles(document, isDark), "Material 3", document, isDark)
                assertSameColors(core, unstyledRoles(document, isDark), "Unstyled", document, isDark)
                val actualSlots = if (isDark) slots.dark else slots.light
                assertSameColors(customDynamicSlots(document, isDark), actualSlots, "Custom", document, isDark)
            }
            assertEquals(fluentShadesOf(document), resolved.fluentShades, "Fluent in $document")
        }
    }

    @Test
    fun resolve_primaryOverride_expressiveThemeMatchesTheCoreScheme() {
        val rendered = renderExpressive(overrides, defaults = false)

        overrides.forEachIndexed { index, document ->
            val roles = exports.resolve(document, StandardOnly).standardRoles()
            forEachMode { isDark ->
                val core = coreRoles(referenceScheme(document, isDark))
                val expressive = expressiveRoles(document, rendered[index].mode(isDark), isDark)
                assertSameColors(core, expressive, "Material 3 Expressive", document, isDark)
                assertSameColors(core, roles.mode(isDark), "engine", document, isDark)
            }
        }
    }

    /**
     * A primary override moves the primary palette alone. Secondary, tertiary, neutral, neutral
     * variant and error stay the ones the seed gives without any override.
     */
    @Test
    fun resolve_primaryOverride_keepsEveryOtherPaletteOnTheSeed() {
        val resolver = ThemeResolver()
        for (document in overrides) {
            val theme = resolver.resolve(document)
            val override = checkNotNull(document.keyColors.primary)

            forEachMode { isDark ->
                val scheme = theme.scheme(isDark)
                val seedOnly = seedOnlyScheme(document, isDark)
                val context = "dark $isDark in $document"

                assertSamePalette(TonalPalette.fromInt(override.value), scheme.primaryPalette, "primary, $context")
                assertSamePalette(seedOnly.secondaryPalette, scheme.secondaryPalette, "secondary, $context")
                assertSamePalette(seedOnly.tertiaryPalette, scheme.tertiaryPalette, "tertiary, $context")
                assertSamePalette(seedOnly.neutralPalette, scheme.neutralPalette, "neutral, $context")
                assertSamePalette(seedOnly.neutralVariantPalette, scheme.neutralVariantPalette, "variant, $context")
                assertSamePalette(seedOnly.errorPalette, scheme.errorPalette, "error, $context")
            }
        }
    }

    private fun seedOnlyScheme(
        document: ThemeDocument,
        isDark: Boolean,
    ): DynamicScheme =
        DynamicScheme(
            seedColor = Color(document.seed.value),
            isDark = isDark,
            style = referenceStyle(document),
            contrastLevel = document.contrastLevel,
            specVersion = referenceSpec(document.spec),
            platform = referencePlatform(document.platform),
        )

    private fun ResolvedExport.standardRoles(): RoleTable = roles.getValue(ContrastVariant.Standard)

    private fun fluentShadesOf(document: ThemeDocument): FluentShades =
        FluentShades(light = fluentShades(document, isDark = false), dark = fluentShades(document, isDark = true))

    /** Whether harmonizing this accent with the theme seed moves its color, alpha aside. */
    private fun Accent.harmonizeMoves(document: ThemeDocument): Boolean {
        val harmonized = seed.asColor().harmonize(document.seed.asColor()).asArgb()
        return harmonize && (harmonized.value and RGB_MASK) != (seed.value and RGB_MASK)
    }

    private fun RoleTable.mode(isDark: Boolean): Map<Role, Argb> = if (isDark) dark else light

    private fun forEachMode(block: (isDark: Boolean) -> Unit) {
        block(false)
        block(true)
    }

    /** Fail with every key whose color differs, rather than two whole maps to compare by eye. */
    private fun <K> assertSameColors(
        expected: Map<K, Argb>,
        actual: Map<K, Argb>,
        adapter: String,
        document: ThemeDocument,
        isDark: Boolean,
    ) {
        assertEquals(expected.keys, actual.keys, "$adapter keys, dark $isDark")
        val differing = expected.filter { (key, color) -> actual[key] != color }
        if (differing.isNotEmpty()) {
            val lines = differing.map { (key, color) -> "$key wanted $color, got ${actual[key]}" }
            fail("$adapter, dark $isDark, in $document\n" + lines.joinToString("\n"))
        }
    }

    private fun assertSamePalette(
        expected: TonalPalette,
        actual: TonalPalette,
        context: String,
    ) {
        assertEquals(RampSet.Tones.map(expected::tone), RampSet.Tones.map(actual::tone), context)
    }

    private companion object {
        const val DOCUMENT_COUNT = 50
        const val OVERRIDE_SEED = 1170
        const val RGB_MASK = 0xFFFFFF

        val StandardOnly = ExportPrefs()
        val AllContrasts = ExportPrefs(mode = ExportMode.Frozen, frozenVariants = FrozenVariants.AllContrasts)
    }
}
