package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One option of a picker or a tab strip.
 *
 * @property[value] What picking it selects.
 * @property[label] What it says, or its content description where only a swatch shows.
 * @property[testTag] The tag on its selectable node.
 * @property[icon] Shown before the label.
 * @property[badge] A short count after the label.
 */
@Immutable
internal data class Choice<T>(
    val value: T,
    val label: String,
    val testTag: String,
    val icon: ImageVector? = null,
    val badge: String? = null,
)
