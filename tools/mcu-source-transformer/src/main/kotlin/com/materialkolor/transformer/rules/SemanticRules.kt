package com.materialkolor.transformer.rules

import com.materialkolor.transformer.edits.SourceEdits
import com.materialkolor.transformer.psi.nodes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

private val policies: Map<String, (KtFile, SourceEdits) -> Unit> = mapOf(
    "hct/Hct.kt" to ::immutableHct,
    "palettes/TonalPalette.kt" to ::tonalPaletteCache,
    "temperature/TemperatureCache.kt" to ::temperatureCache,
    "dynamiccolor/ContrastCurve.kt" to ::contrastCurve,
    "contrast/Contrast.kt" to ::contrastFloatOverloads,
    "blend/Blend.kt" to ::blendHarmonize,
    "utils/ColorUtils.kt" to ::colorUtilsLuminance,
    "dynamiccolor/DynamicColor.kt" to ::dynamicColorFactory,
    "dynamiccolor/DynamicScheme.kt" to ::schemeConveniences,
    "dynamiccolor/ColorSpec.kt" to ::colorSpecDefault,
    "score/Score.kt" to ::scoreFallback,
    "hct/ViewingConditions.kt" to ::viewingConditionsVisibility,
    "hct/Cam16.kt" to ::cam16Visibility,
)

/**
 * Implementation types the library hides without touching their declarations otherwise.
 */
private val internalTypes = mapOf(
    "utils/MathUtils.kt" to "MathUtils",
    "dynamiccolor/ColorSpecs.kt" to "ColorSpecs",
    "quantize/Quantizer.kt" to "Quantizer",
    "quantize/QuantizerMap.kt" to "QuantizerMap",
    "quantize/QuantizerResult.kt" to "QuantizerResult",
    "quantize/QuantizerWu.kt" to "QuantizerWu",
    "quantize/QuantizerWsmeans.kt" to "QuantizerWsmeans",
    "quantize/PointProvider.kt" to "PointProvider",
    "quantize/PointProviderLab.kt" to "PointProviderLab",
)

/**
 * Small API policies, each bounded to a reviewed declaration. No color algorithm bodies live here.
 */
internal fun semantics(
    file: KtFile,
    edits: SourceEdits,
) {
    val path = file.name
    val expectedPath = file.packageFqName.asString().replace('.', '/') + "/" + path.substringAfterLast('/')
    require(path == expectedPath) { "$path:0: semantic-inventory: expected $expectedPath" }

    policies[path]?.invoke(file, edits)

    internalTypes[path]?.let { name ->
        val type = file.type(name)
        if (!type.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
            edits.insert(type.textRange.startOffset, "internal ", Rule.ImplementationVisibility)
        }
    }
}

private fun immutableHct(
    file: KtFile,
    edits: SourceEdits,
) {
    val hct = file.type("Hct") as KtClass
    val constructorUnchanged = hct.primaryConstructor?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true &&
        hct.primaryConstructorParameters.singleOrNull()?.let { parameter ->
            parameter.name == "argb" && parameter.typeReference?.text == "Int" && !parameter.hasValOrVar()
        } == true &&
        hct.secondaryConstructors.isEmpty()
    require(constructorUnchanged) {
        "${file.name}:${hct.textOffset}: hct-mutation-surface: constructor changed"
    }

    val methods = hct.declarations.filterIsInstance<KtNamedFunction>()
    val names = setOf("toInt", "setHue", "setChroma", "setTone", "toString", "inViewingConditions", "setInternalState")
    require(methods.map { it.name }.toSet() == names && methods.size == names.size) {
        "${file.name}:${hct.textOffset}: hct-mutation-surface: method inventory changed: ${methods.map { it.name }}"
    }

    val properties = hct.declarations.filterIsInstance<KtProperty>()
    require(properties.map { it.name }.toSet() == setOf("hue", "chroma", "tone", "argb")) {
        "${file.name}:${hct.textOffset}: hct-mutation-surface: property inventory changed"
    }

    val noPublicSetters = properties.all { property ->
        property.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
            property.setter?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true
    }
    require(noPublicSetters) {
        "${file.name}:${hct.textOffset}: hct-mutation-surface: publicly writable property"
    }

    require(methods.single { it.name == "setInternalState" }.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
        "${file.name}:${hct.textOffset}: hct-mutation-surface: initialization became public"
    }

    require(file.nodes<KtCallExpression>().count { it.calleeExpression?.text == "setInternalState" } == 4) {
        "${file.name}:${hct.textOffset}: hct-mutation-surface: internal mutation structure changed"
    }

    for ((old, new) in listOf("setHue" to "withHue", "setChroma" to "withChroma", "setTone" to "withTone")) {
        val method = methods.single { it.name == old }
        val signatureUnchanged = method.valueParameters
            .singleOrNull()
            ?.typeReference
            ?.text == "Double" &&
            method.typeReference == null
        require(signatureUnchanged) {
            "${file.name}:${method.textOffset}: hct-copy-method: changed $old signature"
        }

        val statement = method.bodyBlockExpression?.statements?.singleOrNull() as? KtCallExpression
        require(statement?.calleeExpression?.text == "setInternalState") {
            "${file.name}:${method.textOffset}: hct-copy-method: changed $old body"
        }
        edits.replace(requireNotNull(method.nameIdentifier), new, Rule.HctCopyMethod)
        edits.insert(requireNotNull(method.valueParameterList).textRange.endOffset, ": Hct", Rule.HctCopyType)
        edits.replace(requireNotNull(statement.calleeExpression), "return Hct", Rule.HctCopyBody)
    }

    append(
        type = hct,
        source =
            """
            override fun equals(other: Any?): Boolean = other is Hct && argb == other.argb
            override fun hashCode(): Int = argb
            fun isBlue(): Boolean = isBlue(hue)
            fun isYellow(): Boolean = isYellow(hue)
            fun isCyan(): Boolean = isCyan(hue)
            fun withHue(newHue: Float): Hct = withHue(newHue.toDouble())
            fun withChroma(newChroma: Float): Hct = withChroma(newChroma.toDouble())
            fun withTone(newTone: Float): Hct = withTone(newTone.toDouble())
            """.trimIndent(),
        rule = Rule.HctValueContract,
        edits = edits,
    )
}

private fun tonalPaletteCache(
    file: KtFile,
    edits: SourceEdits,
) {
    val type = file.type("TonalPalette")
    val cache = type.declarations.filterIsInstance<KtProperty>().single { it.name == "cache" }
    require(!cache.hasModifier(KtTokens.PRIVATE_KEYWORD) && cache.isVar) {
        "${file.name}:${cache.textOffset}: palette-cache: cache shape changed"
    }

    edits.insert(cache.textRange.startOffset, "private ", Rule.HidePaletteCache)
    valueEquality(type, listOf("hue" to "Double", "chroma" to "Double", "keyColor" to "Hct"), edits)
}

private fun temperatureCache(
    file: KtFile,
    edits: SourceEdits,
) {
    val type = file.type("TemperatureCache")
    valueEquality(type, listOf("input" to "Hct"), edits)
    hideConstructorInputs(type, edits)

    // Keep Kotlin property ergonomics without colliding with the JVM getter's method name.
    val method = type.declarations
        .filterIsInstance<KtNamedFunction>()
        .single { it.name == "getAnalogousColors" && it.valueParameters.isEmpty() }

    val delegatingBody = method.bodyBlockExpression
        ?.statements
        ?.singleOrNull()
        ?.text
    require(delegatingBody == "return getAnalogousColors(5, 12)") {
        "${file.name}:${method.textOffset}: analogous-property: changed delegating body"
    }

    val trivia = method.text.substringBefore("fun getAnalogousColors")
    edits.replace(
        node = method,
        replacement = trivia + "val analogousColors: List<Hct>\n    get() = getAnalogousColors(5, 12)",
        rule = Rule.AnalogousProperty,
    )
}

private fun contrastCurve(
    file: KtFile,
    edits: SourceEdits,
) {
    val type = file.type("ContrastCurve")
    valueEquality(type, listOf("low", "normal", "medium", "high").map { it to "Double" }, edits)
    hideConstructorInputs(type, edits)
}

private fun contrastFloatOverloads(
    file: KtFile,
    edits: SourceEdits,
) {
    val type = file.type("Contrast")
    val additions = listOf("lighter", "lighterUnsafe", "darker", "darkerUnsafe").joinToString("\n") { name ->
        val original = type.declarations.filterIsInstance<KtNamedFunction>().single { it.name == name }
        require(original.valueParameters.map { it.typeReference?.text } == listOf("Double", "Double")) {
            "${file.name}:${original.textOffset}: contrast-float: signature changed"
        }

        val optional = if (name.endsWith("Unsafe")) "" else "?"

        "  fun $name(tone: Double, ratio: Float): Float$optional = " +
            "$name(tone, ratio.toDouble())$optional.toFloat()"
    }
    append(type, additions, Rule.ContrastFloat, edits)
}

private fun blendHarmonize(
    file: KtFile,
    edits: SourceEdits,
) {
    append(
        type = file.type("Blend"),
        source = "  fun harmonize(designColor: Hct, sourceColor: Hct): Hct = " +
            "Hct.fromInt(harmonize(designColor.toInt(), sourceColor.toInt()))",
        rule = Rule.HarmonizeHct,
        edits = edits,
    )
}

private fun colorUtilsLuminance(
    file: KtFile,
    edits: SourceEdits,
) {
    append(
        type = file.type("ColorUtils"),
        source = "  fun calculateLuminance(argb: Int): Double = xyzFromArgb(argb)[1] / 100.0",
        rule = Rule.LuminanceMember,
        edits = edits,
    )
}

private fun dynamicColorFactory(
    file: KtFile,
    edits: SourceEdits,
) {
    val type = file.type("DynamicColor")
    require(type.hasModifier(KtTokens.DATA_KEYWORD)) {
        "${file.name}:${type.textOffset}: dynamic-color-factory: upstream data contract changed"
    }

    val companion = (type as KtClass).companionObjects.single()
    require(companion.declarations.none { declaration -> declaration.name == "fromPalette" }) {
        "${file.name}:${companion.textOffset}: dynamic-color-factory: factory already exists"
    }

    val initialTone = companion.declarations
        .filterIsInstance<KtNamedFunction>()
        .single { func -> func.name == "getInitialToneFromBackground" }

    val background = initialTone.valueParameters.single()
    require(background.defaultValue == null) {
        "${file.name}:${background.textOffset}: initial-tone-default: default changed"
    }

    edits.insert(background.textRange.endOffset, " = null", Rule.InitialToneDefault)

    append(
        type = companion,
        source =
            """
            fun fromPalette(
              name: String,
              palette: (DynamicScheme) -> TonalPalette,
              tone: (DynamicScheme) -> Double,
              isBackground: Boolean = false,
            ): DynamicColor = DynamicColor(name = name, palette = palette, tone = tone, isBackground = isBackground)
            """.trimIndent(),
        rule = Rule.DynamicColorFactory,
        edits = edits,
    )
}

private fun colorSpecDefault(
    file: KtFile,
    edits: SourceEdits,
) {
    enumDefault(file, "SpecVersion", "SPEC_2021", edits)
}

private fun viewingConditionsVisibility(
    file: KtFile,
    edits: SourceEdits,
) {
    val type = file.type("ViewingConditions") as KtClass
    val inputs = type.primaryConstructorParameters.filter { it.name in setOf("ncb", "c", "nc", "fl", "z") }
    require(inputs.size == 5) { "${file.name}:0: viewing-visibility: input inventory changed" }

    inputs.forEach { parameter ->
        val keyword = requireNotNull(parameter.modifierList?.getModifier(KtTokens.INTERNAL_KEYWORD))
        edits.replace(keyword, "public", Rule.ViewingVisibility)
    }
}

private fun cam16Visibility(
    file: KtFile,
    edits: SourceEdits,
) {
    val methods =
        setOf(
            "viewed",
            "xyzInViewingConditions",
            "fromIntInViewingConditions",
            "fromXyzInViewingConditions",
            "fromJch",
        )

    for (name in methods) {
        val method = file.nodes<KtNamedFunction>().single { it.name == name }
        val keyword = requireNotNull(method.modifierList?.getModifier(KtTokens.INTERNAL_KEYWORD)) {
            "${file.name}:${method.textOffset}: cam16-visibility: $name is no longer internal"
        }
        edits.replace(keyword, "public", Rule.Cam16Visibility)
    }
}

private fun valueEquality(
    type: KtClassOrObject,
    inputs: List<Pair<String, String>>,
    edits: SourceEdits,
) {
    require(type is KtClass && type.primaryConstructorParameters.map { it.name to it.typeReference?.text } == inputs) {
        "${type.containingKtFile.name}:${type.textOffset}: value-equality: ${type.name} constructor inputs changed"
    }
    require(type.declarations.none { it.name in setOf("equals", "hashCode", "toString") }) {
        "${type.containingKtFile.name}:${type.textOffset}: value-equality: upstream equality already exists"
    }

    val comparison = inputs.joinToString(" && ") { (name, kind) ->
        if (kind == "Double") "$name.toBits() == other.$name.toBits()" else "$name == other.$name"
    }

    val fields = inputs.joinToString(", ") { (name, _) -> $$"$$name=$$$name" }
    val hash = inputs
        .map { (name, _) ->
            "$name.hashCode()"
        }.reduce { expression, next -> "(31 * $expression + $next)" }

    append(
        type = type,
        source = "  override fun equals(other: Any?): Boolean = other is ${type.name} && $comparison\n" +
            "  override fun hashCode(): Int = $hash\n" +
            "  override fun toString(): String = \"${type.name}($fields)\"",
        rule = Rule.ValueEquality,
        edits = edits,
    )
}

private fun scoreFallback(
    file: KtFile,
    edits: SourceEdits,
) {
    val functions = file
        .type("Score")
        .declarations
        .filterIsInstance<KtNamedFunction>()
        .filter { it.name == "score" }

    require(functions.map { it.valueParameters.size } == listOf(1, 2, 3, 4)) {
        "${file.name}:0: score-fallback: overload inventory changed"
    }

    // Keep the upstream overloads. Defaults on the full overload also enable named optional arguments.
    for (function in functions.filter { it.valueParameters.size >= 3 }) {
        val parameter = function.valueParameters.single { it.name == "fallbackColorArgb" }
        require(parameter.typeReference?.text == "Int") {
            "${file.name}:${parameter.textOffset}: score-fallback: fallback signature changed"
        }
        edits.replace(requireNotNull(parameter.typeReference), "Int?", Rule.ScoreNullableFallback)
    }

    val full = functions.last()
    for ((name, default) in mapOf("desired" to "4", "fallbackColorArgb" to "0xff4285f4.toInt()", "filter" to "true")) {
        val parameter = full.valueParameters.single { it.name == name }
        require(parameter.defaultValue == null) {
            "${file.name}:${parameter.textOffset}: score-default: upstream default changed"
        }
        edits.insert(parameter.textRange.endOffset, " = $default", Rule.ScoreDefault)
    }

    val fallback = full.nodes<KtCallExpression>().single { it.text == "add(fallbackColorArgb)" }
    val statement = requireNotNull(fallback.parent)
    require(statement.text == "colors.add(fallbackColorArgb)") {
        "${file.name}:${statement.textOffset}: score-optional-fallback: unexpected receiver ${statement.text}"
    }

    edits.replace(
        node = statement,
        replacement = "if (fallbackColorArgb != null) colors.add(fallbackColorArgb)",
        rule = Rule.ScoreOptionalFallback,
    )
}

private fun schemeConveniences(
    file: KtFile,
    edits: SourceEdits,
) {
    val type = file.type("DynamicScheme") as KtClass
    val expectedParameters = listOf(
        "sourceColorHctList",
        "variant",
        "isDark",
        "contrastLevel",
        "platform",
        "specVersion",
        "primaryPalette",
        "secondaryPalette",
        "tertiaryPalette",
        "neutralPalette",
        "neutralVariantPalette",
        "errorPalette",
    )
    require(type.primaryConstructorParameters.map { it.name } == expectedParameters) {
        "${file.name}:${type.textOffset}: scheme-copy: constructor changed"
    }

    val errors = (type.primaryConstructorParameters + type.secondaryConstructors.flatMap { it.valueParameters })
        .filter { it.name == "errorPalette" }
    require(errors.size == 2 && errors.all { it.defaultValue == null }) {
        "${file.name}:0: scheme-error-default: constructor changed"
    }

    errors.forEach {
        edits.insert(it.textRange.endOffset, " = TonalPalette.fromHueAndChroma(25.0, 84.0)", Rule.SchemeErrorDefault)
    }

    append(
        type = type,
        source =
            """
            fun copy(
              sourceColorHctList: List<Hct> = this.sourceColorHctList,
              variant: Variant = this.variant,
              isDark: Boolean = this.isDark,
              contrastLevel: Double = this.contrastLevel,
              platform: Platform = this.platform,
              specVersion: SpecVersion = this.specVersion,
              primaryPalette: TonalPalette = this.primaryPalette,
              secondaryPalette: TonalPalette = this.secondaryPalette,
              tertiaryPalette: TonalPalette = this.tertiaryPalette,
              neutralPalette: TonalPalette = this.neutralPalette,
              neutralVariantPalette: TonalPalette = this.neutralVariantPalette,
              errorPalette: TonalPalette = this.errorPalette,
            ): DynamicScheme = DynamicScheme(
              sourceColorHctList, variant, isDark, contrastLevel, platform, specVersion,
              primaryPalette, secondaryPalette, tertiaryPalette, neutralPalette, neutralVariantPalette, errorPalette,
            )

            val primaryDim: Int? get() = dynamicColors.primaryDim?.let { getArgb(it) }
            val secondaryDim: Int? get() = dynamicColors.secondaryDim?.let { getArgb(it) }
            val tertiaryDim: Int? get() = dynamicColors.tertiaryDim?.let { getArgb(it) }
            val errorDim: Int? get() = dynamicColors.errorDim?.let { getArgb(it) }
            """.trimIndent(),
        rule = Rule.SchemeCopy,
        edits = edits,
    )

    enumDefault(file, "Platform", "PHONE", edits)
}

private fun enumDefault(
    file: KtFile,
    name: String,
    value: String,
    edits: SourceEdits,
) {
    val type = file.nodes<KtClass>().single { it.name == name }
    require(type.isEnum() && type.companionObjects.isEmpty()) {
        "${file.name}:${type.textOffset}: enum-default: enum shape changed"
    }

    val entries = type.declarations.filterIsInstance<KtEnumEntry>()
    require(entries.any { it.name == value }) { "${file.name}:${type.textOffset}: enum-default: missing default" }

    // PSI includes each entry's comma in its own text, the enum separator is inserted after it.
    val offset = requireNotNull(type.body?.rBrace).textRange.startOffset
    edits.insert(offset, ";\n    companion object { val Default: $name = $value }\n  ", Rule.EnumDefault)
}

private fun KtFile.type(name: String): KtClassOrObject =
    declarations
        .filterIsInstance<KtClassOrObject>()
        .single { it.name == name }

private fun append(
    type: KtClassOrObject,
    source: String,
    rule: Rule,
    edits: SourceEdits,
) {
    edits.insert(requireNotNull(type.body?.rBrace).textRange.startOffset, "\n$source\n\n", rule)
}

private fun hideConstructorInputs(
    type: KtClassOrObject,
    edits: SourceEdits,
) {
    (type as KtClass).primaryConstructorParameters.forEach { param ->
        require(!param.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            "${type.containingKtFile.name}:${param.textOffset}: value-input-visibility: upstream already private"
        }
        edits.insert(param.textRange.startOffset, "private ", Rule.ValueInputVisibility)
    }
}
