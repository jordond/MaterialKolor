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

            // Not api. The adapter's whole surface is Compose Unstyled types, so a consumer already
            // has to declare Unstyled to call any of it, and pinning our version on them helps
            // nobody.
            compileOnly(libs.composeUnstyled.theming)
        }

        commonTest.dependencies {
            implementation(libs.composeUnstyled.theming)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
