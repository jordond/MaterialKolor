package com.materialkolor.builder.domain.model

import com.materialkolor.builder.domain.DocumentArb
import com.materialkolor.builder.domain.color.Argb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class ThemeDocumentJsonTest {
    private val json = Json

    @Test
    fun themeDocument_randomDocuments_surviveAJsonRoundTrip() {
        DocumentArb().documents(count = 1_000).forEach { document ->
            val text = json.encodeToString(ThemeDocument.serializer(), document)
            assertEquals(document, json.decodeFromString(ThemeDocument.serializer(), text), text)
        }
    }

    @Test
    fun themeDocument_default_survivesAJsonRoundTrip() {
        val text = json.encodeToString(ThemeDocument.serializer(), ThemeDocument.Default)
        assertEquals(ThemeDocument.Default, json.decodeFromString(ThemeDocument.serializer(), text))
    }

    @Test
    fun themeDocument_default_holdsTheDefaultSeed() {
        assertEquals(DEFAULT_SEED, ThemeDocument.Default.seed)
        assertEquals("#D9653B", ThemeDocument.Default.seed.toHex())
    }

    @Test
    fun themeDocument_pinnedRoles_serializeUnderTheirRoleNames() {
        val document = ThemeDocument.Default.copy(pins = mapOf(Role.Primary to RolePin(light = Argb(0x00FF0000))))

        val text = json.encodeToString(ThemeDocument.serializer(), document)

        assertTrue(text.contains("\"Primary\""), text)
        assertTrue(text.contains("\"#FF0000\""), text)
    }
}
