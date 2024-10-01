package com.materialkolor.builder.export.model.library

import com.materialkolor.builder.export.variable
import com.materialkolor.builder.settings.model.ColorSettings

fun mkColorsKt(
    packageName: String,
    colors: ColorSettings,
): String {
    val colorList = listOfNotNull(
        if (colors.primary == null) colors.seed.variable("Seed")
        else colors.primary.variable("Primary"),
        colors.secondary.variable("Secondary"),
        colors.tertiary.variable("Tertiary"),
        colors.error.variable("Error"),
        colors.neutral.variable("Neutral"),
        colors.neutralVariant.variable("NeutralVariant"),
    )

    return """
    package $packageName
    
    import androidx.compose.ui.graphics.Color
    
    ${colorList.joinToString("\n    ")}
    """.trimIndent()
}
