package com.materialkolor.builder.domain.model

import com.materialkolor.builder.domain.DocumentArb
import com.materialkolor.builder.domain.color.Argb
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    /**
     * The keys every serializable class in the model writes.
     *
     * Each property carries an explicit `@SerialName`, so a Kotlin rename cannot quietly move a key
     * a saved document already holds. This table is the other half of that, it goes red when a name
     * does move, which is the moment to decide whether the old documents need reading too.
     */
    @Test
    fun model_everySerializedClass_keepsTheKeysItAlreadyWrites() {
        val wireNames = listOf(
            ThemeDocument.serializer() to listOf(
                "seed",
                "seedSource",
                "keyColors",
                "style",
                "cmfTertiarySeed",
                "contrast",
                "spec",
                "platform",
                "amoled",
                "accents",
                "pins",
                "library",
                "expressive",
                "motionScheme",
                "themeName",
                "customTones",
            ),
            KeyColors.serializer() to listOf("primary", "secondary", "tertiary", "error", "neutral", "neutralVariant"),
            Accent.serializer() to listOf("name", "seed", "harmonize", "light", "dark", "threshold"),
            FamilyTones.serializer() to listOf("color", "container"),
            SeedSource.Preset.serializer() to listOf("id"),
            SeedSource.Image.serializer() to listOf("name", "candidates"),
            RolePin.serializer() to listOf("light", "dark"),
            CustomTone.serializer() to listOf("light", "dark"),
        )

        wireNames.forEach { (serializer, names) ->
            val descriptor = serializer.descriptor
            val written = List(descriptor.elementsCount) { index -> descriptor.getElementName(index) }

            assertEquals(names, written, "${descriptor.serialName} writes different keys than it used to")
        }
    }

    @Test
    fun themeDocument_pinnedRoles_serializeUnderTheirRoleNames() {
        val document = ThemeDocument.Default.copy(pins = mapOf(Role.Primary to RolePin(light = Argb(0x00FF0000))))

        val text = json.encodeToString(ThemeDocument.serializer(), document)

        assertTrue(text.contains("\"Primary\""), text)
        assertTrue(text.contains("\"#FF0000\""), text)
    }
}
