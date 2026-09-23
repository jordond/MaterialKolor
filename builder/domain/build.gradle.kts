plugins {
    id("materialkolor.builder.kotlin")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The serializers are public, and every one of them names a type from the core artifact, so core
            // travels with the module. Nothing public names a Json type, so the format stays an implementation
            // detail and a consumer that wants to encode brings its own Json.
            api(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
