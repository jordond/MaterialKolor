import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    id("materialkolor.builder.web")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "builder.js"
            }

            testTask {
                useKarma { useChromeHeadless() }
            }
        }

        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":builder:app"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.kotlinx.browser)
            implementation(libs.kermit)
        }

        wasmJsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
