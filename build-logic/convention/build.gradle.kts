plugins { `kotlin-dsl` }

kotlin {
    jvmToolchain(
        libs.versions.jvmTarget
            .get()
            .toInt(),
    )
}

dependencies {
    implementation(localGroovy())
    compileOnly(libs.bundles.logic.plugins)
}

gradlePlugin {
    plugins {
        register("materialKolorRoot") {
            id = "materialkolor.root"
            implementationClass = "com.materialkolor.convention.plugin.RootPlugin"
        }

        register("materialKolorLibrary") {
            id = "materialkolor.library"
            implementationClass = "com.materialkolor.convention.plugin.LibraryPlugin"
        }

        register("materialKolorComposeLibrary") {
            id = "materialkolor.library.compose"
            implementationClass = "com.materialkolor.convention.plugin.ComposeLibraryPlugin"
        }
    }
}
