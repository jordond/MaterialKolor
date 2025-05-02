import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.poko) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.binaryCompatibility)
    alias(libs.plugins.spotless)
}

apiValidation {
    ignoredProjects.addAll(
        listOf("mcu-upstream"),
    )
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory.set(rootDir.resolve("dokka"))
}

subprojects {
    apply {
        plugin(rootProject.libs.plugins.spotless.get().pluginId)
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            ktlint(libs.versions.ktlint.get()).setEditorConfigPath("${project.rootDir}/.editorconfig")
            target("**/*.kt")
            targetExclude(
                "${layout.buildDirectory}/**/*.kt",
            )
            toggleOffOn()
            endWithNewline()
        }
    }
}
