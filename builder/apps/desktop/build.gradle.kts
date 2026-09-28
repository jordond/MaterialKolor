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

val java21Launcher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
afterEvaluate {
    tasks.named<JavaExec>("run") {
        executable(java21Launcher.get().executablePath.asFile.absolutePath)
    }
}
