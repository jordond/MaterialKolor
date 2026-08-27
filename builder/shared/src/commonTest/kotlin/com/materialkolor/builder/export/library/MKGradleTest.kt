package com.materialkolor.builder.export.library

import com.materialkolor.builder.BuildKonfig
import com.materialkolor.builder.export.model.library.buildImplementation
import com.materialkolor.builder.export.model.library.gradleKts
import com.materialkolor.builder.export.model.library.libsVersionsToml
import kotlin.test.Test
import kotlin.test.assertEquals

class MKGradleTest {

    private val mkVersion = BuildKonfig.MATERIAL_KOLOR_VERSION
    private val mkLib = "com.materialkolor:material-kolor:$mkVersion"

    @Test
    fun testLibsVersionsToml() {
        val expected = """
            [versions]
            materialKolor = "$mkVersion"

            [libraries]
            materialKolor = "$mkLib"
        """.trimIndent()

        assertEquals(expected, libsVersionsToml(mkVersion))
    }

    @Test
    fun testBuildImplementationWithVersionCatalog() {
        val expected = "implementation(libs.materialKolor)"
        assertEquals(expected, buildImplementation(version = mkVersion, useVersionCatalog = true))
    }

    @Test
    fun testBuildImplementationWithoutVersionCatalog() {
        val expected = "implementation(\"$mkLib\")"
        assertEquals(expected, buildImplementation(version = mkVersion, useVersionCatalog = false))
    }

    @Test
    fun testGradleKtsMultiplatformWithVersionCatalog() {
        val expected = """
            kotlin {
                sourceSets {
                    commonMain.dependencies {
                       implementation(libs.materialKolor)
                    }
                }
            }
        """.trimIndent()

        assertEquals(
            expected,
            gradleKts(version = mkVersion, isMultiplatform = true, useVersionCatalog = true),
        )
    }

    @Test
    fun testGradleKtsAndroidOnlyWithVersionCatalog() {
        val expected = """
            dependencies {
                implementation(libs.materialKolor)
            }
        """.trimIndent()

        assertEquals(
            expected,
            gradleKts(version = mkVersion, isMultiplatform = false, useVersionCatalog = true),
        )
    }

    @Test
    fun testGradleKtsMultiplatformWithoutVersionCatalog() {
        val expected = """
            kotlin {
                sourceSets {
                    commonMain.dependencies {
                       implementation("$mkLib")
                    }
                }
            }
        """.trimIndent()

        assertEquals(
            expected,
            gradleKts(version = mkVersion, isMultiplatform = true, useVersionCatalog = false),
        )
    }

    @Test
    fun testGradleKtsAndroidOnlyWithoutVersionCatalog() {
        val expected = """
            dependencies {
                implementation("$mkLib")
            }
        """.trimIndent()

        assertEquals(
            expected,
            gradleKts(version = mkVersion, isMultiplatform = false, useVersionCatalog = false),
        )
    }
}
