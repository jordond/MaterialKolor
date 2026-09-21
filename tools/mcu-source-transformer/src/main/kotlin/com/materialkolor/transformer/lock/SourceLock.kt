package com.materialkolor.transformer.lock

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

internal const val SCHEMA_VERSION = 1
internal const val POLICY_VERSION = 1

internal fun ByteArray.sha256(): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }

internal data class SourceLock(
    val upstreamRevision: String,
    val licenseSha256: String,
    val files: Map<String, String>,
) {
    fun validate(input: File): Map<String, String> {
        require(input.isDirectory && !Files.isSymbolicLink(input.toPath())) {
            "input-lock: missing source directory $input"
        }

        val paths = input.walkTopDown().filter { it.isFile || Files.isSymbolicLink(it.toPath()) }.toList()
        require(paths.none { Files.isSymbolicLink(it.toPath()) }) { "input-lock: symbolic links are not source inputs" }

        val sources = paths
            .filter { it.extension == "kt" }
            .associateBy { it.relativeTo(input).invariantSeparatorsPath }
            .toSortedMap()

        require(sources.keys == files.keys) {
            "input-lock: source inventory changed; " +
                "added=${sources.keys - files.keys}, removed=${files.keys - sources.keys}"
        }

        val contents = sources.mapValues { (_, file) -> file.readBytes() }
        val changed = contents.filter { (path, bytes) -> bytes.sha256() != files.getValue(path) }.keys
        require(changed.isEmpty()) {
            "input-lock: source hashes changed: $changed; update the reviewed lock explicitly"
        }

        val license = File(input.parentFile, "LICENSE")
        require(license.isFile && license.readBytes().sha256() == licenseSha256) {
            "input-lock: upstream LICENSE hash changed or missing"
        }

        return contents.mapValues { (path, bytes) ->
            val text = bytes.toString(Charsets.UTF_8)
            require(bytes.contentEquals(text.toByteArray(Charsets.UTF_8))) {
                "$path: input-lock: source is not valid UTF-8"
            }

            text
        }
    }

    companion object {
        fun read(file: File): SourceLock {
            val payload = try {
                Json.decodeFromString(LockPayload.serializer(), file.readText())
            } catch (failure: SerializationException) {
                throw IllegalArgumentException("input-lock: ${failure.message}", failure)
            }

            require(payload.schemaVersion == SCHEMA_VERSION && payload.policyVersion == POLICY_VERSION) {
                "input-lock: unsupported lock schema/policy"
            }

            require(payload.upstreamRevision.matches(Regex("[0-9a-f]{40}"))) { "input-lock: invalid revision" }
            require(payload.licenseSha256.matches(Regex("[0-9a-f]{64}"))) { "input-lock: invalid license hash" }

            val files = payload.files.toSortedMap()
            for ((path, hash) in files) {
                require(path.matches(Regex("[a-z]+/[A-Za-z][A-Za-z0-9]*\\.kt"))) {
                    "input-lock: unsafe source path $path"
                }
                require(hash.matches(Regex("[0-9a-f]{64}"))) { "input-lock: invalid hash for $path" }
            }

            require(files.isNotEmpty()) { "input-lock: empty source inventory" }
            return SourceLock(payload.upstreamRevision, payload.licenseSha256, files)
        }
    }
}

@Serializable
private data class LockPayload(
    val schemaVersion: Int,
    val policyVersion: Int,
    val upstreamRevision: String,
    val licenseSha256: String,
    val files: Map<String, String>,
)
