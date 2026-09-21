package com.materialkolor.transformer.edits

import com.materialkolor.transformer.rules.Rule
import org.jetbrains.kotlin.com.intellij.psi.PsiElement

/**
 * Offsets always refer to the original source.
 *
 * No formatter or PSI mutation is involved, and the Adapter has already normalized line endings to
 * LF by the time edits are collected here.
 */
internal class SourceEdits(
    private val name: String,
    private val source: String,
) {
    private val edits = mutableListOf<Edit>()

    fun replace(
        node: PsiElement,
        replacement: String,
        rule: Rule,
    ) = add(node.textRange.startOffset, node.textRange.endOffset, replacement, rule)

    fun insert(
        offset: Int,
        text: String,
        rule: Rule,
    ) = add(offset, offset, text, rule)

    private fun add(
        start: Int,
        end: Int,
        text: String,
        rule: Rule,
    ) {
        edits += Edit(start, end, text, rule)
    }

    fun apply(): Result {
        val ordered = edits.sortedWith(compareBy<Edit> { it.start }.thenBy { it.end })
        ordered.zipWithNext().forEach { (first, second) ->
            require(first.end <= second.start) {
                "$name:${second.start}: source-ranges: overlapping rules ${first.rule}/${second.rule}"
            }
        }

        val output = StringBuilder(source)
        ordered.asReversed().forEach { output.replace(it.start, it.end, it.replacement) }

        return Result(output.toString(), ordered)
    }
}
