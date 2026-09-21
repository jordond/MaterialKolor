package com.materialkolor.convention

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

internal fun Project.configureMcuPublishing() {
    // Ship the notices once in each artifact, including source-only publications.
    tasks.withType<AbstractArchiveTask>().configureEach {
        from(rootProject.layout.projectDirectory.file("LICENSE")) {
            into("META-INF")
            rename { "LICENSE-MaterialKolor" }
        }

        from(rootProject.layout.projectDirectory.file("tools/mcu-upstream/src/main/LICENSE")) {
            into("META-INF")
            rename { "LICENSE-material-color-utilities" }
        }
    }

    // The file repository `verifyMcuPublication` reads before anything reaches a real one.
    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "McuVerification"
                url = rootProject.layout.buildDirectory.dir("mcu-verification-repository").get().asFile.toURI()
            }
        }
    }

    // Unversioned local builds are snapshots. Release automation supplies VERSION_NAME explicitly.
    extensions.configure<MavenPublishBaseExtension> {
        coordinates(version = providers.gradleProperty("VERSION_NAME").orElse("0.0.0-SNAPSHOT").get())
        pom {
            licenses {
                license {
                    name.set("Apache License, Version 2.0 (upstream Material Color Utilities)")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    distribution.set("repo")
                }
            }
        }
    }
}
