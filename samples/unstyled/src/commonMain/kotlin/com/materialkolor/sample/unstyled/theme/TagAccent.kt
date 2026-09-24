package com.materialkolor.sample.unstyled.theme

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.unstyled.MaterialKolorTokens

/**
 * The token a chip of this tag is filled with. Each tag gets the container of its own accent.
 */
internal val TaskTag.accent: ThemeToken<Color>
    get() = when (this) {
        TaskTag.Personal -> MaterialKolorTokens.primaryContainer
        TaskTag.Work -> MaterialKolorTokens.secondaryContainer
        TaskTag.Errand -> MaterialKolorTokens.tertiaryContainer
    }

/**
 * The token the tag name is written in on its [accent].
 */
internal val TaskTag.onAccent: ThemeToken<Color>
    get() = when (this) {
        TaskTag.Personal -> MaterialKolorTokens.onPrimaryContainer
        TaskTag.Work -> MaterialKolorTokens.onSecondaryContainer
        TaskTag.Errand -> MaterialKolorTokens.onTertiaryContainer
    }
