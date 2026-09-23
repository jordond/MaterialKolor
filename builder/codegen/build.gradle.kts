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

// b-109
// The golden tests compare by default. Pass -Pgolden.update=true to rewrite the goldens and their hashes instead.
tasks.named<Test>("jvmTest") {
    systemProperty("golden.update", providers.gradleProperty("golden.update").getOrElse("false"))
}
