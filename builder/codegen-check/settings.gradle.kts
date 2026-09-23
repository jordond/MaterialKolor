// The compile check of the builder's export. `./gradlew :builder:codegen:writeCompileFixtures` lays every golden
// case into a project under build/fixtures, one per target and mode, and this build compiles them against the
// local MaterialKolor modules, since the 6.0.0 coordinates the exports name are not published yet.
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

val fixtures = file("build/fixtures")
val catalog = File(fixtures, "libs.versions.toml")
check(catalog.isFile) { "There are no compile fixtures yet, run ./gradlew :builder:codegen:writeCompileFixtures first" }

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        // The repository's own catalog, for the plugins and for what every Compose project has anyway.
        create("repo") { from(files("../../gradle/libs.versions.toml")) }
        // The catalogs the cases generate, merged.
        create("libs") { from(files(catalog)) }
    }
}

rootProject.name = "codegen-check"

fixtures
    .listFiles { dir -> File(dir, "build.gradle.kts").isFile }
    .orEmpty()
    .sortedBy { dir -> dir.name }
    .forEach { dir ->
        include(":${dir.name}")
        project(":${dir.name}").projectDir = dir
    }

includeBuild("../..") {
    dependencySubstitution {
        listOf("core", "material3", "unstyled", "fluent", "palette").forEach { name ->
            substitute(module("com.materialkolor:material-kolor-$name")).using(project(":material-kolor-$name"))
        }
    }
}
