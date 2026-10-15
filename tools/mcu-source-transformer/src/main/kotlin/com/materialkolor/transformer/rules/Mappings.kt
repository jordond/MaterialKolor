package com.materialkolor.transformer.rules

/**
 * The complete JVM-symbol translation table for library mode. It holds every Java name the passes
 * recognize, rewrite or remove. The edit logic consuming each entry stays in the rule files.
 */
internal object Mappings {
    const val COMPAT_PACKAGE = "com.materialkolor.compat"

    val collectionImports = mapOf(
        "java.util.ArrayList" to "kotlin.collections.ArrayList",
        "java.util.HashMap" to "kotlin.collections.HashMap",
        "java.util.LinkedHashMap" to "kotlin.collections.LinkedHashMap",
    )

    const val RANDOM_IMPORT = "java.util.Random"
    const val RANDOM_REPLACEMENT = "$COMPAT_PACKAGE.JavaRandom"

    val removedImports = setOf(
        "java.util.Arrays",
        "java.util.Collections",
        "java.util.Locale",
        "java.text.DecimalFormat",
    )

    val mathMethods = mapOf(
        "toRadians" to MathMethod(COMPAT_PACKAGE, Rule.MathToRadians),
        "toDegrees" to MathMethod(COMPAT_PACKAGE, Rule.MathToDegrees),
        "max" to MathMethod("kotlin.math", Rule.MathMax),
    )

    val jvmStaticAnnotations = setOf("JvmStatic", "kotlin.jvm.JvmStatic")

    const val SUPPRESSION_ANNOTATION = "SuppressWarnings"

    val allowedAnnotations = setOf("Suppress", "ConsistentCopyVisibility")

    val forbiddenReferences =
        setOf("java", "javax", "System", "Thread", "Class", "ClassLoader", "Runtime", "Math", "SuppressWarnings")

    val sourcePackages = setOf(
        "blend",
        "contrast",
        "dislike",
        "dynamiccolor",
        "hct",
        "palettes",
        "quantize",
        "scheme",
        "score",
        "temperature",
        "utils",
    )
}

internal data class MathMethod(
    val target: String,
    val rule: Rule,
)
