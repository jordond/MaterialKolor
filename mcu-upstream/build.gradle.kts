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
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("com.google.errorprone:error_prone_annotations:2.25.0")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
//    testImplementation("io.kotest:kotest-assertions-collections:5.8.0")
    testImplementation(project(":material-color-utilities"))
}

tasks.test {
    useJUnitPlatform()
}