package com.materialkolor.builder.kit.token

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// b-314
class FontLicensesTest {
    @Test
    fun readFontLicense_bricolageGrotesque_isTheOflWithItsOwnCopyright() =
        runTest {
            val text = readFontLicense(ShippedFont.BricolageGrotesque)

            text shouldStartWith "Copyright 2022 The Bricolage Grotesque Project Authors"
            text shouldContain "SIL OPEN FONT LICENSE Version 1.1"
        }

    @Test
    fun readFontLicense_jetBrainsMono_isTheOflWithItsOwnCopyright() =
        runTest {
            val text = readFontLicense(ShippedFont.JetBrainsMono)

            text shouldStartWith "Copyright 2020 The JetBrains Mono Project Authors"
            text shouldContain "SIL OPEN FONT LICENSE Version 1.1"
        }
}
