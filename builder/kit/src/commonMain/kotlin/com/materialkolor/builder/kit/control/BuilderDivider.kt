package com.materialkolor.builder.kit.control

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledVerticalSeparator
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentDivider
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.DividerStyle
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.material.MaterialDivider

/**
 * A hairline between two regions.
 *
 * It is decoration, so a screen reader skips it.
 *
 * @param[modifier] Applied to the line.
 * @param[orientation] Horizontal to split a column, vertical to split a row.
 */
@Composable
public fun BuilderDivider(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialDivider(modifier, orientation)
        Library.Unstyled -> HeadlessDivider(UnstyledActionStyles.divider, modifier, orientation)
        Library.Fluent -> FluentDivider(modifier, orientation) // fluent-placeholder
        Library.Custom -> HeadlessDivider(CustomActionStyles.divider, modifier, orientation)
    }
}

/** A divider drawn from [style] with Compose Unstyled's separators. */
@Composable
internal fun HeadlessDivider(
    style: DividerStyle,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
) {
    when (orientation) {
        Orientation.Horizontal -> UnstyledHorizontalSeparator(style.color, modifier, style.thickness)
        Orientation.Vertical -> UnstyledVerticalSeparator(style.color, modifier, style.thickness)
    }
}
