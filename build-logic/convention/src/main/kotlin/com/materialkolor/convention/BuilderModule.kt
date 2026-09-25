package com.materialkolor.convention

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.time.Duration

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

    configureTestTimeout()
}

/**
 * Fails a builder test task that runs far past its normal time instead of letting it hang.
 *
 * The slowest suite, app's jvmTest, takes about 80 seconds, so five minutes means a test that
 * never goes idle, not a slow machine. Pass `-Pbuilder.testTimeoutMinutes=<n>` to change it.
 */
private fun Project.configureTestTimeout() {
    val minutes = providers.gradleProperty("builder.testTimeoutMinutes").map(String::toLong).orElse(5L)
    tasks.withType<Test>().configureEach {
        timeout.set(minutes.map(Duration::ofMinutes))
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
        // The Compose compiler plugin needs the runtime on the compile classpath of every module it
        // processes, even the ones that hold no composables.
        sourceSets.commonMain.dependencies {
            implementation(library("compose-runtime"))
        }

        sourceSets.jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
