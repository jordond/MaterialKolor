package com.materialkolor.builder.codegen.dsl

import kotlin.test.Test
import kotlin.test.assertEquals

class GradleBuilderTest {
    @Test
    fun gradleFile_kmpDependencies_writesNestedBlocks() {
        val file = gradleFile(path = "snippets/build.gradle.kts") {
            comment("Add this to the module that holds your theme.")
            block("kotlin") {
                block("sourceSets") {
                    block("commonMain.dependencies") {
                        dependency("implementation", "com.materialkolor:material-kolor-core:6.0.0")
                    }
                }
            }
        }

        val expected =
            """
            // Add this to the module that holds your theme.
            kotlin {
                sourceSets {
                    commonMain.dependencies {
                        implementation("com.materialkolor:material-kolor-core:6.0.0")
                    }
                }
            }

            """.trimIndent()

        assertEquals(expected, file.text)
        assertEquals(Language.Kotlin, file.language)
    }

    @Test
    fun gradleFile_catalogAccessor_writesTheExpressionUnquoted() {
        val file = gradleFile(path = "snippets/build.gradle.kts") {
            block("dependencies") {
                dependency("implementation", ref("libs.material.kolor.core"))
                blankLine()
                call("implementation") {
                    argument(ref("libs.fluent"))
                }
            }
        }

        val expected =
            """
            dependencies {
                implementation(libs.material.kolor.core)

                implementation(libs.fluent)
            }

            """.trimIndent()

        assertEquals(expected, file.text)
    }
}
