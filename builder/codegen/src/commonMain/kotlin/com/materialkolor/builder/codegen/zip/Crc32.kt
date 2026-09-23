package com.materialkolor.builder.codegen.zip

/**
 * The standard CRC-32, the checksum every zip entry carries.
 *
 * It is plain Int math over a lookup table, so the JVM and wasm work it out the same way.
 */
internal object Crc32 {
    /** The checksum of [bytes], held unsigned in the 32 bits of an Int. */
    fun of(bytes: ByteArray): Int {
        var crc = -1
        bytes.forEach { byte ->
            crc = Table[(crc xor byte.toInt()) and 0xFF] xor (crc ushr 8)
        }

        return crc.inv()
    }

    private val Table: IntArray = IntArray(256) { index ->
        var value = index
        repeat(8) {
            value = if (value and 1 != 0) (value ushr 1) xor REVERSED_POLYNOMIAL else value ushr 1
        }
        value
    }

    private const val REVERSED_POLYNOMIAL = 0xEDB88320.toInt()
}
