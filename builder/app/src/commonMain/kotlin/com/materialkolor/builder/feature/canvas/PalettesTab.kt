package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResult

// stub

/**
 * The Palettes tab, the tonal palettes the roles of [result] are drawn from (B-308).
 *
 * @param[result] The resolved theme the canvas shows.
 * @param[mode] Which modes to show, laid out like the Roles tab.
 * @param[filter] The vision filter the canvas is drawn through, or null for none.
 * @param[modifier] Applied to the tab.
 */
@Composable
internal fun PalettesTab(
    result: ThemeResult,
    mode: PreviewMode,
    filter: ColorMatrix?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize())
}
