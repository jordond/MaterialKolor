import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

plugins {
    id("materialkolor.builder.compose")
    alias(libs.plugins.roborazzi)
}

// The Compose plugin arrives through the convention plugin, so this script has no generated
// `compose` accessor and reaches the extension by type instead.
val composeExtension = extensions.getByType<ComposeExtension>()

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":builder:kit"))

            implementation(project(":material-kolor-material3"))
            implementation(project(":material-kolor-unstyled"))

            implementation(libs.compose.foundation)
            implementation(libs.compose.resources)
            implementation(libs.compose.material3)
            implementation(libs.composeUnstyled)
            implementation(libs.composeUnstyled.theming)
            implementation(libs.lucide)

            // b-405
            implementation(project(":material-kolor-fluent"))
            implementation(libs.fluent)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(composeExtension.dependencies.desktop.currentOs)
            implementation(libs.roborazzi.compose.desktop)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

composeExtension.extensions.getByType<ResourcesExtension>().apply {
    packageOfResClass = "com.materialkolor.builder.preview.generated.resources"
    publicResClass = false
}
