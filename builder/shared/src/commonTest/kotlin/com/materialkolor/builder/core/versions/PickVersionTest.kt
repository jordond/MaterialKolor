package com.materialkolor.builder.core.versions

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PickVersionTest {
    @Test
    fun pickVersion_newerStableOnTheLine_isPicked() {
        pickVersion("6.0.0", listOf("5.9.0", "6.0.0", "6.0.1", "6.2.0", "7.0.0")) shouldBe "6.2.0"
    }

    @Test
    fun pickVersion_stableAndNewerPreRelease_prefersTheStable() {
        pickVersion("2.10.0", listOf("2.10.0", "2.11.0", "2.12.0-alpha01", "2.12.0-rc01")) shouldBe "2.11.0"
    }

    @Test
    fun pickVersion_preReleaseFloor_takesTheNewestPreReleaseWhenNoStableCounts() {
        val published = listOf("1.4.0", "1.5.0-alpha27", "1.5.0-alpha28", "1.5.0-beta01", "1.5.0-alpha30")

        pickVersion("1.5.0-alpha28", published) shouldBe "1.5.0-beta01"
    }

    @Test
    fun pickVersion_preReleaseFloor_takesTheStableOnceItShips() {
        val published = listOf("1.12.0-alpha03", "1.12.0-rc01", "1.12.0", "1.13.0-alpha01")

        pickVersion("1.12.0-alpha03", published) shouldBe "1.12.0"
    }

    @Test
    fun pickVersion_preReleaseKinds_rankDevAlphaBetaRc() {
        val published = listOf("1.1.0-dev1234", "1.1.0-rc1", "1.1.0-beta.2", "1.1.0-alpha03")

        pickVersion("1.1.0-dev1000", published) shouldBe "1.1.0-rc1"
        pickVersion("1.1.0-dev1000", published.dropLast(3)) shouldBe "1.1.0-dev1234"
    }

    @Test
    fun pickVersion_leadingV_keepsThePublishedSpelling() {
        pickVersion("v0.1.0", listOf("v0.0.9", "v0.1.0", "v0.1.2", "v0.2.0")) shouldBe "v0.1.2"
    }

    @Test
    fun pickVersion_majorZero_staysOnTheMinorLine() {
        pickVersion("0.1.0", listOf("0.1.0", "0.1.3", "0.2.0", "1.0.0")) shouldBe "0.1.3"
    }

    @Test
    fun pickVersion_nothingAtOrAboveTheFloor_isTheFloor() {
        pickVersion("6.0.0", listOf("5.0.0", "6.0.0-rc01", "7.0.0")) shouldBe "6.0.0"
        pickVersion("6.0.0", emptyList()) shouldBe "6.0.0"
    }

    @Test
    fun pickVersion_floorThatDoesNotRead_isTheFloor() {
        pickVersion("6.0.0-SNAPSHOT", listOf("6.0.0", "6.1.0")) shouldBe "6.0.0-SNAPSHOT"
    }

    @Test
    fun pickVersion_versionThatDoesNotRead_isPassedOver() {
        pickVersion("6.0.0", listOf("6.0.1", "6.9.0+dev12", "6.x", "")) shouldBe "6.0.1"
    }
}
