plugins {
    id("materialkolor.library.compose")
}

kotlin {
    android {
        namespace = "com.materialkolor.palette"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":material-kolor-core"))
            api(libs.kmpalette.core)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
