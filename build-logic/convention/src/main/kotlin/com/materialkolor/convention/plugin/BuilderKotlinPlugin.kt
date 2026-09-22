package com.materialkolor.convention.plugin

import com.materialkolor.convention.BuilderWasmRuntime
import com.materialkolor.convention.configureBuilderModule
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The plain Kotlin half of the builder, domain and codegen.
 *
 * These modules never see Compose, so their wasm tests run on node.
 */
class BuilderKotlinPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")

        target.configureBuilderModule(BuilderWasmRuntime.NodeJs)
    }
}
