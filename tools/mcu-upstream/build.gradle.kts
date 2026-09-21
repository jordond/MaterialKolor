import com.materialkolor.convention.mcu.GenerateMcuSources

plugins {
    id("java-library")
    id("java-test-fixtures")
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

val mcuTransformer = configurations.create("mcuTransformer") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME)) }
}

// The generated header records the reviewed pin, so the revision is read from the lock at generation time instead of
// being hard-coded in the generator.
val sourceLock = rootProject.layout.projectDirectory.file("gradle/mcu-upstream.lock.json")

val upstreamSources = rootProject.layout.projectDirectory.dir("tools/mcu-upstream/src/main/kotlin")
val upstreamLicense = rootProject.layout.projectDirectory.file("tools/mcu-upstream/src/main/LICENSE")

val generateMcuReference = tasks.register<GenerateMcuSources>("generateMcuReference") {
    description = "Relocates the pinned upstream Kotlin into upstream.kotlin.* for the conformance tests."
    dependsOn(rootProject.tasks.named("verifyMcuUpstream"))
    classpath = mcuTransformer
    sourceDirectory.set(upstreamSources)
    licenseFile.set(upstreamLicense)
    lockFile.set(sourceLock)
    outputDirectory.set(layout.buildDirectory.dir("generated/mcu/reference"))
    reportFile.set(layout.buildDirectory.file("reports/mcu-reference.tsv"))
    mode.set("reference")
    parserVersion.set(libs.versions.mcu.psi)
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java"))
        }

        kotlin {
            // Exclude Kotlin sources from the upstream submodule
            setSrcDirs(emptyList<String>())
        }
    }

    testFixtures {
        kotlin {
            setSrcDirs(emptyList<String>())
            srcDir(generateMcuReference.flatMap { it.outputDirectory })
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.errorProneAnnotations)

    mcuTransformer(project(":mcu-source-transformer"))

    testImplementation(kotlin("test"))
    testImplementation(project(":material-color-utilities"))
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava { dependsOn(rootProject.tasks.named("verifyMcuUpstream")) }

tasks.register<JavaExec>("printMcuRoleGoldens") {
    group = "maintenance"
    description = "Regenerate the dynamic color role fixtures from the unmodified Kotlin reference."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("conformance.GenerateGoldenFixtures")
    args(sourceLock.asFile.path)
}

tasks.register<JavaExec>("printMcuQuantizerGoldens") {
    group = "maintenance"
    description = "Regenerate the quantizer fixtures from the unmodified Kotlin reference."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("conformance.GenerateQuantizerFixtures")
    args(sourceLock.asFile.path)
}
