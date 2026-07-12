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
            packageName = "foo.bar.biz.buzz"
        )

        val result = mkThemeKt("MyTheme", settings, animate = true)

        val expected = """
${header(settings)}
package foo.bar.biz.buzz

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun MyTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
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

        val result = mkThemeKt("MyTheme", settings, animate = false)

        val expected = """
${header(settings)}
package com.example.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun MyTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
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

        val result = mkThemeKt("MyTheme", settings, animate = true)

        val expected = """
${header(settings)}
package com.example.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun MyTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
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

    @Test
    fun testMkThemeKtWithMaterialExpressive() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF0000FF),
            ),
            isDarkMode = false,
            selectedImage = null,
            packageName = "foo.bar.biz.buzz",
            useMaterialExpressive = true,
        )

        val result = mkThemeKt("AppTheme", settings, animate = true)

        val expected = """
${header(settings)}
package foo.bar.biz.buzz

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
        style = PaletteStyle.TonalSpot,
        seedColor = SeedColor,
    )
    
    DynamicMaterialExpressiveTheme(
        state = dynamicThemeState,
        motionScheme = MotionScheme.expressive(),
        animate = true,
        content = content,
    )
}
""".trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun testMkThemeKtWithMaterialExpressiveAndAllColors() {
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
            contrast = Contrast.High,
            style = PaletteStyle.Vibrant,
            selectedImage = null,
            isAmoled = true,
            packageName = "test.expressive.app",
            useMaterialExpressive = true,
        )

        val result = mkThemeKt("ExpressiveTheme", settings, animate = false)

        val expected = """
${header(settings)}
package test.expressive.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun ExpressiveTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
        style = PaletteStyle.Vibrant,
        contrastLevel = 1.0,
        isAmoled = true,
        primary = Primary,
        secondary = Secondary,
        tertiary = Tertiary,
        error = Error,
        neutral = Neutral,
        neutralVariant = NeutralVariant,
    )
    
    DynamicMaterialExpressiveTheme(
        state = dynamicThemeState,
        motionScheme = MotionScheme.expressive(),
        animate = false,
        content = content,
    )
}
""".trimIndent()

        assertEquals(expected, result)
    }
}
