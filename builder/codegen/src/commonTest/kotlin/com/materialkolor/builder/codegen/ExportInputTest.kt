package com.materialkolor.builder.codegen

import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FrozenVariants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExportInputTest {
    @Test
    fun sourcePath_multiplatform_usesCommonMain() {
        assertEquals(
            "src/commonMain/kotlin/com/example/theme/Theme.kt",
            Fixtures.Default.input.sourcePath("Theme.kt"),
        )
    }

    @Test
    fun sourcePath_androidOnly_usesMain() {
        assertEquals("src/main/kotlin/com/example/theme/Theme.kt", Fixtures.AndroidOnly.input.sourcePath("Theme.kt"))
    }

    @Test
    fun packagePath_customPackage_usesSlashes() {
        val input = Fixtures.Default.input.copy(prefs = ExportPrefs(packageName = "dev.jordond.app.ui"))

        assertEquals("dev/jordond/app/ui", input.packagePath)
    }

    @Test
    fun target_libraryAndExpressive_followTheDocument() {
        assertEquals(ExportTarget.Material3, Fixtures.Default.input.target)
        assertEquals(ExportTarget.Material3Expressive, Fixtures.ExpressiveOnTonalSpot2021.input.target)

        val fluent = Fixtures.Default.with(
            document = Fixtures.Default.input.document
                .copy(library = Library.Fluent),
        )
        assertEquals(ExportTarget.Fluent, fluent.input.target)
    }

    @Test
    fun fluentBinding_moduleAvailability_picksModuleOrInline() {
        assertEquals(FluentBinding.Module, Fixtures.Versions.fluentBinding)
        assertEquals(FluentBinding.Inline, Fixtures.Versions.copy(fluentModuleAvailable = false).fluentBinding)
    }

    @Test
    fun exportInput_blankShareUrl_isRejected() {
        assertFailsWith<IllegalArgumentException> { Fixtures.Default.input.copy(shareUrl = " ") }
    }

    @Test
    fun fixtures_default_linksToTheDefaultShareCode() {
        assertEquals("https://materialkolor.com/t/AdllOwAAAAAT", Fixtures.Default.input.shareUrl)
    }

    @Test
    fun fixtures_all_haveDistinctNamesAndDocuments() {
        assertEquals(
            Fixtures.all.size,
            Fixtures.all
                .map { it.name }
                .toSet()
                .size,
        )
        assertEquals(
            Fixtures.all.size,
            Fixtures.all
                .map { it.input }
                .toSet()
                .size,
        )
    }

    @Test
    fun fixtures_frozenAllContrasts_resolvesEveryVariant() {
        val prefs = ExportPrefs(mode = ExportMode.Frozen, frozenVariants = FrozenVariants.AllContrasts)
        val custom = Fixtures.Default.with(
            document = Fixtures.Default.input.document
                .copy(library = Library.Custom),
            prefs = prefs,
        )

        assertEquals(ContrastVariant.entries.toSet(), custom.input.resolved.roles.keys)
        assertEquals(ContrastVariant.entries.toSet(), custom.input.resolved.customSlots.keys)
        assertNull(custom.input.resolved.fluentShades)
    }

    @Test
    fun fixtures_pinsAndAmoled_areBakedIntoTheRoles() {
        val pinned = Fixtures.Pins.input.resolved.roles
            .getValue(ContrastVariant.Standard)
        val pins = Fixtures.Pins.input.document.pins
        assertEquals(pins.getValue(Role.Primary).light, pinned.light[Role.Primary])
        assertEquals(pins.getValue(Role.Primary).dark, pinned.dark[Role.Primary])
        assertEquals(pins.getValue(Role.Outline).dark, pinned.dark[Role.Outline])

        val amoled = Fixtures.Amoled.input.resolved.roles
            .getValue(ContrastVariant.Standard)
        assertEquals(0xFF000000.toInt(), amoled.dark.getValue(Role.Surface).value)
        assertNotEquals(0xFF000000.toInt(), amoled.light.getValue(Role.Surface).value)
    }

    @Test
    fun fixtures_accentsAndFluent_resolveOnlyWhenAskedFor() {
        assertEquals(
            listOf("Brand", "Success", "Warning"),
            Fixtures.ThreeAccents.input.resolved.accents
                .map { it.name },
        )

        val fluent = Fixtures.Default.with(
            document = Fixtures.Default.input.document
                .copy(library = Library.Fluent),
        )
        assertNotNull(fluent.input.resolved.fluentShades)
        assertNull(Fixtures.Default.input.resolved.fluentShades)
    }
}
