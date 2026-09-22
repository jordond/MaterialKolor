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
        namespace = "com.materialkolor.sample.fluent"
        compileSdk = libs.versions.sdk.compile.get().toInt()
        minSdk = libs.versions.sdk.min.library.get().toInt()

        withHostTest {}

        // Fluent publishes Java 17 bytecode, so this sample cannot sit on 11 the way the
        // custom-theme sample does.
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

            implementation(project(":material-kolor-fluent"))

            // material-kolor-fluent does not bring Fluent along, so a consumer picks its own
            // version. This sample is that consumer.
            implementation(libs.fluent)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.materialkolor.sample.fluent.MainKt"
    }
}
