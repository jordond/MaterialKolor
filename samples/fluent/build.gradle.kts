import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.multiplatform.android.library)
}

kotlin {
    applyDefaultHierarchyTemplate()
    explicitApi()

    android {
        namespace = "com.materialkolor.sample.fluent"
        compileSdk = libs.versions.sdk.compile.get().toInt()
        minSdk = libs.versions.sdk.min.library.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)

            implementation(project(":material-kolor-fluent"))
            implementation(libs.fluent)
            // Fluent v0.1.0 brings haze 1.6.6, which was built for Compose 1.8 and calls a ShaderBrush method
            // that Compose 1.12 changed. Haze 1.7.3 is built for Compose 1.12 and keeps the API Fluent uses.
            implementation(libs.haze)
            implementation(project(":samples:shared"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.materialkolor.sample.fluent.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}
