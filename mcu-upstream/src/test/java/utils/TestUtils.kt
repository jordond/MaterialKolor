package utils

import com.materialkolor.hct.Cam16
import com.materialkolor.hct.Hct
import io.kotest.matchers.equals.shouldBeEqual

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