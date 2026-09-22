package com.materialkolor.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Which wasm runtime a builder module is compiled and tested against.
 *
 * Only the site ships, so the modules that hold no UI run their tests on node, which is quicker to
 * start and needs no headless browser.
 */
internal enum class BuilderWasmRuntime {
    NodeJs,
    Browser,
}

/**
 * Targets and test wiring shared by every builder module.
 *
 * The builder is not published, so this deliberately does not go through the library convention.
 */
@OptIn(ExperimentalWasmDsl::class)
internal fun Project.configureBuilderModule(runtime: BuilderWasmRuntime) {
    extensions.configure<KotlinMultiplatformExtension> {
        applyDefaultHierarchyTemplate()

        explicitApi()

        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(version("jvmTarget")))
            }
        }

        wasmJs {
            when (runtime) {
                BuilderWasmRuntime.NodeJs -> nodejs()
                BuilderWasmRuntime.Browser -> browser()
            }
        }

        sourceSets.commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(library("kotest-assertions"))
        }

        jvmToolchain(intVersion("jvmTarget"))
    }
}

/**
 * The Compose additions on top of [configureBuilderModule].
 *
 * The stability file lives beside the builder modules because the types it names come from the
 * library and from domain, neither of which runs the Compose plugin.
 */
internal fun Project.configureBuilderCompose() {
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("builder/compose-stability.conf"),
        )
    }

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
