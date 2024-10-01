package com.materialkolor.builder.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import materialkolorbuilder.app.generated.resources.JetBrainsMonoNL_Bold
import materialkolorbuilder.app.generated.resources.JetBrainsMonoNL_ExtraLight
import materialkolorbuilder.app.generated.resources.JetBrainsMonoNL_Light
import materialkolorbuilder.app.generated.resources.JetBrainsMonoNL_Medium
import materialkolorbuilder.app.generated.resources.JetBrainsMonoNL_Regular
import materialkolorbuilder.app.generated.resources.JetBrainsMonoNL_SemiBold
import materialkolorbuilder.app.generated.resources.Res
import org.jetbrains.compose.resources.Font

val JetBrainsMono: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.JetBrainsMonoNL_Bold, weight = FontWeight.Bold),
        Font(Res.font.JetBrainsMonoNL_SemiBold, weight = FontWeight.SemiBold),
        Font(Res.font.JetBrainsMonoNL_Medium, weight = FontWeight.Medium),
        Font(Res.font.JetBrainsMonoNL_Regular, weight = FontWeight.Normal),
        Font(Res.font.JetBrainsMonoNL_Light, weight = FontWeight.Light),
        Font(Res.font.JetBrainsMonoNL_ExtraLight, weight = FontWeight.ExtraLight),
    )

val AppTypography
    @Composable
    get() = Typography().let { type ->
        type.copy(
            bodyLarge = type.bodyLarge.copy(fontWeight = FontWeight.ExtraLight),
            bodyMedium = type.bodyMedium.copy(fontWeight = FontWeight.ExtraLight),
            bodySmall = type.bodySmall.copy(fontWeight = FontWeight.ExtraLight),
            labelLarge = type.labelLarge.copy(fontWeight = FontWeight.Light),
            labelMedium = type.labelMedium.copy(fontWeight = FontWeight.Light),
            labelSmall = type.labelSmall.copy(fontWeight = FontWeight.Light),
        )
    }
