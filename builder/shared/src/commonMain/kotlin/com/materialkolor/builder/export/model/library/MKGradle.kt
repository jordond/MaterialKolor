package com.materialkolor.builder.export.model.library

private fun mkLib(version: String) = "com.materialkolor:material-kolor:$version"

fun libsVersionsToml(version: String): String = """
    [versions]
    materialKolor = "$version"

    [libraries]
    materialKolor = "${mkLib(version)}"
""".trimIndent()

fun buildImplementation(
    version: String,
    useVersionCatalog: Boolean,
): String = if (useVersionCatalog) {
    "implementation(libs.materialKolor)"
} else {
    "implementation(\"${mkLib(version)}\")"
}

fun gradleKts(
    version: String,
    isMultiplatform: Boolean,
    useVersionCatalog: Boolean,
): String {
    val lib = buildImplementation(version, useVersionCatalog)
    return if (isMultiplatform) {
        """
        kotlin {
            sourceSets {
                commonMain.dependencies {
                   $lib
                }
            }
        }
       """.trimIndent()
    } else {
        """
        dependencies {
            $lib
        }
        """.trimIndent()
    }
}
