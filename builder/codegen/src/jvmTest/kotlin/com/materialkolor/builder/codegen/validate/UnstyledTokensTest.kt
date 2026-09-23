package com.materialkolor.builder.codegen.validate

import com.materialkolor.builder.codegen.GoldenHarness
import com.materialkolor.builder.codegen.target.unstyled.COLORS_PROPERTY
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Holds the reserved Unstyled token names to `MaterialKolorTokens` as it is checked in.
 *
 * Fifteen of the names are typed by hand and the rest assume each role's token is named after it,
 * so a token added, renamed or dropped in the library fails here rather than slipping past the
 * accent name check.
 */
class UnstyledTokensTest {
    @Test
    fun unstyledTokens_matchEveryTokenMaterialKolorTokensDeclares() {
        val source = File(GoldenHarness.repoRoot(), TOKENS_SOURCE)
        val declared = source.readLines().mapNotNull { line -> TokenLine.find(line)?.groupValues?.get(1) }.toSet()

        assertEquals(63, declared.size, "Expected every token in $source")
        assertEquals(declared, UnstyledTokens - COLORS_PROPERTY)
    }

    private companion object {
        const val TOKENS_SOURCE =
            "material-kolor-unstyled/src/commonMain/kotlin/com/materialkolor/unstyled/MaterialKolorTokens.kt"
        val TokenLine = Regex("""^\s*public val (\w+): ThemeToken<Color>""")
    }
}
