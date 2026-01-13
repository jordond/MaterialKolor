@file:Suppress("OPT_IN_USAGE")

import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.multiplatform.android.library)
    alias(libs.plugins.poko)
    alias(libs.plugins.dokka)
    alias(libs.plugins.publish)
}

kotlin {
    explicitApi()

    applyDefaultHierarchyTemplate()

    @Suppress("UnstableApiUsage")
    androidLibrary {
        compileSdk = libs.versions.sdk.compile.get().toInt()
        minSdk = libs.versions.sdk.min.get().toInt()
        namespace = "com.materialkolor.colorutilities"

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js(IR) {
        browser()
    }

    wasmJs {
        browser()
    }

    macosX64()
    macosArm64()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "material-color-utilities"
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}
