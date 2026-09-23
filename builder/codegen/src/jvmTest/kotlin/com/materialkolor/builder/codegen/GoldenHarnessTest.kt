package com.materialkolor.builder.codegen

import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.Language
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.codegen.text.headerCaseFiles
import java.io.File
import java.util.zip.CRC32
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoldenHarnessTest {
    private val scratch: File = createTempDirectory("golden").toFile()
    private val goldenRoot = File(scratch, "golden")
    private val hashesFile = File(scratch, "GoldenHashes.kt")

    @AfterTest
    fun cleanUp() {
        scratch.deleteRecursively()
    }

    @Test
    fun headerDefault_golden_matchesTheCheckedInFile() {
        GoldenHarness.Default.verify("header-default", headerCaseFiles(Fixtures.Default.input))
    }

    @Test
    fun goldenHashes_checkedInFile_matchesTheGoldenTree() {
        GoldenHarness.Default.verifyHashes()
    }

    @Test
    fun verify_matchingGolden_passes() {
        harness(update = true).verify("sample", listOf(file("Theme.kt", "one")))

        harness(update = false).verify("sample", listOf(file("Theme.kt", "one")))
    }

    @Test
    fun verify_changedText_failsNamingTheFile() {
        harness(update = true).verify("sample", listOf(file("src/Theme.kt", "one")))

        val failure = assertFailsWith<AssertionError> {
            harness(update = false).verify("sample", listOf(file("src/Theme.kt", "two")))
        }

        val message = failure.message.orEmpty()
        assertTrue("sample/src/Theme.kt differs" in message, message)
        assertTrue("-Pgolden.update=true" in message, message)
    }

    @Test
    fun verify_missingGolden_failsAskingForAnUpdate() {
        val failure = assertFailsWith<AssertionError> {
            harness(update = false).verify("sample", listOf(file("Theme.kt", "one")))
        }

        assertTrue("There is no golden for case sample" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun verify_extraFileOnDisk_fails() {
        harness(update = true).verify("sample", listOf(file("Theme.kt", "one"), file("Color.kt", "two")))

        assertFailsWith<AssertionError> {
            harness(update = false).verify("sample", listOf(file("Theme.kt", "one")))
        }
    }

    @Test
    fun verify_updateMode_rewritesTheCaseAndDropsStaleFiles() {
        harness(update = true).verify("sample", listOf(file("Theme.kt", "one"), file("Color.kt", "two")))
        harness(update = true).verify("sample", listOf(file("Theme.kt", "three")))

        assertEquals("three\n", File(goldenRoot, "sample/Theme.kt").readText())
        assertFalse(File(goldenRoot, "sample/Color.kt").exists())
    }

    @Test
    fun verify_updateMode_writesHashesForEveryCase() {
        val first = listOf(file("Theme.kt", "one"))
        val second = listOf(file("a/Color.kt", "two"), file("Theme.kt", "three"))
        harness(update = true).verify("first-case", first)
        harness(update = true).verify("second-case", second)

        val expected =
            """
            package com.materialkolor.builder.codegen

            /**
             * The CRC-32 of every golden case, as `GoldenDigest` works it out.
             *
             * The JVM golden harness writes this file whenever it updates the goldens, so rather than editing
             * it by hand, run the codegen jvmTest task with -Pgolden.update=true.
             */
            internal object GoldenHashes {
                val cases: Map<String, Long> = mapOf(
                    "first-case" to 0x${hex(GoldenDigest.of(first))}L,
                    "second-case" to 0x${hex(GoldenDigest.of(second))}L,
                )
            }

            """.trimIndent()

        assertEquals(expected, hashesFile.readText())
        harness(update = false).verifyHashes()
    }

    @Test
    fun verifyHashes_staleFile_fails() {
        harness(update = true).verify("sample", listOf(file("Theme.kt", "one")))
        hashesFile.writeText("stale")

        assertFailsWith<AssertionError> { harness(update = false).verifyHashes() }
    }

    @Test
    fun verify_badCaseName_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            harness(update = true).verify("Bad Name", listOf(file("Theme.kt", "one")))
        }
        assertFailsWith<IllegalArgumentException> {
            harness(update = true).verify("sample", listOf(file("../Theme.kt", "one")))
        }
    }

    @Test
    fun crc32_anyBytes_matchesJavaZip() {
        assertEquals(0xCBF43926L, GoldenDigest.crc32("123456789".encodeToByteArray()))

        listOf("", "a", "Color(0xFF6750A4)\n", "ünïcödé ✓", "x".repeat(10_000)).forEach { text ->
            val bytes = text.encodeToByteArray()
            val expected = CRC32().apply { update(bytes) }.value

            assertEquals(expected, GoldenDigest.crc32(bytes), text.take(20))
        }
    }

    private fun harness(update: Boolean): GoldenHarness = GoldenHarness(goldenRoot, hashesFile, update)

    private fun file(
        path: String,
        text: String,
    ): GeneratedFile = GeneratedFile(path, Language.Kotlin, listOf(listOf(Token(TokenKind.Plain, text))))

    private fun hex(value: Long): String = value.toString(radix = 16).uppercase().padStart(8, '0')
}
