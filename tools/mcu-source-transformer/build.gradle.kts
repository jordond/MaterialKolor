plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.mcu.psi)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test-junit"))
    testImplementation(gradleTestKit())
}

kotlin {
    jvmToolchain(17)
}

val alternateParser = configurations.create("alternateParser")

dependencies {
    alternateParser("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
}

tasks.register<Test>("testAlternateParser") {
    description = "Smoke-tests the independently pinned parser upgrade against the same transformer binary."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath.filter {
        !it.name.startsWith("kotlin-compiler-embeddable-")
    } + alternateParser
    exclude("**/GenerationLifecycleTest*")
    shouldRunAfter(tasks.test)
}

// The lifecycle test builds disposable Gradle projects that register the production task classes.
// Those projects run offline with an empty Gradle user home, so the compiled convention jar and a
// standard library for it are handed over as files instead of being resolved by the fixture.
val fixtureTaskRuntime = configurations.create("fixtureTaskRuntime")

dependencies {
    fixtureTaskRuntime("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
}

val conventionJar = rootProject.file("build-logic/convention/build/libs/convention.jar")

tasks.withType<Test>().configureEach {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:jar"))
    systemProperty("mcu.projectRoot", rootProject.projectDir.absolutePath)
    systemProperty("mcu.toolClasspath", sourceSets.main.get().runtimeClasspath.asPath)
    systemProperty("mcu.taskClasspath", (files(conventionJar) + fixtureTaskRuntime).asPath)
    systemProperty("mcu.parserVersion", libs.versions.mcu.psi.get())
}
