// Add these to the build file of the module that holds your theme.
// Compose Fluent needs JVM 17 and has no macOS native target.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.materialKolor.core)
            implementation(libs.composeFluent)
        }
    }
}
