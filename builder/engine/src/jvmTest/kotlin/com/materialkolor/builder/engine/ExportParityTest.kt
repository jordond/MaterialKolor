package com.materialkolor.builder.engine

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FrozenVariants
import com.materialkolor.builder.engine.export.ExportResolver
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.engine.resolve.part
import com.materialkolor.fluent.toFluentShades
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * The gate between the generated code and the builder's preview.
 *
 * [EngineParityTest] already holds the engine to every library adapter. This holds what the
 * generator writes to the preview. A frozen export has to write the preview's color for every
 * role, slot, accent and shade under the name that carries it. A dynamic export has to pass the
 * arguments the engine resolves with, and the library call with those arguments has to give the
 * preview's colors.
 */
class ExportParityTest {
    private val themes = ThemeResolver()
    private val exports = ExportResolver(themes)
    private val documents =
        ParityDocuments(seed = DOCUMENT_SEED).documents(DOCUMENT_COUNT) +
            ParityDocuments(seed = OVERRIDE_SEED).primaryOverrides(OVERRIDE_COUNT)

    @Test
    fun frozenExport_everyTargetAndDocument_writesThePreviewColors() {
        for (target in ExportTarget.entries) {
            for (document in documents.map { document -> document.on(target) }) {
                val written = ColorLiterals.read(export(document, FrozenPrefs))
                val expected = frozenColors(target, document)
                val differing = expected.filter { (name, color) -> written[name] != color }

                if (differing.isNotEmpty()) {
                    val lines = differing.map { (name, color) -> "$name wanted $color, wrote ${written[name]}" }
                    fail("$target frozen, in $document\n" + lines.joinToString("\n"))
                }
            }
        }
    }

    @Test
    fun dynamicMaterial3Export_everyDocument_passesTheArgumentsTheEngineResolvesWith() {
        assertPassesTheEngineArguments(ExportTarget.Material3)
    }

    @Test
    fun dynamicExpressiveExport_everyDocument_passesTheArgumentsTheEngineResolvesWith() {
        assertPassesTheEngineArguments(ExportTarget.Material3Expressive)
    }

    @Test
    fun dynamicUnstyledExport_everyDocument_passesTheArgumentsTheEngineResolvesWith() {
        assertPassesTheEngineArguments(ExportTarget.Unstyled)
    }

    @Test
    fun dynamicFluentExport_everyDocument_passesTheArgumentsTheEngineResolvesWith() {
        assertPassesTheEngineArguments(ExportTarget.Fluent)
    }

    @Test
    fun dynamicFluentInlineExport_everyDocument_passesTheArgumentsTheEngineResolvesWith() {
        assertPassesTheEngineArguments(ExportTarget.Fluent, InlineFluentVersions)
    }

    @Test
    fun dynamicCustomExport_everyDocument_passesTheArgumentsTheEngineResolvesWith() {
        assertPassesTheEngineArguments(ExportTarget.Custom)
    }

    @Test
    fun dynamicMaterial3Export_everyDocument_givesThePreviewColors() {
        assertGivesThePreviewColors(ExportTarget.Material3)
    }

    @Test
    fun dynamicExpressiveExport_everyDocument_givesThePreviewColors() {
        assertGivesThePreviewColors(ExportTarget.Material3Expressive)
    }

    @Test
    fun dynamicUnstyledExport_everyDocument_givesThePreviewColors() {
        assertGivesThePreviewColors(ExportTarget.Unstyled)
    }

    @Test
    fun dynamicFluentExport_everyDocument_givesThePreviewColors() {
        assertGivesThePreviewColors(ExportTarget.Fluent)
    }

    @Test
    fun dynamicFluentInlineExport_everyDocument_givesThePreviewColors() {
        assertGivesThePreviewColors(ExportTarget.Fluent, InlineFluentVersions)
    }

    @Test
    fun dynamicCustomExport_everyDocument_givesThePreviewColors() {
        assertGivesThePreviewColors(ExportTarget.Custom)
    }

    /**
     * Every document's dynamic [target] export passes the arguments the engine resolves its preview with.
     */
    private fun assertPassesTheEngineArguments(
        target: ExportTarget,
        versions: ExportVersions = Versions,
    ) {
        for (document in documents.map { document -> document.on(target) }) {
            val context = "${target.dynamicName(versions)}, in $document"
            val written = DynamicExport.read(target, export(document, DynamicPrefs, versions), context).arguments
            val preview = themes.resolve(document)

            assertEquals(SchemeArguments.of(target, document, preview), written, context)
        }
    }

    /**
     * Every document's dynamic [target] export gives the preview's colors in both modes: every role
     * with its pins, every Custom slot with its tones, every accent family and every Fluent shade,
     * each worked out from what the export writes.
     */
    private fun assertGivesThePreviewColors(
        target: ExportTarget,
        versions: ExportVersions = Versions,
    ) {
        for (document in documents.map { document -> document.on(target) }) {
            val context = "${target.dynamicName(versions)}, in $document"
            val written = DynamicExport.read(target, export(document, DynamicPrefs, versions), context)
            val preview = themes.resolve(document)

            for (isDark in listOf(false, true)) {
                val expected = dynamicColors(target, preview, isDark)
                val colors = written.colors(isDark)
                val differing = (expected.keys + colors.keys).filter { name -> expected[name] != colors[name] }

                if (differing.isNotEmpty()) {
                    val lines = differing.map { name -> "$name wanted ${expected[name]}, wrote ${colors[name]}" }
                    fail("$context, dark $isDark\n" + lines.joinToString("\n"))
                }
            }
        }
    }

    private fun export(
        document: ThemeDocument,
        prefs: ExportPrefs,
        versions: ExportVersions = Versions,
    ): List<GeneratedFile> =
        generate(
            ExportInput(
                document = document,
                prefs = prefs,
                resolved = exports.resolve(document, prefs),
                versions = versions,
                shareUrl = "https://materialkolor.com/t/parity",
            ),
        )

    /**
     * Every color a frozen export of [document] for [target] should write, by the name it sits under.
     */
    private fun frozenColors(
        target: ExportTarget,
        document: ThemeDocument,
    ): Map<String, Argb> =
        buildMap {
            for (variant in ContrastVariant.entries) {
                val preview = themes.resolve(document.atContrast(variant))
                val prefix = variant.prefix
                for (isDark in listOf(false, true)) {
                    val mode = if (isDark) "Dark" else "Light"
                    val lowerMode = mode.replaceFirstChar(Char::lowercaseChar)
                    val scope = if (prefix.isEmpty()) lowerMode else "$prefix$mode"
                    when (target) {
                        ExportTarget.Material3, ExportTarget.Material3Expressive -> {
                            preview.roleColors(isDark).forEach { (role, color) ->
                                put("${role.name.lowerFirst()}$mode${prefix.upperFirst()}", color)
                            }
                        }
                        ExportTarget.Unstyled -> {
                            preview.roleColors(isDark).forEach { (role, color) ->
                                put("${scope}Colors/${role.name.lowerFirst()}", color)
                            }
                        }
                        ExportTarget.Custom -> {
                            preview.customSlots.mode(isDark).forEach { (slot, color) ->
                                put("${scope}ThemeColors/${slot.name.lowerFirst()}", color)
                            }
                        }
                        ExportTarget.Fluent -> {
                            if (variant == ContrastVariant.Standard) {
                                preview.fluentShades(isDark).byShade().forEach { (shade, color) ->
                                    put("${mode}ThemeShades/$shade", color)
                                }
                            }
                        }
                    }
                    if (variant == ContrastVariant.Standard) putAll(accentColors(target, preview, isDark))
                }
            }
        }

    /**
     * Every color the preview shows for a dynamic [target] export in the mode [isDark] picks, by the
     * name `DynamicExport.colors` gives it.
     */
    private fun dynamicColors(
        target: ExportTarget,
        preview: ThemeResult,
        isDark: Boolean,
    ): Map<String, Argb> {
        val roles = preview.roleColors(isDark).mapKeys { (role, _) -> role.name.lowerFirst() }
        val families = preview.accents.families
            .flatMap { family ->
                val name = family.accent.name.lowerFirst()
                AccentPart.entries.map { part ->
                    val key = when (target) {
                        ExportTarget.Unstyled -> "ThemeTokens.${part.unstyledToken(name)}"
                        else -> "$name/${part.property}"
                    }
                    key to family.mode(isDark).part(part)
                }
            }.toMap()
        return when (target) {
            ExportTarget.Material3, ExportTarget.Material3Expressive, ExportTarget.Unstyled -> {
                roles + families
            }
            ExportTarget.Fluent -> {
                preview.fluentShades(isDark).byShade()
            }
            ExportTarget.Custom -> {
                preview.customSlots.mode(isDark).mapKeys { (slot, _) -> slot.name.lowerFirst() } + families
            }
        }
    }

    /**
     * Every accent color [target] writes in the mode [isDark] picks, by the name it sits under.
     */
    private fun accentColors(
        target: ExportTarget,
        preview: ThemeResult,
        isDark: Boolean,
    ): Map<String, Argb> {
        val mode = if (isDark) "dark" else "light"
        return preview.accents.families
            .flatMap { family ->
                val name = family.accent.name.lowerFirst()
                AccentPart.entries.mapNotNull { part ->
                    val color = family.mode(isDark).part(part)
                    val path = when (target) {
                        ExportTarget.Material3, ExportTarget.Material3Expressive -> {
                            "extended${mode.upperFirst()}/$name/${part.property}"
                        }
                        ExportTarget.Unstyled -> {
                            "${mode}Colors/${part.unstyledToken(name)}"
                        }
                        ExportTarget.Custom -> {
                            "${mode}ThemeColors/$name/${part.property}"
                        }
                        ExportTarget.Fluent -> {
                            null
                        }
                    }
                    path?.let { key -> key to color }
                }
            }.toMap()
    }

    private companion object {
        const val DOCUMENT_SEED = 114
        const val DOCUMENT_COUNT = 30
        const val OVERRIDE_SEED = 1140
        const val OVERRIDE_COUNT = 10

        val FrozenPrefs = ExportPrefs(mode = ExportMode.Frozen, frozenVariants = FrozenVariants.AllContrasts)
        val DynamicPrefs = ExportPrefs(mode = ExportMode.Dynamic)
        val Versions = ExportVersions(
            builder = "2.0.0",
            materialKolor = "6.0.0",
            fluent = "v0.1.0",
            composeUnstyled = "2.10.0",
        )

        /**
         * A release that does not publish `material-kolor-fluent`, so a Fluent export builds its shades inline.
         */
        val InlineFluentVersions = Versions.copy(fluentModuleAvailable = false)
    }
}

/**
 * [this] document as an export for [target] sees it.
 */
private fun ThemeDocument.on(target: ExportTarget): ThemeDocument {
    val library = when (target) {
        ExportTarget.Material3, ExportTarget.Material3Expressive -> Library.Material3
        ExportTarget.Unstyled -> Library.Unstyled
        ExportTarget.Fluent -> Library.Fluent
        ExportTarget.Custom -> Library.Custom
    }
    return copy(library = library, expressive = target == ExportTarget.Material3Expressive).forTarget(target)
}

/**
 * How a failure names the dynamic export of [this] target, the inline Fluent binding apart.
 */
private fun ExportTarget.dynamicName(versions: ExportVersions): String =
    if (this == ExportTarget.Fluent && !versions.fluentModuleAvailable) "$this dynamic inline" else "$this dynamic"

/**
 * [this] document at the contrast [variant] stands for, the way `ExportResolver` resolves it.
 */
private fun ThemeDocument.atContrast(variant: ContrastVariant): ThemeDocument =
    when (variant) {
        ContrastVariant.Standard -> this
        ContrastVariant.Medium -> copy(contrast = ContrastLevel.Medium)
        ContrastVariant.High -> copy(contrast = ContrastLevel.High)
    }

/**
 * How the names of [this] variant's colors start, empty for standard.
 */
private val ContrastVariant.prefix: String
    get() = when (this) {
        ContrastVariant.Standard -> ""
        ContrastVariant.Medium -> "mediumContrast"
        ContrastVariant.High -> "highContrast"
    }

private fun ThemeResult.roleColors(isDark: Boolean) =
    (if (isDark) roles.dark else roles.light).mapValues { (_, entry) -> entry.argb }

/**
 * The Fluent shades the preview's own primary palette gives in the mode [isDark] picks.
 */
private fun ThemeResult.fluentShades(isDark: Boolean): FluentShadeValues {
    val shades = scheme(isDark).primaryPalette.toFluentShades()
    return FluentShadeValues(
        dark3 = shades.dark3.asArgb(),
        dark2 = shades.dark2.asArgb(),
        dark1 = shades.dark1.asArgb(),
        base = shades.base.asArgb(),
        light1 = shades.light1.asArgb(),
        light2 = shades.light2.asArgb(),
        light3 = shades.light3.asArgb(),
    )
}

/**
 * The Unstyled token an accent part is written under, as in `onBrandContainer`.
 */
private fun AccentPart.unstyledToken(accent: String): String =
    when (this) {
        AccentPart.Color -> accent
        AccentPart.OnColor -> "on${accent.upperFirst()}"
        AccentPart.Container -> "${accent}Container"
        AccentPart.OnContainer -> "on${accent.upperFirst()}Container"
    }

/**
 * The `ColorFamily` property an accent part is written under.
 */
private val AccentPart.property: String
    get() = when (this) {
        AccentPart.Color -> "color"
        AccentPart.OnColor -> "onColor"
        AccentPart.Container -> "colorContainer"
        AccentPart.OnContainer -> "onColorContainer"
    }

private fun String.lowerFirst(): String = replaceFirstChar(Char::lowercaseChar)

private fun String.upperFirst(): String = replaceFirstChar(Char::uppercaseChar)
