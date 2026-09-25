package com.materialkolor.builder.domain.color

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline
import kotlin.math.abs

/**
 * How much contrast a scheme is generated with, held in hundredths so it stays exact.
 *
 * The engine takes contrast as a double between -1.0 and 1.0. Storing that double would let a
 * saved theme drift when it is written and read back, so the document keeps whole hundredths and
 * [toDouble] does the division on the way into the engine.
 *
 * A document only ever holds one of the four named [Stops]. Links and saved projects written before
 * that can carry a level in between, and [snapped] moves them onto the nearest one as they are read.
 *
 * @property[hundredths] The level in hundredths, from -100 to 100.
 */
@JvmInline
@Serializable(with = ContrastLevelSerializer::class)
public value class ContrastLevel(
    public val hundredths: Int,
) {
    init {
        require(hundredths in -100..100) { "Contrast is -100 to 100 hundredths, got $hundredths" }
    }

    /**
     * The level as the double the engine expects, -1.0 to 1.0.
     */
    public fun toDouble(): Double = hundredths / 100.0

    /**
     * The named level nearest this one, which is this one when it already is a named level.
     */
    public fun snapped(): ContrastLevel = nearest(toDouble())

    override fun toString(): String = "ContrastLevel($hundredths)"

    public companion object {
        /**
         * Less contrast than the spec default, for people who find the default harsh.
         */
        public val Reduced: ContrastLevel = ContrastLevel(-100)

        /**
         * The level every scheme uses until someone asks for another.
         */
        public val Standard: ContrastLevel = ContrastLevel(0)

        /**
         * Halfway to [High].
         */
        public val Medium: ContrastLevel = ContrastLevel(50)

        /**
         * The most contrast the engine offers.
         */
        public val High: ContrastLevel = ContrastLevel(100)

        /**
         * The four levels the contrast control offers, from least to most.
         */
        public val Stops: List<ContrastLevel> = listOf(Reduced, Standard, Medium, High)

        /**
         * The named level nearest [value], the one a link or a project with a level in between
         * opens at. A value halfway between two levels goes to the one nearer [Standard], so 0.25
         * and -0.5 open at Standard and 0.75 at Medium. A value past either end goes to that end.
         */
        public fun nearest(value: Double): ContrastLevel =
            Stops.minWith(
                compareBy<ContrastLevel> { level -> abs(level.toDouble() - value) }
                    .thenBy { level -> abs(level.hundredths) },
            )
    }
}

/**
 * Writes a [ContrastLevel] as its hundredths, so the range check runs again on the way back in.
 */
public object ContrastLevelSerializer : KSerializer<ContrastLevel> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.materialkolor.builder.domain.color.ContrastLevel", PrimitiveKind.INT)

    override fun serialize(
        encoder: Encoder,
        value: ContrastLevel,
    ) {
        encoder.encodeInt(value.hundredths)
    }

    override fun deserialize(decoder: Decoder): ContrastLevel = ContrastLevel(decoder.decodeInt())
}
