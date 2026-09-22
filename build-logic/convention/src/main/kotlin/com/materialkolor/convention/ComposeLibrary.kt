package com.materialkolor.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@OptIn(ExperimentalWasmDsl::class)
internal fun Project.configureComposeLibrary() {
    extensions.configure<KotlinMultiplatformExtension> {
        // Compose UI tests on the web need a webpack bundle so Skiko can load, see CMP-4906.
        js {
            binaries.executable()
        }

        wasmJs {
            binaries.executable()
        }

        sourceSets.commonMain.dependencies {
            implementation(library("compose-runtime"))
            implementation(library("compose-ui"))
            implementation(library("compose-foundation"))
        }
    }
}
