package com.materialkolor.builder.codegen

import com.materialkolor.builder.codegen.dsl.GeneratedFile
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Holds generated files to the goldens checked in under `src/jvmTest/resources/golden`.
 *
 * Each case is a directory named after it, holding every file the case generates at its own path.
 * A normal run compares and fails on the first file that differs. With `-Pgolden.update=true` on
 * the Gradle command line the harness rewrites the case instead, and rewrites `GoldenHashes.kt` in
 * commonTest from whatever the golden tree then holds, so the wasm tests can check the same bytes.
 *
 * @property[goldenRoot] The directory the cases live in.
 * @property[hashesFile] The `GoldenHashes.kt` source file the hashes are written to.
 * @property[update] Whether to rewrite rather than compare.
 */
internal class GoldenHarness(
    private val goldenRoot: File,
    private val hashesFile: File,
    private val update: Boolean,
) {
    /** Compares [files] with the golden [case], or rewrites that case in update mode. */
    fun verify(
        case: String,
        files: List<GeneratedFile>,
    ) {
        require(CASE_NAME.matches(case)) { "A golden case name is lower case words joined by dashes, got '$case'" }
        require(files.isNotEmpty()) { "Golden case $case generated no files" }
        files.forEach { file ->
            require(!file.path.startsWith("/") && ".." !in file.path.split('/')) {
                "Golden case $case writes outside itself at ${file.path}"
            }
        }
        require(files.map { it.path }.distinct().size == files.size) { "Golden case $case repeats a path" }

        val caseDir = File(goldenRoot, case)
        if (update) {
            caseDir.deleteRecursively()
            files.forEach { file ->
                val target = File(caseDir, file.path)
                target.parentFile.mkdirs()
                target.writeText(file.text)
            }
            writeHashes()
            return
        }

        val onDisk = readCase(caseDir)
        if (onDisk.isEmpty()) fail("There is no golden for case $case. $UPDATE_HINT")

        val generated = files.associate { file -> file.path to file.text }
        assertEquals(
            onDisk.keys.sorted(),
            generated.keys.sorted(),
            "Golden case $case writes a different set of files. $UPDATE_HINT",
        )
        generated.forEach { (path, text) ->
            assertEquals(onDisk.getValue(path), text, "Golden $case/$path differs. $UPDATE_HINT")
        }
    }

    /** Checks `GoldenHashes.kt` agrees with the golden tree, or rewrites it in update mode. */
    fun verifyHashes() {
        if (update) {
            writeHashes()
            return
        }

        val committed = if (hashesFile.exists()) hashesFile.readText() else ""
        assertEquals(renderHashes(), committed, "GoldenHashes.kt is out of date. $UPDATE_HINT")
    }

    /** The CRC-32 of every case in the golden tree, by case name. */
    fun hashes(): Map<String, Long> =
        goldenRoot
            .listFiles { file -> file.isDirectory }
            .orEmpty()
            .sortedBy { it.name }
            .associate { caseDir -> caseDir.name to GoldenDigest.of(readCase(caseDir)) }

    /** The source of `GoldenHashes.kt` for the golden tree as it stands. */
    fun renderHashes(): String {
        val entries = hashes()
        val value = if (entries.isEmpty()) {
            "emptyMap()"
        } else {
            entries.entries.joinToString(separator = "", prefix = "mapOf(\n", postfix = "    )") { (case, hash) ->
                "        \"$case\" to 0x${hash.toString(radix = 16).uppercase().padStart(8, '0')}L,\n"
            }
        }

        return HASHES_TEMPLATE.replace("{{cases}}", value)
    }

    private fun writeHashes() {
        hashesFile.parentFile.mkdirs()
        hashesFile.writeText(renderHashes())
    }

    /** Every file of one case, by its path relative to the case with forward slashes. */
    private fun readCase(caseDir: File): Map<String, String> {
        if (!caseDir.isDirectory) return emptyMap()

        return caseDir
            .walkTopDown()
            .filter { it.isFile }
            .associate { file -> file.relativeTo(caseDir).invariantSeparatorsPath to file.readText() }
    }

    companion object {
        private val CASE_NAME = Regex("[a-z0-9]+(-[a-z0-9]+)*")

        private const val UPDATE_HINT =
            "If the change is intended, rerun the codegen jvmTest with -Pgolden.update=true and review the diff."

        private val HASHES_TEMPLATE =
            """
            |package com.materialkolor.builder.codegen
            |
            |/**
            | * The CRC-32 of every golden case, as `GoldenDigest` works it out.
            | *
            | * The JVM golden harness writes this file whenever it updates the goldens, so rather than editing
            | * it by hand, run the codegen jvmTest task with -Pgolden.update=true.
            | */
            |internal object GoldenHashes {
            |    val cases: Map<String, Long> = {{cases}}
            |}
            |
            """.trimMargin()

        /** The harness for this module's own goldens, in update mode when Gradle was asked for it. */
        val Default: GoldenHarness by lazy {
            val module = File(repoRoot(), "builder/codegen")

            GoldenHarness(
                goldenRoot = File(module, "src/jvmTest/resources/golden"),
                hashesFile = File(module, "src/commonTest/kotlin/com/materialkolor/builder/codegen/GoldenHashes.kt"),
                update = System.getProperty("golden.update").toBoolean(),
            )
        }

        /** The repository root, found by walking up from wherever Gradle ran the tests. */
        fun repoRoot(): File =
            generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
                .firstOrNull { dir -> File(dir, "settings.gradle.kts").isFile && File(dir, "builder").isDirectory }
                ?: error("Could not find the repository root above ${System.getProperty("user.dir")}")
    }
}
