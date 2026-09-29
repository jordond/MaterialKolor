package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The golden cases of the dependency snippets and the readme, by case name.
 *
 * Each case holds every file an export writes outside `src`, so the theme files stay in the goldens
 * of their own target. They live in common code so the wasm tests hold them to the same hashes.
 */
internal object SnippetsCases {
    private val Unstyled: ThemeDocument = Fixtures.Base.copy(library = Library.Unstyled)
    private val Fluent: ThemeDocument = Fixtures.Base.copy(library = Library.Fluent)
    private val Custom: ThemeDocument = Fixtures.Base.copy(library = Library.Custom)
    private val Expressive: ThemeDocument = Fixtures.Base.copy(expressive = true)
    private val Inklet: ThemeDocument = Fixtures.Base.copy(library = Library.Inklet)

    private val Frozen: ExportPrefs = ExportPrefs(mode = ExportMode.Frozen)
    private val Android: ExportPrefs = ExportPrefs(multiplatform = false)

    val all: Map<String, ExportInput> = mapOf(
        "snippets-material3-dynamic-default" to Fixtures.input(Fixtures.Base),
        "snippets-material3-dynamic-android-no-catalog" to Fixtures.input(
            document = Fixtures.Base,
            prefs = ExportPrefs(multiplatform = false, versionCatalog = false),
        ),
        "snippets-material3-frozen-default" to Fixtures.input(Fixtures.Base, Frozen),
        "snippets-expressive-dynamic-android" to Fixtures.input(Expressive, Android),
        "snippets-unstyled-dynamic-default" to Fixtures.input(Unstyled),
        "snippets-unstyled-frozen-no-catalog" to Fixtures.input(Unstyled, Frozen.copy(versionCatalog = false)),
        "snippets-fluent-dynamic-default" to Fixtures.input(Fluent),
        "snippets-fluent-dynamic-inline" to Fixtures.input(
            document = Fluent,
            versions = Fixtures.Versions.copy(fluentModuleAvailable = false),
        ),
        "snippets-fluent-frozen-android" to Fixtures.input(Fluent, Frozen.copy(multiplatform = false)),
        "snippets-custom-dynamic-default" to Fixtures.input(Custom),
        "snippets-custom-frozen-android" to Fixtures.input(Custom, Frozen.copy(multiplatform = false)),
        "snippets-inklet-dynamic-default" to Fixtures.input(Inklet),
        "snippets-inklet-frozen-android" to Fixtures.input(Inklet, Frozen.copy(multiplatform = false)),
    )

    fun files(case: String): List<GeneratedFile> = generate(all.getValue(case)).filterNot { it.path.startsWith("src/") }
}

class SnippetsTest {
    @Test
    fun snippets_everyCase_matchesTheGoldenHash() {
        SnippetsCases.all.keys.forEach { case ->
            assertEquals(GoldenHashes.cases[case], GoldenDigest.of(SnippetsCases.files(case)), case)
        }
    }
}
