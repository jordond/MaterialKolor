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

            // Not api. The adapter's whole surface is Fluent types, so a consumer already has to
            // declare Fluent to call any of it, and pinning our version on them helps nobody.
            compileOnly(libs.fluent)
        }

        commonTest.dependencies {
            implementation(libs.fluent)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
