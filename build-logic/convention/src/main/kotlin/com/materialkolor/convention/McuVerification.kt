package com.materialkolor.convention

import com.materialkolor.convention.mcu.CandidateMcuUpstreamLock
import com.materialkolor.convention.mcu.VerifyMcuPublication
import com.materialkolor.convention.mcu.VerifyMcuUpstream
import com.materialkolor.convention.mcu.mcuLibraryModules
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

internal fun Project.registerMcuVerificationTasks() {
    tasks.register<VerifyMcuUpstream>("verifyMcuUpstream") {
        group = "verification"
        description = "Validate the live upstream Gitlink, revision, clean tree and reviewed source hashes."
        repositoryDirectory.set(layout.projectDirectory)
        upstreamDirectory.set(layout.projectDirectory.dir("tools/mcu-upstream/src/main"))
        lockFile.set(layout.projectDirectory.file("gradle/mcu-upstream.lock.json"))
    }

    tasks.register<CandidateMcuUpstreamLock>("candidateMcuUpstreamLock") {
        group = "maintenance"
        description = "Write a candidate source lock under build for explicit upstream-update review."
        upstreamDirectory.set(layout.projectDirectory.dir("tools/mcu-upstream/src/main"))
        candidateFile.set(layout.buildDirectory.file("mcu-upstream.lock.candidate.json"))
    }

    // String task paths are resolved strictly, so a missing platform check fails the aggregate.
    tasks.register("verifyMcuJvm") {
        group = "verification"
        description = "Run the transformer, parity and JVM library tests."
        dependsOn(":mcu-source-transformer:test", ":mcu-source-transformer:testAlternateParser", ":mcu-upstream:test")
        dependsOn(":material-color-utilities:verifyMcuParserAgreement")
        for (module in mcuLibraryModules) {
            dependsOn(":$module:jvmTest")
        }
    }

    tasks.register("verifyMcuWeb") {
        group = "verification"
        description = "Run the library tests on Node and in a headless browser, for JS and Wasm."
        dependsOn(":material-color-utilities:jsNodeTest", ":material-color-utilities:wasmJsNodeTest")
        for (module in mcuLibraryModules) {
            dependsOn(":$module:jsBrowserTest", ":$module:wasmJsBrowserTest")
        }
    }

    tasks.register("verifyMcuAndroid") {
        group = "verification"
        description = "Assemble the Android variants, run their host tests and lint them."
        for (module in mcuLibraryModules) {
            dependsOn(
                ":$module:assembleAndroidMain",
                ":$module:testAndroidHostTest",
                ":$module:lintAnalyzeAndroidHostTest",
            )
        }
    }

    tasks.register("verifyMcuApple") {
        group = "verification"
        description = "Run the macOS and simulator tests, then compile and link every published Apple target."
        for (module in mcuLibraryModules) {
            dependsOn(
                ":$module:macosArm64Test", ":$module:iosSimulatorArm64Test",
                ":$module:compileKotlinIosArm64", ":$module:linkDebugFrameworkIosArm64",
                ":$module:linkDebugFrameworkIosSimulatorArm64",
            )
        }
    }

    tasks.register<VerifyMcuPublication>("verifyMcuPublication") {
        group = "verification"
        description =
            "Inspect file-repository publications, source archives and docs, then compile an artifact-only consumer."
        dependsOn("dokkaGenerate")

        for (module in mcuLibraryModules) {
            dependsOn(":$module:publishAllPublicationsToMcuVerificationRepository")
        }

        repositoryDirectory.set(layout.buildDirectory.dir("mcu-verification-repository"))
        consumerDirectory.set(layout.projectDirectory.dir("tools/publication-consumer"))
        consumerBuildDirectory.set(layout.buildDirectory.dir("mcu-publication-consumer"))
        lockFile.set(layout.projectDirectory.file("gradle/mcu-upstream.lock.json"))
        generatedReport.set(project(":material-color-utilities").layout.buildDirectory.file("reports/mcu-sources.tsv"))
        gradleWrapper.set(layout.projectDirectory.file("gradlew"))
        artifactVersion.set(providers.gradleProperty("VERSION_NAME").orElse("0.0.0-SNAPSHOT"))
        kotlinVersion.set(version("kotlin"))
    }
}
