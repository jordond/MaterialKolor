plugins {
    id("materialkolor.builder.kotlin")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Nothing public here names a Json type, the serializers only expose KSerializer, so the format stays
            // an implementation detail and a consumer that wants to encode brings its own Json.
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
