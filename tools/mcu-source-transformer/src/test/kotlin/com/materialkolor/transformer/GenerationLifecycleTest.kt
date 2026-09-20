package com.materialkolor.transformer

import com.materialkolor.transformer.lock.sha256
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Exercises the production Gradle task classes and transformer in disposable Git submodules.
 */
class GenerationLifecycleTest {
    @Test
    fun generationTracksReviewedSourceChangesAndRestoration() =
        fixtures { sandbox ->
            val fixture = sandbox.checkout("first checkout")
            val originalLock = fixture.lock.readText()
            val originalRevision = fixture.revision()

            val first = fixture.build()
            first.expect(TaskOutcome.SUCCESS)
            val originalOutput = fixture.generated.readText()
            val originalReport = fixture.report.readText()
            assertTrue(originalOutput.contains("package upstream.kotlin.utils"))
            assertFalse(originalReport.contains(fixture.root.absolutePath), "Manifest must be relocatable")

            val second = fixture.build()
            second.expect(TaskOutcome.UP_TO_DATE)
            assertTrue(second.output.contains("Reusing configuration cache."), second.output)
            second.expectVerification()
            assertEquals(originalReport, fixture.report.readText())

            fixture.source.writeText(SOURCE.replace("= 1", "= 2"))
            fixture.commitUpstream("Change audited fixture input")
            fixture.reviewCurrentPin()
            fixture.build().expect(TaskOutcome.SUCCESS)
            assertTrue(fixture.generated.readText().contains("= 2"))
            assertFalse(originalReport == fixture.report.readText())

            git(fixture.upstream, "checkout", "--detach", originalRevision)
            git(fixture.root, "add", "upstream")
            fixture.lock.writeText(originalLock)
            fixture.build().expect(TaskOutcome.FROM_CACHE)
            assertEquals(originalOutput, fixture.generated.readText())
            assertEquals(originalReport, fixture.report.readText())
            assertEquals("", git(fixture.upstream, "status", "--porcelain"))
        }

    @Test
    fun exclusivelyOwnedOutputIsRepairedAndAbsentDirectoryRestoresFromCache() =
        fixtures { sandbox ->
            val fixture = sandbox.checkout("output lifecycle")
            fixture.build().expect(TaskOutcome.SUCCESS)
            val expectedOutput = fixture.generated.readText()
            val expectedReport = fixture.report.readText()

            fixture.generated.writeText("// external modification\n")
            fixture.build().expectRegeneration()
            assertEquals(expectedOutput, fixture.generated.readText())

            assertTrue(fixture.generated.delete())
            fixture.build().expectRegeneration()
            assertEquals(expectedOutput, fixture.generated.readText())

            val unexpected = fixture.output.resolve("stale/Unexpected.kt")
            unexpected.parentFile.mkdirs()
            unexpected.writeText("package stale\nclass Unexpected\n")
            val unrelated = fixture.output.resolve("unexpected.txt").apply { writeText("stale") }
            fixture.build().expect(TaskOutcome.SUCCESS)
            assertFalse(unexpected.exists(), "Stale source must never enter compilation")
            assertFalse(unrelated.exists(), "Task owns the entire output directory")
            assertEquals(expectedOutput, fixture.generated.readText())

            fixture.report.writeText("changed manifest")
            fixture.build().expectRegeneration()
            assertEquals(expectedReport, fixture.report.readText())

            assertTrue(fixture.output.deleteRecursively())
            assertTrue(fixture.report.delete())
            val restored = fixture.build()
            restored.expect(TaskOutcome.FROM_CACHE)
            restored.expectVerification()
            assertEquals(expectedOutput, fixture.generated.readText())
            assertEquals(expectedReport, fixture.report.readText())
        }

    @Test
    fun cachedOutputsNeverBypassLiveGitOrReviewedLockValidation() =
        fixtures { sandbox ->
            val fixture = sandbox.checkout("identity refusals")
            fixture.build().expect(TaskOutcome.SUCCESS)
            val originalRevision = fixture.revision()
            val originalLock = fixture.lock.readText()
            val originalSource = fixture.source.readText()

            // Both up-to-date and cache-restoration requests must execute provenance validation.
            fixture.source.appendText("// unreviewed edit\n")
            fixture.rejects("must be clean")
            fixture.source.writeText(originalSource)
            assertTrue(fixture.output.deleteRecursively())
            fixture.upstream.resolve("untracked.txt").writeText("unreviewed")
            fixture.rejects("must be clean")
            assertTrue(fixture.upstream.resolve("untracked.txt").delete())

            val gitMarker = fixture.upstream.resolve(".git")
            val savedMarker = gitMarker.readText()
            assertTrue(gitMarker.delete())
            fixture.rejects("uninitialized")
            gitMarker.writeText(savedMarker)

            fixture.source.writeText(SOURCE.replace("= 1", "= 3"))
            fixture.commitUpstream("Unreviewed revision")
            fixture.rejects("checkout revision does not match")
            git(fixture.root, "add", "upstream")
            fixture.rejects("Gitlink/lock mismatch")
            git(fixture.upstream, "checkout", "--detach", originalRevision)
            git(fixture.root, "add", "upstream")

            fixture.lock.writeText(originalLock.replace(fixture.source.readBytes().sha256(), "0".repeat(64)))
            fixture.rejects("source hash changed")
            fixture.lock.writeText(originalLock)

            fixture.lock.writeText(originalLock.replace("utils/Fixture.kt", "utils/Missing.kt"))
            fixture.rejects("source inventory drift")
            fixture.lock.writeText(originalLock)

            val licenseHash = fixture.upstream
                .resolve("LICENSE")
                .readBytes()
                .sha256()
            fixture.lock.writeText(originalLock.replace(licenseHash, "0".repeat(64)))
            fixture.rejects("LICENSE changed")
            fixture.lock.writeText(originalLock)

            fixture.lock.writeText(originalLock.replace("\"policyVersion\": 1", "\"policyVersion\": 77"))
            fixture.rejects("Unsupported MCU lock schema or transformation policy")
            fixture.lock.writeText(originalLock)

            fixture.build().expect(TaskOutcome.FROM_CACHE)
            assertEquals(originalSource, fixture.source.readText())
            assertEquals("", git(fixture.upstream, "status", "--porcelain"))
        }

    @Test
    fun offlineRelocatedCheckoutReusesSharedCacheAndProducesIdenticalManifest() =
        fixtures { sandbox ->
            val original = sandbox.checkout("original")
            original.build().expect(TaskOutcome.SUCCESS)
            val expectedSource = original.generated.readText()
            val expectedReport = original.report.readText()

            val relocated = sandbox.checkout("another path with spaces")
            val restored = relocated.build()
            restored.expect(TaskOutcome.FROM_CACHE)
            restored.expectVerification()
            assertEquals(expectedSource, relocated.generated.readText())
            assertEquals(expectedReport, relocated.report.readText())
            assertFalse(relocated.report.readText().contains(sandbox.directory.absolutePath))
            assertEquals("", git(relocated.upstream, "status", "--porcelain"))
        }

    private fun BuildResult.expect(outcome: TaskOutcome) {
        assertEquals(outcome, assertNotNull(task(":generateFixture"), output).outcome, output)
        expectVerification()
    }

    private fun BuildResult.expectVerification() {
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(task(":verifyFixture"), output).outcome, output)
    }

    private fun BuildResult.expectRegeneration() {
        val outcome = assertNotNull(task(":generateFixture"), output).outcome
        assertTrue(outcome == TaskOutcome.SUCCESS || outcome == TaskOutcome.FROM_CACHE, output)
        expectVerification()
    }

    private fun fixtures(test: (Sandbox) -> Unit) = temporaryDirectory("mcu-gradle-lifecycle-") { test(Sandbox(it)) }

    private class Sandbox(
        val directory: File,
    ) {
        private val sourceRepository = directory.resolve("source-repository")
        private val buildCache = directory.resolve("shared-build-cache")
        private val toolClasspath = snapshotClasspath("mcu.toolClasspath", "tool-classpath")

        // The compiled production task classes and their standard library, taken as a jar rather
        // than as sources: the fixture runs offline with an empty Gradle user home, so it could
        // not resolve a Kotlin compiler, and the jar is the exact bytecode the real build runs.
        private val taskClasspath = snapshotClasspath("mcu.taskClasspath", "task-classpath")

        /** Copies a snapshot classpath under [root] and renders it as a Groovy file list. */
        private fun relocate(classpath: List<File>, root: File, into: String): String = classpath
            .mapIndexed { index, source ->
                val relocated = root.resolve("$into/$index/${source.name}")
                if (source.exists()) source.copyRecursively(relocated)
                quote(relocated.absolutePath)
            }.joinToString(", ")

        private fun snapshotClasspath(property: String, into: String): List<File> = requiredProperty(property)
            .split(File.pathSeparator)
            .mapIndexed { index, path ->
                val source = File(path)
                val snapshot = directory.resolve("$into/$index/${source.name}")
                // Keep inputs stable even when another build recompiles the tool in this checkout.
                if (source.exists()) source.copyRecursively(snapshot)
                snapshot
            }

        init {
            check(taskClasspath.any { it.name == "convention.jar" && it.isFile }) {
                "Missing production task classes in ${taskClasspath.joinToString(File.pathSeparator)}"
            }
            sourceRepository.resolve("kotlin/utils").mkdirs()
            sourceRepository.resolve("kotlin/utils/Fixture.kt").writeText(SOURCE)
            sourceRepository.resolve("LICENSE").writeText("Fixture license retained verbatim.\n")
            git(sourceRepository, "init", "--quiet")
            git(sourceRepository, "add", ".")
            git(sourceRepository, "commit", "--quiet", "-m", "Create audited fixture")
        }

        fun checkout(name: String): Fixture {
            val root = directory.resolve(name).apply { mkdirs() }
            git(root, "init", "--quiet")
            git(
                root,
                "-c",
                "protocol.file.allow=always",
                "submodule",
                "add",
                "--quiet",
                sourceRepository.absolutePath,
                "upstream",
            )
            val fixture = Fixture(root)
            fixture.reviewCurrentPin()
            root.resolve("settings.gradle").writeText(
                """
                rootProject.name = 'mcu-generation-fixture'
                buildCache { local { directory = file(${quote(buildCache.absolutePath)}) } }
                """.trimIndent(),
            )
            root.resolve("gradle.properties").writeText(
                "org.gradle.jvmargs=-Xmx256m -Dfile.encoding=UTF-8\norg.gradle.workers.max=1\n",
            )
            val fixtureClasspath = relocate(toolClasspath, root, "tool-runtime")
            val fixtureTaskClasspath = relocate(taskClasspath, root, "task-runtime")
            root.resolve("build.gradle").writeText(
                """
                buildscript { dependencies { classpath files($fixtureTaskClasspath) } }
                def verify = tasks.register('verifyFixture', com.materialkolor.convention.mcu.VerifyMcuUpstream) {
                    repositoryDirectory.set(layout.projectDirectory)
                    upstreamDirectory.set(layout.projectDirectory.dir('upstream'))
                    lockFile.set(layout.projectDirectory.file('gradle/mcu-upstream.lock.json'))
                }
                tasks.register('generateFixture', com.materialkolor.convention.mcu.GenerateMcuSources) {
                    dependsOn(verify)
                    sourceDirectory.set(layout.projectDirectory.dir('upstream/kotlin'))
                    lockFile.set(layout.projectDirectory.file('gradle/mcu-upstream.lock.json'))
                    licenseFile.set(layout.projectDirectory.file('upstream/LICENSE'))
                    outputDirectory.set(layout.buildDirectory.dir('generated'))
                    reportFile.set(layout.buildDirectory.file('generation-report.tsv'))
                    mode.set('reference')
                    parserVersion.set(${quote(requiredProperty("mcu.parserVersion"))})
                    classpath = files($fixtureClasspath)
                }
                """.trimIndent(),
            )
            return fixture
        }
    }

    private class Fixture(
        val root: File,
    ) {
        val upstream = root.resolve("upstream")
        val source = upstream.resolve("kotlin/utils/Fixture.kt")
        val lock = root.resolve("gradle/mcu-upstream.lock.json")
        val output = root.resolve("build/generated")
        val generated = output.resolve("utils/Fixture.kt")
        val report = root.resolve("build/generation-report.tsv")

        fun revision(): String = git(upstream, "rev-parse", "HEAD")

        fun commitUpstream(message: String) {
            git(upstream, "add", ".")
            git(upstream, "commit", "--quiet", "-m", message)
        }

        fun reviewCurrentPin() {
            git(root, "add", "upstream")
            lock.parentFile.mkdirs()
            lock.writeText(
                """
                {
                  "schemaVersion": 1,
                  "policyVersion": 1,
                  "upstreamRevision": "${revision()}",
                  "licenseSha256": "${upstream.resolve("LICENSE").readBytes().sha256()}",
                  "files": {
                    "utils/Fixture.kt": "${source.readBytes().sha256()}"
                  }
                }
                """.trimIndent() + "\n",
            )
        }

        fun build(): BuildResult = runner().build()

        fun rejects(message: String) {
            val result = runner().buildAndFail()
            assertEquals(
                TaskOutcome.FAILED,
                assertNotNull(result.task(":verifyFixture"), result.output).outcome,
                result.output,
            )
            assertEquals(
                null,
                result.task(":generateFixture"),
                "Generation/cache reuse must not follow failed provenance:\n${result.output}",
            )
            assertTrue(result.output.contains(message), result.output)
        }

        private fun runner(): GradleRunner =
            GradleRunner
                .create()
                .withProjectDir(root)
                .withArguments(
                    "generateFixture",
                    "--offline",
                    "--build-cache",
                    "--configuration-cache",
                    "--stacktrace",
                    "--console=plain",
                    "--max-workers=1",
                    "-Dorg.gradle.java.installations.auto-download=false",
                )
    }

    private companion object {
        const val SOURCE = "// Upstream fixture comment.\npackage utils\nclass Fixture { val value: Int = 1 }\n"

        fun quote(value: String): String = "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"

        fun git(
            directory: File,
            vararg arguments: String,
        ): String {
            val output = File.createTempFile("mcu-fixture-git-", ".log")
            try {
                val command = listOf(
                    "git",
                    "-c",
                    "user.name=MCU fixture",
                    "-c",
                    "user.email=mcu-fixture@example.invalid",
                    "-c",
                    "commit.gpgsign=false",
                    "-C",
                    directory.absolutePath,
                ) + arguments
                val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output).start()
                try {
                    check(process.waitFor(30, TimeUnit.SECONDS)) { "Fixture Git command timed out: $command" }
                    check(process.exitValue() == 0) { "Fixture Git command failed: $command\n${output.readText()}" }
                    return output.readText().trim()
                } finally {
                    // A timed-out child would otherwise outlive the test run.
                    process.destroyForcibly()
                }
            } finally {
                output.delete()
            }
        }
    }
}
