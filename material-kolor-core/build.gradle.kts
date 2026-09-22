plugins {
    id("materialkolor.library.compose")
    alias(libs.plugins.poko)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.materialkolor"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.colormath)
            implementation(libs.kotlinx.coroutines.core)

            api(project(":material-color-utilities"))
            api(libs.kotlinx.serialization.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
