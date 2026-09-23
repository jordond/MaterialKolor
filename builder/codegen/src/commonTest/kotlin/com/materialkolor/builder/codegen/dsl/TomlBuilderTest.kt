package com.materialkolor.builder.codegen.dsl

import kotlin.test.Test
import kotlin.test.assertEquals

class TomlBuilderTest {
    @Test
    fun tomlFile_catalogSnippet_writesTablesAndInlineTables() {
        val file = tomlFile(path = "gradle/libs.versions.toml") {
            comment("Add these to your version catalog.")
            table("versions") {
                key("materialKolor", "6.0.0")
            }
            table("libraries") {
                inlineTable("material-kolor-core") {
                    entry("module", "com.materialkolor:material-kolor-core")
                    entry("version.ref", "materialKolor")
                }
            }
        }

        val expected =
            """
            # Add these to your version catalog.

            [versions]
            materialKolor = "6.0.0"

            [libraries]
            material-kolor-core = { module = "com.materialkolor:material-kolor-core", version.ref = "materialKolor" }

            """.trimIndent()

        assertEquals(expected, file.text)
        assertEquals(Language.Toml, file.language)
    }

    @Test
    fun tomlFile_tableHeader_isTokenisedAsATable() {
        val file = tomlFile(path = "libs.versions.toml") {
            table("versions") {
                key("materialKolor", "6.0.0")
            }
        }

        val kinds = file.lines.flatten().map { it.kind }

        assertEquals(TokenKind.TomlTable, kinds[1])
        assertEquals(TokenKind.TomlKey, file.lines[1].first().kind)
    }
}
