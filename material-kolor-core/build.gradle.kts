import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("materialkolor.library")
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.poko)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    explicitApi()

    @Suppress("UnstableApiUsage")
    android {
        namespace = "com.materialkolor"

        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.file("consumer-rules.pro")
        }
    }

    js {
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
    }

    sourceSets {
        all {
            languageSettings {
                optIn("com.materialkolor.InternalMaterialKolorApi")
            }
        }

        commonMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.colormath)

            api(project(":material-color-utilities"))
            api(libs.kotlinx.serialization.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
