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
            api(project(":builder:kit"))

            implementation(materialKolor("material3"))
            implementation(materialKolor("unstyled"))

            implementation(libs.compose.foundation)
            implementation(libs.compose.resources)
            implementation(libs.compose.material3)
            implementation(libs.composeUnstyled)
            implementation(libs.composeUnstyled.theming)
            implementation(libs.lucide)

            implementation(materialKolor("fluent"))
            implementation(libs.fluent)
        }

        // Inklet publishes no js, so only the targets it ships for see it. The js fallback draws
        // Inklet documents as plain Material 3 through the actuals in jsMain.
        val inkletMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.inklet)
            }
        }
        jvmMain.get().dependsOn(inkletMain)
        wasmJsMain.get().dependsOn(inkletMain)

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(composeExtension.dependencies.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

composeExtension.extensions.getByType<ResourcesExtension>().apply {
    packageOfResClass = "com.materialkolor.builder.preview.generated.resources"
    publicResClass = false
}

abstract class ModuleSources : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dir: DirectoryProperty

    override fun asArguments(): List<String> = listOf("-Dbuilder.sourceDir=${dir.get().asFile.absolutePath}")
}

tasks.named<Test>("jvmTest") {
    jvmArgumentProviders += objects.newInstance<ModuleSources>().apply { dir = layout.projectDirectory.dir("src") }
}
