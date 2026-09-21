pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    val kotlinVersion = providers.gradleProperty("kotlinVersion").getOrElse("2.4.20")
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
    }
}

rootProject.name = "mcu-published-consumer"
