@file:Suppress("UNUSED_VARIABLE", "OPT_IN_USAGE")

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    targetHierarchy.default()

    android {
        publishAllLibraryVariants()
    }

    jvm()

    js(IR) {
        browser()
    }

    macosX64()
    macosArm64()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "material-color-utilities"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

    }
}

android {
    compileSdk = libs.versions.sdk.compile.get().toInt()
    namespace = "com.materialkolor.colorutilities"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(jdkVersion = 11)
    }
}