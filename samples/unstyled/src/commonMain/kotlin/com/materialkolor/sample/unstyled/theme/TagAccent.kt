package com.materialkolor.sample.unstyled.theme

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.unstyled.MaterialKolorTokens

internal val TaskTag.accent: ThemeToken<Color>
    get() = when (this) {
        TaskTag.Personal -> MaterialKolorTokens.primaryContainer
        TaskTag.Work -> MaterialKolorTokens.secondaryContainer
        TaskTag.Errand -> MaterialKolorTokens.tertiaryContainer
    }

internal val TaskTag.onAccent: ThemeToken<Color>
    get() = when (this) {
        TaskTag.Personal -> MaterialKolorTokens.onPrimaryContainer
        TaskTag.Work -> MaterialKolorTokens.onSecondaryContainer
        TaskTag.Errand -> MaterialKolorTokens.onTertiaryContainer
    }
