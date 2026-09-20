package com.materialkolor.convention

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureSpotless() {
    subprojects {
        // Container projects such as `:builder` own no sources, but a whole-tree Kotlin target there still matches
        // every file of their children.
        if (subprojects.isNotEmpty()) return@subprojects

        pluginManager.apply("com.diffplug.spotless")

        val formatterExcludes = buildList {
            add("build/**")
            // `:mcu-upstream` reads its Kotlin straight from the pinned submodule. Upstream files
            // keep upstream formatting, and the provenance gate requires the tree stay clean.
            if (name == "mcu-upstream") add("src/main/kotlin/**")
        }

        extensions.configure<SpotlessExtension> {
            kotlin {
                ktlint(version("ktlint")).setEditorConfigPath("$rootDir/.editorconfig")
                target("**/*.kt")
                targetExclude(*formatterExcludes.toTypedArray())
                toggleOffOn()
                endWithNewline()
            }
        }
    }
}
