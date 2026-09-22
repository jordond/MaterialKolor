import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("materialkolor.library.compose")
}

materialKolorLibrary {
    macos = false
    minSdk = 23
    jvmTarget = JvmTarget.JVM_17
}

kotlin {
    android {
        namespace = "com.materialkolor.unstyled"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":material-kolor-core"))

            // Not api. The adapter's whole surface is Compose Unstyled types, so a consumer already
            // has to declare Unstyled to call any of it, and pinning our version on them helps
            // nobody.
            compileOnly(libs.composeUnstyled.theming)
        }

        // Klibs record their dependencies in their own manifest, whatever the Gradle scope says,
        // so an iOS, JS or Wasm consumer resolves com.composables:composeunstyled-theming when it compiles against this
        // module. Leaving it compileOnly on those targets publishes a klib that asks the resolver
        // for a library the metadata never supplied, and the consumer gets
        // `KLIB resolver: Could not find "com.composables:composeunstyled-theming"` rather than a useful error.
        // compileOnly is genuinely supported on JVM and Android, so the version stays their choice
        // there. See KT-70727.
        nativeMain.dependencies { api(libs.composeUnstyled.theming) }
        jsMain.dependencies { api(libs.composeUnstyled.theming) }
        wasmJsMain.dependencies { api(libs.composeUnstyled.theming) }

        commonTest.dependencies {
            implementation(libs.composeUnstyled.theming)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
