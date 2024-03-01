package utils

import com.materialkolor.hct.Cam16
import com.materialkolor.hct.Hct
import io.kotest.matchers.equals.shouldBeEqual

infix fun Hct.shouldBeEqualColor(expected: hct.Hct) {
    toInt() shouldBeEqual expected.toInt()
}

infix fun hct.Hct.shouldBeEqualColor(expected: Hct) {
    toInt() shouldBeEqual expected.toInt()
}

infix fun Cam16.shouldBeEqualColor(expected: hct.Cam16) {
    toInt() shouldBeEqual expected.toInt()
}

infix fun hct.Cam16.shouldBeEqualColor(expected: Cam16) {
    toInt() shouldBeEqual expected.toInt()
}