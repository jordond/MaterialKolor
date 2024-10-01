package com.materialkolor.builder.export.model.library

import com.materialkolor.builder.BuildKonfig

private val mkVersion = BuildKonfig.MATERIAL_KOLOR_VERSION
private val mkLib = "com.materialkolor:material-kolor:$mkVersion"

fun libsVersionsToml(): String = """
    [versions]
    materialKolor = "$mkVersion"
    
    [libraries]
    materialKolor = "$mkLib"
""".trimIndent()

fun buildImplementation(useVersionCatalog: Boolean): String = if (useVersionCatalog) {
    "implementation(libs.materialKolor)"
} else {
    "implementation(\"$mkLib\")"
}

fun gradleKts(
    isMultiplatform: Boolean,
    useVersionCatalog: Boolean,
): String {
    val lib = buildImplementation(useVersionCatalog)
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
