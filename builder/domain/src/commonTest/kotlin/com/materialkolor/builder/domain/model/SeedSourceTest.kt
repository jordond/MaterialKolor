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
    fun seedSource_imageDecodedWithMoreThanFiveCandidates_isRejected() {
        val five = imageJson(candidates = listOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#00FFFF"))
        val six = imageJson(candidates = listOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#00FFFF", "#FF00FF"))

        // The five candidate case decodes, so the six candidate case can only be the require block firing.
        assertEquals(5, (json.decodeFromString(SeedSource.serializer(), five) as SeedSource.Image).candidates.size)
        assertFailsWith<IllegalArgumentException> { json.decodeFromString(SeedSource.serializer(), six) }
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

    /**
     * An image seed as it is saved, written by hand so a decode can be handed more than it accepts.
     */
    private fun imageJson(candidates: List<String>): String {
        val listed = candidates.joinToString(separator = ", ") { candidate -> "\"$candidate\"" }

        return """{"type": "Image", "name": "cactus.png", "candidates": [$listed]}"""
    }
}
