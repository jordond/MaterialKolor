package com.materialkolor.builder.domain.link

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Base64UrlTest {
    @Test
    fun encode_rfc4648Vectors_matchWithoutPadding() {
        val vectors =
            mapOf(
                "" to "",
                "f" to "Zg",
                "fo" to "Zm8",
                "foo" to "Zm9v",
                "foob" to "Zm9vYg",
                "fooba" to "Zm9vYmE",
                "foobar" to "Zm9vYmFy",
            )
        vectors.forEach { (text, encoded) ->
            assertEquals(encoded, Base64Url.encode(text.encodeToByteArray()))
            assertContentEquals(text.encodeToByteArray(), Base64Url.decode(encoded))
        }
    }

    @Test
    fun encode_bytesThatNeedTheLastTwoCharacters_useTheUrlAlphabet() {
        val bytes = byteArrayOf(0xFB.toByte(), 0xFF.toByte(), 0xBF.toByte())
        assertEquals("-_-_", Base64Url.encode(bytes))
        assertContentEquals(bytes, Base64Url.decode("-_-_"))
    }

    @Test
    fun decode_randomBytes_roundTrip() {
        val random = Random(20260922)
        repeat(2_000) {
            val bytes = random.nextBytes(random.nextInt(until = 64))
            assertContentEquals(bytes, Base64Url.decode(Base64Url.encode(bytes)))
        }
    }

    @Test
    fun decode_textThatIsNotCanonical_isNull() {
        listOf("Zg==", "Zm8=", "+/8", "Z", "Zm9vY", "Zh", "Zm9", "Zm 9v", "Zm9v\n").forEach { text ->
            assertNull(Base64Url.decode(text), text)
        }
    }
}
