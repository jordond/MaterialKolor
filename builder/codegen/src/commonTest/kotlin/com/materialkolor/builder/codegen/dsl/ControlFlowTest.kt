package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.text.Literals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Writes [value] after `val x = ` as a top level property would, without the file around it.
 */
private fun written(value: Expression): String {
    val writer = CodeWriter()
    writer.expression(value, listOf(plainToken("val x = ")))

    return writer.result.joinToString("\n") { line -> line.joinToString("") { it.text } }
}

private val LongLight = "light".repeat(12)
private val LongDark = "dark".repeat(15)

class ControlFlowTest {
    @Test
    fun ifElse_shortBranches_staysOnOneLine() {
        val value = ifElse(
            condition = ref("isDark"),
            whenTrue = Literals.colorLiteral(0xFFD0BCFF.toInt()),
            whenFalse = Literals.colorLiteral(0xFF6750A4.toInt()),
        )

        assertEquals("val x = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)", written(value))
    }

    @Test
    fun ifElse_pastTheColumnLimit_breaksIntoBraces() {
        val value = ifElse(ref("isDark"), ref(LongLight), ref(LongDark))

        val expected =
            """
            val x = if (isDark) {
                $LongLight
            } else {
                $LongDark
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun ifElse_elseIf_chainsTheBranchesEvenWhenShort() {
        val value = ifElse(ref("a"), ref("b"), ifElse(ref("c"), ref("d"), ref("e")))

        val expected =
            """
            val x = if (a) {
                b
            } else if (c) {
                d
            } else {
                e
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun ifElse_branchThatBreaks_breaksTheIfToo() {
        val value = ifElse(
            condition = ref("isDark"),
            whenTrue = call("darkColors", multiline = true) { argument("primary", ref("Primary")) },
            whenFalse = ref("LightColors"),
        )

        val expected =
            """
            val x = if (isDark) {
                darkColors(
                    primary = Primary,
                )
            } else {
                LightColors
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun whenExpression_subjectAndOtherwise_writesABranchPerLine() {
        val value = whenExpression(ref("mode")) {
            branch(ref("Mode").member("Light"), ref("LightColors"))
            branch(ref("Mode").member("Dark"), ref("DarkColors"))
            otherwise(call("error") { argument(Literals.string("Unknown mode")) })
        }

        val expected =
            """
            val x = when (mode) {
                Mode.Light -> LightColors
                Mode.Dark -> DarkColors
                else -> error("Unknown mode")
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun whenExpression_noSubjectAndOtherwiseFirst_writesABareWhenEndingInElse() {
        val value = whenExpression {
            otherwise(ref("LightColors"))
            branch(ref("isDark"), ref("DarkColors"))
        }

        val expected =
            """
            val x = when {
                isDark -> DarkColors
                else -> LightColors
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun whenExpression_longBranchValue_breaksInsideTheBranch() {
        val value = whenExpression(ref("mode")) {
            branch(
                condition = ref("Mode").member("Dark"),
                value = call("darkColorScheme") {
                    argument("primary", ref("p".repeat(60)))
                    argument("secondary", ref("s".repeat(40)))
                },
            )
        }

        val expected =
            """
            val x = when (mode) {
                Mode.Dark -> darkColorScheme(
                    primary = ${"p".repeat(60)},
                    secondary = ${"s".repeat(40)},
                )
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun whenExpression_noBranches_fails() {
        assertFailsWith<IllegalArgumentException> { whenExpression(ref("mode")) {} }
    }

    @Test
    fun lambda_singleExpression_staysOnOneLine() {
        val withParameter = lambda("scheme") {
            statement(ref("scheme").call("copy") { argument("primary", ref("SeedColor")) })
        }

        assertEquals("val x = { scheme -> scheme.copy(primary = SeedColor) }", written(withParameter))
        assertEquals("val x = { content() }", written(lambda { call("content") }))
        assertEquals("val x = {}", written(lambda {}))
    }

    @Test
    fun lambda_severalStatements_breaksAfterTheArrow() {
        val value = lambda("scheme") {
            assign("seed", ref("scheme").member("primary"))
            statement(ref("scheme").call("copy") { argument("tertiary", ref("seed")) })
        }

        val expected =
            """
            val x = { scheme ->
                val seed = scheme.primary
                scheme.copy(tertiary = seed)
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun lambda_pastTheColumnLimit_breaksAndSoDoesItsBody() {
        val value = lambda("scheme") {
            statement(
                ref("scheme").call("copy") {
                    argument("primary", ref("p".repeat(50)))
                    argument("secondary", ref("s".repeat(50)))
                },
            )
        }

        val expected =
            """
            val x = { scheme ->
                scheme.copy(
                    primary = ${"p".repeat(50)},
                    secondary = ${"s".repeat(50)},
                )
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun lambda_asANamedArgument_breaksTheCallAroundIt() {
        val value = call(Symbols.RememberDynamicMaterialThemeState) {
            argument("seedColor", ref("SeedColor"))
            argument(
                name = "modifyColorScheme",
                value = lambda("scheme") {
                    assign("seed", ref("scheme").member("primary"))
                    statement(ref("scheme").call("copy") { argument("tertiary", ref("seed")) })
                },
            )
        }

        val expected =
            """
            val x = rememberDynamicMaterialThemeState(
                seedColor = SeedColor,
                modifyColorScheme = { scheme ->
                    val seed = scheme.primary
                    scheme.copy(tertiary = seed)
                },
            )
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun member_chain_writesEachDot() {
        assertEquals(
            "val x = MaterialTheme.colorScheme.primary",
            written(ref(Symbols.MaterialTheme).member("colorScheme").member("primary")),
        )
        assertEquals("val x = LocalExtendedColors.current", written(ref("LocalExtendedColors").member("current")))
    }

    @Test
    fun member_receiverThatBreaks_readsOffTheClosingParenthesis() {
        val value = call("rememberColors", multiline = true) { argument(ref("seed")) }.member("primary")

        val expected =
            """
            val x = rememberColors(
                seed,
            ).primary
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun memberCall_onAReceiver_writesTheCallAfterTheDot() {
        val onTone = ref("palette").call(Symbols.OnTone) {
            argument(Literals.int(40))
            argument(ref(Symbols.ContrastThreshold).member("WCAG_AA_NORMAL_TEXT"))
        }

        assertEquals("val x = MotionScheme.expressive()", written(ref(Symbols.MotionScheme).call("expressive")))
        assertEquals("val x = palette.onTone(40, ContrastThreshold.WCAG_AA_NORMAL_TEXT)", written(onTone))
        assertEquals(
            "val x = seed.harmonize(seedColor)",
            written(ref("seed").call(Symbols.Harmonize) { argument(ref("seedColor")) }),
        )
    }

    @Test
    fun memberCall_pastTheColumnLimit_breaksTheArguments() {
        val value = ref("scheme").call("copy") {
            argument("primary", ref("p".repeat(50)))
            argument("secondary", ref("s".repeat(50)))
        }

        val expected =
            """
            val x = scheme.copy(
                primary = ${"p".repeat(50)},
                secondary = ${"s".repeat(50)},
            )
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun memberCall_receiverThatBreaks_fails() {
        assertFailsWith<IllegalArgumentException> {
            whenExpression { otherwise(ref("x")) }.call("copy")
        }
    }

    // b-111b
    @Test
    fun index_memberKey_writesTheKeyInBrackets() {
        val value = ref("properties").index(ref("ThemeTokens").member("colors"))

        assertEquals("val x = properties[ThemeTokens.colors]", written(value))
    }

    @Test
    fun index_receiverOrKeyThatBreaks_fails() {
        val breaks = whenExpression { otherwise(ref("x")) }

        assertFailsWith<IllegalArgumentException> { breaks.index(ref("key")) }
        assertFailsWith<IllegalArgumentException> { ref("map").index(breaks) }
    }

    @Test
    fun infix_provides_writesTheNameBetweenTheSides() {
        assertEquals(
            "val x = LocalExtendedColors provides colors",
            written(infix(ref("LocalExtendedColors"), "provides", ref("colors"))),
        )
    }

    @Test
    fun infix_rightSideThatBreaks_keepsTheLeftOnTheOpeningLine() {
        val right = call("ExtendedColors", multiline = true) { argument("brand", ref("brand")) }

        val expected =
            """
            val x = LocalExtendedColors provides ExtendedColors(
                brand = brand,
            )
            """.trimIndent()

        assertEquals(expected, written(infix(ref("LocalExtendedColors"), "provides", right)))
    }

    @Test
    fun classLiteral_symbol_writesDoubleColonClass() {
        assertEquals(
            "val x = ExperimentalMaterial3ExpressiveApi::class",
            written(classLiteral(Symbols.ExperimentalMaterial3ExpressiveApi)),
        )
    }

    @Test
    fun trailingLambda_noArguments_dropsTheParentheses() {
        val value = call(Symbols.StaticCompositionLocalOf) {
            trailingLambda { call("ExtendedColors") }
        }

        assertEquals("val x = staticCompositionLocalOf { ExtendedColors() }", written(value))
    }

    @Test
    fun trailingLambda_typeArgument_goesBetweenTheNameAndTheLambda() {
        val value = call(Symbols.StaticCompositionLocalOf) {
            typeArgument(type("AppColors"))
            trailingLambda {
                call("error") { argument(Literals.string("No AppColors provided")) }
            }
        }

        assertEquals("val x = staticCompositionLocalOf<AppColors> { error(\"No AppColors provided\") }", written(value))
    }

    @Test
    fun trailingLambda_shortProvider_staysOnOneLine() {
        val value = call(Symbols.CompositionLocalProvider) {
            argument(infix(ref("LocalExtendedColors"), "provides", ref("colors")))
            trailingLambda { call("content") }
        }

        assertEquals(
            "val x = CompositionLocalProvider(LocalExtendedColors provides colors) { content() }",
            written(value),
        )
    }

    @Test
    fun trailingLambda_severalStatements_breaksOnlyTheLambda() {
        val value = call(Symbols.Remember) {
            argument(ref("seedColor"))
            argument(ref("isDark"))
            trailingLambda {
                assign("seed", ref("seedColor").call(Symbols.Harmonize) { argument(ref("Primary")) })
                call("ExtendedColors") { argument("seed", ref("seed")) }
            }
        }

        val expected =
            """
            val x = remember(seedColor, isDark) {
                val seed = seedColor.harmonize(Primary)
                ExtendedColors(seed = seed)
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun trailingLambda_argumentsPastTheColumnLimit_breaksThemAboveTheLambda() {
        val value = call(Symbols.CompositionLocalProvider) {
            argument(infix(ref("LocalExtendedColors"), "provides", ref("e".repeat(50))))
            argument(infix(ref("LocalAppColors"), "provides", ref("a".repeat(40))))
            trailingLambda { call("content") }
        }

        val expected =
            """
            val x = CompositionLocalProvider(
                LocalExtendedColors provides ${"e".repeat(50)},
                LocalAppColors provides ${"a".repeat(40)},
            ) {
                content()
            }
            """.trimIndent()

        assertEquals(expected, written(value))
    }

    @Test
    fun trailingLambda_givenTwice_fails() {
        assertFailsWith<IllegalArgumentException> {
            call(Symbols.Remember) {
                trailingLambda { call("first") }
                trailingLambda { call("second") }
            }
        }
    }

    @Test
    fun kotlinFile_symbolsInsideTheNewForms_areImported() {
        val optIn = AnnotationSpec(Symbols.OptIn, listOf(classLiteral(Symbols.ExperimentalMaterial3ExpressiveApi)))

        val file = kotlinFile(path = "Theme.kt", packageName = "com.example") {
            property(
                name = "Local",
                value = call(Symbols.StaticCompositionLocalOf) {
                    typeArgument(type(Symbols.ColorScheme))
                    trailingLambda { call(Symbols.LightColorScheme) }
                },
            )
            function(name = "AppTheme", annotations = listOf(optIn)) {
                body {
                    assign(
                        name = "motion",
                        value = whenExpression(ref("style")) {
                            branch(
                                condition = ref(Symbols.PaletteStyle).member("Expressive"),
                                value = ref(Symbols.MotionScheme).call("expressive"),
                            )
                        },
                    )
                    assign(
                        name = "seed",
                        value = ifElse(
                            condition = ref("isDark"),
                            whenTrue = ref("seed").call(Symbols.Harmonize) { argument(ref("primary")) },
                            whenFalse = ref("primary"),
                        ),
                    )
                    statement(infix(ref("Local"), "provides", callOf(Symbols.DarkColorScheme)))
                    statement(lambda { call(Symbols.DynamicMaterialTheme) })
                }
            }
        }

        val imports = file.text.lines().filter { it.startsWith("import ") }

        assertEquals(
            listOf(
                "import androidx.compose.material3.ColorScheme",
                "import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "import androidx.compose.material3.MotionScheme",
                "import androidx.compose.material3.darkColorScheme",
                "import androidx.compose.material3.lightColorScheme",
                "import androidx.compose.runtime.staticCompositionLocalOf",
                "import com.materialkolor.PaletteStyle",
                "import com.materialkolor.ktx.harmonize",
                "import com.materialkolor.material3.DynamicMaterialTheme",
            ),
            imports,
        )
    }
}
