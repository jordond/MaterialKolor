import com.materialkolor.convention.materialKolor

plugins {
    id("materialkolor.builder.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":builder:domain"))
            api(materialKolor("core"))
            implementation(materialKolor("palette"))
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
            implementation(materialKolor("material3"))
            implementation(libs.compose.material3)
        }

        // b-117
        // The export parity gate checks the engine against every library adapter an export calls.
        // The expressive theme is composed to read its own defaults, hence the UI test runtime.
        // Unstyled and Fluent only reach compile time in their modules, so the tests add them.
        jvmTest.dependencies {
            implementation(materialKolor("unstyled"))
            implementation(libs.composeUnstyled.theming)
            implementation(materialKolor("fluent"))
            implementation(libs.fluent)
            implementation(libs.compose.ui.test)
            implementation(project.extensions.getByType<org.jetbrains.compose.ComposeExtension>().dependencies.desktop.currentOs)
        }

        // b-114
        // The export parity gate generates each export and holds its colors to the preview. The Material 3 module
        // it builds the dynamic scheme with is already on the test classpath from b-115.
        jvmTest.dependencies {
            implementation(project(":builder:codegen"))
        }
    }
}
