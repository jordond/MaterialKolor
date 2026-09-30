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
}
