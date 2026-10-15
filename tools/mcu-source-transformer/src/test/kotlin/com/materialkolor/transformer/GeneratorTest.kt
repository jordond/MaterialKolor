package com.materialkolor.transformer

import com.materialkolor.transformer.lock.SourceLock
import com.materialkolor.transformer.lock.sha256
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratorTest {
    private val parserVersion = requiredProperty("mcu.parserVersion")

    @Test
    fun sourceFailuresPreserveCompleteOutputAndManifest() =
        fixture { root ->
            val source = File(root, "input/kotlin/utils/Fixture.kt")
            source.parentFile.mkdirs()
            source.writeText("package utils\nval value = 1\n")
            val lock = writeLock(root)
            generate(root, lock)
            val before = File(root, "output/utils/Fixture.kt").readBytes()
            val report = File(root, "report.tsv").readBytes()
            source.writeText("package utils\nfun broken( {")
            writeLock(root)
            assertFailsWith<IllegalArgumentException> { generate(root, lock) }
            assertTrue(before.contentEquals(File(root, "output/utils/Fixture.kt").readBytes()))
            assertTrue(report.contentEquals(File(root, "report.tsv").readBytes()))
        }

    @Test
    fun manifestsAreDeterministicAcrossOutputLocationsAndStaleFilesAreRemoved() =
        fixture { root ->
            val source = File(root, "input/kotlin/utils/Fixture.kt")
            source.parentFile.mkdirs()
            source.writeText("package utils\nval value = 1\n")
            val lock = writeLock(root)
            generate(root, lock)
            val first = File(root, "report.tsv").readText()
            File(root, "output/Stale.kt").writeText("invalid")
            generate(root, lock)
            assertFalse(File(root, "output/Stale.kt").exists())
            assertEquals(first, File(root, "report.tsv").readText())
            generate(
                File(root, "input/kotlin"),
                File(root, "relocated-output"),
                File(root, "relocated-report.tsv"),
                lock,
                Mode.Reference,
                parserVersion,
            )
            assertEquals(first, File(root, "relocated-report.tsv").readText())
            assertFalse(first.contains(root.absolutePath))
            assertTrue(first.contains("# policyVersion\t1"))
            assertTrue(first.contains("# parserVersion\t$parserVersion"))
        }

    @Test
    fun reviewedInventoryRejectsChangesMissingFilesAndLicenseDrift() =
        fixture { root ->
            val source = File(root, "input/kotlin/utils/Fixture.kt")
            source.parentFile.mkdirs()
            source.writeText("package utils\nval value = 1\n")
            val lockFile = writeLock(root)
            val lock = SourceLock.read(lockFile)
            source.appendText("// change\n")
            assertFailsWith<IllegalArgumentException> { lock.validate(File(root, "input/kotlin")) }
            source.delete()
            assertFailsWith<IllegalArgumentException> { lock.validate(File(root, "input/kotlin")) }
            source.writeText("package utils\nval value = 1\n")
            File(root, "input/LICENSE").appendText("changed")
            assertFailsWith<IllegalArgumentException> { lock.validate(File(root, "input/kotlin")) }
        }

    @Test
    fun lockRejectsUnknownKeysAndUnsafePaths() =
        fixture { root ->
            val source = File(root, "input/kotlin/utils/Fixture.kt")
            source.parentFile.mkdirs()
            source.writeText("package utils")
            val lock = writeLock(root)
            val valid = lock.readText()
            for (invalid in listOf(
                valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": 1, \"other\": 1"),
                valid.replace("utils/Fixture.kt", "../Fixture.kt"),
            )) {
                lock.writeText(invalid)
                assertFailsWith<IllegalArgumentException> { SourceLock.read(lock) }
            }
        }

    private fun generate(
        root: File,
        lock: File,
    ) = generate(
        File(root, "input/kotlin"),
        File(root, "output"),
        File(root, "report.tsv"),
        lock,
        Mode.Reference,
        parserVersion,
    )

    private fun writeLock(root: File): File {
        val input = File(root, "input/kotlin")
        val license = File(root, "input/LICENSE")
        if (!license.exists()) license.writeText("fixture license\n")
        val files = input.walkTopDown().filter { it.isFile }.sortedBy { it.path }.joinToString(",\n") {
            "    \"${it.relativeTo(input).invariantSeparatorsPath}\": \"${it.readBytes().sha256()}\""
        }
        return File(root, "lock.json").also { lock ->
            lock.writeText(
                """
                {
                  "schemaVersion": 1,
                  "policyVersion": 1,
                  "upstreamRevision": "${"a".repeat(40)}",
                  "licenseSha256": "${license.readBytes().sha256()}",
                  "files": {
                $files
                  }
                }
                """.trimIndent(),
            )
        }
    }

    private fun fixture(block: (File) -> Unit) = temporaryDirectory("mcu-transformer-test-", block)
}
