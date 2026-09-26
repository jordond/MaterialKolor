plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    jvm()

    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":builder:shared"))

            implementation(compose.desktop.currentOs)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.materialkolor.builder.desktop.MainKt"
    }
}

// stateholder 3.1.0 ships Java 21 bytecode, so the desktop `run` task starts on a 21 launcher while
// the module still compiles for 17. Compose registers `run` after evaluation and points it at the JDK
// Gradle runs on. This swaps in the 21 launcher when the task is configured, so only a build that
// runs the desktop app looks it up.
val java21Launcher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
afterEvaluate {
    tasks.named<JavaExec>("run") {
        executable(java21Launcher.get().executablePath.asFile.absolutePath)
    }
}
