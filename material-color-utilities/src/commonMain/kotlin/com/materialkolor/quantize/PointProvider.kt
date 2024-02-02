/*
 * Copyright 2021 Google LLC
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
package com.materialkolor.quantize

/**
 * An interface to allow use of different color spaces by quantizers.
 */
internal interface PointProvider {

    /**
     * The four components in the color space of an sRGB color.
     *
     * @param[argb] argb The ARGB (i.e. hex code) representation of this color.
     * @return The four components in the color space of an sRGB color.
     */
    fun fromInt(argb: Int): DoubleArray

    /**
     * The ARGB (i.e. hex code) representation of this color.
     *
     * @param[point] The four components in the color space of an sRGB color.
     * @return The ARGB (i.e. hex code) representation of this color.
     */
    fun toInt(point: DoubleArray): Int

    /**
     * Squared distance between two colors. Distance is defined by scientific color spaces and
     * referred to as delta E.
     *
     * @param[a] The first color.
     * @param[b] The second color.
     * @return The squared distance between the two colors.
     */
    fun distance(a: DoubleArray, b: DoubleArray): Double
}
