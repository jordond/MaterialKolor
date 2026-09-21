package com.materialkolor.transformer

import com.materialkolor.transformer.edits.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdapterTest {
    private fun adapt(source: String): Result = Adapter().use { it.transform("Example.kt", source) }

    @Test fun commentsAndStringsAreNotCode() {
        val source =
            """
            // Math.toRadians(x) and import java.util.Random must remain here.
            package utils
            import java.util.Random
            val note = "Math.toRadians(x)"
            fun angle(x: Double) = Math /* keep */ . toRadians ( x )
            fun random() = Random(42L).nextInt(10)
            """.trimIndent()
        val result = adapt(source).text
        assertTrue(result.contains("// Math.toRadians(x) and import java.util.Random must remain here."))
        assertTrue(result.contains("val note = \"Math.toRadians(x)\""))
        assertTrue(result.contains("com.materialkolor.compat /* keep */ . toRadians ( x )"))
        assertTrue(result.contains("import com.materialkolor.compat.JavaRandom as Random"))
    }

    @Test fun aliasedImportsAreMappedWithoutRenamingTheirUses() {
        val source =
            """
            package utils
            import java.util.Random as Seeded
            import java.util.Arrays as ArrayTools
            import hct.Hct as Color
            fun pick() = Seeded(42L).nextInt(10)
            fun sort(a: Array<Int>) = ArrayTools.sort(a)
            """.trimIndent()
        val result = adapt(source).text
        assertTrue(result.contains("import com.materialkolor.compat.JavaRandom as Seeded"))
        assertTrue(result.contains("import com.materialkolor.hct.Hct as Color"))
        assertTrue(result.contains("Seeded(42L)"))
        assertTrue(result.contains("(a).sort()"))
    }

    @Test fun rejectsShadowedPlatformSymbols() {
        val error = assertFailsWith<IllegalArgumentException> {
            adapt("package utils\nfun angle(Math: UserMath) = Math.toRadians(1.0)")
        }
        assertTrue(error.message.orEmpty().contains("shadowed"))
    }

    @Test fun rejectsUnknownJvmDependency() {
        assertFailsWith<IllegalStateException> {
            adapt(
                "package utils\nimport java.time.Instant\nval x = Instant.now()",
            )
        }
    }

    @Test fun rejectsWildcardJvmImports() {
        assertFailsWith<IllegalArgumentException> { adapt("package utils\nimport java.util.*\nval x = 1") }
    }

    @Test fun rejectsUnknownMathOperation() {
        assertFailsWith<IllegalStateException> { adapt("package utils\nval x = Math.cbrt(1.0)") }
    }

    @Test fun rejectsUnknownFormattingPattern() {
        assertFailsWith<IllegalArgumentException> { adapt("package utils\nfun f() = String.format(\"%d\", 1, 2, 3)") }
    }

    @Test fun rejectsInvalidSourceBeforeEditing() {
        assertFailsWith<IllegalArgumentException> { adapt("package utils\nfun f( {") }
    }

    @Test fun refusesToSilentlyDiscardCommentsInsideReshapedCalls() {
        val error = assertFailsWith<IllegalArgumentException> {
            adapt(
                "package utils\nimport java.util.Collections\n" +
                    "fun f(a: MutableList<Int>, c: Comparator<Int>) = Collections.sort(/*keep*/ a, c)",
            )
        }
        assertTrue(error.message.orEmpty().contains("comments"))
    }

    @Test fun preservesCommentsWithinArrayArguments() {
        val result = adapt(
            "package utils\nimport java.util.Arrays\nfun f(a: Array<Int>) = Arrays.sort(/*keep*/ a)",
        ).text
        assertTrue(result.contains("(/*keep*/ a).sort()"))
    }

    @Test fun nestedCallsAreChangedWithNonoverlappingEdits() {
        val result = adapt("package utils\nfun f(x: Double) = Math.toRadians(Math.toDegrees(x))").text
        assertTrue(result.contains("com.materialkolor.compat.toRadians(com.materialkolor.compat.toDegrees(x))"))
    }

    @Test fun fullyQualifiedMathCallWorks() {
        val result = adapt("package utils\nfun f(x: Double) = java.lang.Math.toRadians(x)").text
        assertTrue(result.contains("com.materialkolor.compat.toRadians(x)"))
    }

    @Test fun generationIsDeterministic() {
        val source = "package utils\nfun f(x: Double) = Math.toRadians(x)"
        Adapter().use { adapter ->
            val first = adapter.transform("Example.kt", source)
            val second = adapter.transform("Example.kt", source)
            assertEquals(first, second)
        }
    }
}
