plugins { kotlin("jvm") }

val mcuVersion = providers.gradleProperty("mcuVersion").get()

repositories {
    exclusiveContent {
        forRepository { maven { url = uri(providers.gradleProperty("mcuRepository").get()) } }
        filter { includeGroup("com.materialkolor") }
    }
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation("com.materialkolor:material-color-utilities-jvm:$mcuVersion")
    implementation("com.materialkolor:material-kolor-core-jvm:$mcuVersion")
    implementation("com.materialkolor:material-kolor-material3-jvm:$mcuVersion")
    implementation("com.materialkolor:material-kolor-palette-jvm:$mcuVersion")
    implementation("com.materialkolor:material-kolor-unstyled-jvm:$mcuVersion")
    implementation("com.materialkolor:material-kolor-fluent-jvm:$mcuVersion")

    implementation("io.github.compose-fluent:fluent-desktop:v0.1.0")
    implementation("com.composables:composeunstyled-theming:2.10.0")
}

kotlin { jvmToolchain(17) }
