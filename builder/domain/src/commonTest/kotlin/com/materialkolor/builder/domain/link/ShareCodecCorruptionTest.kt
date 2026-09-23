package com.materialkolor.builder.domain.link

import com.materialkolor.builder.domain.DocumentArb
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.validate.MAX_ACCENTS
import com.materialkolor.builder.domain.validate.MAX_ACCENT_NAME_BYTES
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class ShareCodecCorruptionTest {
    private val default = ThemeDocument.Default
    private val color = Argb(0x336699)

    @Test
    fun decode_everyTruncation_isCorrupt() {
        DocumentArb(seed = 3).documents(count = 200).forEach { document ->
            val code = ShareCodec.encode(document, projectName = "Acme")
            for (length in 0 until code.length) {
                assertEquals(DecodeResult.Corrupt, ShareCodec.decode(code.take(length)), code.take(length))
            }
        }
    }

    @Test
    fun decode_everySingleBitFlipAfterTheVersion_isCorrupt() {
        DocumentArb(seed = 5).documents(count = 200).forEach { document ->
            val bytes = bytesOf(ShareCodec.encode(document, projectName = "Acme"))
            for (index in 1 until bytes.size) {
                for (bit in 0 until 8) {
                    val flipped = bytes.copyOf()
                    flipped[index] = (flipped[index].toInt() xor (1 shl bit)).toByte()
                    val result = ShareCodec.decode(Base64Url.encode(flipped))
                    assertEquals(DecodeResult.Corrupt, result, "byte $index bit $bit")
                }
            }
        }
    }

    @Test
    fun decode_versionTwo_isUnknownVersion() {
        val bytes = bytesOf(ShareCodec.encode(default))
        bytes[0] = 2
        assertEquals(DecodeResult.UnknownVersion, ShareCodec.decode(Base64Url.encode(bytes)))
        assertEquals(DecodeResult.UnknownVersion, ShareCodec.decode(resealed(bytes)))
    }

    @Test
    fun decode_versionZero_isCorrupt() {
        val bytes = bytesOf(ShareCodec.encode(default))
        bytes[0] = 0
        assertEquals(DecodeResult.Corrupt, ShareCodec.decode(resealed(bytes)))
    }

    @Test
    fun decode_textThatIsNotBase64Url_isCorrupt() {
        val code = ShareCodec.encode(default)
        val texts = listOf("", "A", "$code=", "$code ", "héllo", code.replaceRange(3, 4, "+"))
        texts.forEach { text -> assertEquals(DecodeResult.Corrupt, ShareCodec.decode(text), text) }
    }

    @Test
    fun decode_reservedBitInTheTargetByte_isCorrupt() {
        assertReservedBitsRejected(default, index = 5, reserved = 0xF8)
    }

    @Test
    fun decode_reservedBitInTheSectionFlags_isCorrupt() {
        assertReservedBitsRejected(default, index = 7, reserved = 0xC0)
    }

    @Test
    fun decode_reservedBitInTheKeyColorMask_isCorrupt() {
        val document = default.copy(keyColors = default.keyColors.with(KeyColor.Primary, color))
        assertReservedBitsRejected(document, index = 8, reserved = 0xC0)
    }

    @Test
    fun decode_reservedBitInAnAccentFlag_isCorrupt() {
        val document = default.copy(accents = listOf(Accent(name = "brand", seed = color)))
        assertReservedBitsRejected(document, index = 9, reserved = 0xF8)
    }

    @Test
    fun decode_reservedBitInAPinMode_isCorrupt() {
        val document = default.copy(pins = mapOf(Role.Primary to RolePin(light = color)))
        assertReservedBitsRejected(document, index = 10, reserved = 0xFC)
    }

    @Test
    fun decode_reservedBitInTheTargetOptionFlags_isCorrupt() {
        val document = default.copy(themeName = "Brand")
        assertReservedBitsRejected(document, index = 8, reserved = 0xF8)
    }

    @Test
    fun decode_retiredCustomSlotCode_isCorrupt() {
        val document = default.copy(customTones = mapOf(CustomSlot.Primary to CustomTone(light = 50)))
        val bytes = bytesOf(ShareCodec.encode(document))
        val slotIndex = 10
        assertEquals(CustomSlot.Primary.code, bytes.unsigned(slotIndex))
        bytes[slotIndex] = CustomSlot.Surface.code.toByte()
        assertIs<DecodeResult.Ok>(ShareCodec.decode(resealed(bytes)))
        (RETIRED_SLOT_CODES + 49 + 254).forEach { code ->
            bytes[slotIndex] = code.toByte()
            assertEquals(DecodeResult.Corrupt, ShareCodec.decode(resealed(bytes)), "slot code $code")
        }
    }

    @Test
    fun decode_valuesOutsideTheirRange_areCorrupt() {
        val header = bytesOf(ShareCodec.encode(default))
        assertCorruptWith(header) { bytes -> bytes[4] = 0x0F }
        assertCorruptWith(header) { bytes -> bytes[4] = (3 shl 4).toByte() }
        assertCorruptWith(header) { bytes -> bytes[6] = 101 }
        assertCorruptWith(header) { bytes -> bytes[6] = (-101).toByte() }

        val accent = Accent(name = "brand", seed = color, light = FamilyTones(color = 35, container = 90))
        val tones = bytesOf(ShareCodec.encode(default.copy(accents = listOf(accent))))
        assertEquals(35, tones.unsigned(tones.size - 5))
        assertCorruptWith(tones) { bytes -> bytes[bytes.size - 5] = 101 }

        val customTones = mapOf(CustomSlot.Primary to CustomTone(light = 50))
        val custom = bytesOf(ShareCodec.encode(default.copy(customTones = customTones)))
        assertCorruptWith(custom) { bytes -> bytes[11] = 101 }
        assertCorruptWith(custom) { bytes -> bytes[12] = 254.toByte() }
    }

    @Test
    fun decode_spellingsTheWriterNeverProduces_areCorrupt() {
        val header = bytesOf(ShareCodec.encode(default))
        // A section flag with an empty section behind it.
        assertCorrupt(header.sealedWith(flags = 0x01, 0x00))
        assertCorrupt(header.sealedWith(flags = 0x04, 0x00))
        assertCorrupt(header.sealedWith(flags = 0x08, 0x00))
        assertCorrupt(header.sealedWith(flags = 0x10, 0x00))
        assertCorrupt(header.sealedWith(flags = 0x20, 0x00))
        // An accent flagging the default threshold, one flagging a threshold that does not exist,
        // and one flagging tones that are the defaults.
        assertCorrupt(header.sealedWith(flags = 0x04, 1, 0x05, 1, 2, 3, 1, 'a'.code, 0))
        assertCorrupt(header.sealedWith(flags = 0x04, 1, 0x05, 1, 2, 3, 1, 'a'.code, 3))
        assertCorrupt(header.sealedWith(flags = 0x04, 1, 0x03, 1, 2, 3, 1, 'a'.code, 40, 90, 80, 30))
        // A pin with neither mode, and two pins out of order.
        assertCorrupt(header.sealedWith(flags = 0x08, 0x01, 0x00, 0x00))
        assertCorrupt(header.sealedWith(flags = 0x08, 0x02, 0x05, 0x01, 1, 2, 3, 0x00, 0x01, 1, 2, 3))
        // The default theme name spelled out.
        assertCorrupt(header.sealedWith(flags = 0x20, 0x04, 8, *"AppTheme".encodeToByteArray().unsignedValues()))
        // A project name one byte over the limit, one in broken UTF-8, and a length in two bytes.
        assertCorrupt(header.sealedWith(flags = 0x10, 49, *IntArray(49) { 'a'.code }))
        assertCorrupt(header.sealedWith(flags = 0x10, 2, 0xC3, 0x28))
        assertCorrupt(header.sealedWith(flags = 0x10, 0x81, 0x00, 'a'.code))
        // Bytes left over after the last section.
        assertCorrupt(header.sealedWith(flags = 0x00, 0x00))
        // The controls, a well formed project name and a well formed accent threshold.
        assertEquals(DecodeResult.Ok(default, "a"), ShareCodec.decode(header.sealedWith(flags = 0x10, 1, 'a'.code)))
        val accent = Accent(name = "a", seed = Argb(0x010203), threshold = OnColorThreshold.Aaa)
        assertEquals(
            DecodeResult.Ok(default.copy(accents = listOf(accent)), null),
            ShareCodec.decode(header.sealedWith(flags = 0x04, 1, 0x05, 1, 2, 3, 1, 'a'.code, 2)),
        )
    }

    @Test
    fun decode_moreAccentsThanAnExportCarries_isCorrupt() {
        val header = bytesOf(ShareCodec.encode(default))
        // Each accent is plain flags, the seed 010203 and a one letter name.
        val accents = List(MAX_ACCENTS + 1) { index -> listOf(0x00, 1, 2, 3, 1, 'a'.code + index) }
        val eight = header.sealedWith(flags = 0x04, MAX_ACCENTS, *accents.take(MAX_ACCENTS).flatten().toIntArray())
        assertEquals(MAX_ACCENTS, assertIs<DecodeResult.Ok>(ShareCodec.decode(eight)).document.accents.size)
        assertCorrupt(header.sealedWith(flags = 0x04, MAX_ACCENTS + 1, *accents.flatten().toIntArray()))
    }

    @Test
    fun decode_accentNameOverTheLimit_isCorrupt() {
        val header = bytesOf(ShareCodec.encode(default))
        val longest = MAX_ACCENT_NAME_BYTES
        val code = header.sealedWith(flags = 0x04, 1, 0x00, 1, 2, 3, longest, *IntArray(longest) { 'a'.code })
        val accent = Accent(name = "a".repeat(longest), seed = Argb(0x010203), harmonize = false)
        assertEquals(DecodeResult.Ok(default.copy(accents = listOf(accent)), null), ShareCodec.decode(code))
        val tooLong = longest + 1
        assertCorrupt(header.sealedWith(flags = 0x04, 1, 0x00, 1, 2, 3, tooLong, *IntArray(tooLong) { 'a'.code }))
    }

    @Test
    fun decode_randomBytesWithAValidChecksum_neverThrowAndWriteBackToThemselves() {
        val random = Random(DocumentArb.DEFAULT_ARB_SEED)
        repeat(10_000) {
            val bytes = random.nextBytes(random.nextInt(from = 8, until = 40))
            bytes[0] = ShareCodec.VERSION.toByte()
            bytes[5] = (bytes[5].toInt() and 0x07).toByte()
            bytes[6] = random.nextInt(from = -100, until = 101).toByte()
            // Mostly zero section flags, so the header on its own gets exercised as much as the sections.
            bytes[7] = (random.nextInt() and random.nextInt() and 0x3F).toByte()
            assertStable(resealed(bytes + byteArrayOf(0)))
        }
    }

    @Test
    fun decode_mutatedCodesWithAValidChecksum_neverThrowAndWriteBackToThemselves() {
        val random = Random(DocumentArb.DEFAULT_ARB_SEED)
        val arb = DocumentArb(seed = 19)
        repeat(10_000) {
            val bytes = bytesOf(ShareCodec.encode(arb.nextDocument(), projectName = "Acme"))
            repeat(random.nextInt(from = 1, until = 4)) {
                val index = random.nextInt(from = 1, until = bytes.size - 1)
                bytes[index] = random.nextInt().toByte()
            }
            assertStable(resealed(bytes))
        }
    }

    @Test
    fun decode_randomText_neverThrows() {
        val random = Random(DocumentArb.DEFAULT_ARB_SEED)
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_=+/ %é"
        repeat(10_000) {
            val text = CharArray(random.nextInt(until = 40)) { alphabet.random(random) }.concatToString()
            assertStable(text)
        }
    }

    /**
     * Decodes [code] and, when it reads cleanly, checks it writes back to exactly the same text.
     */
    private fun assertStable(code: String) {
        val result = ShareCodec.decode(code)
        if (result is DecodeResult.Ok) assertEquals(code, ShareCodec.encode(result.document, result.projectName))
    }

    private fun assertReservedBitsRejected(
        document: ThemeDocument,
        index: Int,
        reserved: Int,
    ) {
        val bytes = bytesOf(ShareCodec.encode(document))
        assertIs<DecodeResult.Ok>(ShareCodec.decode(resealed(bytes)))
        for (bit in 0 until 8) {
            if ((reserved and (1 shl bit)) == 0) continue
            val flipped = bytes.copyOf().also { copy -> copy[index] = (copy[index].toInt() or (1 shl bit)).toByte() }
            assertEquals(DecodeResult.Corrupt, ShareCodec.decode(resealed(flipped)), "byte $index bit $bit")
        }
    }

    private fun assertCorruptWith(
        bytes: ByteArray,
        change: (ByteArray) -> Unit,
    ) {
        val changed = bytes.copyOf().also(change)
        assertNotEquals(bytes.toList(), changed.toList())
        assertCorrupt(resealed(changed))
    }

    private fun assertCorrupt(code: String) {
        assertEquals(DecodeResult.Corrupt, ShareCodec.decode(code), code)
    }

    /**
     * The eight header bytes of this default code with [flags] as the section byte, then [body],
     * then a fresh checksum.
     */
    private fun ByteArray.sealedWith(
        flags: Int,
        vararg body: Int,
    ): String {
        val header = copyOf(8).also { header -> header[7] = flags.toByte() }
        return resealed(header + ByteArray(body.size) { index -> body[index].toByte() } + byteArrayOf(0))
    }

    private fun ByteArray.unsignedValues(): IntArray = IntArray(size) { index -> unsigned(index) }

    private companion object {
        val RETIRED_SLOT_CODES: List<Int> = (18..29) + (41..45)
    }
}

internal fun bytesOf(code: String): ByteArray = checkNotNull(Base64Url.decode(code)) { "Not base64url, $code" }

/**
 * [bytes] as a code, with its last byte replaced by the checksum of everything before it.
 */
internal fun resealed(bytes: ByteArray): String {
    val sealed = bytes.copyOf()
    sealed[sealed.size - 1] = Crc8.compute(sealed, end = sealed.size - 1).toByte()
    return Base64Url.encode(sealed)
}
