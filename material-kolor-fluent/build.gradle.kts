import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("materialkolor.library.compose")
}

materialKolorLibrary {
    macos = false
    jvmTarget = JvmTarget.JVM_17
}

kotlin {
    android {
        namespace = "com.materialkolor.fluent"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":material-kolor-core"))
            api(libs.fluent)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
