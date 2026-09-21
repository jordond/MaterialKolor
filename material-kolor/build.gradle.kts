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
        commonMain.dependencies {
            implementation(libs.compose.material3.get().toString()) {
                exclude(group = "androidx.compose.material3")
            }
            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.colormath)

            api(project(":material-color-utilities"))
            api(libs.kotlinx.serialization.core)
        }

        androidMain.dependencies {
            compileOnly(libs.androidx.compose.material3)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.androidx.compose.material3)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
