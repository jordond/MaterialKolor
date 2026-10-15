plugins {
    id("materialkolor.library.compose")
}

kotlin {
    android {
        namespace = "com.materialkolor.material3"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.material3.get().toString()) {
                exclude(group = "androidx.compose.material3")
            }

            api(project(":material-kolor-core"))
        }

        androidMain.dependencies {
            compileOnly(libs.androidx.compose.material3)
        }

        androidHostTest.dependencies {
            implementation(libs.androidx.compose.material3)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
