package conformance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SchemeConformanceTest {
    private val generated = ReferenceMcu("com.materialkolor.")
    private val kotlin = ReferenceMcu("upstream.kotlin.")
    private val java = ReferenceMcu("")

    @Test
    fun all2016SchemesMatchBothIndependentReferencesForEveryRole() {
        var schemes = 0
        var roles = 0
        for (seed in SEEDS) {
            for (variant in VARIANT_NAMES) {
                for (spec in listOf("SPEC_2021", "SPEC_2025", "SPEC_2026")) {
                    if (variant == "Cmf" && spec != "SPEC_2026") continue
                    for (dark in listOf(false, true)) {
                        for (contrast in listOf(-1.0, 0.0, 1.0)) {
                            for (platform in listOf("PHONE", "WATCH")) {
                                val case =
                                    SchemeCase(
                                        variant,
                                        spec,
                                        dark,
                                        contrast,
                                        platform,
                                        listOf(seed, 0xff3498db.toInt()),
                                    )
                                assertParity(case)
                                schemes++
                                roles += ROLE_NAMES.size
                            }
                        }
                    }
                }
            }
        }
        assertEquals(2016, schemes)
        assertEquals(118944, roles)
    }

    @Test
    fun grayscaleAndCmfSingleAndDualSourcesPreserveEffectiveSpecsAndNullableRoles() {
        for (seeds in listOf(
            listOf(0xff000000.toInt()),
            listOf(0xffffffff.toInt()),
            listOf(0xff808080.toInt()),
            listOf(0xff6750a4.toInt()),
            listOf(0xff6750a4.toInt(), 0xff3498db.toInt()),
        )) {
            for (variant in VARIANT_NAMES) {
                for (spec in listOf("SPEC_2021", "SPEC_2025", "SPEC_2026")) {
                    if (variant == "Cmf" && spec != "SPEC_2026") continue
                    for (dark in listOf(false, true)) {
                        for (platform in listOf("PHONE", "WATCH")) {
                            assertParity(SchemeCase(variant, spec, dark, 0.25, platform, seeds))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun cmfRejectsPre2026RequestsAndEmptySourceListsRemainInvalid() {
        for (reference in listOf(generated, kotlin, java)) {
            for (spec in listOf("SPEC_2021", "SPEC_2025")) {
                assertFailsWith<IllegalArgumentException> {
                    reference.scheme(SchemeCase("Cmf", spec, false, 0.0, "PHONE", SEEDS.take(1)))
                }
            }
        }
        // Constructors access first() while constructing palettes, before DynamicScheme's require.
        // Preserve that upstream failure rather than inventing a successful empty-source scheme.
        for (reference in listOf(generated, kotlin)) {
            for (variant in VARIANT_NAMES) {
                assertFailsWith<NoSuchElementException>(variant) {
                    reference.scheme(SchemeCase(variant, "SPEC_2026", false, 0.0, "PHONE", emptyList()))
                }
            }
        }
    }

    @Test
    fun directCustomPaletteInjectionMatchesAll59RolesAndRejectsEmptySourceLists() {
        assertEquals(
            59,
            ROLE_NAMES.size,
            "ROLE_NAMES pins the role inventory every reference is compared over. Changing it means " +
                "upstream added or removed a role: update roleArgb() in UpstreamRoleGoldenTest to match, " +
                "then regenerate the golden fixtures.",
        )
        for (spec in listOf("SPEC_2021", "SPEC_2025", "SPEC_2026")) {
            for (dark in listOf(false, true)) {
                for (platform in listOf("PHONE", "WATCH")) {
                    val case = SchemeCase("custom", spec, dark, 0.5, platform, SEEDS.take(2))
                    val raw = kotlin.roles(kotlin.customScheme(case))
                    val actual = generated.roles(generated.customScheme(case))
                    val javaRoles = java.roles(java.customScheme(case))
                    for (role in ROLE_NAMES) {
                        assertEquals(raw[role], actual[role], "$case/$role custom Kotlin")
                        assertEquals(javaRoles[role], actual[role], "$case/$role custom Java")
                    }
                }
            }
        }
        val empty = SchemeCase("custom", "SPEC_2026", false, 0.0, "PHONE", emptyList())
        for (reference in listOf(generated, kotlin, java)) {
            assertFailsWith<IllegalArgumentException> { reference.customScheme(empty) }
        }
    }

    private fun assertParity(case: SchemeCase) {
        val rawScheme = kotlin.scheme(case)
        val generatedScheme = generated.scheme(case)
        val javaScheme = java.scheme(case)
        assertEquals(
            kotlin.effectiveSpec(rawScheme),
            generated.effectiveSpec(generatedScheme),
            "$case effective Kotlin spec",
        )
        assertEquals(
            java.effectiveSpec(javaScheme),
            generated.effectiveSpec(generatedScheme),
            "$case effective Java spec",
        )
        val rawRoles = kotlin.roles(rawScheme)
        val generatedRoles = generated.roles(generatedScheme)
        val javaRoles = java.roles(javaScheme)
        for (role in ROLE_NAMES) {
            assertEquals(rawRoles[role], generatedRoles[role], "$case/$role raw Kotlin vs generated")
            assertEquals(javaRoles[role], generatedRoles[role], "$case/$role raw Java vs generated")
        }
    }

    companion object {
        private val SEEDS = listOf(
            0xffff0000.toInt(),
            0xff00ff00.toInt(),
            0xff0000ff.toInt(),
            0xffffff00.toInt(),
            0xff00ffff.toInt(),
            0xff6750a4.toInt(),
        )
    }
}
