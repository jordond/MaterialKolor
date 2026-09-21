package com.materialkolor.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

@OptIn(ExperimentalAbiValidation::class, ExperimentalWasmDsl::class)
internal fun Project.configureKmpLibrary() {
    val moduleName = name

    extensions.configure<KotlinMultiplatformExtension> {
        applyDefaultHierarchyTemplate()

        abiValidation()

        androidLibraryTarget()?.apply {
            compileSdk = intVersion("sdk-compile")
            minSdk = intVersion("sdk-min-library")

            withHostTest {}

            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }

        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }

        js {
            browser { testTask { useKarma { useChromeHeadless() } } }
        }

        wasmJs {
            browser { testTask { useKarma { useChromeHeadless() } } }
        }

        macosArm64()

        listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = moduleName
            }
        }

        sourceSets.commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmToolchain(intVersion("jvmTarget"))
    }
}

private fun KotlinMultiplatformExtension.androidLibraryTarget(): KotlinMultiplatformAndroidLibraryTarget? =
    (this as ExtensionAware).extensions.findByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
