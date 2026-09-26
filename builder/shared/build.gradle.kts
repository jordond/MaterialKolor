import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("materialkolor.builder.compose")
    alias(libs.plugins.metro)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.kotlinx.serialization)
}

// The Compose plugin arrives through the convention plugin, so this script has no generated
// `compose` accessor and reaches the extension by type instead.
val composeExtension = extensions.getByType<ComposeExtension>()

buildkonfig {
    packageName = "com.materialkolor.builder"

    defaultConfigs {
        buildConfigField(STRING, "BUILDER_VERSION", libs.versions.builder.version.get(), const = true)
        // The one place the exported MaterialKolor version is named.
        buildConfigField(STRING, "MATERIAL_KOLOR_VERSION", libs.versions.materialKolorExport.get(), const = true)
        buildConfigField(STRING, "FLUENT_VERSION", libs.versions.fluent.get(), const = true)
        buildConfigField(STRING, "COMPOSE_UNSTYLED_VERSION", libs.versions.composeUnstyled.get(), const = true)
        buildConfigField(BOOLEAN, "FLUENT_MODULE", "true", const = true)
    }
}

kotlin {
    // Everything here is internal apart from the entry point, the platform interfaces the apps
    // implement and the in-memory stores they borrow, so the explicit API mode the builder convention
    // turns on is off again. ArchitectureTest keeps the rest internal instead.
    explicitApi = ExplicitApiMode.Disabled

    // Compose UI tests on wasm only get the Skiko runtime when webpack bundles them, and that only
    // happens for a target with an executable (CMP-4906). Nothing ships from it, the site is built
    // by `:builder:apps:web`.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // The platform interfaces name domain types and flows, and the apps implement them.
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
            implementation(libs.kermit)
            implementation(libs.kotlinx.collections)
        }

        commonMain.dependencies {
            implementation(libs.compose.resources)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            implementation(composeExtension.dependencies.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test)
        }

        // The app graph has to keep one view model in the browser too, so one Compose UI test runs in headless Chrome.
        wasmJsTest.dependencies {
            implementation(libs.compose.ui.test)
        }
    }
}

// The session's strings are the first in this module. `Res` stays internal like the rest.
composeExtension.extensions.getByType<org.jetbrains.compose.resources.ResourcesExtension>().apply {
    packageOfResClass = "com.materialkolor.builder.generated.resources"
    publicResClass = false
}

// ArchitectureTest reads every builder module's Kotlin sources straight off disk, so they are inputs
// of the JVM test task. Without this a cached pass can hide a new violation in another module.
tasks.named<Test>("jvmTest") {
    inputs
        .files(
            fileTree(rootDir.resolve("builder")) {
                // The modules ArchitectureTest's BUILDER_MODULES lists.
                listOf("domain", "codegen", "engine", "kit", "preview", "shared", "apps/web", "apps/desktop")
                    .forEach { module -> include("$module/src/*/kotlin/**/*.kt") }
            },
        ).withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("builderSources")
}

// stateholder 3.1.0 ships Java 21 bytecode, so the JVM tests start on a 21 launcher while the module
// still compiles for 17.
val java21Launcher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
tasks.withType<Test>().configureEach {
    javaLauncher.set(java21Launcher)
}
