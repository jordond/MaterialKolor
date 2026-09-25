package com.materialkolor.builder.kit.token

import com.materialkolor.builder.kit.generated.resources.Res

/**
 * The fonts the kit ships, each under the SIL Open Font License 1.1.
 *
 * The license text travels with the font files, so About can show each one in full with
 * [readFontLicense].
 */
public enum class ShippedFont(
    internal val licenseFile: String,
) {
    /**
     * The brand face the chrome and the poster are set in.
     */
    BricolageGrotesque("files/OFL-BricolageGrotesque.txt"),

    /**
     * The monospace face the code and the hex values are set in.
     */
    JetBrainsMono("files/OFL-JetBrainsMono.txt"),

    /**
     * The face Fluent's own components are set in. It ships as a renamed subset, Builder Fluent
     * Sans, since the license reserves the name Selawik.
     */
    Selawik("files/OFL-Selawik.txt"),
}

/**
 * The full license [font] ships under, its copyright line first.
 */
public suspend fun readFontLicense(font: ShippedFont): String = Res.readBytes(font.licenseFile).decodeToString()
