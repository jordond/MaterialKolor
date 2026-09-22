import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("materialkolor.library.compose")
}

materialKolorLibrary {
    macos = false
    jvmTarget = JvmTarget.JVM_17
}

kotlin {
    android {
        namespace = "com.materialkolor.fluent"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":material-kolor-core"))

            // Not api. The adapter's whole surface is Fluent types, so a consumer already has to
            // declare Fluent to call any of it, and pinning our version on them helps nobody.
            compileOnly(libs.fluent)
        }

        // Klibs record their dependencies in their own manifest, whatever the Gradle scope says,
        // so an iOS, JS or Wasm consumer resolves io.github.compose-fluent:fluent when it compiles against this
        // module. Leaving it compileOnly on those targets publishes a klib that asks the resolver
        // for a library the metadata never supplied, and the consumer gets
        // `KLIB resolver: Could not find "io.github.compose-fluent:fluent"` rather than a useful error.
        // compileOnly is genuinely supported on JVM and Android, so the version stays their choice
        // there. See KT-70727.
        nativeMain.dependencies { api(libs.fluent) }
        jsMain.dependencies { api(libs.fluent) }
        wasmJsMain.dependencies { api(libs.fluent) }

        commonTest.dependencies {
            implementation(libs.fluent)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
