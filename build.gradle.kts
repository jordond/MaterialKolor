import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.cocoapods) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.dependencies)
    alias(libs.plugins.binaryCompatibility)
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory.set(rootDir.resolve("dokka"))
}

// Temporary workaround for https://github.com/Kotlin/dokka/issues/2977#issuecomment-1567328937
subprojects {
    tasks {
        val taskClass = "org.jetbrains.kotlin.gradle.targets.native.internal." +
            "CInteropMetadataDependencyTransformationTask"

        @Suppress("UNCHECKED_CAST")
        withType(Class.forName(taskClass) as Class<Task>) {
            onlyIf { gradle.taskGraph.allTasks.none { it is AbstractDokkaTask } }
        }
    }
}