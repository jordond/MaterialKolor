package com.materialkolor.builder.ui.home.preview.device.components.content

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import materialkolorbuilder.app.generated.resources.Halant_Bold
import materialkolorbuilder.app.generated.resources.Halant_Light
import materialkolorbuilder.app.generated.resources.Halant_Medium
import materialkolorbuilder.app.generated.resources.Halant_Regular
import materialkolorbuilder.app.generated.resources.Halant_SemiBold
import materialkolorbuilder.app.generated.resources.Res
import org.jetbrains.compose.resources.Font

val PhoneFont: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.Halant_Light, weight = FontWeight.Light),
        Font(Res.font.Halant_Medium, weight = FontWeight.Medium),
        Font(Res.font.Halant_Regular, weight = FontWeight.Normal),
        Font(Res.font.Halant_SemiBold, weight = FontWeight.SemiBold),
        Font(Res.font.Halant_Bold, weight = FontWeight.Bold),
    )

val PhoneTypography: Typography
    @Composable
    get() = createTypographyWith(PhoneFont)

@Composable
private fun createTypographyWith(family: FontFamily): Typography {
    val base = Typography()
    if (LocalInspectionMode.current) return base

    return Typography().copy(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
    )
}
