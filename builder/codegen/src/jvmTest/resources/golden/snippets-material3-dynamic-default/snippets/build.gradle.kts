// Add these to the build file of the module that holds your theme.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.materialKolor.material3)
        }
    }
}
