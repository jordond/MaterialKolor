package com.materialkolor.sample.unstyled.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The type scale. Colors stay out of it, text takes the content color unless it asks for another.
 */
internal object TasksType {
    /** The app title. */
    val Title: TextStyle = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)

    /** A dialog title. */
    val Heading: TextStyle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)

    /** The summary over the progress bar. */
    val Emphasis: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)

    /** Task titles and running text. The theme's default. */
    val Body: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)

    /** Buttons, tabs and pickers. */
    val Label: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)

    /** Group headings and counts. */
    val Caption: TextStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)

    /** Chips and swatch labels. */
    val Small: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
}
