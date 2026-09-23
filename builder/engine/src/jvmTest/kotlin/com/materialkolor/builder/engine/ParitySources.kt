package com.materialkolor.builder.engine

import com.materialkolor.Contrast
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import kotlin.math.roundToInt
import kotlin.test.fail

/**
 * The scheme arguments a dynamic export passes, with each one it leaves out read as the default
 * of the call it leaves it out of.
 *
 * AMOLED is null for the targets whose call has no such switch.
 */
internal data class SchemeArguments(
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

        /**
         * The arguments the scheme call in [files] passes, the way the call itself reads them. A
         * failure starts with [context] and quotes the call as it was read.
         */
        fun read(
            target: ExportTarget,
            files: List<GeneratedFile>,
            context: String,
        ): SchemeArguments {
            val text = files.filter { file -> file.path.endsWith(".kt") }.joinToString("\n") { file -> file.text }
            val literals = ColorLiterals.read(files)
            val call = SchemeCalls.getValue(target).firstOrNull { name -> Regex("""\b$name\(""").containsMatchIn(text) }
                ?: fail("$context\nThe export never calls ${SchemeCalls.getValue(target)}")
            val expressive = call == "DynamicMaterialExpressiveTheme"
            val arguments = callArguments(text, call)
            val written = arguments.entries.joinToString(prefix = "$call(", postfix = ")") { (name, value) ->
                "$name = $value"
            }

            fun color(name: String): Argb? =
                arguments[name]?.let { value ->
                    colorOf(value, text, literals) ?: fail("$context\n$written passes $name = $value, no color")
                }

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
                seed = color("seedColor") ?: fail("$context\n$written passes no seed"),
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
        private val Declaration = Regex("""\bfun (\w+)\(""")

        private val ExportTarget.hasAmoled: Boolean
            get() = this != ExportTarget.Unstyled && this != ExportTarget.Fluent

        /**
         * The named arguments of the first call to [call] in [text], each as the source text of its
         * value. A `fun` declaring [call] is not a call.
         */
        private fun callArguments(
            text: String,
            call: String,
        ): Map<String, String> {
            val start = checkNotNull(Regex("""(?<!fun )\b$call\(""").find(text)).range.last
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

        /**
         * The color [value] names, a literal, a top-level color or a parameter. A parameter reads as
         * its default, or with none as what the call to its function passes, the way Custom's
         * `rememberThemeColors(seedColor: Color, …)` takes its seed from `AppTheme`.
         */
        private fun colorOf(
            value: String,
            text: String,
            literals: Map<String, Argb>,
            followed: Set<String> = emptySet(),
        ): Argb? {
            ColorLiterals.literal(value)?.let { color -> return color }
            literals[value]?.let { color -> return color }
            if (!Identifier.matches(value) || value in followed) return null
            Regex("""\b$value: Color = (\w+)""").find(text)?.let { default -> return literals[default.groupValues[1]] }
            val passed = passedFor(value, text) ?: return null
            return colorOf(passed, text, literals, followed + value)
        }

        /** What the call to the function declaring the parameter [name] passes for it, if [text] makes that call. */
        private fun passedFor(
            name: String,
            text: String,
        ): String? {
            val parameter = Regex("""(?<!val )\b$name: Color\b""").find(text) ?: return null
            val function = Declaration
                .findAll(text.substring(0, parameter.range.first))
                .lastOrNull()
                ?.groupValues
                ?.get(1) ?: return null
            if (!Regex("""(?<!fun )\b$function\(""").containsMatchIn(text)) return null
            return callArguments(text, function)[name]
        }
    }
}

/**
 * Every `Color(0x…)` literal in a set of generated files, by where it sits.
 *
 * A literal's name is the property, argument or Unstyled token it is assigned to, behind the names
 * of the calls it is nested in, as in `lightThemeColors/brand/color` or `primaryLight`.
 */
internal object ColorLiterals {
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
                        scopes.addLast(
                            Opening
                                .find(line)
                                ?.groupValues
                                ?.get(1)
                                .orEmpty(),
                        )
                    }
                }
            }
        }
}
