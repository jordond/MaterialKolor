/*
 * Copyright 2023 Google LLC
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
package com.materialkolor.dynamiccolor

import com.materialkolor.utils.MathUtils.lerp
import dev.drewhamilton.poko.Poko

/**
 * A class containing a value that changes with the contrast level.
 *
 * Usually represents the contrast requirements for a dynamic color on its background. The four
 * values correspond to values for contrast levels -1.0, 0.0, 0.5, and 1.0 respectively.
 *
 * Creates a `ContrastCurve` object.
 *
 * @param low Value for contrast level -1.0
 * @param normal Value for contrast level 0.0
 * @param medium Value for contrast level 0.5
 * @param high Value for contrast level 1.0
 */
@Poko
public class ContrastCurve(
    private val low: Double,
    private val normal: Double,
    private val medium: Double,
    private val high: Double,
) {

    /**
     * Returns the value at a given contrast level.
     *
     * @param contrastLevel The contrast ratio. 0.0 is the default (normal); -1.0 is the lowest; 1.0
     * is the highest.
     * @return The value. For contrast ratios, a number between 1.0 and 21.0.
     */
    public fun get(contrastLevel: Double): Double = when {
        contrastLevel <= -1.0 -> low
        contrastLevel < 0.0 -> lerp(low, normal, (contrastLevel - -1) / 1)
        contrastLevel < 0.5 -> lerp(normal, medium, (contrastLevel - 0) / 0.5)
        contrastLevel < 1.0 -> lerp(medium, high, (contrastLevel - 0.5) / 0.5)
        else -> high
    }
}
