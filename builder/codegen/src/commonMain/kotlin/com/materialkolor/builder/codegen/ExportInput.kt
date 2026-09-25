package com.materialkolor.builder.codegen

import com.materialkolor.builder.domain.export.ResolvedExport
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * Everything one export is generated from.
 *
 * The generator reads nothing else, so the same input always gives the same files, byte for byte,
 * on every platform the builder runs on.
 *
 * @property[document] The theme being exported.
 * @property[prefs] The options it is exported with, which belong to whoever is exporting rather
 * than to the theme.
 * @property[resolved] Every color a frozen export writes out, already worked out by the engine.
 * The caller resolves it from the document as the target sees it, with
 * `ExportResolver.resolve(document.forTarget(target), prefs)`.
 * @property[versions] The versions the generated files and snippets name.
 * @property[shareUrl] The link that opens this theme in the builder, which the file header points
 * back to.
 */
public data class ExportInput(
    public val document: ThemeDocument,
    public val prefs: ExportPrefs,
    public val resolved: ResolvedExport,
    public val versions: ExportVersions,
    public val shareUrl: String,
) {
    init {
        require(shareUrl.isNotBlank()) { "An export needs the link back to its theme" }
    }

    /**
     * What the export is written for, the library together with whether it is expressive.
     */
    public val target: ExportTarget
        get() = ExportTarget.of(document.library, document.expressive)

    /**
     * The package as a directory path, as in `com/example/theme`.
     */
    public val packagePath: String
        get() = prefs.packageName.replace('.', '/')

    /**
     * Where [file] sits in the project, as in `src/commonMain/kotlin/com/example/theme/Theme.kt`.
     *
     * A multiplatform project keeps shared code in `commonMain`, an Android one in `main`.
     */
    public fun sourcePath(file: String): String {
        val sourceSet = if (prefs.multiplatform) "commonMain" else "main"

        return "src/$sourceSet/kotlin/$packagePath/$file"
    }
}

/**
 * The versions an export names, in its header and in its dependency snippets.
 *
 * The app fills these in from its build config, so a released builder always names the library it
 * was built against.
 *
 * @property[builder] The builder's own version.
 * @property[materialKolor] The MaterialKolor version the generated code is written against.
 * @property[fluent] The Compose Fluent version a Fluent export depends on.
 * @property[composeUnstyled] The Compose Unstyled version an Unstyled export depends on.
 * @property[fluentModuleAvailable] Whether `material-kolor-fluent` is published at [materialKolor].
 * When it is not, a Fluent export builds its shades inline from core.
 */
public data class ExportVersions(
    public val builder: String,
    public val materialKolor: String,
    public val fluent: String,
    public val composeUnstyled: String,
    public val fluentModuleAvailable: Boolean = true,
) {
    /**
     * How a Fluent export gets its colors, which follows from whether the module is published.
     */
    public val fluentBinding: FluentBinding
        get() = if (fluentModuleAvailable) FluentBinding.Module else FluentBinding.Inline
}

/**
 * How a Fluent export turns a seed into Fluent colors.
 */
public enum class FluentBinding {
    /**
     * Through `material-kolor-fluent`, which ships with MaterialKolor 6.0.
     */
    Module,

    /**
     * Through a small shade mapping written into the export itself, on top of core.
     */
    Inline,
}
