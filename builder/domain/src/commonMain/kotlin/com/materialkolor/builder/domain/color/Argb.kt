package com.materialkolor.builder.domain.color

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

/**
 * An opaque color packed as `0xAARRGGBB`.
 *
 * Every color the builder saves is a seed, a key color or a pin, and none of those mean anything
 * half transparent, so the alpha byte is filled in for you instead of being rejected. Two colors
 * that differ only in alpha are therefore the same [Argb].
 *
 * @property[value] The packed color, always with an alpha byte of `0xFF`.
 */
@JvmInline
@Serializable(with = ArgbSerializer::class)
public value class Argb private constructor(
    public val value: Int,
) {
    /**
     * The red channel, 0 to 255.
     */
    public val red: Int
        get() = (value shr 16) and CHANNEL_MASK

    /**
     * The green channel, 0 to 255.
     */
    public val green: Int
        get() = (value shr 8) and CHANNEL_MASK

    /**
     * The blue channel, 0 to 255.
     */
    public val blue: Int
        get() = value and CHANNEL_MASK

    /**
     * The color as `#RRGGBB` in uppercase, the form share links and exported code use.
     *
     * The alpha byte is left out because it is always `0xFF`.
     */
    public fun toHex(): String = "#${red.toHexByte()}${green.toHexByte()}${blue.toHexByte()}"

    override fun toString(): String = toHex()

    public companion object {
        /**
         * Pack [value] as an opaque color, filling in the alpha byte.
         *
         * Every color is built here. The constructor takes its argument as given, and it wins the
         * name `Argb(...)` inside the class body and inside this companion, so write [invoke] by
         * name in those two places. Everywhere else, top level code in this file included, which
         * is where [ArgbSerializer] sits, `Argb(...)` already resolves to this function.
         */
        public operator fun invoke(value: Int): Argb = Argb(value or OPAQUE_ALPHA)

        /**
         * Read a color written as `#RRGGBB` or `#AARRGGBB`, with or without the leading `#`.
         *
         * The alpha byte of an eight digit color is read and then thrown away, the same way the
         * constructor treats it.
         *
         * @throws[IllegalArgumentException] when [hex] is not six or eight hex digits.
         */
        public fun fromHex(hex: String): Argb {
            val digits = hex.removePrefix("#")
            require(digits.length == 6 || digits.length == 8) { "Expected #RRGGBB or #AARRGGBB, got \"$hex\"" }
            return invoke(digits.toLong(radix = 16).toInt())
        }
    }
}

private const val CHANNEL_MASK: Int = 0xFF

private const val OPAQUE_ALPHA: Int = 0xFF shl 24

private fun Int.toHexByte(): String = toString(radix = 16).uppercase().padStart(length = 2, padChar = '0')

/**
 * Writes an [Argb] as its `#RRGGBB` text, so a saved theme stays readable by eye.
 */
public object ArgbSerializer : KSerializer<Argb> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.materialkolor.builder.domain.color.Argb", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Argb,
    ) {
        encoder.encodeString(value.toHex())
    }

    override fun deserialize(decoder: Decoder): Argb = Argb.fromHex(decoder.decodeString())
}
