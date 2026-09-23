import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("materialkolor.builder.compose")
    alias(libs.plugins.metro)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.roborazzi)
}

// The Compose plugin arrives through the convention plugin, so this script has no generated
// `compose` accessor and reaches the extension by type instead.
val composeExtension = extensions.getByType<ComposeExtension>()

buildkonfig {
    packageName = "com.materialkolor.builder"

    defaultConfigs {
        buildConfigField(STRING, "BUILDER_VERSION", libs.versions.builder.version.get(), const = true)
        // The one place the exported MaterialKolor version is named (D9).
        buildConfigField(STRING, "MATERIAL_KOLOR_VERSION", libs.versions.materialKolorExport.get(), const = true)
        buildConfigField(STRING, "FLUENT_VERSION", libs.versions.fluent.get(), const = true)
        buildConfigField(BOOLEAN, "FLUENT_MODULE", "true", const = true)
    }
}

kotlin {
    // Everything here is internal apart from the entry point and the platform interfaces the web
    // module implements, so the explicit API mode the builder convention turns on is off again.
    explicitApi = ExplicitApiMode.Disabled

    sourceSets {
        commonMain.dependencies {
            // The platform interfaces name domain types and flows, and `:builder:web` implements them.
            api(project(":builder:domain"))
            api(libs.kotlinx.coroutines.core)
            api(libs.compose.ui)

            implementation(project(":builder:codegen"))
            implementation(project(":builder:engine"))
            implementation(project(":builder:kit"))
            implementation(project(":builder:preview"))

            implementation(libs.compose.foundation)
            implementation(libs.metrox.viewmodel)
            implementation(libs.metrox.viewmodel.compose)
            implementation(libs.stateHolder)
            implementation(libs.stateHolder.viewModel)
            implementation(libs.stateHolder.compose)
            implementation(libs.stateHolder.dispatcher)
            implementation(libs.stateHolder.dispatcher.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.kstore)
            implementation(libs.kermit)
            implementation(libs.kotlinx.collections)
        }

        jvmMain.dependencies {
            implementation(composeExtension.dependencies.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kstore.file)
            implementation(libs.filekit.dialogs)
        }

        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test)
            implementation(libs.roborazzi.compose.desktop)
        }
    }
}

composeExtension.extensions.getByType<DesktopExtension>().application {
    mainClass = "com.materialkolor.builder.desktop.MainKt"
}
