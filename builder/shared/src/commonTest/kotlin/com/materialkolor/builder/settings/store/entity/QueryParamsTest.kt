package com.materialkolor.builder.settings.store.entity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.settings.model.KeyColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueryParamsTest {

    @Test
    fun testSettingsEntityToQueryParams() {
        val settingsEntity = SettingsEntity(
            colors = mapOf(
                KeyColor.Seed to 0xFFFF0000.toInt(),
                KeyColor.Primary to 0xFF00FF00.toInt(),
                KeyColor.Secondary to 0xFF0000FF.toInt(),
            ),
            isDarkMode = true,
            contrast = Contrast.High.value,
            selectedPresetId = "preset1",
            style = PaletteStyle.Expressive,
        )

        val queryParams = settingsEntity.toQueryParams()

        val expected = """
            ?
            color_seed=FFFF0000&
            color_primary=FF00FF00&
            color_secondary=FF0000FF&
            dark_mode=true&
            style=Expressive&
            selected_preset_id=preset1&
            contrast=1.0
            """.trimIndent().replace("\n", "")

        assertEquals(expected, queryParams)
    }

    @Test
    fun testStringToSettingsEntity() {
        val queryParams = """
            color_seed=FFFF0000&
            color_primary=FF00FF00&
            color_secondary=FF0000FF&
            dark_mode=true&
            contrast=1.0&
            selected_preset_id=preset1
            &style=Expressive
            """.trimIndent().replace("\n", "")

        val settingsEntity = queryParams.toSettingsEntity()

        assertEquals(Color(0xFFFF0000).toArgb(), settingsEntity.colors[KeyColor.Seed])
        assertEquals(Color(0xFF00FF00).toArgb(), settingsEntity.colors[KeyColor.Primary])
        assertEquals(Color(0xFF0000FF).toArgb(), settingsEntity.colors[KeyColor.Secondary])
        assertEquals(true, settingsEntity.isDarkMode)
        assertEquals(Contrast.High.value, settingsEntity.contrast)
        assertEquals("preset1", settingsEntity.selectedPresetId)
        assertEquals(PaletteStyle.Expressive, settingsEntity.style)
    }

    @Test
    fun testSettingsEntityToQueryParamsWithEmptyValues() {
        val settingsEntity = SettingsEntity(
            colors = mapOf(),
            isDarkMode = null,
            contrast = Contrast.Default.value,
            selectedPresetId = null,
            style = PaletteStyle.TonalSpot,
        )

        val queryParams = settingsEntity.toQueryParams()

        assertEquals(
            expected = "?dark_mode=false",
            actual = queryParams,
        )
    }

    @Test
    fun testStringToSettingsEntityWithMissingValues() {
        val queryParams = "color_seed=FFFF0000&contrast=0.5&style=Neutral"

        val settingsEntity = queryParams.toSettingsEntity()

        assertEquals(Color(0xFFFF0000).toArgb(), settingsEntity.colors[KeyColor.Seed])
        assertEquals(null, settingsEntity.isDarkMode)
        assertEquals(0.5, settingsEntity.contrast)
        assertEquals(null, settingsEntity.selectedPresetId)
        assertEquals(PaletteStyle.Neutral, settingsEntity.style)
    }

    @Test
    fun testParsingWithQuestionMark() {
        val params = "?color_seed=FFCBDDEE"
        val entity = params.toSettingsEntity()
        assertTrue(entity.colors.containsKey(KeyColor.Seed))
        assertEquals(0xFFCBDDEE.toInt(), entity.colors[KeyColor.Seed])
    }

    @Test
    fun testAllKeyColorValues() {
        val settingsEntity = SettingsEntity(
            colors = KeyColor.entries.associateWith { 0xFF000000.toInt() + it.ordinal },
            isDarkMode = true,
            contrast = Contrast.Default.value,
            selectedPresetId = "test",
            style = PaletteStyle.TonalSpot,
        )

        val queryParams = settingsEntity.toQueryParams()
        val reconstructed = queryParams.toSettingsEntity()

        assertEquals(settingsEntity.colors.size, reconstructed.colors.size)
        for (keyColor in KeyColor.entries) {
            assertEquals(settingsEntity.colors[keyColor], reconstructed.colors[keyColor])
        }
    }

    @Test
    fun testAllPaletteStyleValues() {
        for (style in PaletteStyle.entries) {
            val settingsEntity = SettingsEntity(
                colors = mapOf(KeyColor.Seed to 0xFF000000.toInt()),
                isDarkMode = false,
                contrast = Contrast.Default.value,
                selectedPresetId = null,
                style = style,
            )

            val queryParams = settingsEntity.toQueryParams()
            val reconstructed = queryParams.toSettingsEntity()

            assertEquals(style, reconstructed.style)
        }
    }

    @Test
    fun testContrastEdgeCases() {
        val contrastValues = listOf(0.0, 0.5, 1.0, -1.0, 2.0)
        for (contrast in contrastValues) {
            val settingsEntity = SettingsEntity(
                colors = mapOf(KeyColor.Seed to 0xFF000000.toInt()),
                isDarkMode = false,
                contrast = contrast,
                selectedPresetId = null,
                style = PaletteStyle.TonalSpot,
            )

            val queryParams = settingsEntity.toQueryParams()
            val reconstructed = queryParams.toSettingsEntity()

            assertEquals(contrast, reconstructed.contrast)
        }
    }

    @Test
    fun testNullValues() {
        val settingsEntity = SettingsEntity(
            colors = mapOf(KeyColor.Seed to null),
            isDarkMode = null,
            contrast = Contrast.Default.value,
            selectedPresetId = null,
            style = PaletteStyle.TonalSpot,
        )

        val queryParams = settingsEntity.toQueryParams()
        val reconstructed = queryParams.toSettingsEntity()

        // Dark mode is always added
        assertNotNull(reconstructed.isDarkMode)
        assertNull(reconstructed.colors[KeyColor.Seed])
        assertNull(reconstructed.selectedPresetId)
    }

    @Test
    fun testEmptyStringValues() {
        val queryParams = "color_seed=&dark_mode=&contrast=&selected_preset_id=&style="
        val settingsEntity = queryParams.toSettingsEntity()

        assertEquals(emptyMap(), settingsEntity.colors)
        assertNull(settingsEntity.isDarkMode)
        assertEquals(Contrast.Default.value, settingsEntity.contrast)
        assertNull(settingsEntity.selectedPresetId)
        assertEquals(PaletteStyle.TonalSpot, settingsEntity.style)
    }

    @Test
    fun testInvalidInput() {
        val queryParams = "color_fake=ff112233color_seed=INVALID&dark_mode=NOT_BOOLEAN" +
            "&contrast=NOT_NUMBER&style=INVALID_STYLE"
        val settingsEntity = queryParams.toSettingsEntity()

        assertEquals(emptyMap(), settingsEntity.colors)
        assertNull(settingsEntity.isDarkMode)
        assertEquals(Contrast.Default.value, settingsEntity.contrast)
        assertEquals(PaletteStyle.TonalSpot, settingsEntity.style)
    }
}
