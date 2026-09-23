package com.materialkolor.builder.kit.token

import androidx.compose.ui.text.font.FontFamily
import com.materialkolor.builder.kit.generated.resources.BricolageGrotesque_Variable
import com.materialkolor.builder.kit.generated.resources.JetBrainsMono_Variable
import com.materialkolor.builder.kit.generated.resources.Res
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BuilderFontTest {
    @Test
    fun brandFont_readFromResources_decodesAsBricolageGrotesque() =
        runTest {
            val typeface = load(Res.font.BricolageGrotesque_Variable)

            assertTrue(
                typeface.familyName.contains("Bricolage"),
                "expected the brand face, got ${typeface.familyName}",
            )
            assertTrue(typeface.glyphsCount > 200, "expected a Latin subset, got ${typeface.glyphsCount} glyphs")
        }

    @Test
    fun monoFont_readFromResources_decodesAsJetBrainsMono() =
        runTest {
            val typeface = load(Res.font.JetBrainsMono_Variable)

            assertTrue(
                typeface.familyName.contains("JetBrains"),
                "expected the mono face, got ${typeface.familyName}",
            )
            assertTrue(typeface.glyphsCount > 200, "expected a Latin subset, got ${typeface.glyphsCount} glyphs")
        }

    @Test
    fun builderType_builtFromTheTwoFaces_putsTheMonoFaceOnValuesAndCode() {
        val brand = FontFamily.Cursive
        val mono = FontFamily.Monospace

        val type = builderType(brand = brand, mono = mono)

        type.posterHero.fontFamily shouldBe brand
        type.wordmark.fontFamily shouldBe brand
        type.title.fontFamily shouldBe brand
        type.sectionLabel.fontFamily shouldBe brand
        type.body.fontFamily shouldBe brand
        type.label.fontFamily shouldBe brand
        type.value.fontFamily shouldBe mono
        type.code.fontFamily shouldBe mono
    }

    private suspend fun load(resource: FontResource): Typeface {
        val bytes = getFontResourceBytes(getSystemResourceEnvironment(), resource)
        return assertNotNull(
            FontMgr.default.makeFromData(Data.makeFromBytes(bytes)),
            "Skia could not decode the subset, which means the face would silently fall back",
        )
    }
}
