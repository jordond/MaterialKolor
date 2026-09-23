package com.materialkolor.builder.codegen.zip

import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.codegen.zipArchive
import com.materialkolor.builder.domain.persist.ExportPrefs
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZipWriterTest {
    @Test
    fun zipArchive_generatedFiles_readBackEntryForEntry() {
        val files = generate(Fixtures.Default.input)

        val entries = readStream(zipArchive("AppTheme", files))

        assertEquals(files.map { file -> "AppTheme/${file.path}" }, entries.map { it.first.name })
        files.zip(entries).forEach { (file, entry) ->
            assertContentEquals(file.text.encodeToByteArray(), entry.second, file.path)
            assertEquals(ZipEntry.STORED, entry.first.method, file.path)
        }
    }

    @Test
    fun zipArchive_multiplatform_laysFilesOutUnderTheThemeFolder() {
        val names = readStream(zipArchive("AppTheme", generate(Fixtures.Default.input))).map { it.first.name }

        assertEquals(
            listOf(
                "AppTheme/src/commonMain/kotlin/com/example/theme/Color.kt",
                "AppTheme/src/commonMain/kotlin/com/example/theme/Theme.kt",
                "AppTheme/gradle/libs.versions.toml",
                "AppTheme/snippets/build.gradle.kts",
                "AppTheme/README.md",
            ),
            names,
        )
    }

    @Test
    fun zipArchive_androidOnlyWithoutCatalog_usesTheMainSourceSet() {
        val input = Fixtures.input(
            document = Fixtures.Default.input.document,
            prefs = ExportPrefs(multiplatform = false, versionCatalog = false),
        )

        val names = readStream(zipArchive("AppTheme", generate(input))).map { it.first.name }

        assertEquals(
            listOf(
                "AppTheme/src/main/kotlin/com/example/theme/Color.kt",
                "AppTheme/src/main/kotlin/com/example/theme/Theme.kt",
                "AppTheme/snippets/build.gradle.kts",
                "AppTheme/README.md",
            ),
            names,
        )
    }

    @Test
    fun write_entries_openThroughTheCentralDirectory() {
        val entries = listOf(
            ZipWriter.Entry("Thème/README.md", "Déjà vu\n".encodeToByteArray()),
            ZipWriter.Entry("Thème/empty.txt", ByteArray(0)),
            ZipWriter.Entry("Thème/src/Theme.kt", Random(7).nextBytes(4096)),
        )
        val file = File.createTempFile("zip-writer", ".zip")

        try {
            file.writeBytes(ZipWriter.write(entries))
            ZipFile(file).use { zip ->
                assertEquals(entries.map { it.path }, zip.entries().toList().map { it.name })
                entries.forEach { entry ->
                    val read = zip.getEntry(entry.path)
                    assertEquals(entry.bytes.size.toLong(), read.size, entry.path)
                    assertEquals(javaCrc(entry.bytes), read.crc, entry.path)
                    assertContentEquals(entry.bytes, zip.getInputStream(read).readBytes(), entry.path)
                }
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun write_sameEntries_giveTheSameBytes() {
        val files = generate(Fixtures.ThreeAccents.input)

        assertContentEquals(zipArchive("AppTheme", files), zipArchive("AppTheme", files))
        val times = readStream(zipArchive("AppTheme", files)).map { it.first.lastModifiedTime.toMillis() }.distinct()
        assertEquals(1, times.size)
    }

    @Test
    fun write_repeatedPath_isRejected() {
        val entry = ZipWriter.Entry("AppTheme/README.md", ByteArray(1))

        assertFailsWith<IllegalArgumentException> { ZipWriter.write(listOf(entry, entry)) }
    }

    @Test
    fun entry_pathOutsideTheArchive_isRejected() {
        val paths = listOf(
            "/AppTheme/README.md",
            "AppTheme/../README.md",
            "AppTheme//README.md",
            "AppTheme\\README.md",
            "AppTheme/",
        )

        paths.forEach { path ->
            assertFailsWith<IllegalArgumentException>(path) { ZipWriter.Entry(path, ByteArray(0)) }
        }
    }

    @Test
    fun crc32_anyBytes_matchesJavaZip() {
        val random = Random(42)
        val samples =
            listOf(ByteArray(0), "123456789".encodeToByteArray(), random.nextBytes(1), random.nextBytes(10_000))

        samples.forEach { bytes ->
            assertEquals(javaCrc(bytes), Crc32.of(bytes).toLong() and 0xFFFFFFFFL)
        }
    }

    private fun readStream(bytes: ByteArray): List<Pair<ZipEntry, ByteArray>> =
        ZipInputStream(ByteArrayInputStream(bytes)).use { stream ->
            generateSequence { stream.nextEntry }
                .map { entry -> entry to stream.readBytes() }
                .toList()
        }

    private fun javaCrc(bytes: ByteArray): Long = CRC32().apply { update(bytes) }.value
}
