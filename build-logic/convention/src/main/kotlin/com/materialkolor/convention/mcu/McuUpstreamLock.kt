package com.materialkolor.convention.mcu

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.TreeSet

/**
 * Reviewed source identity shared by the lightweight provenance and generation tasks.
 *
 * The PSI transformer reads the same file and validates every field before it will generate anything.
 */
internal class McuUpstreamLock(file: File) {
    val revision: String
    val licenseHash: String
    val files: Map<String, String>

    init {
        val json = JsonSlurper().parse(file, "UTF-8") as Map<*, *>
        if (json["schemaVersion"] != McuLockSchema.SCHEMA_VERSION ||
            json["policyVersion"] != McuLockSchema.POLICY_VERSION
        ) {
            throw GradleException("Unsupported MCU lock schema or transformation policy: $file")
        }

        revision = string(json["upstreamRevision"], McuLockSchema.REVISION_PATTERN, "revision", file)
        licenseHash = string(json["licenseSha256"], McuLockSchema.HASH_PATTERN, "license hash", file)
        files = inventory(json["files"], file)
    }

    fun verify(source: Path) {
        val actual = kotlinSourcePaths(source)
        if (actual != files.keys) {
            val missing = TreeSet(files.keys).apply { removeAll(actual) }
            val added = TreeSet(actual).apply { removeAll(files.keys) }
            throw GradleException("MCU source inventory drift; missing=$missing, added=$added")
        }

        for ((path, expected) in files) {
            if (hash(source.resolve(path)) != expected) {
                throw GradleException(
                    "MCU source hash changed: $path; review the upstream pin and lock together",
                )
            }
        }

        if (hash(source.parent.resolve("LICENSE")) != licenseHash) {
            throw GradleException("MCU upstream LICENSE changed; review its lock hash")
        }
    }

    companion object {
        private fun string(value: Any?, pattern: String, field: String, file: File): String {
            if (value !is String || !value.matches(Regex(pattern))) {
                throw GradleException("Malformed MCU source lock $field: $file")
            }

            return value
        }

        private fun inventory(value: Any?, file: File): Map<String, String> {
            if (value !is Map<*, *> || value.isEmpty()) {
                throw GradleException("Malformed MCU source lock inventory: $file")
            }

            for ((path, hash) in value) {
                if (path !is String || !path.matches(Regex(McuLockSchema.SOURCE_PATH_PATTERN))) {
                    throw GradleException("Unsafe MCU lock path: $path")
                }
                string(hash, McuLockSchema.HASH_PATTERN, "hash for $path", file)
            }

            @Suppress("UNCHECKED_CAST")
            return value as Map<String, String>
        }

        /**
         * Every Kotlin source under [root], as slash-separated paths relative to it.
         */
        fun kotlinSourcePaths(root: Path): Set<String> = relativePaths(root) { path ->
            path.toString().endsWith(".kt")
        }

        /**
         * Every regular file under [root], as slash-separated paths relative to it.
         */
        fun allPaths(root: Path): Set<String> = relativePaths(root) { true }

        private fun relativePaths(root: Path, include: (Path) -> Boolean): Set<String> {
            if (!Files.isDirectory(root)) {
                return emptySet()
            }
            Files.walk(root).use { stream ->
                return stream.iterator().asSequence()
                    .filter { path -> Files.isRegularFile(path) && include(path) }
                    .map { path -> root.relativize(path).toString().replace(File.separatorChar, '/') }
                    .toCollection(TreeSet())
            }
        }

        fun hash(path: Path): String = hash(Files.readAllBytes(path))

        fun hash(content: ByteArray): String =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))
    }
}
