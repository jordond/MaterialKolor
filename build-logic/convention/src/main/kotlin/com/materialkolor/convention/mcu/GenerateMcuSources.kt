package com.materialkolor.convention.mcu

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.TreeSet

/**
 * Adapts the pinned upstream Kotlin into the sources this project publishes.
 *
 * The work happens in a forked process because the transformer needs the Kotlin compiler and its
 * PSI classes, and those must stay on the tool's own runtime classpath rather than this build's.
 */
@CacheableTask
abstract class GenerateMcuSources : JavaExec() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val lockFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val licenseFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Input
    abstract val mode: Property<String>

    @get:Input
    abstract val parserVersion: Property<String>

    private var unexpectedFiles: Boolean? = null

    init {
        mainClass.set("com.materialkolor.transformer.MainKt")
        mode.convention("library")
        maxHeapSize = "512m"
        outputs.upToDateWhen { task -> !(task as GenerateMcuSources).hasUnexpectedFiles() }
        outputs.doNotCacheIf("Unexpected files in the exclusively owned generated tree") { task ->
            (task as GenerateMcuSources).hasUnexpectedFiles()
        }
    }

    /**
     * Whether the generated tree holds anything the reviewed lock does not account for.
     *
     * Gradle asks both the up-to-date check and the cacheability check, so the answer is read
     * once per task instance instead of reparsing the lock and re-walking the tree each time.
     */
    private fun hasUnexpectedFiles(): Boolean {
        unexpectedFiles?.let { cached -> return cached }

        val actual = TreeSet(McuUpstreamLock.allPaths(outputDirectory.get().asFile.toPath()))
        actual.removeAll(McuUpstreamLock(lockFile.get().asFile).files.keys)

        val result = actual.isNotEmpty()
        unexpectedFiles = result

        return result
    }

    @TaskAction
    override fun exec() {
        args = listOf(
            sourceDirectory.get().asFile.absolutePath,
            outputDirectory.get().asFile.absolutePath,
            reportFile.get().asFile.absolutePath,
            lockFile.get().asFile.absolutePath,
            mode.get(),
            parserVersion.get(),
        )

        super.exec()
    }
}
