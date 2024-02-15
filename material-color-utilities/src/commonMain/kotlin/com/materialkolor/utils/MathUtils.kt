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
package com.materialkolor.utils

import kotlin.math.abs

/**
 * Utility methods for mathematical operations.
 */
internal object MathUtils {

    /**
     * The signum function.
     *
     * @param[num] the number to check
     * @return 1 if num > 0, -1 if num < 0, and 0 if num = 0
     */
    fun signum(num: Double): Int = when {
        num < 0 -> -1
        num == 0.0 -> 0
        else -> 1
    }

    /**
     * The linear interpolation function.
     *
     * @param[start] the start value
     * @param[stop] the stop value
     * @param[amount] the amount to interpolate
     * @return start if amount = 0 and stop if amount = 1
     */
    fun lerp(start: Double, stop: Double, amount: Double): Double {
        return (1.0 - amount) * start + amount * stop
    }

    /**
     * Sanitizes a degree measure as an integer.
     *
     * @param[degrees] the degree measure to sanitize.
     * @return a degree measure between 0 (inclusive) and 360 (exclusive).
     */
    fun sanitizeDegrees(degrees: Int): Int {
        var sanitized = degrees % 360
        if (sanitized < 0) {
            sanitized += 360
        }
        return sanitized
    }

    /**
     * Sanitizes a degree measure as a floating-point number.
     *
     * @param[degrees] the degree measure to sanitize.
     * @return a degree measure between 0.0 (inclusive) and 360.0 (exclusive).
     */
    fun sanitizeDegrees(degrees: Double): Double {
        var sanitized = degrees % 360.0
        if (sanitized < 0) {
            sanitized += 360.0
        }
        return sanitized
    }

    /**
     * Sign of direction change needed to travel from one angle to another.
     *
     *
     * For angles that are 180 degrees apart from each other, both directions have the same travel
     * distance, so either direction is shortest. The value 1.0 is returned in this case.
     *
     * @param[from] The angle travel starts from, in degrees.
     * @param[to] The angle travel ends at, in degrees.
     * @return -1 if decreasing from leads to the shortest travel distance, 1 if increasing from leads
     * to the shortest travel distance.
     */
    fun rotationDirection(from: Double, to: Double): Double {
        val increasingDifference = sanitizeDegrees(to - from)
        return if (increasingDifference <= 180.0) 1.0 else -1.0
    }


    /**
     * Distance of two points on a circle, represented using degrees.
     *
     * @param[a] the first point
     * @param[b] the second point
     * @return the distance between the two points
     */
    fun differenceDegrees(a: Double, b: Double): Double {
        return 180.0 - abs(abs(a - b) - 180.0)
    }

    /**
     * Multiplies a 1x3 row vector with a 3x3 matrix.
     *
     * @param[row] the row vector
     * @param[matrix] the matrix
     * @return the resulting row vector
     */
    fun matrixMultiply(row: DoubleArray, matrix: Array<DoubleArray>): DoubleArray {
        val a = row[0] * matrix[0][0] + row[1] * matrix[0][1] + row[2] * matrix[0][2]
        val b = row[0] * matrix[1][0] + row[1] * matrix[1][1] + row[2] * matrix[1][2]
        val c = row[0] * matrix[2][0] + row[1] * matrix[2][1] + row[2] * matrix[2][2]
        return doubleArrayOf(a, b, c)
    }

    /**
     * Converts an angle in degrees to radians.
     *
     * @param[angrad] angle in radians
     * @return angle in degrees
     */
    fun toDegrees(angrad: Double): Double = angrad * 57.29577951308232


    /**
     * Converts an angle in radians to degrees.
     *
     * @param[angdeg] angle in degrees
     * @return angle in radians
     */
    fun toRadians(angdeg: Double): Double = angdeg * 0.017453292519943295
}
