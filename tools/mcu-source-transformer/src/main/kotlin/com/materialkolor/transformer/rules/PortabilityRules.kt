package com.materialkolor.transformer.rules

import com.materialkolor.transformer.edits.SourceEdits
import com.materialkolor.transformer.psi.nodes
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/**
 * Supported operations only. The reviewed source/rule lock bounds the eligible production files.
 */
internal fun portability(
    file: KtFile,
    edits: SourceEdits,
): Set<String> {
    val name = file.name
    val aliases = mutableMapOf<String, String>()
    val removedNames = mutableSetOf<String>()
    val visibleImports = file.importDirectives.filterNot { it.isAllUnder }.map { import ->
        import.aliasName ?: requireNotNull(import.importedFqName).shortName().asString()
    }

    require(visibleImports.size == visibleImports.toSet().size) { "$name:0: symbol-binding: ambiguous import names" }

    for (directive in file.importDirectives) {
        val imported = requireNotNull(directive.importedFqName).asString()
        if ((imported == "kotlin.jvm" || imported.startsWith("kotlin.jvm."))) {
            require(imported == "kotlin.jvm.JvmStatic" && !directive.isAllUnder && directive.aliasName == null) {
                "$name:${directive.textOffset}: jvm-import: unsupported JVM import $imported"
            }
        }

        if (!imported.startsWith("java.") && !imported.startsWith("javax.")) continue
        require(!directive.isAllUnder) {
            "$name:${directive.textOffset}: jvm-import: wildcard JVM imports require review"
        }

        val simple = directive.aliasName ?: imported.substringAfterLast('.')
        require(aliases.put(simple, imported) == null) {
            "$name:${directive.textOffset}: jvm-import: ambiguous alias $simple"
        }

        when (imported) {
            in Mappings.collectionImports -> {
                edits.replace(
                    node = requireNotNull(directive.importedReference),
                    replacement = Mappings.collectionImports.getValue(imported),
                    rule = Rule.CollectionImport,
                )
            }
            Mappings.RANDOM_IMPORT -> {
                edits.replace(
                    node = requireNotNull(directive.importedReference),
                    replacement = Mappings.RANDOM_REPLACEMENT,
                    rule = Rule.RngImport,
                )

                if (directive.aliasName == null) {
                    edits.insert(directive.textRange.endOffset, " as Random", Rule.RngAlias)
                }
            }
            in Mappings.removedImports -> {
                edits.replace(directive, "", Rule.RemoveJvmImport)
                removedNames += simple
            }
            else -> {
                error("$name:${directive.textOffset}: jvm-import: unsupported JVM import $imported")
            }
        }
    }

    val suspicious = aliases.keys + setOf("Math", "String")
    val shadows = file.nodes<KtNamedDeclaration>().filter { it.name in suspicious }
    require(shadows.isEmpty()) {
        "$name:${shadows.firstOrNull()?.textOffset}: symbol-binding: potentially shadowed platform symbols " +
            "${shadows.map { it.name }} require semantic review"
    }

    var jvmStatic = false
    for (annotation in file.nodes<KtAnnotationEntry>()) {
        when (val type = annotation.typeReference?.text) {
            in Mappings.jvmStaticAnnotations -> {
                jvmStatic = true
            }
            Mappings.SUPPRESSION_ANNOTATION -> {
                edits.replace(requireNotNull(annotation.typeReference), "Suppress", Rule.Suppression)
            }
            in Mappings.allowedAnnotations -> {}
            else -> {
                error("$name:${annotation.textOffset}: annotation: unsupported annotation $type")
            }
        }
    }

    if (jvmStatic && file.importDirectives.none { it.importedFqName?.asString() == "kotlin.jvm.JvmStatic" }) {
        edits.insert(
            offset = requireNotNull(file.packageDirective).textRange.endOffset,
            text = "\n\nimport kotlin.jvm.JvmStatic",
            rule = Rule.JvmStaticImport,
        )
    }

    for (qualified in file.nodes<KtDotQualifiedExpression>()) {
        val call = qualified.selectorExpression as? KtCallExpression ?: continue
        val method = (call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: continue
        val receiver = qualified.receiverExpression
        val receiverName = receiver.qualifiedName()
        val target = aliases[receiverName] ?: when (receiverName) {
            "Math" -> "java.lang.Math"
            "String" -> "java.lang.String"
            else -> receiverName
        }

        val args = call.valueArguments.map { requireNotNull(it.getArgumentExpression()) }

        fun arity(expected: Int) =
            require(args.size == expected && call.lambdaArguments.isEmpty()) {
                "$name:${qualified.textOffset}: portability-call: unexpected $target.$method signature"
            }

        when (target) {
            "java.lang.Math" -> {
                arity(if (method == "max") 2 else 1)
                val mapped = Mappings.mathMethods[method]
                    ?: error("$name:${qualified.textOffset}: math: unsupported Math.$method")

                edits.replace(receiver, mapped.target, mapped.rule)
            }
            "java.util.Arrays" -> {
                require(method == "sort") { "$name:${qualified.textOffset}: array-sort: unsupported Arrays.$method" }
                arity(1)
                edits.replace(qualified, "${requireNotNull(call.valueArgumentList).text}.sort()", Rule.ArraySort)
            }
            "java.util.Collections" -> {
                when (method) {
                    "sort" -> {
                        arity(2)
                        edits.replace(qualified, "(${args[0].text}).sortWith(${args[1].text})", Rule.ListSort)
                    }
                    "unmodifiableList" -> {
                        arity(1)
                        edits.replace(qualified, "(${args[0].text}).toList()", Rule.PrivateListCopy)
                    }
                    else -> {
                        error("$name:${qualified.textOffset}: collections: unsupported Collections.$method")
                    }
                }
            }
            "java.lang.String" -> {
                require(method == "format") {
                    "$name:${qualified.textOffset}: string-format: unsupported String.$method"
                }
                arity(4)
                require(args[0].text == "\"#%02x%02x%02x\"") {
                    "$name:${qualified.textOffset}: rgb-format: unknown String.format pattern"
                }

                val list = args.drop(1).joinToString { it.text }
                edits.replace(
                    node = qualified,
                    replacement = "\"#\" + listOf($list).joinToString(\"\") { it.toString(16).padStart(2, '0') }",
                    rule = Rule.RgbFormat,
                )
            }
        }

        if (method == "lowercase" && args.size == 1) {
            val argument = args[0] as? KtDotQualifiedExpression
            if (aliases[argument?.receiverExpression?.qualifiedName()] == "java.util.Locale") {
                require(argument?.selectorExpression?.text == "ENGLISH") {
                    "$name:${qualified.textOffset}: enum-lowercase: unsupported locale"
                }

                edits.replace(requireNotNull(call.valueArgumentList), "()", Rule.EnumLowercase)
            }
        }

        val constructor = receiver as? KtCallExpression
        val constructorName = (constructor?.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
        if (aliases[constructorName] == "java.text.DecimalFormat") {
            val pattern = constructor
                ?.valueArguments
                ?.singleOrNull()
                ?.getArgumentExpression()
                ?.text
            require(method == "format" && pattern == "\"0.0\"") {
                "$name:${qualified.textOffset}: diagnostic-format: unexpected DecimalFormat usage"
            }

            arity(1)
            edits.replace(
                node = qualified,
                replacement = "${Mappings.COMPAT_PACKAGE}.formatContrast(${args[0].text})",
                rule = Rule.DiagnosticFormatPolicy,
            )
        }
    }

    return removedNames
}

/**
 * Removed imports are only safe when every use was consumed by an audited operation.
 *
 * Checking the transformed tree also catches qualified references that bypass imports.
 */
internal fun rejectJvmReferences(
    file: KtFile,
    removedNames: Set<String>,
) {
    val forbidden = Mappings.forbiddenReferences + removedNames
    for (reference in file.nodes<KtNameReferenceExpression>()) {
        require(reference.getReferencedName() !in forbidden) {
            "${file.name}:${reference.textOffset}: jvm-reference: unsupported JVM reference ${reference.text}"
        }
    }
}
