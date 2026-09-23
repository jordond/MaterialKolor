package com.materialkolor.builder.engine

import com.materialkolor.Contrast
import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FrozenVariants
import com.materialkolor.builder.engine.export.ExportResolver
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.engine.resolve.part
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.fluent.toFluentShades
import kotlin.math.roundToInt
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
    fun dynamicExport_everyTargetAndDocument_passesTheArgumentsTheEngineResolvesWith() {
        for (target in ExportTarget.entries) {
            for (document in documents.map { document -> document.on(target) }) {
                val written = SchemeArguments.read(target, export(document, DynamicPrefs))
                val preview = themes.resolve(document)

                assertEquals(SchemeArguments.of(target, document, preview), written, "$target dynamic, in $document")
            }
        }
    }

    @Test
    fun dynamicExport_everyTargetAndDocument_libraryCallGivesThePreviewColors() {
        for (target in ExportTarget.entries) {
            for (document in documents.map { document -> document.on(target) }) {
                val written = SchemeArguments.read(target, export(document, DynamicPrefs)).applyTo(document)
                val preview = themes.resolve(document)

                for (isDark in listOf(false, true)) {
                    val context = "$target dynamic, dark $isDark, in $document"
                    when (target) {
                        ExportTarget.Material3, ExportTarget.Material3Expressive -> {
                            assertEquals(preview.roleColors(isDark), material3Roles(written, isDark), context)
                        }
                        ExportTarget.Unstyled -> {
                            assertEquals(preview.roleColors(isDark), unstyledRoles(written, isDark), context)
                        }
                        ExportTarget.Fluent -> {
                            assertEquals(preview.fluentShades(isDark), fluentShades(written, isDark), context)
                        }
                        ExportTarget.Custom -> {
                            assertEquals(preview.customSlots.mode(isDark), customDynamicSlots(written, isDark), context)
                        }
                    }
                }
            }
        }
    }

    private fun export(
        document: ThemeDocument,
        prefs: ExportPrefs,
    ): List<GeneratedFile> =
        generate(
            ExportInput(
                document = document,
                prefs = prefs,
                resolved = exports.resolve(document, prefs),
                versions = Versions,
                shareUrl = "https://materialkolor.com/t/parity",
            ),
        )

    /** Every color a frozen export of [document] for [target] should write, by the name it sits under. */
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
                                preview.fluentShades(isDark).named().forEach { (shade, color) ->
                                    put("${mode}ThemeShades/$shade", color)
                                }
                            }
                        }
                    }
                    if (variant == ContrastVariant.Standard) putAll(accentColors(target, preview, isDark))
                }
            }
        }

    /** Every accent color [target] writes in the mode [isDark] picks, by the name it sits under. */
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
                        ExportTarget.Unstyled -> "${mode}Colors/${part.unstyledToken(name)}"
                        ExportTarget.Custom -> "${mode}ThemeColors/$name/${part.property}"
                        ExportTarget.Fluent -> null
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
    }
}

/**
 * The scheme arguments a dynamic export passes, with each one it leaves out read as the default
 * of the call it leaves it out of.
 *
 * AMOLED is null for the targets whose call has no such switch.
 */
private data class SchemeArguments(
    val seed: Argb,
    val keyColors: KeyColors,
    val style: Style,
    val cmfTertiarySeed: Argb?,
    val contrast: ContrastLevel,
    val effectiveSpec: SpecVersion,
    val platform: SchemePlatform,
    val amoled: Boolean?,
) {
    /** [document] with these arguments in place of its own. */
    fun applyTo(document: ThemeDocument): ThemeDocument =
        document.copy(
            seed = seed,
            keyColors = keyColors,
            style = style,
            cmfTertiarySeed = cmfTertiarySeed,
            contrast = contrast,
            spec = effectiveSpec,
            platform = platform,
            amoled = amoled ?: document.amoled,
        )

    companion object {
        /** The arguments the engine resolved [preview] with, for the document as [target] sees it. */
        fun of(
            target: ExportTarget,
            document: ThemeDocument,
            preview: ThemeResult,
        ): SchemeArguments =
            SchemeArguments(
                seed = document.seed,
                keyColors = document.keyColors,
                style = document.style,
                cmfTertiarySeed = document.cmfTertiarySeed.takeIf { document.style == Style.Cmf },
                contrast = document.contrast,
                effectiveSpec = preview.effectiveSpec,
                platform = document.platform,
                amoled = document.amoled.takeIf { target.hasAmoled },
            )

        /** The arguments the scheme call in [files] passes, the way the call itself reads them. */
        fun read(
            target: ExportTarget,
            files: List<GeneratedFile>,
        ): SchemeArguments {
            val text = files.filter { file -> file.path.endsWith(".kt") }.joinToString("\n") { file -> file.text }
            val literals = ColorLiterals.read(files)
            val call = SchemeCalls.getValue(target).firstOrNull { name -> Regex("""\b$name\(""").containsMatchIn(text) }
                ?: fail("The $target export never calls ${SchemeCalls.getValue(target)}")
            val expressive = call == "DynamicMaterialExpressiveTheme"
            val arguments = callArguments(text, call)

            fun color(name: String): Argb? = arguments[name]?.let { value -> colorOf(value, text, literals) }

            val style = arguments["style"]?.let { value ->
                Style.valueOf(value.removePrefix("PaletteStyle.").substringBefore('('))
            } ?: if (expressive) Style.Expressive else Style.TonalSpot
            val spec = arguments["specVersion"]
                ?.let { value -> ColorSpec.SpecVersion.valueOf(value.substringAfterLast('.')) }
                ?: if (expressive) ColorSpec.SpecVersion.SPEC_2025 else ColorSpec.SpecVersion.Default
            val platform = arguments["platform"]
                ?.let { value -> DynamicScheme.Platform.valueOf(value.substringAfterLast('.')) }
                ?: DynamicScheme.Platform.Default
            val contrast = arguments["contrastLevel"]?.toDouble() ?: Contrast.Default.value

            return SchemeArguments(
                seed = checkNotNull(color("seedColor")) { "$call passes no seed" },
                keyColors = KeyColors(
                    primary = color("primary"),
                    secondary = color("secondary"),
                    tertiary = color("tertiary"),
                    neutral = color("neutral"),
                    neutralVariant = color("neutralVariant"),
                    error = color("error"),
                ),
                style = style,
                cmfTertiarySeed = arguments["style"]
                    ?.takeIf { style == Style.Cmf }
                    ?.takeIf { value -> "= " in value }
                    ?.let { value -> colorOf(value.substringAfter("= ").removeSuffix(")"), text, literals) },
                contrast = ContrastLevel((contrast * 100).roundToInt()),
                effectiveSpec = EffectiveSpec.of(style, SpecVersion.valueOf("Spec" + spec.name.removePrefix("SPEC_"))),
                platform = SchemePlatform.entries.single { entry ->
                    entry.name.equals(platform.name, ignoreCase = true)
                },
                amoled = if (target.hasAmoled) amoledOf(arguments, text) else null,
            )
        }

        /**
         * The library calls each target's dynamic export can build its scheme with, the first one
         * present being the one that does. Pins move a Material 3 scheme into a theme state first,
         * and an Unstyled one into a scheme per mode.
         */
        private val SchemeCalls = mapOf(
            ExportTarget.Material3 to listOf("rememberDynamicMaterialThemeState", "DynamicMaterialTheme"),
            ExportTarget.Material3Expressive to listOf(
                "rememberDynamicMaterialThemeState",
                "DynamicMaterialExpressiveTheme",
            ),
            ExportTarget.Unstyled to listOf("dynamicColorSchemes", "rememberDynamicScheme"),
            ExportTarget.Fluent to listOf("rememberFluentColors"),
            ExportTarget.Custom to listOf("rememberDynamicScheme"),
        )

        private val Identifier = Regex("""\w+""")

        private val ExportTarget.hasAmoled: Boolean
            get() = this != ExportTarget.Unstyled && this != ExportTarget.Fluent

        /** The named arguments of the first call to [call] in [text], each as the source text of its value. */
        private fun callArguments(
            text: String,
            call: String,
        ): Map<String, String> {
            val start = checkNotNull(Regex("""\b$call\(""").find(text)).range.last
            val arguments = mutableListOf(StringBuilder())
            var depth = 0
            for (char in text.substring(start)) {
                when (char) {
                    '(', '{' -> depth++
                    ')', '}' -> depth--
                }
                if (depth == 0) break
                when {
                    char == ',' && depth == 1 -> arguments += StringBuilder()
                    depth > 1 || char != '(' -> arguments.last().append(char)
                }
            }
            return arguments
                .map { argument -> argument.toString().trim() }
                .filter { argument -> " = " in argument }
                .associate { argument -> argument.substringBefore(" = ") to argument.substringAfter(" = ") }
        }

        /** The AMOLED switch of the call, or for Custom, which applies it through `MaterialKolors`, of the file. */
        private fun amoledOf(
            arguments: Map<String, String>,
            text: String,
        ): Boolean {
            val value = arguments["isAmoled"] ?: Regex("""\bisAmoled = (\w+)""").find(text)?.groupValues?.get(1)
            return value?.toBooleanStrict() ?: false
        }

        /** The color [value] names, a literal, a top-level color or a parameter whose default is one. */
        private fun colorOf(
            value: String,
            text: String,
            literals: Map<String, Argb>,
        ): Argb? {
            ColorLiterals.literal(value)?.let { color -> return color }
            literals[value]?.let { color -> return color }
            if (!Identifier.matches(value)) return null
            val default = Regex("""\b$value: Color = (\w+)""").find(text)?.groupValues?.get(1) ?: return null
            return literals[default]
        }
    }
}

/**
 * Every `Color(0x…)` literal in a set of generated files, by where it sits.
 *
 * A literal's name is the property, argument or Unstyled token it is assigned to, behind the names
 * of the calls it is nested in, as in `lightThemeColors/brand/color` or `primaryLight`.
 */
private object ColorLiterals {
    private val Literal = Regex("""Color\(0x([0-9A-Fa-f]{8})\)""")
    private val Named = Regex("""^\s*(?:val\s+|ThemeTokens\.)?(\w+)(?:\s*:[^=]+)?\s*(?:=|to)\s*Color\(0x""")
    private val Opening = Regex("""^\s*(?:(?:private\s+)?val\s+)?(\w+)(?:\s*:[^=]+)?\s*=\s*.*[({]$""")

    fun literal(value: String): Argb? =
        Literal.matchEntire(value.trim())?.let { match -> Argb(match.groupValues[1].toLong(radix = 16).toInt()) }

    fun read(files: List<GeneratedFile>): Map<String, Argb> =
        buildMap {
            files.filter { file -> file.path.endsWith(".kt") }.forEach { file ->
                val scopes = ArrayDeque<String>()
                for (line in file.text.lines().map(String::trimEnd)) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue
                    val literal = Literal.find(line)
                    val named = Named.find(line)
                    if (literal != null && named != null) {
                        val color = Argb(literal.groupValues[1].toLong(radix = 16).toInt())
                        put((scopes.filter(String::isNotEmpty) + named.groupValues[1]).joinToString("/"), color)
                        continue
                    }
                    if (trimmed.startsWith(")") || trimmed.startsWith("}")) scopes.removeLastOrNull()
                    if (trimmed.endsWith("(") || trimmed.endsWith("{")) {
                        scopes.addLast(Opening.find(line)?.groupValues?.get(1).orEmpty())
                    }
                }
            }
        }
}

/** [this] document as an export for [target] sees it. */
private fun ThemeDocument.on(target: ExportTarget): ThemeDocument {
    val library = when (target) {
        ExportTarget.Material3, ExportTarget.Material3Expressive -> Library.Material3
        ExportTarget.Unstyled -> Library.Unstyled
        ExportTarget.Fluent -> Library.Fluent
        ExportTarget.Custom -> Library.Custom
    }
    return copy(library = library, expressive = target == ExportTarget.Material3Expressive).forTarget(target)
}

/** [this] document at the contrast [variant] stands for, the way `ExportResolver` resolves it. */
private fun ThemeDocument.atContrast(variant: ContrastVariant): ThemeDocument =
    when (variant) {
        ContrastVariant.Standard -> this
        ContrastVariant.Medium -> copy(contrast = ContrastLevel.Medium)
        ContrastVariant.High -> copy(contrast = ContrastLevel.High)
    }

/** How the names of [this] variant's colors start, empty for standard. */
private val ContrastVariant.prefix: String
    get() = when (this) {
        ContrastVariant.Standard -> ""
        ContrastVariant.Medium -> "mediumContrast"
        ContrastVariant.High -> "highContrast"
    }

private fun ThemeResult.roleColors(isDark: Boolean) =
    (if (isDark) roles.dark else roles.light).mapValues { (_, entry) -> entry.argb }

/** The Fluent shades the preview's own primary palette gives in the mode [isDark] picks. */
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

private fun FluentShadeValues.named(): Map<String, Argb> =
    mapOf(
        "dark3" to dark3,
        "dark2" to dark2,
        "dark1" to dark1,
        "base" to base,
        "light1" to light1,
        "light2" to light2,
        "light3" to light3,
    )

/** The Unstyled token an accent part is written under, as in `onBrandContainer`. */
private fun AccentPart.unstyledToken(accent: String): String =
    when (this) {
        AccentPart.Color -> accent
        AccentPart.OnColor -> "on${accent.upperFirst()}"
        AccentPart.Container -> "${accent}Container"
        AccentPart.OnContainer -> "on${accent.upperFirst()}Container"
    }

/** The `ColorFamily` property an accent part is written under. */
private val AccentPart.property: String
    get() = when (this) {
        AccentPart.Color -> "color"
        AccentPart.OnColor -> "onColor"
        AccentPart.Container -> "colorContainer"
        AccentPart.OnContainer -> "onColorContainer"
    }

private fun String.lowerFirst(): String = replaceFirstChar(Char::lowercaseChar)

private fun String.upperFirst(): String = replaceFirstChar(Char::uppercaseChar)
