plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.errorProneAnnotations)

    testImplementation(kotlin("test"))
    testImplementation(project(":material-color-utilities"))
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}