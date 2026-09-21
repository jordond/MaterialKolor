package com.materialkolor.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.version(name: String): String = libs.findVersion(name).get().toString()

internal fun Project.intVersion(name: String): Int = version(name).toInt()
