package com.materialkolor.convention.mcu

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.util.TreeSet

/**
 * Checks that both compiler front ends produced the same generated tree.
 *
 * The shipped sources come from the pinned parser and the alternate generation reruns the same
 * rules through the catalogue's Kotlin. Byte-identical output proves the tree reflects the reviewed
 * rules rather than one parser version, so any divergence between the two trees fails the build
 * with the offending paths.
 */
@DisableCachingByDefault(because = "Comparing two locally generated trees is cheaper than a cache round trip")
abstract class VerifyMcuParserAgreement : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val primaryDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val alternateDirectory: DirectoryProperty

    @TaskAction
    fun verify() {
        val primaryRoot = primaryDirectory.get().asFile.toPath()
        val alternateRoot = alternateDirectory.get().asFile.toPath()

        val primaryPaths = TreeSet(McuUpstreamLock.allPaths(primaryRoot))
        val alternatePaths = TreeSet(McuUpstreamLock.allPaths(alternateRoot))
        if (primaryPaths != alternatePaths) {
            throw GradleException(
                "MCU parser disagreement in the generated file inventory. " +
                    "Pinned parser only: ${primaryPaths - alternatePaths}. " +
                    "Alternate parser only: ${alternatePaths - primaryPaths}.",
            )
        }

        val divergent = primaryPaths.filter { path ->
            Files.mismatch(primaryRoot.resolve(path), alternateRoot.resolve(path)) >= 0
        }

        if (divergent.isNotEmpty()) {
            throw GradleException(
                "MCU parser disagreement. The pinned and alternate front ends generated different " +
                    "text for ${divergent.size} file(s), so the output depends on the parser " +
                    "version instead of the reviewed rules alone: $divergent",
            )
        }
    }
}
