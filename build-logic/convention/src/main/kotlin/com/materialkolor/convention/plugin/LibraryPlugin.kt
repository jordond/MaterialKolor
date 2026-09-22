package com.materialkolor.convention.plugin

import com.materialkolor.convention.configureKmpLibrary
import com.materialkolor.convention.configureMcuPublishing
import com.materialkolor.convention.registerMaterialKolorLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class LibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val settings = target.registerMaterialKolorLibraryExtension()

        with(target.pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.android.kotlin.multiplatform.library")
            apply("org.jetbrains.dokka")
            apply("com.vanniktech.maven.publish")
        }

        target.configureKmpLibrary(settings)
        target.configureMcuPublishing()
    }
}
