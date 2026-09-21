package com.materialkolor.serialization

import com.materialkolor.PaletteStyle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Writes a [PaletteStyle] as a single string, so it works with every kotlinx format, Json, Cbor,
 * protobuf and the rest, and with formats that only accept primitives for map keys.
 *
 * The nine objects serialize under the names the old enum used, `"TonalSpot"` and its siblings, so
 * documents written against 5.x still read. A [PaletteStyle.Cmf] carries its tertiary seed color
 * along as `Cmf:AARRGGBB`, and one without a seed is plain `"Cmf"`.
 *
 * [PaletteStyle] is annotated with this serializer, so `Json.encodeToString(style)` picks it up on
 * its own. Reading a string no style answers to fails with a [SerializationException].
 */
public object PaletteStyleSerializer : KSerializer<PaletteStyle> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.materialkolor.PaletteStyle", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: PaletteStyle,
    ) {
        encoder.encodeString(value.toStorageString())
    }

    override fun deserialize(decoder: Decoder): PaletteStyle {
        val value = decoder.decodeString()
        return PaletteStyle.fromStorageString(value)
            ?: throw SerializationException("Unknown PaletteStyle \"$value\"")
    }
}
