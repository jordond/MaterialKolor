package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.FluentBinding
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.GradleScope
import com.materialkolor.builder.codegen.dsl.gradleFile
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.dsl.tomlFile
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * Where the version catalog snippet sits in the export.
 */
internal const val CATALOG_PATH: String = "gradle/libs.versions.toml"

/**
 * Where the build file snippet sits in the export.
 */
internal const val BUILD_SNIPPET_PATH: String = "snippets/build.gradle.kts"

/**
 * One library an export depends on, as a catalog entry and as a plain coordinate.
 *
 * @property[alias] The catalog alias, which Gradle turns into the `libs.` accessor.
 * @property[module] The group and artifact, as in `com.materialkolor:material-kolor-core`.
 * @property[versionKey] The key the version sits under in the catalog's versions table.
 * @property[version] The version itself.
 */
internal class Dependency(
    val alias: String,
    val module: String,
    val versionKey: String,
    val version: String,
) {
    /**
     * The accessor a build file reads the catalog entry by, as in `libs.materialKolor.core`.
     */
    val accessor: String
        get() = "libs." + alias.replace('-', '.')

    /**
     * The coordinate a build file names without a catalog.
     */
    val coordinate: String
        get() = "$module:$version"
}

/**
 * The dependency snippets that go with an export.
 *
 * A dynamic export needs the MaterialKolor module for its target. A frozen export needs no
 * MaterialKolor at all, so Material 3, Expressive and Custom get no snippet. In either mode an
 * Unstyled export needs the Compose Unstyled theming library, which `material-kolor-unstyled` leaves
 * to the app on JVM and Android, and a Fluent export needs Compose Fluent. Every MaterialKolor module
 * is named at the one pinned version (D9).
 */
internal object Snippets {
    /**
     * The catalog snippet when [ExportInput.prefs] asks for one, then the build file snippet, or nothing.
     */
    fun files(input: ExportInput): List<GeneratedFile> {
        val dependencies = dependencies(input)
        if (dependencies.isEmpty()) return emptyList()

        return listOfNotNull(
            if (input.prefs.versionCatalog) catalogFile(dependencies) else null,
            buildFile(input, dependencies),
        )
    }

    /**
     * What [input] depends on, MaterialKolor first. Empty when it needs nothing beyond Compose.
     */
    fun dependencies(input: ExportInput): List<Dependency> =
        when (input.prefs.mode) {
            ExportMode.Dynamic -> {
                when (input.target) {
                    ExportTarget.Material3, ExportTarget.Material3Expressive -> {
                        listOf(materialKolor("material3", input))
                    }
                    ExportTarget.Unstyled -> {
                        listOf(materialKolor("unstyled", input), unstyledTheming(input))
                    }
                    ExportTarget.Fluent -> {
                        when (input.versions.fluentBinding) {
                            FluentBinding.Module -> listOf(materialKolor("fluent", input), fluent(input))
                            FluentBinding.Inline -> listOf(materialKolor("core", input), fluent(input))
                        }
                    }
                    ExportTarget.Custom -> {
                        listOf(materialKolor("core", input))
                    }
                }
            }
            ExportMode.Frozen -> {
                when (input.target) {
                    ExportTarget.Material3, ExportTarget.Material3Expressive, ExportTarget.Custom -> emptyList()
                    ExportTarget.Unstyled -> listOf(unstyledTheming(input))
                    ExportTarget.Fluent -> listOf(fluent(input))
                }
            }
        }

    /**
     * The R9 note for [target], or null when the target runs everywhere Compose does.
     */
    fun platformNote(target: ExportTarget): String? =
        when (target) {
            ExportTarget.Material3, ExportTarget.Material3Expressive, ExportTarget.Custom -> {
                null
            }
            ExportTarget.Unstyled -> {
                "Compose Unstyled needs JVM 17 and Android minSdk 23, and has no macOS native target."
            }
            ExportTarget.Fluent -> {
                "Compose Fluent needs JVM 17 and has no macOS native target."
            }
        }

    /**
     * The MaterialKolor [module], at the one pinned version.
     */
    private fun materialKolor(
        module: String,
        input: ExportInput,
    ): Dependency =
        Dependency(
            alias = "materialKolor-$module",
            module = "com.materialkolor:material-kolor-$module",
            versionKey = "materialKolor",
            version = input.versions.materialKolor,
        )

    private fun fluent(input: ExportInput): Dependency =
        Dependency(
            alias = "composeFluent",
            module = "io.github.compose-fluent:fluent",
            versionKey = "composeFluent",
            version = input.versions.fluent,
        )

    private fun unstyledTheming(input: ExportInput): Dependency =
        Dependency(
            alias = "composeUnstyled-theming",
            module = "com.composables:composeunstyled-theming",
            versionKey = "composeUnstyled",
            version = input.versions.composeUnstyled,
        )

    private fun catalogFile(dependencies: List<Dependency>): GeneratedFile =
        tomlFile(CATALOG_PATH) {
            comment("Merge these into the version catalog of your own project.")
            table("versions") {
                dependencies.distinctBy { it.versionKey }.forEach { dependency ->
                    key(dependency.versionKey, dependency.version)
                }
            }
            table("libraries") {
                dependencies.forEach { dependency ->
                    inlineTable(dependency.alias) {
                        entry("module", dependency.module)
                        entry("version.ref", dependency.versionKey)
                    }
                }
            }
        }

    private fun buildFile(
        input: ExportInput,
        dependencies: List<Dependency>,
    ): GeneratedFile =
        gradleFile(BUILD_SNIPPET_PATH) {
            comment("Add these to the build file of the module that holds your theme.")
            platformNote(input.target)?.let { note -> comment(note) }
            if (input.prefs.multiplatform) {
                block("kotlin") {
                    block("sourceSets") {
                        block("commonMain.dependencies") { implementations(input, dependencies) }
                    }
                }
            } else {
                block("dependencies") { implementations(input, dependencies) }
            }
        }

    private fun GradleScope.implementations(
        input: ExportInput,
        dependencies: List<Dependency>,
    ) {
        dependencies.forEach { dependency ->
            if (input.prefs.versionCatalog) {
                dependency(IMPLEMENTATION, ref(dependency.accessor))
            } else {
                dependency(IMPLEMENTATION, dependency.coordinate)
            }
        }
    }

    private const val IMPLEMENTATION = "implementation"
}
