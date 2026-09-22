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
            compileOnly(libs.fluent)
        }

        nativeMain.dependencies { api(libs.fluent) }
        jsMain.dependencies { api(libs.fluent) }
        wasmJsMain.dependencies { api(libs.fluent) }

        commonTest.dependencies {
            implementation(libs.fluent)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
