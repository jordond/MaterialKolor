plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "${libs.versions.app.name.get()}.app"
    compileSdk = libs.versions.sdk.compile.get().toInt()

    defaultConfig {
        applicationId = libs.versions.app.name.get()
        minSdk = libs.versions.sdk.min.app.get().toInt()
        targetSdk = libs.versions.sdk.compile.get().toInt()
        versionCode = libs.versions.app.code.get().toInt()
        versionName = libs.versions.app.version.get()
    }

    signingConfigs {
        create("release") {
            storeFile = project.rootDir.resolve("keystore.key")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEYSTORE_KEY_ALIAS")
            keyPassword = System.getenv("KEYSTORE_KEY_PASSWORD")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":builder:shared"))
}
