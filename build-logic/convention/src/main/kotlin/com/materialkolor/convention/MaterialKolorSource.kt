package com.materialkolor.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * The MaterialKolor [module] a builder module depends on, named without its `material-kolor-` prefix.
 *
 * By default it is the module in this repo, so a library change shows up in the builder right away.
 * With `-Pmaterialkolor.useLocal=false` it is the published module at `materialKolorExport`, the version
 * every export pins. The production builder is built that way, so it draws each theme with the same
 * library the exported code compiles against.
 */
fun Project.materialKolor(module: String): Dependency =
    if (useLocalMaterialKolor) {
        dependencies.project(mapOf("path" to ":material-kolor-$module"))
    } else {
        dependencies.create("com.materialkolor:material-kolor-$module:${version("materialKolorExport")}")
    }

private val Project.useLocalMaterialKolor: Boolean
    get() = providers.gradleProperty("materialkolor.useLocal").map(String::toBoolean).getOrElse(true)
