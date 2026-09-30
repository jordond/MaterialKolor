package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.text.Literals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The files of the `dsl-forms` golden case, a small theme file that uses every form the code DSL
 * has beyond plain calls.
 *
 * Spotless lints the golden like any other source, so this case is the proof that the forms come
 * out the way ktlint would write them. It lives in common code so the wasm tests can check it too.
 */
internal fun formsCaseFiles(): List<GeneratedFile> =
    listOf(
        kotlinFile(path = "Forms.kt", packageName = "com.example.theme") {
            property("SeedColor", Literals.colorLiteral(0xFF6750A4.toInt()))
            property(
                name = "ModifyScheme",
                value = lambda("scheme") {
                    statement(ref("scheme").call("copy") { argument("primary", ref("SeedColor")) })
                },
                type = lambdaType(parameters = listOf(type(Symbols.ColorScheme)), returns = type(Symbols.ColorScheme)),
            )
            classDeclaration(
                name = "ColorFamily",
                kind = ClassKind.DataClass,
                annotations = listOf(AnnotationSpec(Symbols.Immutable)),
            ) {
                property("color", Symbols.Color)
                property("onColor", Symbols.Color)
            }
            extendedColors()
            objectDeclaration(name = "ThemeDefaults", visibility = Visibility.Internal) {
                property("Style", ref(Symbols.PaletteStyle).member("Expressive"))
            }
            property(
                name = "LocalExtendedColors",
                value = call(Symbols.StaticCompositionLocalOf) {
                    typeArgument(type("ExtendedColors"))
                    trailingLambda {
                        call("error") { argument(Literals.string("No ExtendedColors provided")) }
                    }
                },
            )
            property(
                name = "brand",
                value = ref("primary").call(Symbols.Harmonize) { argument(ref("SeedColor")) },
                type = type(Symbols.Color),
                visibility = Visibility.Internal,
                receiver = type(Symbols.ColorScheme),
            )
            toFamily()
            swapTones()
            appTheme()
        },
    )

private fun KotlinFileScope.extendedColors() {
    classDeclaration(name = "ExtendedColors") {
        property("brand", type("ColorFamily"))
        property("isDark", Symbols.Boolean, default = Literals.boolean(false))
        body {
            property(
                name = "accent",
                value = ifElse(
                    condition = ref("isDark"),
                    whenTrue = Literals.colorLiteral(0xFFB3E5FC.toInt()),
                    whenFalse = Literals.colorLiteral(0xFF01579B.toInt()),
                ),
                type = type(Symbols.Color),
            )
            function(name = "onBrand", returns = type(Symbols.Color)) {
                parameter("palette", Symbols.TonalPalette)
                body {
                    assign("tone", ifElse(ref("isDark"), Literals.int(80), Literals.int(40)))
                    returns(
                        ref("palette").call(Symbols.OnTone) {
                            argument(ref("tone"))
                            argument(ref(Symbols.ContrastThreshold).member("WCAG_AA_NORMAL_TEXT"))
                        },
                    )
                }
            }
        }
    }
}

private fun KotlinFileScope.toFamily() {
    function(
        name = "toFamily",
        returns = type("ColorFamily"),
        visibility = Visibility.Private,
        receiver = type(Symbols.Color),
    ) {
        parameter("isDark", Symbols.Boolean)
        body {
            assign(
                name = "onColor",
                value = whenExpression {
                    branch(ref("isDark"), Literals.colorLiteral(0xFF000000.toInt()))
                    otherwise(Literals.colorLiteral(0xFFFFFFFF.toInt()))
                },
            )
            returns(
                callOf("ColorFamily") {
                    argument("color", ref("this"))
                    argument("onColor", ref("onColor"))
                },
            )
        }
    }
}

private fun KotlinFileScope.swapTones() {
    function(name = "swapTones", visibility = Visibility.Private) {
        parameter("tones", type("IntArray"))
        body {
            assign("first", ref("tones").index(Literals.int(0)))
            reassign(ref("tones").index(Literals.int(0)), ref("tones").index(Literals.int(1)))
            reassign(ref("tones").index(Literals.int(1)), ref("first"))
        }
    }
}

private fun KotlinFileScope.appTheme() {
    val optIn = AnnotationSpec(Symbols.OptIn, listOf(classLiteral(Symbols.ExperimentalMaterial3ExpressiveApi)))

    function(name = "AppTheme", annotations = listOf(optIn, AnnotationSpec(Symbols.Composable))) {
        parameter("darkTheme", Symbols.Boolean, default = call(Symbols.IsSystemInDarkTheme))
        parameter("content", lambdaType(annotations = listOf(Symbols.Composable)))
        body {
            assign("brand", ref(Symbols.MaterialTheme).member("colorScheme").member("brand"))
            assign(
                name = "extendedColors",
                value = callOf(Symbols.Remember) {
                    argument(ref("brand"))
                    argument(ref("darkTheme"))
                    trailingLambda {
                        assign("family", ref("brand").call("toFamily") { argument(ref("darkTheme")) })
                        call("ExtendedColors") {
                            argument("brand", ref("family"))
                            argument("isDark", ref("darkTheme"))
                        }
                    }
                },
            )
            assign(
                name = "motionScheme",
                value = whenExpression(ref("ThemeDefaults").member("Style")) {
                    branch(
                        condition = ref(Symbols.PaletteStyle).member("Expressive"),
                        value = ref(Symbols.MotionScheme).call("expressive"),
                    )
                    otherwise(ref(Symbols.MotionScheme).call("standard"))
                },
            )
            blankLine()
            call(Symbols.CompositionLocalProvider) {
                argument(infix(ref("LocalExtendedColors"), "provides", ref("extendedColors")))
                trailingLambda {
                    call(Symbols.DynamicMaterialExpressiveTheme) {
                        argument("seedColor", ref("SeedColor"))
                        argument("motionScheme", ref("motionScheme"))
                        argument("isDark", ref("darkTheme"))
                        argument("modifyColorScheme", lambda("scheme") { statement(modifiedScheme()) })
                        argument("content", ref("content"))
                    }
                }
            }
        }
    }
}

private fun modifiedScheme(): Expression {
    val brand = ref("extendedColors").member("brand")
    val tertiary = ref("scheme").member("tertiary")

    return ref("scheme").call("copy") {
        argument(
            name = "primary",
            value = ifElse(
                condition = ref("darkTheme"),
                whenTrue = Literals.colorLiteral(0xFFD0BCFF.toInt()),
                whenFalse = Literals.colorLiteral(0xFF6750A4.toInt()),
            ),
        )
        argument(
            name = "tertiary",
            value = ifElse(
                condition = ref("darkTheme"),
                whenTrue = brand.member("color").call(Symbols.Harmonize) { argument(tertiary) },
                whenFalse = brand.member("onColor").call(Symbols.Harmonize) { argument(tertiary) },
            ),
        )
    }
}

class FormsCaseTest {
    @Test
    fun formsCase_anyPlatform_matchesTheGoldenHash() {
        assertEquals(GoldenHashes.cases["dsl-forms"], GoldenDigest.of(formsCaseFiles()))
    }
}
