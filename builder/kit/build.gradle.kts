import com.materialkolor.convention.materialKolor
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

plugins {
    id("materialkolor.builder.compose")
}

// The Compose plugin arrives through the convention plugin, so this script has no generated
// `compose` accessor and reaches the extension by type instead.
val composeExtension = extensions.getByType<ComposeExtension>()

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":builder:engine"))

            // The code viewer renders codegen tokens, so the token kinds are part of what a skin
            // has to colour. `TokenKind` is in the signature of `CodePalette`, so anyone who builds
            // or reads a palette needs the type too.
            api(project(":builder:codegen"))

            implementation(materialKolor("material3"))

            implementation(libs.compose.foundation)
            implementation(libs.compose.resources)
            implementation(libs.compose.material3)
            implementation(libs.composeUnstyled)
            implementation(libs.lucide)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(composeExtension.dependencies.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

composeExtension.extensions.getByType<ResourcesExtension>().apply {
    packageOfResClass = "com.materialkolor.builder.kit.generated.resources"
    publicResClass = false
}
