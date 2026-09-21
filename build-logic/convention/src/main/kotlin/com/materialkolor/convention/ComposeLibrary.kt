package com.materialkolor.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureComposeLibrary() {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.commonMain.dependencies {
            implementation(library("compose-runtime"))
            implementation(library("compose-ui"))
            implementation(library("compose-foundation"))
        }
    }
}
