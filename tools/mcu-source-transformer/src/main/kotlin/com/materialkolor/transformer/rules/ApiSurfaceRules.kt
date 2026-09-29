package com.materialkolor.transformer.rules

import com.materialkolor.transformer.edits.SourceEdits
import com.materialkolor.transformer.psi.nodes
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

private const val OPT_IN_MARKER = "com.materialkolor.InternalMaterialKolorApi"
private const val POKO = "dev.drewhamilton.poko.Poko"

/**
 * Implementation types the library hides without touching their declarations otherwise.
 */
private val internalTypes = mapOf(
    "utils/MathUtils.kt" to "MathUtils",
    "dynamiccolor/ColorSpecs.kt" to "ColorSpecs",
    "dynamiccolor/ColorSpec2021.kt" to "ColorSpec2021",
    "dynamiccolor/ColorSpec2025.kt" to "ColorSpec2025",
    "dynamiccolor/ColorSpec2026.kt" to "ColorSpec2026",
    "hct/HctSolver.kt" to "HctSolver",
    "quantize/Quantizer.kt" to "Quantizer",
    "quantize/QuantizerMap.kt" to "QuantizerMap",
    "quantize/QuantizerResult.kt" to "QuantizerResult",
    "quantize/QuantizerWu.kt" to "QuantizerWu",
    "quantize/QuantizerWsmeans.kt" to "QuantizerWsmeans",
    "quantize/PointProvider.kt" to "PointProvider",
    "quantize/PointProviderLab.kt" to "PointProviderLab",
)

/**
 * Members of public types that the library hides, by file, then by owner. A companion owner is written `Type.Companion`.
 */
private val internalMembers = mapOf(
    "utils/ColorUtils.kt" to
        ("ColorUtils" to setOf("argbFromLinrgb", "linearized", "delinearized", "whitePointD65", "labF", "labInvf")),
    "dynamiccolor/DynamicScheme.kt" to
        (
            "DynamicScheme.Companion" to
                setOf("DEFAULT_SPEC_VERSION", "DEFAULT_PLATFORM", "getPiecewiseValue", "getRotatedHue")
        ),
    "dynamiccolor/DynamicColor.kt" to
        (
            "DynamicColor.Companion" to
                setOf("foregroundTone", "enableLightForeground", "toneAllowsLightForeground")
        ),
    "hct/ViewingConditions.kt" to ("ViewingConditions" to setOf("rgbD")),
)

/**
 * The only public data classes the library accepts, each shipped as a Poko class. Poko keeps the data class
 * equality, hash code and diagnostics without the `copy` and `componentN` functions that break binary
 * compatibility whenever upstream adds a property.
 */
private val pokoClasses = mapOf(
    "dynamiccolor/DynamicColor.kt" to "DynamicColor",
    "dynamiccolor/ToneDeltaPair.kt" to "ToneDeltaPair",
    "palettes/CorePalettes.kt" to "CorePalettes",
    "hct/Cam16.kt" to "Cam16",
    "hct/ViewingConditions.kt" to "ViewingConditions",
)

/**
 * Poko classes whose upstream code still calls `copy`. They keep it as an internal member.
 */
private val internalCopies = setOf("DynamicColor")

/**
 * Array properties whose content, not identity, defines equality.
 */
private val arrayContent = mapOf("ViewingConditions" to "rgbD")

/**
 * Public types that only MaterialKolor may subclass without opting in.
 */
private val subclassOptInTypes = mapOf(
    "dynamiccolor/ColorSpec.kt" to "ColorSpec",
    "dynamiccolor/DynamicScheme.kt" to "DynamicScheme",
)

/**
 * Narrows the public ABI. Runs before the implementation-visibility pass so annotations land ahead of modifiers.
 */
internal fun apiSurface(
    file: KtFile,
    edits: SourceEdits,
) {
    val path = file.name

    rejectUnreviewedDataClasses(file)
    pokoClasses[path]?.let { name -> pokoClass(file, name, edits) }
    subclassOptInTypes[path]?.let { name -> requireSubclassOptIn(file, name, edits) }
    optInSubclasses(file, edits)
    internalMembers[path]?.let { (owner, names) -> hideMembers(file, owner, names, edits) }

    internalTypes[path]?.let { name ->
        val type = file.type(name)
        if (!type.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
            edits.insert(modifierOffset(type), "internal ", Rule.ImplementationVisibility)
        }
    }
}

/**
 * Fails when upstream adds a public data class, so each one is a deliberate Poko or internal decision.
 */
private fun rejectUnreviewedDataClasses(file: KtFile) {
    val hiddenType = internalTypes[file.name]
    for (type in file.nodes<KtClass>().filter { it.isData() }) {
        if (type.isTopLevel() && type.name == pokoClasses[file.name]) continue

        val owners = generateSequence<KtClassOrObject>(type) { owner -> owner.getStrictParentOfType<KtClassOrObject>() }
        val hidden = owners.any { owner ->
            owner.hasModifier(KtTokens.INTERNAL_KEYWORD) ||
                owner.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
                (owner.isTopLevel() && owner.name == hiddenType)
        }
        require(hidden) {
            "${file.name}:${type.textOffset}: poko-class: public data class ${type.name} needs a Poko or internal decision"
        }
    }
}

private fun pokoClass(
    file: KtFile,
    name: String,
    edits: SourceEdits,
) {
    val type = file.type(name) as KtClass
    val modifiers = requireNotNull(type.modifierList) {
        "${file.name}:${type.textOffset}: poko-class: $name is no longer a data class"
    }

    val annotations = modifiers.annotationEntries.map { entry -> entry.shortName?.asString() }
    val keywords = KtTokens.MODIFIER_KEYWORDS_ARRAY.filter { token -> modifiers.hasModifier(token) }
    val reviewedShape = type.isData() &&
        keywords == listOf<KtModifierKeywordToken>(KtTokens.DATA_KEYWORD) &&
        annotations.all { annotation -> annotation == "ConsistentCopyVisibility" }
    require(reviewedShape) {
        "${file.name}:${type.textOffset}: poko-class: $name modifiers changed to ${modifiers.text}"
    }

    val keyword = requireNotNull(type.getClassOrInterfaceKeyword())
    edits.replace(modifiers.textRange.startOffset, keyword.textRange.startOffset, "@Poko\n", Rule.PokoClass)
    addImport(file, POKO, Rule.PokoClass, edits)

    arrayContent[name]?.let { property ->
        val parameter = type.primaryConstructorParameters.single { parameter -> parameter.name == property }
        val plainArray = parameter.typeReference?.text == "DoubleArray" && parameter.annotationEntries.isEmpty()
        require(plainArray) {
            "${file.name}:${parameter.textOffset}: poko-class: $name.$property shape changed"
        }
        edits.insert(modifierOffset(parameter), "@Poko.ReadArrayContent ", Rule.PokoClass)
    }

    if (name in internalCopies) {
        internalCopy(type, edits)
    }
}

/**
 * Mirrors the data class `copy` the upstream sources call, kept out of the public ABI.
 */
private fun internalCopy(
    type: KtClass,
    edits: SourceEdits,
) {
    val parameters = type.primaryConstructorParameters
    require(parameters.all { parameter -> parameter.hasValOrVar() && parameter.typeReference != null }) {
        "${type.containingKtFile.name}:${type.textOffset}: poko-class: ${type.name} constructor shape changed"
    }
    require(type.declarations.none { declaration -> declaration.name == "copy" }) {
        "${type.containingKtFile.name}:${type.textOffset}: poko-class: ${type.name} already declares copy"
    }

    val inputs = parameters.joinToString("") { parameter ->
        "    ${parameter.name}: ${parameter.typeReference?.text} = this.${parameter.name},\n"
    }
    val arguments = parameters.joinToString("") { parameter -> "    ${parameter.name} = ${parameter.name},\n" }

    append(
        type = type,
        source = "  internal fun copy(\n$inputs  ): ${type.name} = ${type.name}(\n$arguments  )",
        rule = Rule.PokoClass,
        edits = edits,
    )
}

private fun requireSubclassOptIn(
    file: KtFile,
    name: String,
    edits: SourceEdits,
) {
    val type = file.type(name)
    require(type.annotationEntries.isEmpty()) {
        "${file.name}:${type.textOffset}: subclass-opt-in-required: $name annotations changed"
    }

    edits.insert(
        offset = annotationOffset(type),
        text = "@SubclassOptInRequired(InternalMaterialKolorApi::class)\n",
        rule = Rule.SubclassOptInRequired,
    )
    addImport(file, OPT_IN_MARKER, Rule.SubclassOptInRequired, edits)
}

/**
 * Every in-module subclass or implementer of an opt-in type opts in on its own declaration.
 */
private fun optInSubclasses(
    file: KtFile,
    edits: SourceEdits,
) {
    val supertypes = subclassOptInTypes.values.toSet()
    val subclasses = file.nodes<KtClassOrObject>().filter { type ->
        type.superTypeListEntries.any { entry ->
            entry.typeReference
                ?.text
                ?.substringBefore('<')
                ?.substringAfterLast('.') in supertypes
        }
    }

    for (type in subclasses) {
        val supported = type is KtClass && type.isTopLevel() && type.annotationEntries.isEmpty()
        require(supported) {
            "${file.name}:${type.textOffset}: subclass-opt-in: unsupported subclass shape ${type.name}"
        }
        edits.insert(annotationOffset(type), "@OptIn(InternalMaterialKolorApi::class)\n", Rule.SubclassOptIn)
    }

    if (subclasses.isNotEmpty()) {
        addImport(file, OPT_IN_MARKER, Rule.SubclassOptIn, edits)
    }
}

private fun hideMembers(
    file: KtFile,
    ownerPath: String,
    names: Set<String>,
    edits: SourceEdits,
) {
    val type = file.type(ownerPath.substringBefore('.'))
    val owner: KtClassOrObject = if (ownerPath.endsWith(".Companion")) {
        (type as KtClass).companionObjects.single()
    } else {
        type
    }

    val constructorProperties = (owner as? KtClass)
        ?.primaryConstructorParameters
        .orEmpty()
        .filter { parameter -> parameter.hasValOrVar() }

    for (name in names) {
        val matches = (owner.declarations + constructorProperties).filter { declaration -> declaration.name == name }
        val member = requireNotNull(matches.singleOrNull()) {
            "${file.name}:${owner.textOffset}: member-visibility: expected one $ownerPath.$name, found ${matches.size}"
        }

        val visibility = KtTokens.VISIBILITY_MODIFIERS.types.filter { token ->
            member.hasModifier(token as KtModifierKeywordToken)
        }
        require(visibility.isEmpty()) {
            "${file.name}:${member.textOffset}: member-visibility: $ownerPath.$name is no longer public"
        }

        // A static bridge of an internal member would still be public on the JVM, under a mangled name.
        val modifiers = member.modifierList
        val jvmStatic = modifiers?.annotationEntries.orEmpty().any { entry ->
            entry.shortName?.asString() == "JvmStatic"
        }
        if (jvmStatic) {
            val onlyJvmStatic = modifiers?.annotationEntries?.size == 1 &&
                KtTokens.MODIFIER_KEYWORDS_ARRAY.none { token -> modifiers.hasModifier(token) }
            require(onlyJvmStatic) {
                "${file.name}:${member.textOffset}: member-visibility: $ownerPath.$name modifiers changed"
            }
            edits.replace(modifiers.textRange.startOffset, keywordOffset(member), "internal ", Rule.MemberVisibility)
        } else {
            edits.insert(keywordOffset(member), "internal ", Rule.MemberVisibility)
        }
    }
}

/**
 * Where an annotation goes, after any KDoc and before every modifier.
 */
private fun annotationOffset(type: KtClassOrObject): Int =
    type.modifierList?.textRange?.startOffset ?: requireNotNull(type.getDeclarationKeyword()).textRange.startOffset

/**
 * Where a visibility modifier goes, after annotations and before the other modifiers.
 */
private fun modifierOffset(declaration: KtDeclaration): Int {
    val firstKeyword = declaration.modifierList
        ?.node
        ?.getChildren(null)
        ?.firstOrNull { child -> child.elementType is KtModifierKeywordToken }
    return firstKeyword?.startOffset ?: keywordOffset(declaration)
}

private fun keywordOffset(declaration: KtDeclaration): Int {
    val keyword = when (declaration) {
        is KtNamedFunction -> declaration.funKeyword
        is KtProperty -> declaration.valOrVarKeyword
        is KtParameter -> declaration.valOrVarKeyword
        is KtClassOrObject -> declaration.getDeclarationKeyword()
        else -> null
    }
    return requireNotNull(keyword) {
        "${declaration.containingKtFile.name}:${declaration.textOffset}: member-visibility: unsupported declaration"
    }.textRange.startOffset
}

private fun addImport(
    file: KtFile,
    fqName: String,
    rule: Rule,
    edits: SourceEdits,
) {
    val last = file.importDirectives.lastOrNull()
    if (last != null) {
        edits.insert(last.textRange.endOffset, "\nimport $fqName", rule)
    } else {
        edits.insert(requireNotNull(file.packageDirective).textRange.endOffset, "\n\nimport $fqName", rule)
    }
}
