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

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "builder-js.js"
            }

            testTask {
                useKarma { useChromeHeadless() }
            }
        }

        binaries.executable()
    }

    sourceSets {
        webMain.dependencies {
            implementation(project(":builder:shared"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.kotlinx.browser)
            implementation(libs.kermit)
        }

        webTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
