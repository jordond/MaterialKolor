package com.materialkolor.builder.domain.model

import com.materialkolor.builder.domain.color.Argb
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SeedSourceTest {
    private val json = Json

    @Test
    fun seedSource_imageWithMoreThanFiveCandidates_isRejected() {
        val candidates = List(6) { index -> Argb(index) }

        assertFailsWith<IllegalArgumentException> { SeedSource.Image(name = "cactus.png", candidates = candidates) }
    }

    @Test
    fun seedSource_imageWithFiveCandidates_isAccepted() {
        val candidates = List(5) { index -> Argb(index) }
        val source = SeedSource.Image(name = "cactus.png", candidates = candidates)

        assertEquals(candidates, source.candidates)
    }

    @Test
    fun seedSource_everyKind_survivesAJsonRoundTrip() {
        val sources = listOf(
            SeedSource.Typed,
            SeedSource.Picked,
            SeedSource.Eyedropper,
            SeedSource.Shuffled,
            SeedSource.Preset(id = "monstera"),
            SeedSource.Image(name = "monstera.png", candidates = listOf(Argb(0x00D9653B))),
        )

        sources.forEach { source ->
            val text = json.encodeToString(SeedSource.serializer(), source)
            assertEquals(source, json.decodeFromString(SeedSource.serializer(), text), text)
        }
    }
}
