import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The site shell. It is the one module that talks to the browser, so it has a single target and
// skips the builder convention, which would add a JVM target it has no use for.
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "builder.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":builder:app"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.kstore.storage)
            implementation(libs.kotlinx.browser)
            implementation(libs.kermit)
        }
    }
}

// b-220
// The interop tests are plain DOM checks with no Compose scene. `wasmJsBrowserTest` runs them in
// headless Chrome, the same way `:builder:app` runs its browser test.
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
    }

    sourceSets {
        wasmJsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
