package com.materialkolor.builder.kit.control

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher

/** Matches a node that plays [role]. */
internal fun hasRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

/** One ink on one ground a control draws, and the least contrast the pair may have. */
internal class InkPair(
    val name: String,
    val ink: Color,
    val ground: Color,
    val minimum: Double,
)

/** The WCAG contrast ratio of two opaque colours. */
internal fun contrast(
    a: Color,
    b: Color,
): Double {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05) / (darker + 0.05)
}

/** Every pair that falls short of its minimum, one line each, headed by the [mode] it was seen in. */
internal fun Iterable<InkPair>.shortfalls(mode: String): List<String> =
    mapNotNull { pair ->
        val ratio = contrast(pair.ink, pair.ground)
        if (ratio < pair.minimum) "$mode ${pair.name} ${"%.2f".format(ratio)} < ${pair.minimum}" else null
    }
