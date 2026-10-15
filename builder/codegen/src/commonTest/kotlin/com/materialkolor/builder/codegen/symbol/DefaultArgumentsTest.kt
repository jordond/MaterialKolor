package com.materialkolor.builder.codegen.symbol

import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.dsl.kotlinFile
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultArgumentsTest {
    @Test
    fun optionalArgument_expressiveWrapperOnTonalSpot2021_writesStyleAndSpec() {
        val document = Fixtures.ExpressiveOnTonalSpot2021.input.document
        val text = themeCall(DefaultArguments.DynamicMaterialExpressiveTheme, document)

        assertTrue("style = Style.TonalSpot" in text, text)
        assertTrue("specVersion = Spec.Spec2021" in text, text)
        assertFalse("contrastLevel" in text, text)
        assertFalse("platform" in text, text)
    }

    @Test
    fun optionalArgument_plainWrapperOnTonalSpot2025_leavesBothOut() {
        val document = Fixtures.Default.input.document
            .copy(spec = SpecVersion.Spec2025)
        val text = themeCall(DefaultArguments.DynamicMaterialTheme, document)

        assertFalse("style" in text, text)
        assertFalse("specVersion" in text, text)
    }

    @Test
    fun optionalArgument_plainWrapperOnTonalSpot2021_writesTheSpec() {
        val text = themeCall(DefaultArguments.DynamicMaterialTheme, Fixtures.Default.input.document)

        assertFalse("style" in text, text)
        assertTrue("specVersion = Spec.Spec2021" in text, text)
    }

    @Test
    fun optionalArgument_watchUnder2025_writesOnlyThePlatform() {
        val text = themeCall(DefaultArguments.DynamicMaterialTheme, Fixtures.Watch2025.input.document)

        assertFalse("specVersion" in text, text)
        assertTrue("platform = Platform.Watch" in text, text)
    }

    /**
     * The call a target would write for [document], with each value spelled as its enum name.
     */
    private fun themeCall(
        defaults: SchemeDefaults,
        document: ThemeDocument,
    ): String =
        kotlinFile(path = "Theme.kt", packageName = "com.example") {
            function(name = "AppTheme") {
                body {
                    call(defaults.function) {
                        argument("seedColor", ref("SeedColor"))
                        optionalArgument(defaults.style, document.style) { ref("Style.${it.name}") }
                        optionalArgument(defaults.contrastLevel, document.contrast) { ref("${it.hundredths}") }
                        optionalArgument(defaults.specVersion, document.spec) { ref("Spec.${it.name}") }
                        optionalArgument(defaults.platform, document.platform) { ref("Platform.${it.name}") }
                    }
                }
            }
        }.text
}
