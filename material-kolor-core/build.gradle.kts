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

            api(project(":material-color-utilities"))
            api(libs.kotlinx.serialization.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
