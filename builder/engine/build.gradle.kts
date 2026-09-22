plugins {
    id("materialkolor.builder.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":builder:domain"))
            api(project(":material-kolor-core"))
            implementation(project(":material-kolor-palette"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections)
        }
    }
}
