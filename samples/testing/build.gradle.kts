import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    explicitApi()

    // Java 11 bytecode, so custom-theme can run the contract as well as the Java 17 samples.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    sourceSets {
        jvmMain.dependencies {
            api(project(":samples:shared"))
            api(libs.compose.ui.test)
            api(compose.desktop.currentOs)
            // JUnit 4, the runner the samples' jvmTest tasks already use, so inherited tests run there.
            api(kotlin("test-junit"))
        }
    }
}
