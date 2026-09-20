package com.materialkolor.convention.plugin

import com.materialkolor.convention.configureSpotless
import com.materialkolor.convention.registerMcuVerificationTasks
import org.gradle.api.Plugin
import org.gradle.api.Project

class RootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureSpotless()
        target.registerMcuVerificationTasks()
    }
}
