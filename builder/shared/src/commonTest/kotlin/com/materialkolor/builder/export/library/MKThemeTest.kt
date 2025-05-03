package com.materialkolor.builder.export.library

import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.export.model.header
import com.materialkolor.builder.export.model.library.mkThemeKt
import com.materialkolor.builder.settings.model.ColorSettings
import com.materialkolor.builder.settings.model.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class MKThemeTest {

    @Test
    fun testMkThemeKtWithAllColors() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF000000),
                primary = Color(0xFF111111),
                secondary = Color(0xFF222222),
                tertiary = Color(0xFF333333),
                error = Color(0xFF444444),
                neutral = Color(0xFF555555),
                neutralVariant = Color(0xFF666666),
            ),
            isDarkMode = false,
            contrast = Contrast.Default,
            style = PaletteStyle.TonalSpot,
            selectedImage = null,
            isAmoled = false,
        )

        val result = mkThemeKt("com.example", "MyTheme", settings, animate = true)

        val expected = """
${header(settings)}
package com.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = darkTheme,
        style = PaletteStyle.TonalSpot,
        primary = Primary,
        secondary = Secondary,
        tertiary = Tertiary,
        error = Error,
        neutral = Neutral,
        neutralVariant = NeutralVariant,
    )
    
    DynamicMaterialTheme(
        state = dynamicThemeState,
        animate = true,
        content = content,
    )
}
""".trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun testMkThemeKtWithSeedColor() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF000000),
                primary = null,
                secondary = null,
                tertiary = null,
                error = null,
                neutral = null,
                neutralVariant = null,
            ),
            isDarkMode = true,
            contrast = Contrast.High,
            style = PaletteStyle.Vibrant,
            selectedImage = null,
            isAmoled = true,
        )

        val result = mkThemeKt("com.example", "MyTheme", settings, animate = false)

        val expected = """
${header(settings)}
package com.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = darkTheme,
        style = PaletteStyle.Vibrant,
        contrastLevel = 1.0,
        isAmoled = true,
        seedColor = SeedColor,
    )
    
    DynamicMaterialTheme(
        state = dynamicThemeState,
        animate = false,
        content = content,
    )
}
""".trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun testMkThemeKtWithMixedColors() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF000000),
                primary = Color(0xFF111111),
                secondary = null,
                tertiary = Color(0xFF333333),
                error = null,
                neutral = Color(0xFF555555),
                neutralVariant = null,
            ),
            isDarkMode = false,
            contrast = Contrast.Medium,
            style = PaletteStyle.Expressive,
            selectedImage = null,
            isAmoled = false,
        )

        val result = mkThemeKt("com.example", "MyTheme", settings, animate = true)

        val expected = """
${header(settings)}
package com.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = darkTheme,
        style = PaletteStyle.Expressive,
        contrastLevel = 0.5,
        primary = Primary,
        tertiary = Tertiary,
        neutral = Neutral,
    )
    
    DynamicMaterialTheme(
        state = dynamicThemeState,
        animate = true,
        content = content,
    )
}
""".trimIndent()

        assertEquals(expected, result)
    }
}
