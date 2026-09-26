package com.materialkolor.builder.feature.projects

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ShareHostTest {
    @Test
    fun nameToRename_aNewGoodName_isTheDraftTrimmed() {
        nameToRename("  Lighthouse ", current = "Harbour") shouldBe "Lighthouse"
    }

    @Test
    fun nameToRename_theNameTheProjectHas_renamesNothing() {
        nameToRename("Harbour ", current = "Harbour").shouldBeNull()
    }

    @Test
    fun nameToRename_aBlankOrTooLongDraft_renamesNothing() {
        nameToRename("   ", current = "Harbour").shouldBeNull()
        nameToRename("x".repeat(49), current = "Harbour").shouldBeNull()
    }
}
