plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation(project(":samples:shared"))
    implementation(project(":samples:custom-theme"))
    implementation(project(":samples:fluent"))
    implementation(project(":samples:unstyled"))
}

tasks.register<JavaExec>("screenshots") {
    val outputDir = layout.buildDirectory.dir("screenshots").get().asFile
    val readmeDir = layout.projectDirectory.dir("images").asFile

    group = "samples"
    description = "Renders every sample in light and dark into build/screenshots, and the README shots into images."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.materialkolor.sample.screenshots.MainKt"
    systemProperty("java.awt.headless", "true")
    args(outputDir.absolutePath, readmeDir.absolutePath)
}
