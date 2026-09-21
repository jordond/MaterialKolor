@file:Suppress("OPT_IN_USAGE")

import com.materialkolor.convention.mcu.GenerateMcuSources
import com.materialkolor.convention.mcu.VerifyMcuParserAgreement

plugins {
    id("materialkolor.library")
    alias(libs.plugins.poko)
}

fun Project.transformerClasspath(name: String): Configuration =
    configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME)) }
    }

val mcuTransformer = transformerClasspath("mcuTransformer")

// Running the same rules through a newer front end is what proves the output is not an artifact of
// one compiler version, so this copy pins the catalogue's Kotlin instead of the reviewed parser.
val alternateMcuParser = transformerClasspath("alternateMcuParser").apply {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
}

dependencies {
    mcuTransformer(project(":mcu-source-transformer"))
    alternateMcuParser(project(":mcu-source-transformer"))
}

// The reviewed inputs every generation reads. `:mcu-upstream` pins the same three for the reference tree it generates.
val upstreamSources = rootProject.layout.projectDirectory.dir("tools/mcu-upstream/src/main/kotlin")
val upstreamLicense = rootProject.layout.projectDirectory.file("tools/mcu-upstream/src/main/LICENSE")
val sourceLock = rootProject.layout.projectDirectory.file("gradle/mcu-upstream.lock.json")

val generateMcuSources = tasks.register<GenerateMcuSources>("generateMcuSources") {
    dependsOn(rootProject.tasks.named("verifyMcuUpstream"))
    classpath = mcuTransformer
    sourceDirectory.set(upstreamSources)
    licenseFile.set(upstreamLicense)
    lockFile.set(sourceLock)
    outputDirectory.set(layout.buildDirectory.dir("generated/mcu/commonMain"))
    reportFile.set(layout.buildDirectory.file("reports/mcu-sources.tsv"))
    parserVersion.set(libs.versions.mcu.psi)
}

val generateMcuWithAlternateParser = tasks.register<GenerateMcuSources>("generateMcuWithAlternateParser") {
    dependsOn(rootProject.tasks.named("verifyMcuUpstream"))
    classpath = alternateMcuParser
    sourceDirectory.set(upstreamSources)
    licenseFile.set(upstreamLicense)
    lockFile.set(sourceLock)
    outputDirectory.set(layout.buildDirectory.dir("generated/mcu-alternate/commonMain"))
    reportFile.set(layout.buildDirectory.file("reports/mcu-alternate.tsv"))
    parserVersion.set(libs.versions.kotlin)
}

tasks.register<VerifyMcuParserAgreement>("verifyMcuParserAgreement") {
    group = "verification"
    description = "Check that the pinned and alternate parser generations produced identical sources."
    primaryDirectory.set(generateMcuSources.flatMap { task -> task.outputDirectory })
    alternateDirectory.set(generateMcuWithAlternateParser.flatMap { task -> task.outputDirectory })
}

kotlin {
    android {
        namespace = "com.materialkolor.colorutilities"
    }

    js {
        nodejs { testTask { useMocha { timeout = "120s" } } }
    }

    wasmJs {
        nodejs { testTask { useMocha { timeout = "120s" } } }
    }

    sourceSets {
        commonMain { kotlin.srcDir(generateMcuSources.flatMap { it.outputDirectory }) }

        jvmTest.dependencies {
            implementation(project(":mcu-upstream"))
            // The namespace-only Kotlin reference, which `:mcu-upstream` carries as test fixtures.
            implementation(project.dependencies.testFixtures(project(":mcu-upstream")))
        }
    }
}

tasks.matching { it.name.startsWith("dokka") || it.name.endsWith("SourcesJar") }.configureEach {
    dependsOn(generateMcuSources)
}
