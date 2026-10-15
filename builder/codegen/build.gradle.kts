plugins {
    id("materialkolor.builder.kotlin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":builder:domain"))
        }
    }
}

// The golden tests compare by default. Pass -Pgolden.update=true to rewrite the goldens and their hashes instead.
tasks.named<Test>("jvmTest") {
    systemProperty("golden.update", providers.gradleProperty("golden.update").getOrElse("false"))

    val repo = rootProject.layout.projectDirectory
    val libraryModules = listOf(
        "material-color-utilities",
        "material-kolor-core",
        "material-kolor-material3",
        "material-kolor-unstyled",
        "material-kolor-fluent",
        "material-kolor-palette",
    )
    val sourceRoots = listOf("samples", "builder/shared", "builder/kit", "builder/preview")
    inputs
        .files(
            libraryModules.map { module ->
                fileTree(repo.dir(module)) {
                    include("api/jvm/*.api", "**/*.kt")
                    exclude("**/build/**")
                }
            },
            sourceRoots.map { root ->
                fileTree(repo.dir(root)) {
                    include("**/*.kt")
                    exclude("**/build/**")
                }
            },
            repo.file("README.md"),
        ).withPropertyName("repoSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// Lays every golden case into the fixture projects of builder/codegen-check, which compiles them against the local
// modules with `./gradlew -p builder/codegen-check compileKotlinJvm`.
tasks.register<JavaExec>("writeCompileFixtures") {
    group = "verification"
    description = "Writes the compile check fixtures of builder/codegen-check from every golden case."
    val testCompilation = kotlin.jvm().compilations.getByName("test")
    classpath(testCompilation.output.allOutputs, testCompilation.runtimeDependencyFiles)
    mainClass.set("com.materialkolor.builder.codegen.check.CompileFixturesKt")
    args(rootProject.layout.projectDirectory.dir("builder/codegen-check/build/fixtures").asFile.absolutePath)
}
