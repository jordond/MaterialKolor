package com.materialkolor.convention.mcu

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

/**
 * Checks the pinned upstream checkout against the reviewed source lock.
 *
 * Compares the recorded Gitlink, the checked-out revision, a clean working tree and the reviewed
 * file hashes, so nothing is generated from an input nobody has read. It must execute even when the
 * generated source task is up to date or restored from the build cache, which is why it declares
 * its Git inputs as internal and is never cacheable.
 */
@DisableCachingByDefault(because = "Validates live Git provenance on every generation request")
abstract class VerifyMcuUpstream : DefaultTask() {
    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:Internal
    abstract val upstreamDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val lockFile: RegularFileProperty

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @TaskAction
    fun verify() {
        val root = repositoryDirectory.get().asFile
        val upstream = upstreamDirectory.get().asFile
        if (!File(upstream, ".git").exists()) {
            throw GradleException("MCU upstream is uninitialized. Run git submodule update --init --recursive.")
        }

        val git = McuGit(execOperations)
        val lock = McuUpstreamLock(lockFile.get().asFile)
        val relative = root.toPath().relativize(upstream.toPath()).toString()

        val gitlink = git.run(root, "ls-files", "--stage", "--", relative)
        if (!gitlink.startsWith("160000 ${lock.revision} 0\t")) {
            throw GradleException(
                "MCU Gitlink/lock mismatch. Review and stage the upstream Gitlink with gradle/mcu-upstream.lock.json.",
            )
        }

        if (git.run(upstream, "rev-parse", "HEAD") != lock.revision) {
            throw GradleException(
                "MCU checkout revision does not match the reviewed source lock: ${lock.revision}",
            )
        }

        val dirty = git.run(upstream, "status", "--porcelain", "--untracked-files=all")
        if (dirty.isNotEmpty()) {
            throw GradleException("MCU upstream must be clean; do not edit pinned input:\n$dirty")
        }

        lock.verify(upstream.toPath().resolve("kotlin"))
    }
}
