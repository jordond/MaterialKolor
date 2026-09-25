package com.materialkolor.builder.domain.link

import com.materialkolor.builder.domain.model.ThemeDocument
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reads back the committed share code vectors, the same file the link preview Worker checks itself
 * against, so every platform agrees on every code.
 */
class ShareVectorsTest {
    private val vectors: List<ShareVectorEntry> =
        Json.parseToJsonElement(SHARE_VECTORS_JSON).jsonArray.map { element -> ShareVectorEntry(element.jsonObject) }

    @Test
    fun shareVectors_fixture_coversEverySection() {
        assertTrue(vectors.size >= 30, "Only ${vectors.size} vectors")
        val sections = vectors.fold(0) { flags, vector -> flags or bytesOf(vector.code).unsigned(7) }
        assertEquals(0x3F, sections)
        assertTrue(vectors.any { vector -> vector.code.length == 12 })
    }

    /**
     * A vector with a contrast between the named levels, such as -37, opens at the nearest one (D53).
     */
    @Test
    fun shareVectors_everyCode_decodesToItsDocument() {
        vectors.forEach { vector ->
            val document = vector.document.copy(contrast = vector.document.contrast.snapped())
            val expected = DecodeResult.Ok(document, vector.projectName)
            assertEquals(expected, ShareCodec.decode(vector.code), vector.label)
        }
    }

    @Test
    fun shareVectors_everyDocument_encodesToItsCode() {
        vectors.forEach { vector ->
            assertEquals(vector.code, ShareCodec.encode(vector.document, vector.projectName), vector.label)
        }
    }

    @Test
    fun shareVectors_fixedOffsets_holdTheSeedStyleAndLibrary() {
        vectors.forEach { vector ->
            val bytes = bytesOf(vector.code)
            val seedHex = "#" + (1..3).joinToString(separator = "") { index -> bytes.unsigned(index).toHexByte() }
            assertEquals(vector.seedHex, seedHex, vector.label)
            assertEquals(vector.document.seed.toHex(), vector.seedHex, vector.label)
            assertEquals(vector.document.style.name, vector.style, vector.label)
            assertEquals(vector.document.style.code, bytes.unsigned(4) and 0x0F, vector.label)
            assertEquals(vector.document.library.name, vector.library, vector.label)
            assertEquals(vector.document.library.code, bytes.unsigned(5) and 0x03, vector.label)
        }
    }

    private fun Int.toHexByte(): String = toString(radix = 16).uppercase().padStart(length = 2, padChar = '0')
}

/**
 * One entry of the fixture, read field by field.
 */
private class ShareVectorEntry(
    entry: JsonObject,
) {
    val label: String = entry.getValue("label").jsonPrimitive.content
    val document: ThemeDocument = Json.decodeFromJsonElement(ThemeDocument.serializer(), entry.getValue("document"))
    val projectName: String? = entry.getValue("projectName").jsonPrimitive.contentOrNull
    val code: String = entry.getValue("code").jsonPrimitive.content
    val seedHex: String = entry.getValue("seedHex").jsonPrimitive.content
    val library: String = entry.getValue("library").jsonPrimitive.content
    val style: String = entry.getValue("style").jsonPrimitive.content
}
