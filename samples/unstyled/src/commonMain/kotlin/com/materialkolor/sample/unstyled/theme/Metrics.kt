package com.materialkolor.sample.unstyled.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale. Compose Unstyled ships none, so the app keeps its own, the way any app on it would.
 */
internal object Spacing {
    val XSmall: Dp = 4.dp

    /** Between [XSmall] and [Small], for a label and what sits beside it inside a control, or a row of chips. */
    val Tight: Dp = 6.dp

    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val XLarge: Dp = 24.dp
    val XXLarge: Dp = 32.dp
}

/**
 * The shapes every component draws with.
 */
internal object Shapes {
    /** A checkbox. */
    val Small: Shape = RoundedCornerShape(6.dp)

    /** Buttons, fields and the segments inside a picker. */
    val Control: Shape = RoundedCornerShape(8.dp)

    /** A tab, rounded only on top so its indicator sits flat on the line below. */
    val Tab: Shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)

    /** Cards, pickers and the dialog. */
    val Card: Shape = RoundedCornerShape(12.dp)

    /** Chips, filter tabs and the progress bar. */
    val Pill: Shape = RoundedCornerShape(percent = 50)

    /** Seed swatches and icon buttons. */
    val Round: Shape = CircleShape
}

/** How wide the page grows before it centers. */
internal val ContentMaxWidth: Dp = 720.dp

/** The height of a button or a text field. */
internal val ControlHeight: Dp = 40.dp

/** The size of an icon next to a label. */
internal val IconSize: Dp = 18.dp
