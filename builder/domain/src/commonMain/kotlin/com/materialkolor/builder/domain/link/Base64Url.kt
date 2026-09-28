package com.materialkolor.builder.domain.link

/**
 * The URL safe base64 alphabet without padding, the text a share code travels as.
 *
 * Written by hand so every platform reads a code the same way. Decoding is strict, one spelling
 * per byte string, so a code that was hand edited or cut short is refused instead of guessed at.
 */
internal object Base64Url {
    private const val ALPHABET: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private const val SEXTET_MASK: Int = 0x3F

    private const val BYTE_MASK: Int = 0xFF

    /**
     * [bytes] as base64url with no trailing `=`.
     */
    fun encode(bytes: ByteArray): String =
        buildString {
            var index = 0
            while (index < bytes.size) {
                // Three bytes make four characters. A short last group of one or two bytes makes
                // two or three, which is where plain base64 would pad.
                val count = minOf(3, bytes.size - index)
                var chunk = 0
                for (offset in 0 until 3) {
                    val byte = if (offset < count) bytes[index + offset].toInt() and BYTE_MASK else 0
                    chunk = (chunk shl 8) or byte
                }
                for (sextet in 0..count) {
                    append(ALPHABET[(chunk shr (18 - 6 * sextet)) and SEXTET_MASK])
                }
                index += count
            }
        }

    /**
     * The bytes [text] spells, or null when it is not canonical base64url.
     *
     * Padding, the `+` and `/` of plain base64, a length no byte string encodes to and leftover
     * bits that are not zero all count as not canonical.
     */
    fun decode(text: String): ByteArray? {
        if (text.length % 4 == 1) return null
        val bytes = ByteArray(text.length * 3 / 4)
        var bits = 0
        var bitCount = 0
        var written = 0
        for (char in text) {
            val sextet = sextetOf(char)
            if (sextet < 0) return null
            bits = (bits shl 6) or sextet
            bitCount += 6
            if (bitCount >= 8) {
                bitCount -= 8
                bytes[written] = ((bits shr bitCount) and BYTE_MASK).toByte()
                written += 1
                bits = bits and ((1 shl bitCount) - 1)
            }
        }
        if (bits != 0) return null
        return bytes
    }

    private fun sextetOf(char: Char): Int =
        when (char) {
            in 'A'..'Z' -> char - 'A'
            in 'a'..'z' -> char - 'a' + 26
            in '0'..'9' -> char - '0' + 52
            '-' -> 62
            '_' -> 63
            else -> -1
        }
}
