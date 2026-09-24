import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.multiplatform.android.library)
}

kotlin {
    applyDefaultHierarchyTemplate()
    explicitApi()

    android {
        namespace = "com.materialkolor.sample.customtheme"
        compileSdk = libs.versions.sdk.compile.get().toInt()
        minSdk = libs.versions.sdk.min.library.get().toInt()

        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)

            implementation(project(":material-kolor-core"))
            implementation(project(":samples:shared"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        jvmTest.dependencies {
            implementation(project(":samples:testing"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.materialkolor.sample.customtheme.MainKt"
    }
}
