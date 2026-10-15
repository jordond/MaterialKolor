package com.materialkolor.convention.mcu

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import javax.inject.Inject

/**
 * Records what the upstream checkout currently contains, for a maintainer to review.
 *
 * The result is written under the build directory and never replaces the tracked lock on its
 * own. Updating the pin stays an explicit act. Read the candidate, review the upstream diff and the
 * transformation rules, then move the file into `gradle/` by hand.
 */
@DisableCachingByDefault(because = "Explicit maintainer operation inspects the current proposed Git revision")
abstract class CandidateMcuUpstreamLock : DefaultTask() {
    @get:Internal
    abstract val upstreamDirectory: DirectoryProperty

    @get:OutputFile
    abstract val candidateFile: RegularFileProperty

    @get:Inject
    protected abstract val execOperations: ExecOperations

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun candidate() {
        val upstream = upstreamDirectory.get().asFile
        val git = McuGit(execOperations)
        if (git.run(upstream, "status", "--porcelain", "--untracked-files=all").isNotEmpty()) {
            throw GradleException("Candidate MCU input must be clean before recording hashes")
        }

        val sources = upstream.toPath().resolve("kotlin")
        val candidate = linkedMapOf<String, Any>(
            "schemaVersion" to McuLockSchema.SCHEMA_VERSION,
            "policyVersion" to McuLockSchema.POLICY_VERSION,
            "upstreamRevision" to git.run(upstream, "rev-parse", "HEAD"),
            "licenseSha256" to McuUpstreamLock.hash(upstream.toPath().resolve("LICENSE")),
        )

        val hashes = linkedMapOf<String, String>()
        for (path in McuUpstreamLock.kotlinSourcePaths(sources)) {
            hashes[path] = McuUpstreamLock.hash(sources.resolve(path))
        }

        if (hashes.isEmpty()) {
            throw GradleException("Candidate has no Kotlin source tree")
        }

        candidate["files"] = hashes

        val output = candidateFile.get().asFile.toPath()
        Files.createDirectories(output.parent)
        Files.writeString(output, JsonOutput.prettyPrint(JsonOutput.toJson(candidate)) + "\n")
        logger.lifecycle(
            "Wrote candidate only: {}. Review source, rules and parity before replacing the tracked lock.",
            output,
        )
    }
}
