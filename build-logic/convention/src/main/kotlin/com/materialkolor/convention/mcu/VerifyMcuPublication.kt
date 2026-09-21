package com.materialkolor.convention.mcu

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Inspects what the libraries actually publish, then compiles against it.
 *
 * Runs over a local file repository. Every coordinate must carry current metadata and a source
 * archive, published metadata must not leak build-only dependencies, published sources must match
 * the generation manifest byte for byte and stay platform neutral, and a separate build that sees
 * only the artifacts must still compile against the public API.
 */
@DisableCachingByDefault(because = "Inspects locally published artifacts and compiles an independent consumer")
abstract class VerifyMcuPublication : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repositoryDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val consumerDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val lockFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val generatedReport: RegularFileProperty

    @get:Internal
    abstract val gradleWrapper: RegularFileProperty

    @get:Internal
    abstract val consumerBuildDirectory: DirectoryProperty

    @get:Input
    abstract val artifactVersion: Property<String>

    @get:Input
    abstract val kotlinVersion: Property<String>

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @TaskAction
    fun verify() {
        val repository = repositoryDirectory.get().asFile.toPath()
        val lock = McuUpstreamLock(lockFile.get().asFile)
        val version = artifactVersion.get()
        val coordinates = coordinates()
        val currentFiles = mutableListOf<Path>()

        for (artifact in coordinates) {
            val directory = repository.resolve("com/materialkolor/$artifact/$version")
            val files = currentArtifacts(directory, artifact, version)

            for (extension in listOf(".pom", ".module", "-sources.jar")) {
                if (files.none { path -> path.fileName.toString().endsWith(extension) }) {
                    throw GradleException("Missing current publication: $artifact $version$extension")
                }
            }

            currentFiles.addAll(files)
        }

        val report = generatedReport.get().asFile.toPath()
        val outputHashes = mutableMapOf<String, String>()
        for (line in Files.readAllLines(report)) {
            if (line.startsWith("#") || line.startsWith("path\t") || line.isBlank()) {
                continue
            }

            // path, input hash, output hash, then an optional rule summary that may be empty.
            val columns = line.split("\t")
            if (columns.size < 3 || !columns[2].matches(Regex(McuLockSchema.HASH_PATTERN))) {
                throw GradleException("Malformed generated manifest row in $report: $line")
            }

            outputHashes[columns[0]] = columns[2]
        }

        if (outputHashes.keys != lock.files.keys) {
            throw GradleException("Generated manifest inventory differs from the reviewed lock")
        }

        var metadata = 0
        var sources = 0
        for (path in currentFiles) {
            val name = path.fileName.toString()
            if (name.endsWith(".pom") || name.endsWith(".module")) {
                metadata++
                val text = Files.readString(path)
                for (forbidden in BUILD_ONLY_MARKERS) {
                    if (text.contains(forbidden)) {
                        throw GradleException(
                            "Build-only dependency in published metadata: $path: $forbidden",
                        )
                    }
                }
            }

            if (!name.endsWith(".jar")) {
                continue
            }

            ZipFile(path.toFile()).use { zip ->
                val names = mutableSetOf<String>()
                for (entry in zip.entries()) {
                    if (!names.add(entry.name)) {
                        throw GradleException("Duplicate archive entry: $path: ${entry.name}")
                    }
                }

                if (!name.endsWith("-sources.jar")) {
                    return@use
                }
                sources++

                if ("META-INF/LICENSE-material-color-utilities" !in names) {
                    throw GradleException("Upstream license missing: $path")
                }

                if (!name.startsWith("material-color-utilities")) {
                    return@use
                }

                for (input in lock.files.keys) {
                    val matches = names.filter { value -> value == input || value.endsWith("/$input") }
                    if (matches.size != 1) {
                        throw GradleException("Expected one generated source $input in $path, found ${matches.size}")
                    }

                    zip.getInputStream(zip.getEntry(matches[0])).use { content ->
                        val bytes = content.readAllBytes()
                        if (McuUpstreamLock.hash(bytes) != outputHashes[input]) {
                            throw GradleException("Published source differs from generated manifest: $path: $input")
                        }
                        if (JVM_IMPORT.containsMatchIn(String(bytes, StandardCharsets.UTF_8))) {
                            throw GradleException("JVM-only import in published common source: ${matches[0]}")
                        }
                    }
                }
            }
        }

        // Every coordinate must publish a POM, a Gradle module file and a sources archive, so the
        // minimum count is derived from the coordinate list rather than guessed.
        val expectedMetadata = coordinates.size * 2
        if (metadata < expectedMetadata || sources < coordinates.size) {
            throw GradleException(
                "Publication scan of ${coordinates.size} coordinates found $metadata POM and module files " +
                    "(expected $expectedMetadata) and $sources source archives (expected ${coordinates.size}); " +
                    "a coordinate published less than it owes",
            )
        }

        val consumer = consumerDirectory.get().asFile.toPath()
        val fixture = consumerBuildDirectory.get().asFile.toPath()
        if (Files.exists(fixture)) {
            Files.walk(fixture).use { stream ->
                for (path in stream.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path)
                }
            }
        }

        Files.walk(consumer).use { stream ->
            for (path in stream.iterator().asSequence().filter { source -> Files.isRegularFile(source) }) {
                val target = fixture.resolve(consumer.relativize(path))
                Files.createDirectories(target.parent)
                Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        execOperations.exec {
            commandLine(
                gradleWrapper.get().asFile.absolutePath, "-p", fixture.toString(),
                "compileKotlin", "--configuration-cache", "--no-daemon", "--max-workers=2",
                "-PmcuRepository=$repository", "-PmcuVersion=$version",
                "-PkotlinVersion=${kotlinVersion.get()}",
            )
        }.assertNormalExitValue()

        logger.lifecycle(
            "Verified {} metadata files and {} source archives; artifact-only consumer compiled.",
            metadata,
            sources,
        )
    }

    private companion object {
        /**
         * Generated common sources must not import JVM packages, whatever the upstream input did
         */
        val JVM_IMPORT = Regex("""(?m)^import (java|javax)\.""")

        /**
         * One suffix per Kotlin Multiplatform target both modules publish.
         *
         * This list has to track the targets declared in the two module build scripts. A target
         * added there without a suffix here is never inspected.
         */
        val SHARED_TARGET_SUFFIXES = listOf(
            "", "-android", "-jvm", "-js", "-wasm-js", "-macosarm64", "-iosarm64", "-iossimulatorarm64",
        )

        /**
         * Names that only ever appear in build tooling, so seeing one in published metadata is a leak.
         */
        val BUILD_ONLY_MARKERS =
            listOf("mcu-source-transformer", "mcu-upstream", "kotlin-compiler", "scratchpad")

        /**
         * Coordinates expected in the repository, as published artifact names.
         */
        fun coordinates(): List<String> = mcuLibraryModules.flatMap { module ->
            SHARED_TARGET_SUFFIXES.map { suffix -> "$module$suffix" }
        }

        fun currentArtifacts(directory: Path, artifact: String, version: String): List<Path> {
            if (!Files.isDirectory(directory)) {
                throw GradleException("Missing publication directory: $directory")
            }

            if (!version.endsWith("-SNAPSHOT")) {
                Files.list(directory).use { stream ->
                    return stream.iterator().asSequence()
                        .filter { path ->
                            Files.isRegularFile(path) &&
                                path.fileName.toString().startsWith("$artifact-$version")
                        }
                        .toList()
                }
            }

            try {
                val factory = DocumentBuilderFactory.newInstance()
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)

                val document = factory.newDocumentBuilder().parse(directory.resolve("maven-metadata.xml").toFile())
                val versions = document.getElementsByTagName("snapshotVersion")

                val files = mutableListOf<Path>()
                for (index in 0 until versions.length) {
                    val entry = versions.item(index) as Element
                    val extension = entry.getElementsByTagName("extension").item(0).textContent
                    val value = entry.getElementsByTagName("value").item(0).textContent

                    val classifiers = entry.getElementsByTagName("classifier")
                    val classifier = if (classifiers.length == 0) "" else "-${classifiers.item(0).textContent}"

                    val path = directory.resolve("$artifact-$value$classifier.$extension")
                    if (!Files.isRegularFile(path)) {
                        throw GradleException("Snapshot metadata points to missing artifact: $path")
                    }

                    files.add(path)
                }

                return files
            } catch (error: ParserConfigurationException) {
                throw GradleException("Cannot read current snapshot metadata: $directory", error)
            } catch (error: SAXException) {
                throw GradleException("Cannot read current snapshot metadata: $directory", error)
            }
        }
    }
}
