package com.materialkolor.transformer

import com.materialkolor.transformer.edits.Result
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiSurfaceTest {
    private val root = File(requiredProperty("mcu.projectRoot"), "tools/mcu-upstream/src/main/kotlin")

    private fun upstream(path: String): String = File(root, path).readText()

    private fun library(
        path: String,
        source: String = upstream(path),
    ): Result = Adapter().use { adapter -> adapter.transform(path, source, niceties = true) }

    @Test
    fun scoreCollapsesToOneFunctionWithDefaults() {
        val text = library("score/Score.kt").text
        assertEquals(1, Regex("fun score\\(").findAll(text).count())
        assertTrue(text.contains("desired: Int = 4,"))
        assertTrue(
            text.contains("// Fallback color is Google Blue.\n    fallbackColorArgb: Int? = 0xff4285f4.toInt(),"),
        )
        assertTrue(text.contains("filter: Boolean = true,"))
    }

    @Test
    fun scoreRefusesOverloadsThatNoLongerMatchTheDefaults() {
        val changed = upstream("score/Score.kt").replace(
            "return score(colorsToPopulation, desired, 0xff4285f4.toInt(), true)",
            "return score(colorsToPopulation, desired, 0xff000000.toInt(), true)",
        )
        val error = assertFailsWith<IllegalArgumentException> { library("score/Score.kt", changed) }
        assertTrue(error.message.orEmpty().contains("score-fallback"))
    }

    @Test
    fun dataClassesBecomePokoClasses() {
        for (path in listOf(
            "dynamiccolor/DynamicColor.kt",
            "dynamiccolor/ToneDeltaPair.kt",
            "palettes/CorePalettes.kt",
            "hct/Cam16.kt",
            "hct/ViewingConditions.kt",
        )) {
            val text = library(path).text
            assertFalse(text.contains("data class"), path)
            assertFalse(text.contains("ConsistentCopyVisibility"), path)
            assertTrue(text.contains("@Poko\nclass "), path)
            assertTrue(text.contains("import dev.drewhamilton.poko.Poko"), path)
        }
    }

    @Test
    fun viewingConditionsHideTheirArrayAndCompareItsContent() {
        val text = library("hct/ViewingConditions.kt").text
        assertTrue(text.contains("@Poko.ReadArrayContent internal val rgbD: DoubleArray"))
        assertTrue(text.contains("internal val ncb: Double"))
    }

    @Test
    fun dynamicColorKeepsAnInternalCopy() {
        val text = library("dynamiccolor/DynamicColor.kt").text
        assertTrue(text.contains("  internal fun copy(\n    name: String = this.name,\n"))
        assertTrue(text.contains("    tone: (DynamicScheme) -> Double = this.tone,\n"))
        assertTrue(text.contains("  ): DynamicColor = DynamicColor(\n    name = name,\n"))
    }

    @Test
    fun newPublicDataClassesFail() {
        val source = "package palettes\ndata class Swatch(val argb: Int)\n"
        val error = assertFailsWith<IllegalArgumentException> { library("palettes/Swatch.kt", source) }
        assertTrue(error.message.orEmpty().contains("poko-class: public data class Swatch"))
    }

    @Test
    fun internalDataClassesPass() {
        library("palettes/Swatch.kt", "package palettes\ninternal data class Swatch(val argb: Int)\n")
        library(
            "palettes/Swatch.kt",
            "package palettes\nobject Swatches { private data class Swatch(val argb: Int) }\n",
        )
    }

    @Test
    fun hiddenMembersFailWhenUpstreamDropsThem() {
        val changed = upstream("utils/ColorUtils.kt").replace("fun labInvf(", "fun labInverse(")
        val error = assertFailsWith<IllegalArgumentException> { library("utils/ColorUtils.kt", changed) }
        assertTrue(error.message.orEmpty().contains("member-visibility: expected one ColorUtils.labInvf, found 0"))
    }

    @Test
    fun hiddenMembersDropTheirStaticBridge() {
        val text = library("dynamiccolor/DynamicColor.kt").text
        assertTrue(text.contains("*/\n    internal fun foregroundTone("))
        assertTrue(text.contains("@JvmStatic\n    fun getInitialToneFromBackground("))
    }

    @Test
    fun subclassingNeedsOptInAndSchemesOptIn() {
        for ((path, declaration) in mapOf(
            "dynamiccolor/ColorSpec.kt" to "interface ColorSpec",
            "dynamiccolor/DynamicScheme.kt" to "open class DynamicScheme(",
        )) {
            val text = library(path).text
            assertTrue(text.contains("@SubclassOptInRequired(InternalMaterialKolorApi::class)\n$declaration"), path)
            assertTrue(text.contains("import com.materialkolor.InternalMaterialKolorApi"), path)
        }

        val scheme = library("scheme/SchemeTonalSpot.kt").text
        assertTrue(scheme.contains("@OptIn(InternalMaterialKolorApi::class)\nclass SchemeTonalSpot("))

        val spec = library("dynamiccolor/ColorSpec2021.kt").text
        assertTrue(spec.contains("@OptIn(InternalMaterialKolorApi::class)\ninternal open class ColorSpec2021"))
    }

    @Test
    fun internalTypesKeepTheirDocumentationFirst() {
        val text = library("hct/HctSolver.kt").text
        assertTrue(text.contains("/** A class that solves the HCT equation. */\ninternal object HctSolver {"))
    }

    @Test
    fun hctHarmonizeIsStaticLikeTheIntOverload() {
        val text = library("blend/Blend.kt").text
        assertTrue(text.contains("@JvmStatic\n  fun harmonize(designColor: Hct, sourceColor: Hct): Hct"))
    }

    @Test
    fun floatOverloadsAreGone() {
        assertFalse(library("contrast/Contrast.kt").text.contains("Float"))
        assertFalse(library("hct/Hct.kt").text.contains("Float"))
    }
}
