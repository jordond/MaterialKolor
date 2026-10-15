package com.materialkolor.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Registers the settings a module can override and the callback that reads them.
 *
 * Called before the Kotlin and Android plugins are applied, so the callback registered here runs
 * before theirs. That is what lets it still add a target and still write the Android floor.
 */
internal fun Project.registerMaterialKolorLibraryExtension(): MaterialKolorLibraryExtension {
    val settings = extensions.create<MaterialKolorLibraryExtension>("materialKolorLibrary")
    settings.macos.convention(true)
    settings.minSdk.convention(intVersion("sdk-min-library"))
    settings.jvmTarget.convention(JvmTarget.JVM_11)

    afterEvaluate {
        extensions.configure<KotlinMultiplatformExtension> {
            if (settings.macos.get()) {
                macosArm64()
            }

            androidLibraryTarget()?.minSdk = settings.minSdk.get()
        }
    }

    return settings
}

@OptIn(ExperimentalAbiValidation::class, ExperimentalWasmDsl::class)
internal fun Project.configureKmpLibrary(settings: MaterialKolorLibraryExtension) {
    val moduleName = name

    extensions.configure<KotlinMultiplatformExtension> {
        applyDefaultHierarchyTemplate()

        explicitApi()

        abiValidation()

        androidLibraryTarget()?.apply {
            compileSdk = intVersion("sdk-compile")

            withHostTest {}

            compilerOptions {
                jvmTarget.set(settings.jvmTarget)
            }
        }

        jvm {
            compilerOptions {
                jvmTarget.set(settings.jvmTarget)
            }
        }

        js {
            browser { testTask { useKarma { useChromeHeadless() } } }
        }

        wasmJs {
            browser { testTask { useKarma { useChromeHeadless() } } }
        }

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

    // R8 names the rewritten .kotlin_module file after the module name in the Kotlin metadata, and
    // AGP rejects the default `MaterialKolor:<module>` name when it packages a minified release bundle.
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.moduleName.set(compilerOptions.moduleName.get().replace(':', '_'))
    }
}

private fun KotlinMultiplatformExtension.androidLibraryTarget(): KotlinMultiplatformAndroidLibraryTarget? =
    (this as ExtensionAware).extensions.findByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
