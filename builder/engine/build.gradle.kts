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

            // b-115
            // Core takes and returns Compose colors but keeps Compose UI to itself, and the mapping
            // hands an Argb out as a Color, so the graphics types are part of this module's surface.
            api(libs.compose.ui)
            // Role tables and ramps hand out immutable maps and lists, so consumers need the types too.
            api(libs.kotlinx.collections)
        }

        // b-115
        // The role tables are checked against what the Material 3 module builds for the same scheme.
        jvmTest.dependencies {
            implementation(project(":material-kolor-material3"))
            implementation(libs.compose.material3)
        }
    }
}
