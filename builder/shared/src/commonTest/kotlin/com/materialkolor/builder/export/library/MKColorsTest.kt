package com.materialkolor.builder.export.library

import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.export.model.library.mkColorsKt
import com.materialkolor.builder.settings.model.ColorSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MKColorsTest {

    @Test
    fun testMkColorsKtWithAllColors() {
        val colorSettings = ColorSettings(
            seed = Color(0xFF000000),
            primary = Color(0xFF111111),
            secondary = Color(0xFF222222),
            tertiary = Color(0xFF333333),
            error = Color(0xFF444444),
            neutral = Color(0xFF555555),
            neutralVariant = Color(0xFF666666),
        )

        val result = mkColorsKt("com.example", colorSettings)

        assertTrue(result.contains("package com.example"))
        assertTrue(result.contains("import androidx.compose.ui.graphics.Color"))
        assertTrue(result.contains("val Primary = Color(0xFF111111)"))
        assertTrue(result.contains("val Secondary = Color(0xFF222222)"))
        assertTrue(result.contains("val Tertiary = Color(0xFF333333)"))
        assertTrue(result.contains("val Error = Color(0xFF444444)"))
        assertTrue(result.contains("val Neutral = Color(0xFF555555)"))
        assertTrue(result.contains("val NeutralVariant = Color(0xFF666666)"))
        assertFalse(result.contains("val Seed = Color(0xFF000000)"))
    }

    @Test
    fun testMkColorsKtWithSeedColor() {
        val colorSettings = ColorSettings(
            seed = Color(0xFF000000),
            primary = null,
            secondary = Color(0xFF222222),
            tertiary = Color(0xFF333333),
            error = Color(0xFF444444),
            neutral = Color(0xFF555555),
            neutralVariant = Color(0xFF666666),
        )

        val result = mkColorsKt("com.example", colorSettings)

        assertTrue(result.contains("val SeedColor = Color(0xFF000000)"))
        assertFalse(result.contains("val Primary = "))
    }

    @Test
    fun testMkColorsKtWithMissingColors() {
        val colorSettings = ColorSettings(
            seed = Color(0xFF000000),
            primary = Color(0xFF111111),
            secondary = null,
            tertiary = null,
            error = Color(0xFF444444),
            neutral = null,
            neutralVariant = Color(0xFF666666),
        )

        val result = mkColorsKt("com.example", colorSettings)

        assertTrue(result.contains("val Primary = Color(0xFF111111)"))
        assertTrue(result.contains("val Error = Color(0xFF444444)"))
        assertTrue(result.contains("val NeutralVariant = Color(0xFF666666)"))
        assertFalse(result.contains("val Secondary = "))
        assertFalse(result.contains("val Tertiary = "))
        assertFalse(result.contains("val Neutral = "))
        assertFalse(result.contains("val Seed = "))
    }

    @Test
    fun testMkColorsKtWithDifferentPackageName() {
        val colorSettings = ColorSettings(
            seed = Color(0xFF000000),
            primary = Color(0xFF111111),
            secondary = Color(0xFF222222),
            tertiary = Color(0xFF333333),
            error = Color(0xFF444444),
            neutral = Color(0xFF555555),
            neutralVariant = Color(0xFF666666),
        )

        val result = mkColorsKt("com.differentpackage", colorSettings)

        assertTrue(result.contains("package com.differentpackage"))
    }

    @Test
    fun testMkColorsKtOutputFormat() {
        val colorSettings = ColorSettings(
            seed = Color(0xFF000000),
            primary = Color(0xFF111111),
            secondary = Color(0xFF222222),
            tertiary = null,
            error = null,
            neutral = Color(0xFF555555),
            neutralVariant = null,
        )

        val result = mkColorsKt("com.example", colorSettings)

        val expectedOutput = """
        package com.example
        
        import androidx.compose.ui.graphics.Color
        
        val Primary = Color(0xFF111111)
        val Secondary = Color(0xFF222222)
        val Neutral = Color(0xFF555555)
        """.trimIndent()

        assertEquals(expectedOutput, result)
    }

    @Test
    fun testMkColorsKtWithOnlyPrimaryColor() {
        val colorSettings = ColorSettings(
            seed = Color(0xFF000000),
            primary = Color(0xFF111111),
            secondary = null,
            tertiary = null,
            error = null,
            neutral = null,
            neutralVariant = null,
        )

        val result = mkColorsKt("com.example", colorSettings)

        val expectedOutput = """
            package com.example
            
            import androidx.compose.ui.graphics.Color
            
            val Primary = Color(0xFF111111)
        """.trimIndent()

        assertEquals(expectedOutput, result)
    }

    @Test
    fun testMkColorsKtWithNoColors() {
        val colorSettings = ColorSettings(
            seed = Color(0xFF000000),
            primary = null,
            secondary = null,
            tertiary = null,
            error = null,
            neutral = null,
            neutralVariant = null,
        )

        val result = mkColorsKt("com.example", colorSettings)

        val expectedOutput = """
            package com.example
            
            import androidx.compose.ui.graphics.Color
            
            val SeedColor = Color(0xFF000000)
        """.trimIndent()

        assertEquals(expectedOutput, result)
    }
}
