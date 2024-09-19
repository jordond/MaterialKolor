package utils

import com.materialkolor.hct.Cam16
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe

infix fun Hct.shouldBeExactly(expected: hct.Hct) {
    toInt() shouldBeEqual expected.toInt()
}

infix fun hct.Hct.shouldBeExactly(expected: Hct) {
    toInt() shouldBeEqual expected.toInt()
}

infix fun Cam16.shouldBeExactly(expected: hct.Cam16) {
    toInt() shouldBeEqual expected.toInt()
}

infix fun hct.Cam16.shouldBeExactly(expected: Cam16) {
    toInt() shouldBeEqual expected.toInt()
}

infix fun DoubleArray.shouldBeExactly(expected: DoubleArray) {
    toList() shouldContainExactly expected.toList()
}

infix fun dynamiccolor.DynamicScheme.shouldMatch(other: DynamicScheme) {
    sourceColorHct shouldBeExactly other.sourceColorHct
    sourceColorArgb shouldBeExactly other.sourceColorArgb
    variant.name shouldBeEqual other.variant.name
    isDark shouldBeEqual other.isDark
    contrastLevel shouldBeExactly other.contrastLevel
    primaryPalette shouldMatch other.primaryPalette
    secondaryPalette shouldMatch other.secondaryPalette
    tertiaryPalette shouldMatch other.tertiaryPalette
    neutralPalette shouldMatch other.neutralPalette
    neutralVariantPalette shouldMatch other.neutralVariantPalette
    errorPalette shouldMatch other.errorPalette
}

infix fun palettes.TonalPalette.shouldMatch(expected: TonalPalette) {
    hue shouldBe expected.hue
    chroma shouldBe expected.chroma
    keyColor shouldMatch expected.keyColor
}

infix fun hct.Hct.shouldMatch(expected: Hct) {
    hue shouldBe expected.hue
    chroma shouldBe expected.chroma
    tone shouldBe expected.tone
}

@Suppress("ConstPropertyName")
internal object ContrastLevels {
    const val Reduced = -1.0
    const val Default = 0.0
    const val Medium = 0.5
    const val High = 1.0
}