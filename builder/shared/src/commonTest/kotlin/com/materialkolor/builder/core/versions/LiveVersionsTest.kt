package com.materialkolor.builder.core.versions

import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.core.platform.LibraryVersionSource
import com.materialkolor.builder.fakes.FakeLibraryVersionSource
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private val BAKED =
    ExportVersions(
        builder = "2.0.0",
        materialKolor = "6.0.0",
        fluent = "v0.1.0",
        composeUnstyled = "2.10.0",
        composeMaterial3 = "1.12.0-alpha03",
        androidxMaterial3 = "1.5.0-alpha28",
        fluentModuleAvailable = false,
    )

private const val LIVE_JSON =
    """
    {
      "materialKolor": ["5.9.0", "6.0.0", "6.0.2", "7.0.0-alpha01"],
      "composeMaterial3": ["1.11.0", "1.12.0-alpha03", "1.12.0-alpha05"],
      "androidxMaterial3": ["1.4.0", "1.5.0-alpha28", "1.5.0"],
      "composeUnstyled": ["2.10.0", "2.11.1"],
      "fluent": ["v0.1.0", "v0.1.1"],
      "fetchedAt": "2026-09-27T00:00:00.000Z"
    }
    """

class LiveVersionsTest {
    @Test
    fun exportVersionsOf_everyLibraryListed_picksEachOnItsFloor() {
        exportVersionsOf(BAKED, LIVE_JSON) shouldBe
            BAKED.copy(
                materialKolor = "6.0.2",
                composeMaterial3 = "1.12.0-alpha05",
                androidxMaterial3 = "1.5.0",
                composeUnstyled = "2.11.1",
                fluent = "v0.1.1",
            )
    }

    @Test
    fun exportVersionsOf_keepsTheBuilderAndTheFluentModuleBaked() {
        val live = exportVersionsOf(BAKED, LIVE_JSON)

        live.builder shouldBe BAKED.builder
        live.fluentModuleAvailable shouldBe false
    }

    @Test
    fun exportVersionsOf_missingOrMalformedLibraries_keepTheirBakedVersions() {
        val json = """{"fluent": ["v0.1.1"], "composeUnstyled": "2.11.1", "materialKolor": [6.1], "fetchedAt": "now"}"""

        exportVersionsOf(BAKED, json) shouldBe BAKED.copy(fluent = "v0.1.1")
    }

    @Test
    fun exportVersionsOf_olderThanTheFloor_theFloorWins() {
        val json = """{"materialKolor": ["5.0.0", "5.9.0"], "composeUnstyled": ["1.0.0"]}"""

        exportVersionsOf(BAKED, json) shouldBe BAKED
    }

    @Test
    fun exportVersionsOf_badJsonOrNone_isBaked() {
        exportVersionsOf(BAKED, null) shouldBe BAKED
        exportVersionsOf(BAKED, "<!doctype html>") shouldBe BAKED
        exportVersionsOf(BAKED, """["6.0.2"]""") shouldBe BAKED
        exportVersionsOf(BAKED, "") shouldBe BAKED
    }

    @Test
    fun liveExportVersions_startBakedAndTurnLiveAfterOneFetch() =
        runTest {
            val source = FakeLibraryVersionSource(LIVE_JSON)
            val versions = backgroundScope.liveExportVersions(BAKED, source)

            versions.value shouldBe BAKED
            runCurrent()

            versions.value shouldBe exportVersionsOf(BAKED, LIVE_JSON)
            source.fetches shouldBe 1
        }

    @Test
    fun liveExportVersions_sourceThatThrows_staysBaked() =
        runTest {
            val throwing = object : LibraryVersionSource {
                override suspend fun fetch(): String? = error("Offline")
            }
            val versions = backgroundScope.liveExportVersions(BAKED, throwing)
            runCurrent()

            versions.value shouldBe BAKED
        }
}
