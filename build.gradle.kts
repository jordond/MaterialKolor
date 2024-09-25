import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.poko) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.binaryCompatibility)
}

apiValidation {
    ignoredProjects.addAll(
        listOf("mcu-upstream"),
    )
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory.set(rootDir.resolve("dokka"))
}

allprojects {
    // Workaround for https://github.com/Kotlin/dokka/issues/2977.
    // We disable the C Interop IDE metadata task when generating documentation using Dokka.
    tasks.withType<AbstractDokkaTask> {
        @Suppress("UNCHECKED_CAST")
        val taskClass = Class.forName("org.jetbrains.kotlin.gradle.targets.native.internal.CInteropMetadataDependencyTransformationTask") as Class<Task>
        parent?.subprojects?.forEach {
            dependsOn(it.tasks.withType(taskClass))
        }
    }
}