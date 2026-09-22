import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("materialkolor.library.compose")
}

materialKolorLibrary {
    macos = false
    minSdk = 23
    jvmTarget = JvmTarget.JVM_17
}

kotlin {
    android {
        namespace = "com.materialkolor.unstyled"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":material-kolor-core"))
            api(libs.composeUnstyled.theming)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
