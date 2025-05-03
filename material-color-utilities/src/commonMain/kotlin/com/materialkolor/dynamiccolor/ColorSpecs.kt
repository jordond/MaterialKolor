/*
 * Copyright 2025 Google LLC
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

/**
 * A utility class to get the correct color spec for a given spec version.
 */
internal object ColorSpecs {
    private val SPEC_2021: ColorSpec = ColorSpec2021()
    private val SPEC_2025: ColorSpec = ColorSpec2025()

    fun get(specVersion: ColorSpec.SpecVersion?): ColorSpec =
        if (specVersion === ColorSpec.SpecVersion.SPEC_2025) SPEC_2025 else SPEC_2021
}
