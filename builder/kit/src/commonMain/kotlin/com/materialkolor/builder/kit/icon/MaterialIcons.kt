/*
 * The path data below is copied from the Material Icons Rounded set, as shipped in the
 * androidx.compose.material:material-icons-core 1.7.6 and material-icons-extended 1.7.6 sources.
 * Share, Check, Close, Search, Lock, Info, Add, Delete and Warning come from the core set, every
 * other glyph from the extended set.
 *
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.materialkolor.builder.kit.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The Material3 set, the Rounded Material Icons drawn from path data kept in this file.
 *
 * The data is hand copied so kit does not pull in the whole extended icon pack for 42 glyphs. Each
 * glyph is built the first time it is asked for and kept after that, on the UI thread only.
 */
internal object MaterialIcons : BuilderIcons {
    private val vectors = arrayOfNulls<ImageVector>(IconId.entries.size)

    override fun get(id: IconId): ImageVector =
        vectors[id.ordinal] ?: glyph(id).toVector(id).also { vector -> vectors[id.ordinal] = vector }
}

/** A glyph on the 24 by 24 Material grid, one or more filled paths in SVG path syntax. */
private class MaterialGlyph(
    vararg val paths: String,
    val autoMirror: Boolean = false,
    val evenOdd: Boolean = false,
)

private fun MaterialGlyph.toVector(id: IconId): ImageVector {
    val builder = ImageVector.Builder(
        name = "Material.${id.name}",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    )
    for (data in paths) {
        builder.addPath(
            pathData = addPathNodes(data),
            pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
            fill = SolidColor(Color.Black),
        )
    }
    return builder.build()
}

/** Joins the pieces of one path, split only so each line stays readable. */
private fun path(vararg parts: String): String = parts.joinToString(separator = "")

private fun glyph(id: IconId): MaterialGlyph =
    when (id) {
        // AutoMirrored.Rounded.Undo
        IconId.Undo -> MaterialGlyph(
            path(
                "M12.5 8c-2.65 0 -5.05 0.99 -6.9 2.6L3.71 8.71C3.08 8.08 2 8.52 2 9.41V15c0 0.55 0.45 1 1 1",
                "h5.59c0.89 0 1.34 -1.08 0.71 -1.71l-1.91 -1.91c1.39 -1.16 3.16 -1.88 5.12 -1.88",
                "c3.16 0 5.89 1.84 7.19 4.5c0.27 0.56 0.91 0.84 1.5 0.64c0.71 -0.23 1.07 -1.04 0.75 -1.72",
                "C20.23 10.42 16.65 8 12.5 8Z",
            ),
            autoMirror = true,
        )
        // AutoMirrored.Rounded.Redo
        IconId.Redo -> MaterialGlyph(
            path(
                "M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.16 0 -7.74 2.42 -9.44 5.93",
                "c-0.32 0.67 0.04 1.47 0.75 1.71c0.59 0.2 1.23 -0.08 1.5 -0.64c1.3 -2.66 4.03 -4.5 7.19 -4.5",
                "c1.95 0 3.73 0.72 5.12 1.88l-1.91 1.91c-0.63 0.63 -0.19 1.71 0.7 1.71H21c0.55 0 1 -0.45 1 -1",
                "V9.41c0 -0.89 -1.08 -1.34 -1.71 -0.71l-1.89 1.9Z",
            ),
            autoMirror = true,
        )
        // Rounded.Share
        IconId.Share -> MaterialGlyph(
            path(
                "M18 16.08c-0.76 0 -1.44 0.3 -1.96 0.77L8.91 12.7c0.05 -0.23 0.09 -0.46 0.09 -0.7",
                "s-0.04 -0.47 -0.09 -0.7l7.05 -4.11c0.54 0.5 1.25 0.81 2.04 0.81c1.66 0 3 -1.34 3 -3",
                "s-1.34 -3 -3 -3s-3 1.34 -3 3c0 0.24 0.04 0.47 0.09 0.7L8.04 9.81C7.5 9.31 6.79 9 6 9",
                "c-1.66 0 -3 1.34 -3 3s1.34 3 3 3c0.79 0 1.5 -0.31 2.04 -0.81l7.12 4.16",
                "c-0.05 0.21 -0.08 0.43 -0.08 0.65c0 1.61 1.31 2.92 2.92 2.92s2.92 -1.31 2.92 -2.92",
                "s-1.31 -2.92 -2.92 -2.92Z",
            ),
        )
        // Rounded.IosShare
        IconId.Export -> MaterialGlyph(
            path(
                "M18 8h-2c-0.55 0 -1 0.45 -1 1v0c0 0.55 0.45 1 1 1h2v11H6V10h2c0.55 0 1 -0.45 1 -1v0",
                "c0 -0.55 -0.45 -1 -1 -1H6c-1.1 0 -2 0.9 -2 2v11c0 1.1 0.9 2 2 2h12c1.1 0 2 -0.9 2 -2V10",
                "C20 8.9 19.1 8 18 8Z",
            ),
            path(
                "M12 16L12 16c0.55 0 1 -0.45 1 -1V5h1.79c0.45 0 0.67 -0.54 0.35 -0.85l-2.79 -2.79",
                "c-0.2 -0.2 -0.51 -0.2 -0.71 0L8.85 4.15C8.54 4.46 8.76 5 9.21 5H11v10C11 15.55 11.45 16 12 16Z",
            ),
        )
        // Rounded.ContentCopy
        IconId.Copy -> MaterialGlyph(
            path(
                "M15 20H5V7c0 -0.55 -0.45 -1 -1 -1h0C3.45 6 3 6.45 3 7v13c0 1.1 0.9 2 2 2h10",
                "c0.55 0 1 -0.45 1 -1v0C16 20.45 15.55 20 15 20ZM20 16V4c0 -1.1 -0.9 -2 -2 -2H9C7.9 2 7 2.9 7 4",
                "v12c0 1.1 0.9 2 2 2h9C19.1 18 20 17.1 20 16ZM18 16H9V4h9V16Z",
            ),
        )
        // Rounded.Check
        IconId.Check -> MaterialGlyph(
            path(
                "M9 16.17L5.53 12.7c-0.39 -0.39 -1.02 -0.39 -1.41 0c-0.39 0.39 -0.39 1.02 0 1.41l4.18 4.18",
                "c0.39 0.39 1.02 0.39 1.41 0L20.29 7.71c0.39 -0.39 0.39 -1.02 0 -1.41",
                "c-0.39 -0.39 -1.02 -0.39 -1.41 0L9 16.17Z",
            ),
        )
        // Rounded.Close
        IconId.Close -> MaterialGlyph(
            path(
                "M18.3 5.71c-0.39 -0.39 -1.02 -0.39 -1.41 0L12 10.59L7.11 5.7c-0.39 -0.39 -1.02 -0.39 -1.41 0",
                "c-0.39 0.39 -0.39 1.02 0 1.41L10.59 12L5.7 16.89c-0.39 0.39 -0.39 1.02 0 1.41",
                "c0.39 0.39 1.02 0.39 1.41 0L12 13.41l4.89 4.89c0.39 0.39 1.02 0.39 1.41 0",
                "c0.39 -0.39 0.39 -1.02 0 -1.41L13.41 12l4.89 -4.89c0.38 -0.38 0.38 -1.02 0 -1.4Z",
            ),
        )
        // Rounded.Search
        IconId.Search -> MaterialGlyph(
            path(
                "M15.5 14h-0.79l-0.28 -0.27c1.2 -1.4 1.82 -3.31 1.48 -5.34c-0.47 -2.78 -2.79 -5 -5.59 -5.34",
                "c-4.23 -0.52 -7.79 3.04 -7.27 7.27c0.34 2.8 2.56 5.12 5.34 5.59",
                "c2.03 0.34 3.94 -0.28 5.34 -1.48l0.27 0.28v0.79l4.25 4.25c0.41 0.41 1.08 0.41 1.49 0",
                "c0.41 -0.41 0.41 -1.08 0 -1.49L15.5 14ZM9.5 14C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5",
                "S14 7.01 14 9.5S11.99 14 9.5 14Z",
            ),
        )
        // Rounded.KeyboardCommandKey
        IconId.Command -> MaterialGlyph(
            path(
                "M17.5 3C15.57 3 14 4.57 14 6.5V8h-4V6.5C10 4.57 8.43 3 6.5 3S3 4.57 3 6.5S4.57 10 6.5 10H8v4",
                "H6.5C4.57 14 3 15.57 3 17.5S4.57 21 6.5 21s3.5 -1.57 3.5 -3.5V16h4v1.5c0 1.93 1.57 3.5 3.5 3.5",
                "s3.5 -1.57 3.5 -3.5S19.43 14 17.5 14H16v-4h1.5c1.93 0 3.5 -1.57 3.5 -3.5S19.43 3 17.5 3L17.5 3",
                "ZM16 8V6.5C16 5.67 16.67 5 17.5 5S19 5.67 19 6.5S18.33 8 17.5 8H16L16 8ZM6.5 8",
                "C5.67 8 5 7.33 5 6.5S5.67 5 6.5 5S8 5.67 8 6.5V8H6.5L6.5 8ZM10 14v-4h4v4H10L10 14ZM17.5 19",
                "c-0.83 0 -1.5 -0.67 -1.5 -1.5V16h1.5c0.83 0 1.5 0.67 1.5 1.5S18.33 19 17.5 19L17.5 19ZM6.5 19",
                "C5.67 19 5 18.33 5 17.5S5.67 16 6.5 16H8v1.5C8 18.33 7.33 19 6.5 19L6.5 19Z",
            ),
        )
        // Rounded.Shuffle
        IconId.Shuffle -> MaterialGlyph(
            path(
                "M10.59 9.17L6.12 4.7c-0.39 -0.39 -1.02 -0.39 -1.41 0c-0.39 0.39 -0.39 1.02 0 1.41l4.46 4.46",
                "l1.42 -1.4ZM15.35 4.85l1.19 1.19L4.7 17.88c-0.39 0.39 -0.39 1.02 0 1.41",
                "c0.39 0.39 1.02 0.39 1.41 0L17.96 7.46l1.19 1.19c0.31 0.31 0.85 0.09 0.85 -0.36L20 4.5",
                "c0 -0.28 -0.22 -0.5 -0.5 -0.5h-3.79c-0.45 0 -0.67 0.54 -0.36 0.85ZM14.83 13.41l-1.41 1.41",
                "l3.13 3.13l-1.2 1.2c-0.31 0.31 -0.09 0.85 0.36 0.85h3.79c0.28 0 0.5 -0.22 0.5 -0.5v-3.79",
                "c0 -0.45 -0.54 -0.67 -0.85 -0.35l-1.19 1.19l-3.13 -3.14Z",
            ),
        )
        // Rounded.Colorize
        IconId.Eyedropper -> MaterialGlyph(
            path(
                "M20.71 5.63l-2.34 -2.34c-0.39 -0.39 -1.02 -0.39 -1.41 0l-3.12 3.12l-1.23 -1.21",
                "c-0.39 -0.39 -1.02 -0.38 -1.41 0c-0.39 0.39 -0.39 1.02 0 1.41l0.72 0.72l-8.77 8.77",
                "c-0.1 0.1 -0.15 0.22 -0.15 0.36v4.04c0 0.28 0.22 0.5 0.5 0.5h4.04c0.13 0 0.26 -0.05 0.35 -0.15",
                "l8.77 -8.77l0.72 0.72c0.39 0.39 1.02 0.39 1.41 0c0.39 -0.39 0.39 -1.02 0 -1.41l-1.22 -1.22",
                "l3.12 -3.12c0.41 -0.4 0.41 -1.03 0.02 -1.42ZM6.92 19L5 17.08l8.06 -8.06l1.92 1.92L6.92 19Z",
            ),
        )
        // Rounded.Image
        IconId.Image -> MaterialGlyph(
            path(
                "M21 19V5c0 -1.1 -0.9 -2 -2 -2H5c-1.1 0 -2 0.9 -2 2v14c0 1.1 0.9 2 2 2h14c1.1 0 2 -0.9 2 -2Z",
                "M8.9 13.98l2.1 2.53l3.1 -3.99c0.2 -0.26 0.6 -0.26 0.8 0.01l3.51 4.68",
                "c0.25 0.33 0.01 0.8 -0.4 0.8H6.02c-0.42 0 -0.65 -0.48 -0.39 -0.81L8.12 14",
                "c0.19 -0.26 0.57 -0.27 0.78 -0.02Z",
            ),
        )
        // Rounded.Upload
        IconId.Upload -> MaterialGlyph(
            path(
                "M10 16h4c0.55 0 1 -0.45 1 -1v-5h1.59c0.89 0 1.34 -1.08 0.71 -1.71L12.71 3.7",
                "c-0.39 -0.39 -1.02 -0.39 -1.41 0L6.71 8.29c-0.63 0.63 -0.19 1.71 0.7 1.71L9 10v5",
                "c0 0.55 0.45 1 1 1ZM6 18h12c0.55 0 1 0.45 1 1s-0.45 1 -1 1L6 20c-0.55 0 -1 -0.45 -1 -1",
                "s0.45 -1 1 -1Z",
            ),
        )
        // Rounded.Lock
        IconId.Lock -> MaterialGlyph(
            path(
                "M18 8h-1L17 6c0 -2.76 -2.24 -5 -5 -5S7 3.24 7 6v2L6 8c-1.1 0 -2 0.9 -2 2v10c0 1.1 0.9 2 2 2h12",
                "c1.1 0 2 -0.9 2 -2L20 10c0 -1.1 -0.9 -2 -2 -2ZM12 17c-1.1 0 -2 -0.9 -2 -2s0.9 -2 2 -2",
                "s2 0.9 2 2s-0.9 2 -2 2ZM9 8L9 6c0 -1.66 1.34 -3 3 -3s3 1.34 3 3v2L9 8Z",
            ),
        )
        // Rounded.LockOpen
        IconId.Unlock -> MaterialGlyph(
            path(
                "M12 13c-1.1 0 -2 0.9 -2 2s0.9 2 2 2s2 -0.9 2 -2s-0.9 -2 -2 -2ZM18 8h-1L17 6",
                "c0 -2.76 -2.24 -5 -5 -5c-2.28 0 -4.27 1.54 -4.84 3.75c-0.14 0.54 0.18 1.08 0.72 1.22",
                "c0.53 0.14 1.08 -0.18 1.22 -0.72C9.44 3.93 10.63 3 12 3c1.65 0 3 1.35 3 3v2L6 8",
                "c-1.1 0 -2 0.9 -2 2v10c0 1.1 0.9 2 2 2h12c1.1 0 2 -0.9 2 -2L20 10c0 -1.1 -0.9 -2 -2 -2ZM18 19",
                "c0 0.55 -0.45 1 -1 1L7 20c-0.55 0 -1 -0.45 -1 -1v-8c0 -0.55 0.45 -1 1 -1h10c0.55 0 1 0.45 1 1",
                "v8Z",
            ),
        )
        // Rounded.PushPin
        IconId.Pin -> MaterialGlyph(
            path(
                "M19 12.87c0 -0.47 -0.34 -0.85 -0.8 -0.98C16.93 11.54 16 10.38 16 9V4l1 0c0.55 0 1 -0.45 1 -1",
                "c0 -0.55 -0.45 -1 -1 -1H7C6.45 2 6 2.45 6 3c0 0.55 0.45 1 1 1l1 0v5",
                "c0 1.38 -0.93 2.54 -2.2 2.89C5.34 12.02 5 12.4 5 12.87V13c0 0.55 0.45 1 1 1h4.98L11 21",
                "c0 0.55 0.45 1 1 1c0.55 0 1 -0.45 1 -1l-0.02 -7H18c0.55 0 1 -0.45 1 -1V12.87Z",
            ),
            evenOdd = true,
        )
        // Rounded.Info
        IconId.Info -> MaterialGlyph(
            path(
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10 -4.48 10 -10S17.52 2 12 2ZM12 17",
                "c-0.55 0 -1 -0.45 -1 -1v-4c0 -0.55 0.45 -1 1 -1s1 0.45 1 1v4c0 0.55 -0.45 1 -1 1ZM13 9h-2L11 7",
                "h2v2Z",
            ),
        )
        // Rounded.ExpandMore
        IconId.ChevronDown -> MaterialGlyph(
            path(
                "M15.88 9.29L12 13.17L8.12 9.29c-0.39 -0.39 -1.02 -0.39 -1.41 0c-0.39 0.39 -0.39 1.02 0 1.41",
                "l4.59 4.59c0.39 0.39 1.02 0.39 1.41 0l4.59 -4.59c0.39 -0.39 0.39 -1.02 0 -1.41",
                "c-0.39 -0.38 -1.03 -0.39 -1.42 0Z",
            ),
        )
        // Rounded.ChevronLeft
        IconId.ChevronLeft -> MaterialGlyph(
            path(
                "M14.71 6.71c-0.39 -0.39 -1.02 -0.39 -1.41 0L8.71 11.3c-0.39 0.39 -0.39 1.02 0 1.41l4.59 4.59",
                "c0.39 0.39 1.02 0.39 1.41 0c0.39 -0.39 0.39 -1.02 0 -1.41L10.83 12l3.88 -3.88",
                "c0.39 -0.39 0.38 -1.03 0 -1.41Z",
            ),
        )
        // Rounded.ChevronRight
        IconId.ChevronRight -> MaterialGlyph(
            path(
                "M9.29 6.71c-0.39 0.39 -0.39 1.02 0 1.41L13.17 12l-3.88 3.88c-0.39 0.39 -0.39 1.02 0 1.41",
                "c0.39 0.39 1.02 0.39 1.41 0l4.59 -4.59c0.39 -0.39 0.39 -1.02 0 -1.41L10.7 6.7",
                "c-0.38 -0.38 -1.02 -0.38 -1.41 0.01Z",
            ),
        )
        // Rounded.LightMode
        IconId.Sun -> MaterialGlyph(
            path(
                "M12 7c-2.76 0 -5 2.24 -5 5s2.24 5 5 5s5 -2.24 5 -5S14.76 7 12 7L12 7ZM2 13l2 0",
                "c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1l-2 0c-0.55 0 -1 0.45 -1 1S1.45 13 2 13ZM20 13l2 0",
                "c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1l-2 0c-0.55 0 -1 0.45 -1 1S19.45 13 20 13ZM11 2v2",
                "c0 0.55 0.45 1 1 1s1 -0.45 1 -1V2c0 -0.55 -0.45 -1 -1 -1S11 1.45 11 2ZM11 20v2",
                "c0 0.55 0.45 1 1 1s1 -0.45 1 -1v-2c0 -0.55 -0.45 -1 -1 -1C11.45 19 11 19.45 11 20ZM5.99 4.58",
                "c-0.39 -0.39 -1.03 -0.39 -1.41 0c-0.39 0.39 -0.39 1.03 0 1.41l1.06 1.06",
                "c0.39 0.39 1.03 0.39 1.41 0s0.39 -1.03 0 -1.41L5.99 4.58ZM18.36 16.95",
                "c-0.39 -0.39 -1.03 -0.39 -1.41 0c-0.39 0.39 -0.39 1.03 0 1.41l1.06 1.06",
                "c0.39 0.39 1.03 0.39 1.41 0c0.39 -0.39 0.39 -1.03 0 -1.41L18.36 16.95ZM19.42 5.99",
                "c0.39 -0.39 0.39 -1.03 0 -1.41c-0.39 -0.39 -1.03 -0.39 -1.41 0l-1.06 1.06",
                "c-0.39 0.39 -0.39 1.03 0 1.41s1.03 0.39 1.41 0L19.42 5.99ZM7.05 18.36",
                "c0.39 -0.39 0.39 -1.03 0 -1.41c-0.39 -0.39 -1.03 -0.39 -1.41 0l-1.06 1.06",
                "c-0.39 0.39 -0.39 1.03 0 1.41s1.03 0.39 1.41 0L7.05 18.36Z",
            ),
        )
        // Rounded.DarkMode
        IconId.Moon -> MaterialGlyph(
            path(
                "M11.01 3.05C6.51 3.54 3 7.36 3 12c0 4.97 4.03 9 9 9c4.63 0 8.45 -3.5 8.95 -8",
                "c0.09 -0.79 -0.78 -1.42 -1.54 -0.95c-0.84 0.54 -1.84 0.85 -2.91 0.85",
                "c-2.98 0 -5.4 -2.42 -5.4 -5.4c0 -1.06 0.31 -2.06 0.84 -2.89C12.39 3.94 11.9 2.98 11.01 3.05Z",
            ),
        )
        // Rounded.VerticalSplit
        IconId.Split -> MaterialGlyph(
            path(
                "M4 15h6c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1L4 13c-0.55 0 -1 0.45 -1 1s0.45 1 1 1ZM4 19h6",
                "c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1L4 17c-0.55 0 -1 0.45 -1 1s0.45 1 1 1ZM4 11h6",
                "c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1L4 9c-0.55 0 -1 0.45 -1 1s0.45 1 1 1ZM3 6c0 0.55 0.45 1 1 1",
                "h6c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1L4 5c-0.55 0 -1 0.45 -1 1ZM14 5h6c0.55 0 1 0.45 1 1v12",
                "c0 0.55 -0.45 1 -1 1h-6c-0.55 0 -1 -0.45 -1 -1L13 6c0 -0.55 0.45 -1 1 -1Z",
            ),
        )
        // Rounded.Smartphone
        IconId.Phone -> MaterialGlyph(
            path(
                "M17 1.01L7 1c-1.1 0 -2 0.9 -2 2v18c0 1.1 0.9 2 2 2h10c1.1 0 2 -0.9 2 -2V3",
                "c0 -1.1 -0.9 -1.99 -2 -1.99ZM17 19H7V5h10v14Z",
            ),
        )
        // Rounded.TabletAndroid
        IconId.Tablet -> MaterialGlyph(
            path(
                "M18 0L6 0C4.34 0 3 1.34 3 3v18c0 1.66 1.34 3 3 3h12c1.66 0 3 -1.34 3 -3L21 3",
                "c0 -1.66 -1.34 -3 -3 -3ZM13.5 22h-3c-0.28 0 -0.5 -0.22 -0.5 -0.5s0.22 -0.5 0.5 -0.5h3",
                "c0.28 0 0.5 0.22 0.5 0.5s-0.22 0.5 -0.5 0.5ZM19.25 19L4.75 19L4.75 3h14.5v16Z",
            ),
        )
        // Rounded.DesktopWindows
        IconId.Desktop -> MaterialGlyph(
            path(
                "M21 2L3 2c-1.1 0 -2 0.9 -2 2v12c0 1.1 0.9 2 2 2h7v2L9 20c-0.55 0 -1 0.45 -1 1s0.45 1 1 1h6",
                "c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1h-1v-2h7c1.1 0 2 -0.9 2 -2L23 4c0 -1.1 -0.9 -2 -2 -2ZM20 16",
                "L4 16c-0.55 0 -1 -0.45 -1 -1L3 5c0 -0.55 0.45 -1 1 -1h16c0.55 0 1 0.45 1 1v10",
                "c0 0.55 -0.45 1 -1 1Z",
            ),
        )
        // AutoMirrored.Rounded.ManageSearch
        IconId.Inspect -> MaterialGlyph(
            path(
                "M6 9H3C2.45 9 2 8.55 2 8v0c0 -0.55 0.45 -1 1 -1h3c0.55 0 1 0.45 1 1v0C7 8.55 6.55 9 6 9ZM6 12",
                "H3c-0.55 0 -1 0.45 -1 1v0c0 0.55 0.45 1 1 1h3c0.55 0 1 -0.45 1 -1v0C7 12.45 6.55 12 6 12Z",
                "M19.88 18.29l-3.12 -3.12c-0.86 0.56 -1.89 0.88 -3 0.82c-2.37 -0.11 -4.4 -1.96 -4.72 -4.31",
                "C8.6 8.33 11.49 5.5 14.87 6.07c1.95 0.33 3.57 1.85 4 3.78c0.33 1.46 0.01 2.82 -0.7 3.9",
                "l3.13 3.13c0.39 0.39 0.39 1.02 0 1.41l0 0C20.91 18.68 20.27 18.68 19.88 18.29ZM17 11",
                "c0 -1.65 -1.35 -3 -3 -3s-3 1.35 -3 3s1.35 3 3 3S17 12.65 17 11ZM3 19h8c0.55 0 1 -0.45 1 -1v0",
                "c0 -0.55 -0.45 -1 -1 -1H3c-0.55 0 -1 0.45 -1 1v0C2 18.55 2.45 19 3 19Z",
            ),
            autoMirror = true,
        )
        // Rounded.Visibility
        IconId.Vision -> MaterialGlyph(
            path(
                "M12 4C7 4 2.73 7.11 1 11.5C2.73 15.89 7 19 12 19s9.27 -3.11 11 -7.5C21.27 7.11 17 4 12 4Z",
                "M12 16.5c-2.76 0 -5 -2.24 -5 -5s2.24 -5 5 -5s5 2.24 5 5s-2.24 5 -5 5ZM12 8.5",
                "c-1.66 0 -3 1.34 -3 3s1.34 3 3 3s3 -1.34 3 -3s-1.34 -3 -3 -3Z",
            ),
        )
        // Rounded.Fullscreen
        IconId.Fullscreen -> MaterialGlyph(
            path(
                "M6 14c-0.55 0 -1 0.45 -1 1v3c0 0.55 0.45 1 1 1h3c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1L7 17v-2",
                "c0 -0.55 -0.45 -1 -1 -1ZM6 10c0.55 0 1 -0.45 1 -1L7 7h2c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1L6 5",
                "c-0.55 0 -1 0.45 -1 1v3c0 0.55 0.45 1 1 1ZM17 17h-2c-0.55 0 -1 0.45 -1 1s0.45 1 1 1h3",
                "c0.55 0 1 -0.45 1 -1v-3c0 -0.55 -0.45 -1 -1 -1s-1 0.45 -1 1v2ZM14 6c0 0.55 0.45 1 1 1h2v2",
                "c0 0.55 0.45 1 1 1s1 -0.45 1 -1L19 6c0 -0.55 -0.45 -1 -1 -1h-3c-0.55 0 -1 0.45 -1 1Z",
            ),
        )
        // Rounded.MoreHoriz
        IconId.More -> MaterialGlyph(
            path(
                "M6 10c-1.1 0 -2 0.9 -2 2s0.9 2 2 2s2 -0.9 2 -2s-0.9 -2 -2 -2ZM18 10c-1.1 0 -2 0.9 -2 2",
                "s0.9 2 2 2s2 -0.9 2 -2s-0.9 -2 -2 -2ZM12 10c-1.1 0 -2 0.9 -2 2s0.9 2 2 2s2 -0.9 2 -2",
                "s-0.9 -2 -2 -2Z",
            ),
        )
        // Rounded.Add
        IconId.Plus -> MaterialGlyph(
            path(
                "M18 13h-5v5c0 0.55 -0.45 1 -1 1s-1 -0.45 -1 -1v-5H6c-0.55 0 -1 -0.45 -1 -1s0.45 -1 1 -1h5V6",
                "c0 -0.55 0.45 -1 1 -1s1 0.45 1 1v5h5c0.55 0 1 0.45 1 1s-0.45 1 -1 1Z",
            ),
        )
        // Rounded.Delete
        IconId.Trash -> MaterialGlyph(
            path(
                "M6 19c0 1.1 0.9 2 2 2h8c1.1 0 2 -0.9 2 -2V9c0 -1.1 -0.9 -2 -2 -2H8c-1.1 0 -2 0.9 -2 2v10ZM18 4",
                "h-2.5l-0.71 -0.71c-0.18 -0.18 -0.44 -0.29 -0.7 -0.29H9.91c-0.26 0 -0.52 0.11 -0.7 0.29L8.5 4H6",
                "c-0.55 0 -1 0.45 -1 1s0.45 1 1 1h12c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1Z",
            ),
        )
        // Rounded.Folder
        IconId.Folder -> MaterialGlyph(
            path(
                "M10.59 4.59C10.21 4.21 9.7 4 9.17 4H4c-1.1 0 -1.99 0.9 -1.99 2L2 18c0 1.1 0.9 2 2 2h16",
                "c1.1 0 2 -0.9 2 -2V8c0 -1.1 -0.9 -2 -2 -2h-8l-1.41 -1.41Z",
            ),
        )
        // Rounded.Download
        IconId.Download -> MaterialGlyph(
            path(
                "M16.59 9H15V4c0 -0.55 -0.45 -1 -1 -1h-4c-0.55 0 -1 0.45 -1 1v5H7.41",
                "c-0.89 0 -1.34 1.08 -0.71 1.71l4.59 4.59c0.39 0.39 1.02 0.39 1.41 0l4.59 -4.59",
                "c0.63 -0.63 0.19 -1.71 -0.7 -1.71ZM5 19c0 0.55 0.45 1 1 1h12c0.55 0 1 -0.45 1 -1",
                "s-0.45 -1 -1 -1H6c-0.55 0 -1 0.45 -1 1Z",
            ),
        )
        // Rounded.Warning
        IconId.Warning -> MaterialGlyph(
            path(
                "M4.47 21h15.06c1.54 0 2.5 -1.67 1.73 -3L13.73 4.99c-0.77 -1.33 -2.69 -1.33 -3.46 0L2.74 18",
                "c-0.77 1.33 0.19 3 1.73 3ZM12 14c-0.55 0 -1 -0.45 -1 -1v-2c0 -0.55 0.45 -1 1 -1s1 0.45 1 1v2",
                "c0 0.55 -0.45 1 -1 1ZM13 18h-2v-2h2v2Z",
            ),
        )
        // Rounded.Error
        IconId.Error -> MaterialGlyph(
            path(
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10 -4.48 10 -10S17.52 2 12 2ZM12 13",
                "c-0.55 0 -1 -0.45 -1 -1L11 8c0 -0.55 0.45 -1 1 -1s1 0.45 1 1v4c0 0.55 -0.45 1 -1 1ZM13 17h-2",
                "v-2h2v2Z",
            ),
        )
        // Rounded.UnfoldLess
        IconId.Collapse -> MaterialGlyph(
            path(
                "M8.12 19.3c0.39 0.39 1.02 0.39 1.41 0L12 16.83l2.47 2.47c0.39 0.39 1.02 0.39 1.41 0",
                "c0.39 -0.39 0.39 -1.02 0 -1.41l-3.17 -3.17c-0.39 -0.39 -1.02 -0.39 -1.41 0l-3.17 3.17",
                "c-0.4 0.38 -0.4 1.02 -0.01 1.41ZM15.88 4.7c-0.39 -0.39 -1.02 -0.39 -1.41 0L12 7.17L9.53 4.7",
                "c-0.39 -0.39 -1.02 -0.39 -1.41 0c-0.39 0.39 -0.39 1.03 0 1.42l3.17 3.17",
                "c0.39 0.39 1.02 0.39 1.41 0l3.17 -3.17c0.4 -0.39 0.4 -1.03 0.01 -1.42Z",
            ),
        )
        // Rounded.UnfoldMore
        IconId.Expand -> MaterialGlyph(
            path(
                "M12 5.83l2.46 2.46c0.39 0.39 1.02 0.39 1.41 0c0.39 -0.39 0.39 -1.02 0 -1.41L12.7 3.7",
                "c-0.39 -0.39 -1.02 -0.39 -1.41 0L8.12 6.88c-0.39 0.39 -0.39 1.02 0 1.41",
                "c0.39 0.39 1.02 0.39 1.41 0L12 5.83ZM12 18.17l-2.46 -2.46c-0.39 -0.39 -1.02 -0.39 -1.41 0",
                "c-0.39 0.39 -0.39 1.02 0 1.41l3.17 3.18c0.39 0.39 1.02 0.39 1.41 0l3.17 -3.17",
                "c0.39 -0.39 0.39 -1.02 0 -1.41c-0.39 -0.39 -1.02 -0.39 -1.41 0L12 18.17Z",
            ),
        )
        // Rounded.Keyboard
        IconId.Keyboard -> MaterialGlyph(
            path(
                "M20 5L4 5c-1.1 0 -1.99 0.9 -1.99 2L2 17c0 1.1 0.9 2 2 2h16c1.1 0 2 -0.9 2 -2L22 7",
                "c0 -1.1 -0.9 -2 -2 -2ZM11 8h2v2h-2L11 8ZM11 11h2v2h-2v-2ZM8 8h2v2L8 10L8 8ZM8 11h2v2L8 13v-2Z",
                "M7 13L5 13v-2h2v2ZM7 10L5 10L5 8h2v2ZM15 17L9 17c-0.55 0 -1 -0.45 -1 -1s0.45 -1 1 -1h6",
                "c0.55 0 1 0.45 1 1s-0.45 1 -1 1ZM16 13h-2v-2h2v2ZM16 10h-2L14 8h2v2ZM19 13h-2v-2h2v2ZM19 10h-2",
                "L17 8h2v2Z",
            ),
        )
        // AutoMirrored.Rounded.Help
        IconId.Help -> MaterialGlyph(
            path(
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10 -4.48 10 -10S17.52 2 12 2ZM13 19h-2v-2h2v2Z",
                "M15.07 11.25l-0.9 0.92c-0.5 0.51 -0.86 0.97 -1.04 1.69c-0.08 0.32 -0.13 0.68 -0.13 1.14h-2",
                "v-0.5c0 -0.46 0.08 -0.9 0.22 -1.31c0.2 -0.58 0.53 -1.1 0.95 -1.52l1.24 -1.26",
                "c0.46 -0.44 0.68 -1.1 0.55 -1.8c-0.13 -0.72 -0.69 -1.33 -1.39 -1.53",
                "c-1.11 -0.31 -2.14 0.32 -2.47 1.27c-0.12 0.37 -0.43 0.65 -0.82 0.65h-0.3",
                "C8.4 9 8 8.44 8.16 7.88c0.43 -1.47 1.68 -2.59 3.23 -2.83c1.52 -0.24 2.97 0.55 3.87 1.8",
                "c1.18 1.63 0.83 3.38 -0.19 4.4Z",
            ),
            autoMirror = true,
        )
        // AutoMirrored.Rounded.OpenInNew
        IconId.ExternalLink -> MaterialGlyph(
            path(
                "M18 19H6c-0.55 0 -1 -0.45 -1 -1V6c0 -0.55 0.45 -1 1 -1h5c0.55 0 1 -0.45 1 -1s-0.45 -1 -1 -1H5",
                "c-1.11 0 -2 0.9 -2 2v14c0 1.1 0.9 2 2 2h14c1.1 0 2 -0.9 2 -2v-6c0 -0.55 -0.45 -1 -1 -1",
                "s-1 0.45 -1 1v5c0 0.55 -0.45 1 -1 1ZM14 4c0 0.55 0.45 1 1 1h2.59l-9.13 9.13",
                "c-0.39 0.39 -0.39 1.02 0 1.41c0.39 0.39 1.02 0.39 1.41 0L19 6.41V9c0 0.55 0.45 1 1 1",
                "s1 -0.45 1 -1V4c0 -0.55 -0.45 -1 -1 -1h-5c-0.55 0 -1 0.45 -1 1Z",
            ),
            autoMirror = true,
        )
        // b-508
        // Rounded.History
        IconId.History -> MaterialGlyph(
            path(
                "M13.26 3C8.17 2.86 4 6.95 4 12H2.21c-0.45 0 -0.67 0.54 -0.35 0.85l2.79 2.8c0.2 0.2 0.51 0.2 0.71 0",
                "l2.79 -2.8c0.31 -0.31 0.09 -0.85 -0.36 -0.85H6c0 -3.9 3.18 -7.05 7.1 -7c3.7 0.05 6.85 3.17 6.9 6.87",
                "c0.05 3.91 -3.11 7.13 -7 7.13c-1.61 0 -3.1 -0.55 -4.28 -1.48c-0.4 -0.31 -0.96 -0.28 -1.32 0.08",
                "c-0.42 0.42 -0.39 1.13 0.08 1.49C9 20.29 10.91 21 13 21c5.05 0 9.14 -4.17 9 -9.26",
                "c-0.13 -4.69 -4.05 -8.61 -8.74 -8.74ZM12.75 8c-0.41 0 -0.75 0.34 -0.75 0.75v3.68",
                "c0 0.35 0.19 0.68 0.49 0.86l3.12 1.85c0.36 0.21 0.82 0.09 1.03 -0.26",
                "c0.21 -0.36 0.09 -0.82 -0.26 -1.03l-2.88 -1.71v-3.4c0 -0.4 -0.34 -0.74 -0.75 -0.74Z",
            ),
        )
    }
