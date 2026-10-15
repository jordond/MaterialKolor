package com.materialkolor.transformer

import com.materialkolor.transformer.edits.Result
import com.materialkolor.transformer.lock.SourceLock
import com.materialkolor.transformer.psi.comments
import com.materialkolor.transformer.rules.Rule
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DriftGuardsTest {
    private fun adapt(source: String): Result = Adapter().use { it.transform("utils/Fixture.kt", source) }

    @Test
    fun unknownQualifiedJvmReferencesFailWithoutImports() {
        for (expression in listOf(
            "java.time.Instant.now()",
            "java.lang.System.nanoTime()",
            "javax.crypto.Cipher.getInstance(\"AES\")",
        )) {
            val error = assertFailsWith<IllegalArgumentException> { adapt("package utils\nval value = $expression") }
            assertTrue(error.message.orEmpty().contains("jvm-reference"))
        }
    }

    @Test
    fun unsupportedJvmAnnotationsFail() {
        for (annotation in listOf("JvmField", "JvmOverloads", "java.lang.Deprecated", "UnknownAnnotation")) {
            assertFailsWith<IllegalStateException> { adapt("package utils\n@$annotation val value = 1") }
        }
        assertFailsWith<IllegalArgumentException> { adapt("package utils\nimport kotlin.jvm.*\nval value = 1") }
    }

    @Test
    fun removedImportsCannotLeaveUnsupportedUses() {
        val error = assertFailsWith<IllegalArgumentException> {
            adapt("package utils\nimport java.text.DecimalFormat as Formatter\nval value = Formatter(\"0.0\")")
        }
        assertTrue(error.message.orEmpty().contains("jvm-reference"))
    }

    @Test
    fun diagnosticFormattingUsesExplicitCommonPolicy() {
        val result =
            adapt(
                "package utils\nimport java.text.DecimalFormat\n" +
                    "fun value(x: Double) = DecimalFormat(\"0.0\").format(x)",
            )
        assertTrue(result.text.contains("com.materialkolor.compat.formatContrast(x)"))
        assertFalse(result.text.contains("toString()"))
    }

    @Test
    fun crlfUntouchedTextAndCommentBytesSurvive() {
        val source = "// comment\r\npackage utils\r\nfun value(x: Double) = Math.toDegrees(x)\r\n"
        val result = adapt(source)
        assertEquals(
            "// comment\r\npackage com.materialkolor.utils\r\n" +
                "fun value(x: Double) = com.materialkolor.compat.toDegrees(x)\r\n",
            result.text,
        )
        assertFalse(result.text.replace("\r\n", "").contains('\n'))
        val replayed = StringBuilder(source)
        result.edits.asReversed().forEach { replayed.replace(it.start, it.end, it.replacement) }
        assertEquals(result.text, replayed.toString())
    }

    @Test
    fun mixedLineEndingsAreRejectedRatherThanReformatted() {
        assertFailsWith<IllegalArgumentException> { adapt("package utils\r\nval value = 1\n") }
    }

    @Test
    fun overlappingNestedRewritesFailWithRuleLocations() {
        val error = assertFailsWith<IllegalArgumentException> {
            adapt("package utils\nimport java.util.Arrays\nfun f() = Arrays.sort(arrayOf(Math.toDegrees(1.0)))")
        }
        assertTrue(error.message.orEmpty().contains("overlapping rules array-sort/math-toDegrees"))
    }

    @Test
    fun referenceModePerformsOnlyNamespaceRelocation() {
        val source =
            """
            // import java.util.Random must remain here.
            package hct
            import utils.ColorUtils
            import java.util.Random
            class Hct { var hue = 0.0; fun setHue(value: Double) { hue = value } }
            """.trimIndent()
        val result = Adapter().use { it.transform("hct/Hct.kt", source, niceties = true, mode = Mode.Reference) }
        assertEquals(
            source
                .replace(
                    "package hct",
                    "package upstream.kotlin.hct",
                ).replace("import utils.ColorUtils", "import upstream.kotlin.utils.ColorUtils"),
            result.text,
        )
        assertEquals(setOf(Rule.Package, Rule.ImportPackage), result.edits.map { it.rule }.toSet())
    }

    @Test
    fun hctRefusesAnyNewPublicMethod() {
        val source = upstreamHct().replace("  fun toInt()", "  fun reset() { setInternalState(0) }\n\n  fun toInt()")
        val error = assertFailsWith<IllegalArgumentException> { hct(source) }
        assertTrue(error.message.orEmpty().contains("hct-mutation-surface"))
    }

    @Test
    fun hctRefusesNewPropertiesOrPublicSetters() {
        val source = upstreamHct()
        for (changed in listOf(
            source.replace("  var hue", "  var mutable = 1\n  var hue"),
            source.replace("    private set", ""),
        )) {
            val error = assertFailsWith<IllegalArgumentException> { hct(changed) }
            assertTrue(error.message.orEmpty().contains("hct-mutation-surface"))
        }
    }

    @Test
    fun hctRefusesChangedSetterBodyAndParameter() {
        val source = upstreamHct()
        for (changed in listOf(
            source.replace("setHue(newHue: Double)", "setHue(newHue: Float)"),
            source.replace("setInternalState(HctSolver.solveToInt(newHue, chroma, tone))", "println(newHue)"),
        )) {
            assertFailsWith<IllegalArgumentException> { hct(changed) }
        }
    }

    @Test
    fun completePinnedTreeHasPureReferenceAndDeterministicLibraryTransformations() {
        val root = File(requiredProperty("mcu.projectRoot"))
        val lock = SourceLock.read(File(root, "gradle/mcu-upstream.lock.json"))
        val inputs = lock.validate(File(root, "tools/mcu-upstream/src/main/kotlin"))
        Adapter().use { adapter ->
            for ((name, source) in inputs) {
                val first = adapter.transform(name, source, niceties = true)
                assertEquals(first, adapter.transform(name, source, niceties = true), name)
                val reference = adapter.transform(name, source, niceties = true, mode = Mode.Reference)
                assertTrue(reference.edits.all { it.rule == Rule.Package || it.rule == Rule.ImportPackage }, name)
                assertEquals(comments(source), comments(first.text), name)
            }
        }
    }

    @Test
    fun valueDiagnosticsIncludeOnlyConstructorInputs() {
        val root = File(requiredProperty("mcu.projectRoot"), "tools/mcu-upstream/src/main/kotlin")
        val expected = mapOf(
            "palettes/TonalPalette.kt" to "TonalPalette(hue=\$hue, chroma=\$chroma, keyColor=\$keyColor)",
            "temperature/TemperatureCache.kt" to "TemperatureCache(input=\$input)",
            "dynamiccolor/ContrastCurve.kt" to
                "ContrastCurve(low=\$low, normal=\$normal, medium=\$medium, high=\$high)",
        )
        Adapter().use { adapter ->
            for ((path, diagnostic) in expected) {
                val transformed = adapter.transform(path, File(root, path).readText(), niceties = true)
                assertTrue(transformed.text.contains("override fun toString(): String = \"$diagnostic\""), path)
            }
        }
    }

    private fun hct(source: String): Result = Adapter().use { it.transform("hct/Hct.kt", source, niceties = true) }

    private fun upstreamHct(): String =
        File(requiredProperty("mcu.projectRoot"), "tools/mcu-upstream/src/main/kotlin/hct/Hct.kt").readText()
}
