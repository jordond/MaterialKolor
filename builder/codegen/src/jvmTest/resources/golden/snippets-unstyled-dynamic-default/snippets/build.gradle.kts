// Add these to the build file of the module that holds your theme.
// Compose Unstyled needs JVM 17 and Android minSdk 23, and has no macOS native target.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.materialKolor.unstyled)
        }
    }
}
