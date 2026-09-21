package com.materialkolor.convention.plugin

import com.materialkolor.convention.configureComposeLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project

class ComposeLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("materialkolor.library")
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        target.configureComposeLibrary()
    }
}
