plugins {
    id("java-library")
    id("kotlin")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
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