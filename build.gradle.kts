plugins {
    id("materialkolor.root")
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.multiplatform.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.poko) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka)

    // Builder
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.buildKonfig) apply false
    alias(libs.plugins.compose.hot.reload) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootDir.resolve("dokka"))
    }
}

dependencies {
    dokka(project(":material-color-utilities"))
    dokka(project(":material-kolor-core"))
    dokka(project(":material-kolor-material3"))
    dokka(project(":material-kolor-palette"))
    dokka(project(":material-kolor-unstyled"))
}
