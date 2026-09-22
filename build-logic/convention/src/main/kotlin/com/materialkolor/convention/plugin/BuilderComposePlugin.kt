package com.materialkolor.convention.plugin

import com.materialkolor.convention.BuilderWasmRuntime
import com.materialkolor.convention.configureBuilderCompose
import com.materialkolor.convention.configureBuilderModule
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The Compose half of the builder, engine through to the app.
 *
 * The app and the web module turn [org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension.explicitApi]
 * back off in their own build files.
 */
class BuilderComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        target.configureBuilderModule(BuilderWasmRuntime.Browser)
        target.configureBuilderCompose()
    }
}
