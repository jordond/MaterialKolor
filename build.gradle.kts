plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.multiplatform.android.library) apply false
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

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootDir.resolve("dokka"))
    }
}

dependencies {
    dokka(project(":material-color-utilities"))
    dokka(project(":material-kolor"))
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
