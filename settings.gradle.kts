pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
    }

    includeBuild("build-logic")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("com.gradle.develocity") version "4.5.1"
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")

        publishing.onlyIf { context ->
            context.buildResult.failures.isNotEmpty() && !System.getenv("CI").isNullOrEmpty()
        }
    }
}

rootProject.name = "MaterialKolor"

include(
    ":material-kolor-core",
    ":material-kolor-material3",
    ":material-kolor-palette",
    ":material-kolor-unstyled",
    ":material-kolor-fluent",
    ":material-color-utilities",
    ":mcu-upstream",
    ":builder:shared",
    ":builder:android",
    ":samples:custom-theme",
)

include(":mcu-source-transformer")
project(":mcu-source-transformer").projectDir = file("tools/mcu-source-transformer")
project(":mcu-upstream").projectDir = file("tools/mcu-upstream")
