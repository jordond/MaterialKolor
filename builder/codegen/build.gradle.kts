plugins {
    id("materialkolor.builder.kotlin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":builder:domain"))
        }
    }
}
