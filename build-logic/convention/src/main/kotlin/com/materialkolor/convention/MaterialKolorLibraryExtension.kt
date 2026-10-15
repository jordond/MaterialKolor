package com.materialkolor.convention

import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * The per-module knobs of the `materialkolor.library` convention.
 *
 * A module only sets what it cannot live with. The defaults are what every module published before
 * the adapters arrived used, so leaving the block out keeps the old behavior.
 */
abstract class MaterialKolorLibraryExtension {
    /**
     * Whether the module publishes a macOS target. Off for modules whose dependencies stop at the
     * Apple targets the rest of the ecosystem ships.
     */
    abstract val macos: Property<Boolean>

    /**
     * The Android API floor. Raise it when a dependency of the module needs a higher one.
     */
    abstract val minSdk: Property<Int>

    /**
     * The bytecode level of the Android and JVM targets.
     */
    abstract val jvmTarget: Property<JvmTarget>
}
