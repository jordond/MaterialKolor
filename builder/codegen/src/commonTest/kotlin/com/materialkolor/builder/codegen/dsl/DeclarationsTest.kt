package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.text.Literals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeclarationsTest {
    @Test
    fun classDeclaration_dataClass_writesAPropertyPerLine() {
        val file = kotlinFile(path = "Color.kt", packageName = "com.example") {
            classDeclaration(
                name = "ColorFamily",
                kind = ClassKind.DataClass,
                annotations = listOf(AnnotationSpec(Symbols.Immutable)),
            ) {
                property("color", Symbols.Color)
                property("onColor", Symbols.Color, default = ref("color"))
            }
        }

        val expected =
            """
            package com.example

            import androidx.compose.runtime.Immutable
            import androidx.compose.ui.graphics.Color

            @Immutable
            data class ColorFamily(
                val color: Color,
                val onColor: Color = color,
            )

            """.trimIndent()

        assertEquals(expected, file.text)
    }

    @Test
    fun classDeclaration_singleProperty_stillTakesALineOfItsOwn() {
        val file = kotlinFile(path = "Color.kt", packageName = "com.example") {
            classDeclaration(name = "ExtendedColors") {
                property("brand", type("ColorFamily"))
            }
        }

        val expected =
            """
            package com.example

            class ExtendedColors(
                val brand: ColorFamily,
            )

            """.trimIndent()

        assertEquals(expected, file.text)
    }

    @Test
    fun classDeclaration_body_holdsPropertiesAndFunctionsApart() {
        val file = kotlinFile(path = "Color.kt", packageName = "com.example") {
            classDeclaration(name = "ExtendedColors", visibility = Visibility.Internal) {
                property("brand", type("ColorFamily"))
                body {
                    property("all", call("listOf") { argument(ref("brand")) })
                    property("count", Literals.int(1), type = type(Symbols.Int), visibility = Visibility.Private)
                    function(name = "brandOn", returns = type(Symbols.Color)) {
                        parameter("isDark", Symbols.Boolean)
                        body {
                            returns(ifElse(ref("isDark"), ref("brand").member("onColor"), ref("brand").member("color")))
                        }
                    }
                }
            }
        }

        val expected =
            """
            package com.example

            import androidx.compose.ui.graphics.Color

            internal class ExtendedColors(
                val brand: ColorFamily,
            ) {
                val all = listOf(brand)

                private val count: Int = 1

                fun brandOn(isDark: Boolean): Color {
                    return if (isDark) brand.onColor else brand.color
                }
            }

            """.trimIndent()

        assertEquals(expected, file.text)
    }

    @Test
    fun classDeclaration_noProperties_writesTheBareName() {
        val file = kotlinFile(path = "Marker.kt", packageName = "com.example") {
            classDeclaration(name = "Marker")
        }

        assertEquals("package com.example\n\nclass Marker\n", file.text)
    }

    @Test
    fun classDeclaration_dataClassWithoutProperties_fails() {
        assertFailsWith<IllegalArgumentException> {
            kotlinFile(path = "Marker.kt", packageName = "com.example") {
                classDeclaration(name = "Marker", kind = ClassKind.DataClass)
            }
        }
    }

    @Test
    fun objectDeclaration_members_writesTheBodyOnlyWhenThereIsOne() {
        val file = kotlinFile(path = "Defaults.kt", packageName = "com.example") {
            objectDeclaration(name = "ThemeDefaults", visibility = Visibility.Internal) {
                property("Style", ref(Symbols.PaletteStyle).member("Expressive"))
            }
            objectDeclaration(name = "Empty")
        }

        val expected =
            """
            package com.example

            import com.materialkolor.PaletteStyle

            internal object ThemeDefaults {
                val Style = PaletteStyle.Expressive
            }

            object Empty

            """.trimIndent()

        assertEquals(expected, file.text)
    }

    @Test
    fun function_annotationWithArguments_writesAndImportsIt() {
        val optIn = AnnotationSpec(Symbols.OptIn, listOf(classLiteral(Symbols.ExperimentalMaterial3ExpressiveApi)))

        val file = kotlinFile(path = "Theme.kt", packageName = "com.example") {
            function(name = "AppTheme", annotations = listOf(optIn, AnnotationSpec(Symbols.Composable))) {
                parameter("content", lambdaType(annotations = listOf(Symbols.Composable)))
                body {
                    call("content")
                }
            }
        }

        val expected =
            """
            package com.example

            import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
            import androidx.compose.runtime.Composable

            @OptIn(ExperimentalMaterial3ExpressiveApi::class)
            @Composable
            fun AppTheme(content: @Composable () -> Unit) {
                content()
            }

            """.trimIndent()

        assertEquals(expected, file.text)
    }

    @Test
    fun lambdaType_annotationWithArguments_writesItBeforeTheParameters() {
        val optIn = AnnotationSpec(Symbols.OptIn, listOf(classLiteral(Symbols.ExperimentalMaterial3ExpressiveApi)))
        val lambda = lambdaType(annotations = listOf(optIn))

        assertEquals(
            "@OptIn(ExperimentalMaterial3ExpressiveApi::class) () -> Unit",
            lambda.tokens.joinToString("") { it.text },
        )
        assertEquals(listOf(Symbols.OptIn, Symbols.ExperimentalMaterial3ExpressiveApi, Symbols.Unit), lambda.symbols)
    }

    @Test
    fun function_privateWithReceiver_writesTheExtensionSignature() {
        val file = kotlinFile(path = "Colors.kt", packageName = "com.example") {
            function(
                name = "toColors",
                returns = type("AppColors"),
                visibility = Visibility.Private,
                receiver = type(Symbols.DynamicScheme),
            ) {
                parameter("isDark", Symbols.Boolean)
                body {
                    returns(callOf("AppColors") { argument("primary", ref("primary")) })
                }
            }
        }

        val expected =
            """
            package com.example

            import com.materialkolor.dynamiccolor.DynamicScheme

            private fun DynamicScheme.toColors(isDark: Boolean): AppColors {
                return AppColors(primary = primary)
            }

            """.trimIndent()

        assertEquals(expected, file.text)
    }

    @Test
    fun property_receiverAndVisibility_writesAGetter() {
        val file = kotlinFile(path = "Colors.kt", packageName = "com.example") {
            property(
                name = "brand",
                value = ref("primary").call(Symbols.Harmonize) { argument(ref("SeedColor")) },
                type = type(Symbols.Color),
                visibility = Visibility.Internal,
                receiver = type(Symbols.ColorScheme),
            )
            property(name = "Count", value = Literals.int(1), const = true, visibility = Visibility.Private)
        }

        val expected =
            """
            package com.example

            import androidx.compose.material3.ColorScheme
            import androidx.compose.ui.graphics.Color
            import com.materialkolor.ktx.harmonize

            internal val ColorScheme.brand: Color
                get() = primary.harmonize(SeedColor)

            private const val Count = 1

            """.trimIndent()

        assertEquals(expected, file.text)
    }

    @Test
    fun property_constWithReceiver_fails() {
        assertFailsWith<IllegalArgumentException> {
            kotlinFile(path = "Colors.kt", packageName = "com.example") {
                property(name = "Count", value = Literals.int(1), const = true, receiver = type(Symbols.ColorScheme))
            }
        }
    }

    @Test
    fun classDeclaration_nameMatchesAnImport_failsNamingTheImport() {
        val failure = assertFailsWith<IllegalArgumentException> {
            kotlinFile(path = "Color.kt", packageName = "com.example") {
                classDeclaration(name = "Color") {
                    property("argb", Symbols.Int)
                }
                property("Seed", Literals.colorLiteral(0xFF6750A4.toInt()))
            }
        }

        val message = failure.message.orEmpty()
        assertTrue("androidx.compose.ui.graphics.Color" in message, message)
        assertTrue("declares its own Color" in message, message)
    }

    @Test
    fun objectDeclaration_nameMatchesAnImport_failsNamingTheImport() {
        val failure = assertFailsWith<IllegalArgumentException> {
            kotlinFile(path = "Theme.kt", packageName = "com.example") {
                objectDeclaration(name = "MaterialTheme")
                property("theme", call(Symbols.MaterialTheme))
            }
        }

        assertTrue("declares its own MaterialTheme" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun classDeclaration_memberNameMatchesAnImport_fails() {
        val failure = assertFailsWith<IllegalArgumentException> {
            kotlinFile(path = "Color.kt", packageName = "com.example") {
                objectDeclaration(name = "Colors") {
                    function(name = "harmonize", returns = type(Symbols.Color)) {
                        body {
                            returns(ref("seed").call(Symbols.Harmonize) { argument(ref("primary")) })
                        }
                    }
                }
            }
        }

        assertTrue("declares its own harmonize" in failure.message.orEmpty(), failure.message)
    }
}
