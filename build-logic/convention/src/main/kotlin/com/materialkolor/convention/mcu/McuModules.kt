package com.materialkolor.convention.mcu

/**
 * A published library module and the part of the target set it opts out of.
 *
 * @property name The Gradle project name, which is also the published artifact name.
 * @property macos Whether the module publishes and tests a macOS target.
 */
internal class McuModule(
    val name: String,
    val macos: Boolean = true,
)

/**
 * The published library modules.
 */
internal val mcuLibraryModules =
    listOf(
        McuModule("material-color-utilities"),
        McuModule("material-kolor-core"),
        McuModule("material-kolor-material3"),
    )
