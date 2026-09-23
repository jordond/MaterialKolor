package com.materialkolor.builder.domain.link

/**
 * The CRC-8 a share code ends with, polynomial `0x07`, starting from zero with nothing reflected.
 *
 * It catches every single bit error and most typos in a pasted link, which is all a checksum on a
 * URL has to do. The inputs are a few dozen bytes, so it runs bit by bit without a table.
 */
internal object Crc8 {
    private const val POLYNOMIAL: Int = 0x07

    private const val BYTE_MASK: Int = 0xFF

    private const val TOP_BIT: Int = 0x80

    /**
     * The checksum of [bytes] from [start] up to but not including [end], 0 to 255.
     */
    fun compute(
        bytes: ByteArray,
        start: Int = 0,
        end: Int = bytes.size,
    ): Int {
        var crc = 0
        for (index in start until end) {
            crc = crc xor (bytes[index].toInt() and BYTE_MASK)
            repeat(8) {
                val shifted = (crc shl 1) and BYTE_MASK
                crc = if ((crc and TOP_BIT) != 0) shifted xor POLYNOMIAL else shifted
            }
        }
        return crc
    }
}
