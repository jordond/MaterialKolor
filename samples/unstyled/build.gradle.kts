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
        namespace = "com.materialkolor.sample.unstyled"
        compileSdk = libs.versions.sdk.compile.get().toInt()
        // Compose Unstyled sets the floor, the same one material-kolor-unstyled publishes with.
        minSdk = 23

        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)

            implementation(project(":samples:shared"))
            implementation(project(":material-kolor-unstyled"))
            // The adapter only has compileOnly theming on JVM and Android. This brings it in with the components.
            implementation(libs.composeUnstyled)
            implementation(libs.lucide)
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
        mainClass = "com.materialkolor.sample.unstyled.MainKt"
    }
}
