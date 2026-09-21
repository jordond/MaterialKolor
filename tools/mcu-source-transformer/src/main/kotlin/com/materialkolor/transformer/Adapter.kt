package com.materialkolor.transformer

import com.materialkolor.transformer.edits.Result
import com.materialkolor.transformer.edits.SourceEdits
import com.materialkolor.transformer.psi.PsiSession
import com.materialkolor.transformer.psi.comments
import com.materialkolor.transformer.rules.portability
import com.materialkolor.transformer.rules.rejectJvmReferences
import com.materialkolor.transformer.rules.relocate
import com.materialkolor.transformer.rules.semantics

/**
 * Adapts audited syntax.
 *
 * Semantics are still verified by compilation and the independent parity suites.
 */
internal class Adapter : AutoCloseable {
    private val parser = PsiSession()

    fun transform(
        name: String,
        source: String,
        niceties: Boolean = false,
        mode: Mode = Mode.Library,
    ): Result {
        // IntelliJ's parser uses normalized line separators. Convert its offsets back to the original
        // source by doing all syntax work on LF, then restore CRLF before calculating actual edits.
        if ("\r" in source) {
            val stripped = source.replace("\r\n", "")
            require(!stripped.contains('\r')) { "$name:0: line-endings: unsupported lone CR" }
            require(!stripped.contains('\n')) { "$name:0: line-endings: mixed LF/CRLF" }
            val normalized = source.replace("\r\n", "\n")
            val adapted = transform(name, normalized, niceties, mode)

            fun originalOffset(offset: Int): Int = offset + normalized.take(offset).count { it == '\n' }

            return Result(
                adapted.text.replace("\n", "\r\n"),
                adapted.edits.map { edit ->
                    edit.copy(
                        start = originalOffset(edit.start),
                        end = originalOffset(edit.end),
                        replacement = edit.replacement.replace("\n", "\r\n"),
                    )
                },
            )
        }

        val file = parser.parse(name, source)
        val edits = SourceEdits(name, source)
        relocate(file, edits, mode)

        val removedNames = if (mode != Mode.Library) {
            emptySet()
        } else {
            val removed = portability(file, edits)
            if (niceties) semantics(file, edits)
            removed
        }

        val result = edits.apply()
        val output = parser.parse(name, result.text)
        require(comments(source) == comments(result.text)) {
            "$name:0: comments: rewrite would alter comments; review required"
        }

        if (mode == Mode.Library) {
            rejectJvmReferences(output, removedNames)
        }

        return result
    }

    override fun close() = parser.close()
}
