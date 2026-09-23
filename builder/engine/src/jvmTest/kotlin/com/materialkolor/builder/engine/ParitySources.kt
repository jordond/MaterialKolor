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
    /**
     * A document that holds these arguments and nothing else, so the reference calls build with
     * what the export passes and never with what the document it came from says.
     */
    fun document(): ThemeDocument =
        ThemeDocument(
            seed = seed,
            keyColors = keyColors,
            style = style,
            cmfTertiarySeed = cmfTertiarySeed,
            contrast = contrast,
            spec = effectiveSpec,
            platform = platform,
            amoled = amoled ?: false,
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

        /** The arguments [call] passes in [source], the way the call itself reads them. */
        fun read(
            target: ExportTarget,
            source: ExportSource,
            call: SchemeCall,
        ): SchemeArguments {
            val arguments = call.arguments
            val expressive = call.function == "DynamicMaterialExpressiveTheme"

            fun color(name: String): Argb? =
                arguments[name]?.let { value -> source.color(value, "${call.written} passes $name = $value") }

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
                seed = color("seedColor") ?: source.fail("${call.written} passes no seed"),
                keyColors = KeyColors(
                    primary = color("primary"),
                    secondary = color("secondary"),
                    tertiary = color("tertiary"),
                    neutral = color("neutral"),
                    neutralVariant = color("neutralVariant"),
                    error = color("error"),
                ),
                style = style,
                cmfTertiarySeed = arguments["style"]?.takeIf { style == Style.Cmf }?.let { value ->
                    cmfTertiarySeed(source, value, call)
                },
                contrast = ContrastLevel((contrast * 100).roundToInt()),
                effectiveSpec = EffectiveSpec.of(style, SpecVersion.valueOf("Spec" + spec.name.removePrefix("SPEC_"))),
                platform = SchemePlatform.entries.single { entry ->
                    entry.name.equals(platform.name, ignoreCase = true)
                },
                amoled = if (target.hasAmoled) amoledOf(source, arguments) else null,
            )
        }

        private const val TERTIARY_SEED = "tertiarySeedColor"

        private val ExportTarget.hasAmoled: Boolean
            get() = this != ExportTarget.Unstyled && this != ExportTarget.Fluent

        /**
         * The tertiary seed of `PaletteStyle.Cmf(...)`, none for `PaletteStyle.Cmf()`. Anything past
         * `tertiarySeedColor`, or a seed that names no color, fails the way an unreadable key color does.
         */
        private fun cmfTertiarySeed(
            source: ExportSource,
            style: String,
            call: SchemeCall,
        ): Argb? {
            val open = style.indexOf('(')
            if (open < 0) return null
            val arguments = argumentsAt(style, open)
            if (arguments.any { argument -> !argument.startsWith("$TERTIARY_SEED = ") }) {
                source.fail("${call.written} passes style = $style, which holds more than a $TERTIARY_SEED")
            }
            return arguments.named()[TERTIARY_SEED]?.let { value ->
                source.color(value, "${call.written} passes the CMF tertiarySeedColor = $value")
            }
        }

        /** The AMOLED switch of the call, or for Custom, which applies it through `MaterialKolors`, of the file. */
        private fun amoledOf(
            source: ExportSource,
            arguments: Map<String, String>,
        ): Boolean {
            val value = arguments["isAmoled"] ?: Regex("""\bisAmoled = (\w+)""").find(source.text)?.groupValues?.get(1)
            return value?.toBooleanStrict() ?: false
        }
    }
}

/**
 * The call a dynamic export builds its scheme with, and the named arguments it passes.
 *
 * @property[function] The function called.
 * @property[arguments] The arguments every call to it passes, `isDark` aside.
 * @property[written] The calls as they were read, for failure messages.
 */
internal class SchemeCall(
    val function: String,
    val arguments: Map<String, String>,
    val written: String,
) {
    companion object {
        /**
         * The library calls each target's dynamic export can build its scheme with, the first one
         * present being the one that does. Pins move a Material 3 scheme into a theme state first,
         * and an Unstyled one into a scheme per mode. Fluent calls its module, or with the module not
         * published builds a core scheme inline.
         */
        private val SchemeCalls = mapOf(
            ExportTarget.Material3 to listOf("rememberDynamicMaterialThemeState", "DynamicMaterialTheme"),
            ExportTarget.Material3Expressive to listOf(
                "rememberDynamicMaterialThemeState",
                "DynamicMaterialExpressiveTheme",
            ),
            ExportTarget.Unstyled to listOf("dynamicColorSchemes", "rememberDynamicScheme"),
            ExportTarget.Fluent to listOf("rememberFluentColors", "rememberDynamicScheme"),
            ExportTarget.Custom to listOf("rememberDynamicScheme"),
        )

        /**
         * The scheme call [target]'s export makes in [source]. One call passes the theme's own
         * `isDark`, or none, and two calls pass `false` and `true`, one each, and the same arguments
         * apart from that. A scheme per mode with either mode missing fails.
         */
        fun read(
            target: ExportTarget,
            source: ExportSource,
        ): SchemeCall {
            val candidates = SchemeCalls.getValue(target)
            val function = candidates.firstOrNull { name -> source.callsOf(name).isNotEmpty() }
                ?: source.fail("The export never calls any of $candidates")
            val calls = source.callsOf(function).map { arguments -> arguments.named() }
            val written = calls.joinToString("\n") { arguments ->
                arguments.entries.joinToString(prefix = "$function(", postfix = ")") { (name, value) ->
                    "$name = $value"
                }
            }
            val modes = calls.map { arguments -> arguments[IS_DARK] }
            val oneCall = calls.size == 1 && modes.single() in setOf(null, IS_DARK)
            val callPerMode = calls.size == 2 && modes.sortedBy { mode -> mode.orEmpty() } == listOf("false", "true")
            if (!oneCall && !callPerMode) {
                source.fail(
                    "The export calls $function with isDark = $modes, where it needs one call on the theme's " +
                        "own isDark, or one call with isDark = false and one with isDark = true\n$written",
                )
            }
            val shared = calls.map { arguments -> arguments - IS_DARK }.distinct()
            if (shared.size != 1) source.fail("The $function calls differ in more than isDark\n$written")
            return SchemeCall(function, shared.single(), written)
        }
    }
}

/**
 * The Kotlin files of one export read as one text, with what the parity gate needs to find calls,
 * their arguments and the colors they name. Every failure starts with [context], which names the
 * target and the document.
 */
internal class ExportSource(
    files: List<GeneratedFile>,
    private val context: String,
) {
    val text: String = files.filter { file -> file.path.endsWith(".kt") }.joinToString("\n") { file -> file.text }
    private val literals = ColorLiterals.read(files)

    fun fail(message: String): Nothing = kotlin.test.fail("$context\n$message")

    /**
     * The arguments of every call to [function], in the order the calls are made, each argument as
     * its source text on one line. A `fun` or `class` declaring [function] is not a call.
     */
    fun callsOf(function: String): List<List<String>> =
        Regex("""\b$function\(""")
            .findAll(text)
            .filterNot { match -> declares(match.range.first) }
            .map { match -> argumentsAt(text, match.range.last) }
            .toList()

    /** The arguments of every call to [function] a `val` holds, by the name of that `val`. */
    fun assigned(function: String): Map<String, List<String>> =
        Regex("""\bval (\w+) = $function\(""")
            .findAll(text)
            .associate { match -> match.groupValues[1] to argumentsAt(text, match.range.last) }

    /** The color [value] names, failing with [what] when it names none. */
    fun color(
        value: String,
        what: String,
    ): Argb = colorOf(value) ?: fail("$what, which names no color")

    /**
     * The color [value] names, a literal, a top-level color or a parameter. A parameter reads as
     * its default, or with none as what every call to its function passes, the way Custom's
     * `rememberThemeColors(seedColor: Color, …)` takes its seed from `AppTheme`.
     */
    private fun colorOf(
        value: String,
        followed: Set<String> = emptySet(),
    ): Argb? {
        ColorLiterals.literal(value)?.let { color -> return color }
        literals[value]?.let { color -> return color }
        if (!Identifier.matches(value) || value in followed) return null
        Regex("""\b$value: Color = (\w+)""").find(text)?.let { default -> return literals[default.groupValues[1]] }
        val passed = passedFor(value) ?: return null
        return colorOf(passed, followed + value)
    }

    /** What every call to the function declaring the parameter [name] passes for it, if they agree. */
    private fun passedFor(name: String): String? {
        val parameter = Regex("""(?<!val )\b$name: Color\b""").find(text) ?: return null
        val function = Declaration
            .findAll(text.substring(0, parameter.range.first))
            .lastOrNull()
            ?.groupValues
            ?.get(1) ?: return null
        return callsOf(function)
            .map { arguments -> arguments.named()[name] }
            .distinct()
            .singleOrNull()
    }

    /** Whether the name at [index] is being declared, after `fun`, `fun Receiver.` or `class`. */
    private fun declares(index: Int): Boolean =
        Declaring.containsMatchIn(text.substring(text.lastIndexOf('\n', index) + 1, index))

    private companion object {
        val Identifier = Regex("""\w+""")
        val Declaration = Regex("""\bfun (\w+)\(""")
        val Declaring = Regex("""\b(?:fun|class)\s+(?:\w+\.)?$""")
    }
}

/** The parameter every mode-aware call in an export takes the mode through. */
internal const val IS_DARK = "isDark"

/**
 * The arguments of the call whose `(` sits at [open] in [text], each as its source text with its
 * whitespace folded to single spaces. A trailing comma adds no argument.
 */
internal fun argumentsAt(
    text: String,
    open: Int,
): List<String> {
    val close = closingOf(text, open)
    val arguments = mutableListOf(StringBuilder())
    var depth = 0
    var quoted = false
    for (char in text.substring(open + 1, close)) {
        when {
            char == '"' -> quoted = !quoted
            quoted -> Unit
            char in "({[" -> depth++
            char in ")}]" -> depth--
        }
        if (char == ',' && depth == 0 && !quoted) arguments += StringBuilder() else arguments.last().append(char)
    }
    return arguments.map { argument -> argument.toString().oneLine() }.filter(String::isNotEmpty)
}

/** Where the bracket that closes the one at [open] in [text] sits. */
internal fun closingOf(
    text: String,
    open: Int,
): Int {
    var depth = 0
    var quoted = false
    for (index in open until text.length) {
        val char = text[index]
        when {
            char == '"' -> quoted = !quoted
            quoted -> Unit
            char in "({[" -> depth++
            char in ")}]" -> if (--depth == 0) return index
        }
    }
    error("Nothing closes the bracket at $open in\n$text")
}

/** The named ones of these arguments, by name. */
internal fun List<String>.named(): Map<String, String> =
    mapNotNull { argument -> NamedArgument.matchEntire(argument) }.associate { match ->
        match.groupValues[1] to match.groupValues[2]
    }

/** [this] source text on one line, every run of whitespace a single space. */
internal fun String.oneLine(): String = replace(Whitespace, " ").trim()

private val NamedArgument = Regex("""^(\w+) = (.+)$""")
private val Whitespace = Regex("""\s+""")

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
