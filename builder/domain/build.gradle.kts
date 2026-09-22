plugins {
    id("materialkolor.builder.kotlin")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Generated serializers are part of the public API, so consumers need this on their compile path.
            api(libs.kotlinx.serialization.json)
        }
    }
}
