package com.materialkolor.builder.engine

import com.materialkolor.MaterialKolors
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.ContrastThreshold
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette

/**
 * What a dynamic export's colors come to, read out of its own code and nothing else. That means the
 * arguments of its scheme call, the pins it lays over the scheme, the Custom slots it cuts, the
 * accent families it builds and the Fluent shades it builds inline.
 *
 * Every color is worked out through the library call the export makes, on a scheme built with
 * the arguments it passes, so a swapped mode, a wrong tone or a wrong ramp shows up as a color.
 */
internal class DynamicExport private constructor(
    private val target: ExportTarget,
    private val source: ExportSource,
    private val call: SchemeCall,
    val arguments: SchemeArguments,
) {
    private val document = arguments.document()

    /**
     * Every color the export gives in the mode [isDark] picks, keyed by the name it sits under. A role
     * is keyed by its property, a Custom slot by its property, a Fluent shade by its name, an accent
     * color as `brand/onColor`, or for Unstyled as `ThemeTokens.onBrand`.
     */
    fun colors(isDark: Boolean): Map<String, Argb> =
        when (target) {
            ExportTarget.Material3, ExportTarget.Material3Expressive, ExportTarget.Inklet -> {
                material3Roles(document, isDark).byName() + material3Pins(isDark) + families("ExtendedColors", isDark)
            }
            ExportTarget.Unstyled -> {
                unstyledRoles(document, isDark).byName() + unstyledValues(isDark)
            }
            ExportTarget.Fluent -> {
                if (call.function == "rememberFluentColors") {
                    fluentShades(document, isDark).byShade()
                } else {
                    inlineShades(isDark)
                }
            }
            ExportTarget.Custom -> {
                customColors(isDark)
            }
        }

    /**
     * The roles `modifyColorScheme = { scheme -> scheme.copy(...) }` replaces in the mode [isDark]
     * picks. Each branch is a literal or the scheme's own color for that same role.
     */
    private fun material3Pins(isDark: Boolean): Map<String, Argb> {
        val modify = call.arguments["modifyColorScheme"] ?: return emptyMap()
        val lambda = PinnedScheme.matchEntire(modify)?.takeIf { match -> match.groupValues[1] == match.groupValues[2] }
            ?: source.fail("modifyColorScheme = $modify is not a { scheme -> scheme.copy(...) }")
        val scheme = lambda.groupValues[1]
        val copy = modify.indexOf("$scheme.copy(") + "$scheme.copy".length
        return buildMap {
            argumentsAt(modify, copy).named().forEach { (property, value) ->
                roleNamed(property) ?: source.fail("modifyColorScheme sets $property, which is no role")
                val branch = branchOf(value, isDark)
                val pinned = ColorLiterals.literal(branch)
                when {
                    pinned != null -> {
                        put(property, pinned)
                    }
                    branch != "$scheme.$property" -> {
                        source.fail("modifyColorScheme sets $property ${modeOf(isDark)} to $branch, no color")
                    }
                }
            }
        }
    }

    /**
     * The pins and accent tokens an explicit Unstyled export lays over the mode [isDark] picks. The
     * light values sit before `colorScheme(ColorScheme.Dark)` and the dark ones in it, and each has
     * to come off the scheme built for that mode. An export without pins or accents only has to put
     * each half of `rememberDynamicLightDarkColors` in its own mode.
     */
    private fun unstyledValues(isDark: Boolean): Map<String, Argb> {
        val darkBlock = source.text.indexOf(DARK_BLOCK).takeIf { index -> index >= 0 }
            ?: source.fail("The export never opens $DARK_BLOCK")
        val section = if (isDark) source.text.substring(darkBlock) else source.text.substring(0, darkBlock)
        if (call.function == "rememberDynamicLightDarkColors") {
            val values = if (isDark) "dark" else "light"
            if ("$COLORS_PROPERTY = $values\n" !in section) {
                source.fail("The ${modeOf(isDark)} values are not the $values half of rememberDynamicLightDarkColors")
            }
            return emptyMap()
        }
        val schemes = ThemeValues.findAll(section).map { match -> match.groupValues[1] }.toList()
        val scheme = schemes.singleOrNull()
            ?: source.fail("The ${modeOf(isDark)} values read toThemeValues() off $schemes, not one scheme")
        val schemeMode = source.assigned("rememberDynamicScheme")[scheme]?.named()?.get(IS_DARK)
        if (schemeMode != isDark.toString()) {
            source.fail("The ${modeOf(isDark)} values come off $scheme, which is built with isDark = $schemeMode")
        }

        val maps = MapOf.findAll(section).toList()
        if (maps.size > 1) source.fail("The ${modeOf(isDark)} values add ${maps.size} maps, where one was wanted")
        val entries = maps.singleOrNull()?.let { match -> argumentsAt(section, match.range.last) }.orEmpty()
        val evaluation = Evaluation(source, isDark, palettes())
        return entries.associate { entry ->
            val (owner, token, value) = TokenEntry.matchEntire(entry)?.destructured
                ?: source.fail("The ${modeOf(isDark)} values add $entry, which is no token entry")
            when (owner) {
                "MaterialKolorTokens" -> {
                    roleNamed(token) ?: source.fail("The ${modeOf(isDark)} values pin $token, which is no role")
                    token to source.color(value, "The ${modeOf(isDark)} values pin $token to $value")
                }
                "ThemeTokens" -> {
                    "ThemeTokens.$token" to evaluation.color(value)
                }
                else -> {
                    source.fail("The ${modeOf(isDark)} values add $entry, a token of no known object")
                }
            }
        }
    }

    /**
     * Every Custom slot and accent color `ThemeColors(...)` gets in the mode [isDark] picks, off the
     * scheme and the `MaterialKolors` the export builds, with the AMOLED switch that call passes.
     */
    private fun customColors(isDark: Boolean): Map<String, Argb> {
        requireThemeMode("rememberThemeColors")
        val scheme = referenceScheme(document, isDark)
        val schemeName = source.assigned("rememberDynamicScheme").keys.singleOrNull()
            ?: source.fail("The export holds no single rememberDynamicScheme in a val")
        val (kolorsName, kolorsArguments) = source.assigned("MaterialKolors").entries.singleOrNull()
            ?: source.fail("The export holds no single MaterialKolors in a val")
        if (kolorsArguments.firstOrNull() != schemeName) {
            source.fail("MaterialKolors($kolorsArguments) reads no $schemeName")
        }
        val amoled = kolorsArguments.named()["isAmoled"]?.toBooleanStrict() ?: false
        val names = palettes() + mapOf(schemeName to scheme, kolorsName to MaterialKolors(scheme, isAmoled = amoled))
        return values(source.callsOf("ThemeColors"), "ThemeColors", Evaluation(source, isDark, names))
    }

    /**
     * Every accent color `ExtendedColors(...)` gets in the mode [isDark] picks, none without accents.
     */
    private fun families(
        type: String,
        isDark: Boolean,
    ): Map<String, Argb> {
        val calls = source.callsOf(type)
        if (calls.isEmpty()) return emptyMap()
        requireThemeMode("rememberExtendedColors")
        return values(calls, type, Evaluation(source, isDark, palettes()))
    }

    /**
     * The colors of the one call in [calls], each named argument flattened to `name` or `name/part`.
     */
    private fun values(
        calls: List<List<String>>,
        type: String,
        evaluation: Evaluation,
    ): Map<String, Argb> {
        val arguments = calls.singleOrNull()?.named()
            ?: source.fail("The export builds $type ${calls.size} times, not once")
        return buildMap {
            arguments.forEach { (name, value) ->
                when (val result = evaluation.value(value)) {
                    is Argb -> {
                        put(name, result)
                    }
                    is Map<*, *> -> {
                        result.forEach { (part, color) ->
                            val argb = color as? Argb ?: source.fail("$type gets $name/$part = $color, no color")
                            put("$name/$part", argb)
                        }
                    }
                    else -> {
                        source.fail("$type gets $name = $value, which comes to $result, not a color")
                    }
                }
            }
        }
    }

    /**
     * The shades an inline Fluent export's own `toShades()` cuts in the mode [isDark] picks.
     */
    private fun inlineShades(isDark: Boolean): Map<String, Argb> {
        val colors = source.callsOf("Colors").singleOrNull()?.named()
            ?: source.fail("The export builds no single Colors(...)")
        val darkMode = colors["darkMode"]
        if (darkMode != IS_DARK) source.fail("Colors(...) passes darkMode = $darkMode, not isDark")
        val shades = colors["shades"]?.takeIf { value -> value.endsWith(".$TO_SHADES()") }
            ?: source.fail("Colors(...) passes shades = ${colors["shades"]}, not a palette's $TO_SHADES()")
        val schemeName = source.assigned("rememberDynamicScheme").keys.singleOrNull()
            ?: source.fail("The export holds no single rememberDynamicScheme in a val")
        val evaluation = Evaluation(source, isDark, mapOf(schemeName to referenceScheme(document, isDark)))
        return evaluation.shades(shades)
    }

    /**
     * Every accent ramp the export remembers, by the `val` that holds it.
     */
    private fun palettes(): Map<String, TonalPalette> =
        source.assigned("rememberTonalPalette").mapValues { (name, arguments) ->
            val named = arguments.named()
            if (named.size != arguments.size || !named.keys.all { key -> key == "seed" || key == "harmonizeWith" }) {
                source.fail("$name = rememberTonalPalette($arguments) passes more than a seed and harmonizeWith")
            }
            val seed = named["seed"] ?: source.fail("$name passes no seed")
            tonalPalette(
                seed = source.color(seed, "$name passes seed = $seed"),
                harmonizeWith = named["harmonizeWith"]?.let { value ->
                    source.color(value, "$name passes harmonizeWith = $value")
                },
            )
        }

    /**
     * Every call to [function] passes the theme's own `isDark`, so its `if (isDark)` picks the theme's mode.
     */
    private fun requireThemeMode(function: String) {
        source.callsOf(function).forEach { arguments ->
            val mode = arguments.named()[IS_DARK]
            if (mode != IS_DARK) source.fail("$function is called with isDark = $mode, not the theme's own isDark")
        }
    }

    companion object {
        /**
         * The dynamic export [files] of [target] as its own code reads. A failure starts with [context].
         */
        fun read(
            target: ExportTarget,
            files: List<GeneratedFile>,
            context: String,
        ): DynamicExport {
            val source = ExportSource(files, context)
            val call = SchemeCall.read(target, source)
            return DynamicExport(target, source, call, SchemeArguments.read(target, source, call))
        }

        private const val DARK_BLOCK = "colorScheme(ColorScheme.Dark)"
        private const val COLORS_PROPERTY = "properties[MaterialKolorTokens.colors]"
        private const val TO_SHADES = "toShades"
        private val PinnedScheme = Regex("""^\{ (\w+) -> (\w+)\.copy\(.*\) \}$""")
        private val ThemeValues = Regex("""\b(\w+)\.toThemeValues\(\)""")
        private val MapOf = Regex("""\bmapOf\(""")
        private val TokenEntry = Regex("""^(\w+)\.(\w+) to (.+)$""")
    }
}

/**
 * Works out what one expression an export writes comes to in the mode [isDark] picks, with [names]
 * in scope and, inside a `TonalPalette` helper, [receiver] as `this`.
 *
 * It knows what the exports write and nothing more. That is `if (isDark) a else b`, color, tone and
 * threshold literals, a scheme's ramps, `toneColor`, `onTone`, a `MaterialKolors` role, and a call
 * to a `TonalPalette` helper the export declares itself, whose body it reads and runs.
 */
private class Evaluation(
    private val source: ExportSource,
    private val isDark: Boolean,
    private val names: Map<String, Any>,
    private val receiver: TonalPalette? = null,
) {
    fun value(expression: String): Any {
        IfDark.matchEntire(expression)?.let { match -> return value(match.groupValues[if (isDark) 1 else 2]) }
        ColorLiterals.literal(expression)?.let { color -> return color }
        expression.toIntOrNull()?.let { tone -> return tone }
        names[expression]?.let { named -> return named }
        Threshold.matchEntire(expression)?.let { match -> return ContrastThreshold.valueOf(match.groupValues[1]) }
        if (expression.endsWith(")")) return call(expression)
        Member.matchEntire(expression)?.let { match ->
            return member(value(match.groupValues[1]), match.groupValues[2])
        }
        source.fail("Nothing in the parity reader works out $expression")
    }

    fun color(expression: String): Argb =
        value(expression) as? Argb ?: source.fail("$expression ${modeOf(isDark)} is no color")

    /**
     * The shades `palette.toShades()` in [expression] gives, by shade name.
     */
    fun shades(expression: String): Map<String, Argb> {
        val palette = value(expression.substringBeforeLast('.')) as? TonalPalette
            ?: source.fail("$expression is not called on a palette")
        return helper(palette, expression.substringAfterLast('.').substringBefore('('), emptyMap())
            .mapValues { (shade, color) -> color as? Argb ?: source.fail("The shade $shade comes to $color, no color") }
    }

    private fun call(expression: String): Any {
        val open = openingOf(expression)
        val head = expression.substring(0, open)
        val arguments = argumentsAt(expression, open)
        val function = head.substringAfterLast('.')
        val target: Any? = if ('.' in head) value(head.substringBeforeLast('.')) else receiver
        return when {
            target is TonalPalette && function == "toneColor" -> {
                target.toneColor(tone(arguments.single())).asArgb()
            }
            target is TonalPalette && function == "onTone" && arguments.size == 1 -> {
                target.onTone(tone(arguments[0])).asArgb()
            }
            target is TonalPalette && function == "onTone" && arguments.size == 2 -> {
                target.onTone(tone(arguments[0]), threshold(arguments[1])).asArgb()
            }
            target is TonalPalette && arguments.size == arguments.named().size -> {
                helper(target, function, arguments.named().mapValues { (_, argument) -> value(argument) })
            }
            target is MaterialKolors && arguments.isEmpty() -> {
                val role = roleNamed(function) ?: source.fail("$expression reads $function, which is no role")
                target.roleColor(role).asArgb()
            }
            else -> {
                source.fail("Nothing in the parity reader works out $expression")
            }
        }
    }

    private fun member(
        target: Any,
        name: String,
    ): Any =
        when {
            target is DynamicScheme && name == "primaryPalette" -> target.primaryPalette
            target is DynamicScheme && name == "secondaryPalette" -> target.secondaryPalette
            target is DynamicScheme && name == "tertiaryPalette" -> target.tertiaryPalette
            target is DynamicScheme && name == "errorPalette" -> target.errorPalette
            target is DynamicScheme && name == "neutralPalette" -> target.neutralPalette
            target is DynamicScheme && name == "neutralVariantPalette" -> target.neutralVariantPalette
            else -> source.fail("Nothing in the parity reader works out $name of $target")
        }

    /**
     * What the export's own `fun TonalPalette.[function](...)` gives [palette] for [arguments],
     * each property of what it returns by name. The body's `val`s run in order, and a returned
     * class the export declares takes positional arguments in its declared order.
     */
    private fun helper(
        palette: TonalPalette,
        function: String,
        arguments: Map<String, Any>,
    ): Map<String, Any> {
        val declaration = Regex("""\bfun TonalPalette\.$function\(""").find(source.text)
            ?: source.fail("The export calls $function on a palette but declares no fun TonalPalette.$function")
        val parametersClose = closingOf(source.text, declaration.range.last)
        val bodyOpen = source.text.indexOf('{', parametersClose)
        val body = source.text.substring(bodyOpen + 1, closingOf(source.text, bodyOpen))

        val scope = names.toMutableMap()
        val parameters = argumentsAt(source.text, declaration.range.last).map { parameter ->
            val (name, default) = Parameter.matchEntire(parameter)?.destructured
                ?: source.fail("$function declares $parameter, which the parity reader cannot read")
            scope[name] = arguments[name]
                ?: default.takeIf(String::isNotEmpty)?.let { value ->
                    Evaluation(source, isDark, scope, palette).value(value)
                }
                ?: source.fail("A call to $function passes no $name, and it has no default")
            name
        }
        (arguments.keys - parameters.toSet()).takeIf(Set<String>::isNotEmpty)?.let { unknown ->
            source.fail("A call to $function passes $unknown, which it does not declare")
        }
        Local.findAll(body).forEach { match ->
            val (name, value) = match.destructured
            scope[name] = Evaluation(source, isDark, scope, palette).value(value.oneLine())
        }

        val returned = Returned.find(body) ?: source.fail("$function returns no constructor call")
        val type = returned.groupValues[1]
        val values = argumentsAt(body, returned.range.last)
        val evaluation = Evaluation(source, isDark, scope, palette)
        if (values.size == values.named().size) {
            return values.named().mapValues { (_, value) -> evaluation.value(value) }
        }

        val properties = declaredProperties(type)
        if (properties.size != values.size) source.fail("$function returns $type($values), which declares $properties")
        return properties.zip(values).associate { (property, value) -> property to evaluation.value(value) }
    }

    /**
     * The properties the export's own `data class [type]` declares, in order.
     */
    private fun declaredProperties(type: String): List<String> {
        val declaration = Regex("""\bclass $type\(""").find(source.text)
            ?: source.fail("The export returns $type positionally but does not declare it")
        return argumentsAt(source.text, declaration.range.last).map { property ->
            Property.matchEntire(property)?.groupValues?.get(1) ?: source.fail("$type declares $property")
        }
    }

    private fun tone(expression: String): Int = value(expression) as? Int ?: source.fail("$expression is no tone")

    private fun threshold(expression: String): ContrastThreshold =
        value(expression) as? ContrastThreshold ?: source.fail("$expression is no threshold")

    private companion object {
        val IfDark = Regex("""^if \(isDark\) (.+?) else (.+)$""")
        val Threshold = Regex("""^ContrastThreshold\.(\w+)$""")
        val Member = Regex("""^(.+)\.(\w+)$""")
        val Parameter = Regex("""^(\w+): [\w.<>?]+(?: = (.+))?$""")
        val Local = Regex("""^\s*val (\w+) = (.+)$""", RegexOption.MULTILINE)
        val Returned = Regex("""\breturn (\w+)\(""")
        val Property = Regex("""^val (\w+): .+$""")
    }
}

/**
 * Where the `(` that the closing `)` of [expression] pairs with sits.
 */
private fun openingOf(expression: String): Int {
    var depth = 0
    for (index in expression.indices.reversed()) {
        when (expression[index]) {
            ')', '}', ']' -> depth++
            '(', '{', '[' -> if (--depth == 0) return index
        }
    }
    error("Nothing opens the call in $expression")
}

/**
 * The branch of `if (isDark) a else b` the mode [isDark] picks, or [value] itself when it has none.
 */
private fun branchOf(
    value: String,
    isDark: Boolean,
): String =
    Regex("""^if \(isDark\) (.+?) else (.+)$""").matchEntire(value)?.groupValues?.get(if (isDark) 1 else 2) ?: value

/**
 * The role whose property is [name], as in `surfaceContainerHigh`.
 */
private fun roleNamed(name: String): Role? = Role.entries.firstOrNull { role -> role.name.lowerFirst() == name }

private fun modeOf(isDark: Boolean): String = if (isDark) "in dark mode" else "in light mode"

private fun Map<Role, Argb>.byName(): Map<String, Argb> = mapKeys { (role, _) -> role.name.lowerFirst() }

private fun String.lowerFirst(): String = replaceFirstChar(Char::lowercaseChar)
