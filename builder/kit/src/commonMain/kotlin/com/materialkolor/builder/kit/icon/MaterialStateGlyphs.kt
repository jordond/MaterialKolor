/*
 * The path data below is copied from the Material Icons, as shipped in the
 * androidx.compose.material:material-icons-core 1.7.6 and material-icons-extended 1.7.6 sources.
 * Outlined.Info comes from the core set and Rounded.Autorenew from the extended set.
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

/**
 * The info glyph as a ring round the letter. The Rounded set has only a filled info glyph, so this
 * one is the Outlined set's, which sits on the same grid.
 */
internal fun infoOutlineGlyph(): MaterialGlyph =
    // Outlined.Info
    MaterialGlyph(
        path(
            "M11 7h2v2h-2ZM11 11h2v6h-2ZM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10 -4.48 10 -10",
            "S17.52 2 12 2ZM12 20c-4.41 0 -8 -3.59 -8 -8s3.59 -8 8 -8s8 3.59 8 8s-3.59 8 -8 8Z",
        ),
    )

/**
 * Two arrows chasing round a circle, Material's glyph for work under way. It turns about its
 * centre, so it looks the same at any angle.
 */
internal fun progressGlyph(): MaterialGlyph =
    // Rounded.Autorenew
    MaterialGlyph(
        path(
            "M12 6v1.79c0 0.45 0.54 0.67 0.85 0.35l2.79 -2.79c0.2 -0.2 0.2 -0.51 0 -0.71l-2.79 -2.79",
            "c-0.31 -0.31 -0.85 -0.09 -0.85 0.36L12 4c-4.42 0 -8 3.58 -8 8c0 1.04 0.2 2.04 0.57 2.95",
            "c0.27 0.67 1.13 0.85 1.64 0.34c0.27 -0.27 0.38 -0.68 0.23 -1.04C6.15 13.56 6 12.79 6 12",
            "c0 -3.31 2.69 -6 6 -6ZM17.79 8.71c-0.27 0.27 -0.38 0.69 -0.23 1.04c0.28 0.7 0.44 1.46 0.44 2.25",
            "c0 3.31 -2.69 6 -6 6v-1.79c0 -0.45 -0.54 -0.67 -0.85 -0.35l-2.79 2.79c-0.2 0.2 -0.2 0.51 0 0.71",
            "l2.79 2.79c0.31 0.31 0.85 0.09 0.85 -0.35L12 20c4.42 0 8 -3.58 8 -8c0 -1.04 -0.2 -2.04 -0.57 -2.95",
            "c-0.27 -0.67 -1.13 -0.85 -1.64 -0.34Z",
        ),
    )
