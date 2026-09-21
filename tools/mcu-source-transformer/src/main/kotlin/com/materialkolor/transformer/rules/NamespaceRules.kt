package com.materialkolor.transformer.rules

import com.materialkolor.transformer.Mode
import com.materialkolor.transformer.edits.SourceEdits
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

internal fun relocate(
    file: KtFile,
    edits: SourceEdits,
    mode: Mode,
) {
    val packageName = file.packageFqName.asString()
    require(packageName in Mappings.sourcePackages) {
        "${file.name}:0: namespace: unexpected source package $packageName"
    }

    edits.replace(
        node = requireNotNull(file.packageDirective?.packageNameExpression),
        replacement = "${mode.namespace}.$packageName",
        rule = Rule.Package,
    )

    file.importDirectives.forEach { directive ->
        val imported = requireNotNull(directive.importedFqName).asString()
        if (imported.substringBefore('.') in Mappings.sourcePackages) {
            edits.replace(
                node = requireNotNull(directive.importedReference),
                replacement = "${mode.namespace}.$imported",
                rule = Rule.ImportPackage,
            )
        }
    }
}

internal fun KtExpression?.qualifiedName(): String? =
    when (this) {
        is KtNameReferenceExpression -> {
            getReferencedName()
        }
        is KtDotQualifiedExpression -> {
            val left = receiverExpression.qualifiedName()
            val right = selectorExpression.qualifiedName()

            if (left != null && right != null) "$left.$right" else null
        }
        else -> {
            null
        }
    }
