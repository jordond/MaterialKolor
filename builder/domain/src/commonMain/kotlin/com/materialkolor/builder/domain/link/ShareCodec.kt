package com.materialkolor.builder.domain.link

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.model.withCodeOrNull
import com.materialkolor.builder.domain.validate.MAX_ACCENTS
import com.materialkolor.builder.domain.validate.MAX_ACCENT_NAME_BYTES
import com.materialkolor.builder.domain.validate.MAX_PROJECT_NAME_BYTES
import com.materialkolor.builder.domain.validate.validateAccents

/**
 * What reading a share code came to.
 */
@Immutable
public sealed interface DecodeResult {
    /**
     * The code read cleanly.
     *
     * @property[document] The theme the code carries. Its seed source is always typed, because how
     * a seed was chosen stays on the machine it was chosen on.
     * @property[projectName] The name of the project the theme was shared from, or null when the
     * code carries none.
     */
    @Immutable
    public data class Ok(
        public val document: ThemeDocument,
        public val projectName: String?,
    ) : DecodeResult

    /**
     * The code was written by a newer builder, in a format this one cannot read yet.
     */
    @Immutable
    public data object UnknownVersion : DecodeResult

    /**
     * The code is damaged, cut short, or was never a share code.
     */
    @Immutable
    public data object Corrupt : DecodeResult
}

/**
 * Turns a theme into the short code a `/t/` link carries, and back.
 *
 * The code is base64url over a small binary record. The first eight bytes are always there, the
 * version, the seed as red, green and blue, then a byte each for how the scheme is generated, for
 * the export target, for contrast and for which of the optional sections follow. A theme that
 * changes nothing but its seed is those eight bytes and a checksum, twelve characters in all.
 *
 * The seed sits at bytes 1 to 3 so a page script or the link preview Worker can read it, along
 * with the style in the low four bits of byte 4 and the library in the low two bits of byte 5,
 * without decoding the rest.
 *
 * The writing is canonical. Sections are written only when they differ from the defaults, always
 * in the same order, and pins and custom tones go out in code order, so equal documents give equal
 * codes. Reading is just as strict and refuses anything the writer would never produce, which
 * means a code that reads cleanly writes back to itself. The one exception is a contrast between
 * the four named levels, which older codes can carry and which reads as the nearest level.
 *
 * A document that passes [validateAccents] and holds a named contrast level comes back from its
 * code unchanged apart from its seed source and any custom tone entry with neither tone moved,
 * which the writer drops. The writer refuses more than [MAX_ACCENTS] accents or an accent name over
 * [MAX_ACCENT_NAME_BYTES] UTF-8 bytes, because the reader would refuse them too. The project name
 * is the one field cut to fit rather than refused.
 *
 * The seed source never travels. It is local history, and an image source would leak a file name.
 * Accents travel as their seed, name, harmonize choice, tones and threshold, and nothing else.
 */
public object ShareCodec {
    /**
     * The format version this codec writes, and the only one it reads.
     */
    public const val VERSION: Int = 1

    /**
     * The longest text [decode] reads. With every capped field full and a 255 byte theme name a
     * code is about 1,460 characters, so only a theme name over five kilobytes could go past this,
     * and pasted text that long is refused without being decoded.
     */
    public const val MAX_CODE_LENGTH: Int = 8192

    /**
     * The share code for [document], with [projectName] riding along when there is one.
     *
     * An empty [projectName] is the same as none. One longer than [MAX_PROJECT_NAME_BYTES] UTF-8
     * bytes is cut to fit at a character boundary, so this never fails on a name typed elsewhere.
     *
     * @throws IllegalArgumentException when [document] has more than [MAX_ACCENTS] accents or an
     * accent name over [MAX_ACCENT_NAME_BYTES] UTF-8 bytes, both of which [validateAccents] reports.
     */
    public fun encode(
        document: ThemeDocument,
        projectName: String? = null,
    ): String {
        require(document.accents.size <= MAX_ACCENTS) {
            "A share code carries at most $MAX_ACCENTS accents, not ${document.accents.size}"
        }
        document.accents.forEach { accent ->
            require(accent.name.encodeToByteArray().size <= MAX_ACCENT_NAME_BYTES) {
                "Accent name '${accent.name}' is over $MAX_ACCENT_NAME_BYTES UTF-8 bytes"
            }
        }
        val name = projectName.orEmpty().truncateUtf8(MAX_PROJECT_NAME_BYTES)
        val options = TargetOptions.of(document)
        val sections =
            (if (!document.keyColors.isEmpty()) SECTION_KEY_COLORS else 0) or
                (if (document.cmfTertiarySeed != null) SECTION_CMF_SEED else 0) or
                (if (document.accents.isNotEmpty()) SECTION_ACCENTS else 0) or
                (if (document.pins.isNotEmpty()) SECTION_PINS else 0) or
                (if (name.isNotEmpty()) SECTION_PROJECT_NAME else 0) or
                (if (options.flags != 0) SECTION_TARGET_OPTIONS else 0)

        val writer = ByteWriter()
        writer.byte(VERSION)
        writer.argb(document.seed)
        writer.byte(
            document.style.code or
                (document.spec.code shl SPEC_SHIFT) or
                (document.platform.code shl PLATFORM_SHIFT) or
                (if (document.amoled) AMOLED_FLAG else 0),
        )
        writer.byte(document.library.code or (if (document.expressive) EXPRESSIVE_FLAG else 0))
        writer.byte(document.contrast.hundredths)
        writer.byte(sections)
        if (sections has SECTION_KEY_COLORS) writer.keyColors(document.keyColors)
        document.cmfTertiarySeed?.let { seed -> writer.argb(seed) }
        if (sections has SECTION_ACCENTS) writer.accents(document.accents)
        if (sections has SECTION_PINS) writer.pins(document.pins)
        if (sections has SECTION_PROJECT_NAME) writer.text(name)
        if (sections has SECTION_TARGET_OPTIONS) writer.targetOptions(options)
        writer.byte(Crc8.compute(writer.toByteArray()))
        return Base64Url.encode(writer.toByteArray())
    }

    /**
     * The theme [code] carries, with a contrast between the named levels moved onto the nearest
     * one. Never throws, whatever the text, and text longer than [MAX_CODE_LENGTH] is refused
     * before any of it is read.
     */
    public fun decode(code: String): DecodeResult {
        if (code.length > MAX_CODE_LENGTH) return DecodeResult.Corrupt
        val bytes = Base64Url.decode(code) ?: return DecodeResult.Corrupt
        if (bytes.isEmpty()) return DecodeResult.Corrupt
        val version = bytes[0].toInt() and BYTE_MASK
        if (version > VERSION) return DecodeResult.UnknownVersion
        if (version < VERSION || bytes.size < MIN_CODE_BYTES) return DecodeResult.Corrupt
        val checksum = bytes.size - 1
        if (Crc8.compute(bytes, end = checksum) != (bytes[checksum].toInt() and BYTE_MASK)) return DecodeResult.Corrupt
        return ByteReader(bytes, end = checksum).document() ?: DecodeResult.Corrupt
    }
}

private val DEFAULT_DOCUMENT: ThemeDocument = ThemeDocument.Default

private val DEFAULT_ACCENT: Accent = Accent(name = "", seed = DEFAULT_DOCUMENT.seed)

private val EMPTY_CUSTOM_TONE: CustomTone = CustomTone()

private const val BYTE_MASK: Int = 0xFF
private const val MIN_CODE_BYTES: Int = 9
private const val MAX_TONE: Int = 100
private const val NO_TONE: Int = 0xFF
private const val MAX_VARINT_BYTES: Int = 4

// Byte 4, how the scheme is generated. The style takes the low four bits.
private const val STYLE_MASK: Int = 0x0F
private const val SPEC_SHIFT: Int = 4
private const val SPEC_MASK: Int = 0x03
private const val PLATFORM_SHIFT: Int = 6
private const val PLATFORM_MASK: Int = 0x01
private const val AMOLED_FLAG: Int = 0x80

// Byte 5, the export target. The library takes the low two bits.
private const val LIBRARY_MASK: Int = 0x03
private const val EXPRESSIVE_FLAG: Int = 0x04
private const val TARGET_RESERVED: Int = 0xF8

// Byte 7, which sections follow, in the order they are written.
private const val SECTION_KEY_COLORS: Int = 0x01
private const val SECTION_CMF_SEED: Int = 0x02
private const val SECTION_ACCENTS: Int = 0x04
private const val SECTION_PINS: Int = 0x08
private const val SECTION_PROJECT_NAME: Int = 0x10
private const val SECTION_TARGET_OPTIONS: Int = 0x20
private const val SECTION_RESERVED: Int = 0xC0

private const val KEY_COLORS_RESERVED: Int = 0xC0

private const val ACCENT_HARMONIZE: Int = 0x01
private const val ACCENT_TONES: Int = 0x02
private const val ACCENT_THRESHOLD: Int = 0x04
private const val ACCENT_RESERVED: Int = 0xF8

private const val PIN_LIGHT: Int = 0x01
private const val PIN_DARK: Int = 0x02
private const val PIN_RESERVED: Int = 0xFC

private const val OPTION_MOTION_SCHEME: Int = 0x01
private const val OPTION_CUSTOM_TONES: Int = 0x02
private const val OPTION_THEME_NAME: Int = 0x04
private const val OPTION_RESERVED: Int = 0xF8

private infix fun Int.has(flag: Int): Boolean = (this and flag) != 0

private val KEY_COLORS_IN_CODE_ORDER: List<KeyColor> = KeyColor.entries.sortedBy { slot -> slot.code }

private val DEFAULT_OPTIONS: TargetOptions = TargetOptions.of(DEFAULT_DOCUMENT)

/**
 * The part of the document only the export target reads, which travels as one section.
 */
private class TargetOptions(
    val motionScheme: MotionSchemeChoice,
    val themeName: String,
    val customTones: Map<CustomSlot, CustomTone>,
) {
    val flags: Int
        get() =
            (if (motionScheme != DEFAULT_DOCUMENT.motionScheme) OPTION_MOTION_SCHEME else 0) or
                (if (customTones.isNotEmpty()) OPTION_CUSTOM_TONES else 0) or
                (if (themeName != DEFAULT_DOCUMENT.themeName) OPTION_THEME_NAME else 0)

    companion object {
        // A slot with neither tone moved is the same as no entry, so it never travels.
        fun of(document: ThemeDocument): TargetOptions =
            TargetOptions(
                motionScheme = document.motionScheme,
                themeName = document.themeName,
                customTones = document.customTones.filterValues { tone -> tone != EMPTY_CUSTOM_TONE },
            )
    }
}

private class ByteWriter {
    private var buffer = ByteArray(32)
    private var size = 0

    fun byte(value: Int) {
        if (size == buffer.size) buffer = buffer.copyOf(size * 2)
        buffer[size] = (value and BYTE_MASK).toByte()
        size += 1
    }

    fun argb(color: Argb) {
        byte(color.red)
        byte(color.green)
        byte(color.blue)
    }

    fun varint(value: Int) {
        var rest = value
        while (rest >= 0x80) {
            byte((rest and 0x7F) or 0x80)
            rest = rest ushr 7
        }
        byte(rest)
    }

    fun text(value: String) {
        val bytes = value.encodeToByteArray()
        varint(bytes.size)
        bytes.forEach { value -> byte(value.toInt()) }
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)
}

private fun ByteWriter.keyColors(keyColors: KeyColors) {
    val set = KEY_COLORS_IN_CODE_ORDER.filter { slot -> keyColors[slot] != null }
    byte(set.fold(0) { mask, slot -> mask or (1 shl slot.code) })
    set.forEach { slot -> argb(checkNotNull(keyColors[slot])) }
}

private fun ByteWriter.accents(accents: List<Accent>) {
    varint(accents.size)
    accents.forEach { accent ->
        val tones = accent.light != DEFAULT_ACCENT.light || accent.dark != DEFAULT_ACCENT.dark
        val threshold = accent.threshold != DEFAULT_ACCENT.threshold
        byte(
            (if (accent.harmonize) ACCENT_HARMONIZE else 0) or
                (if (tones) ACCENT_TONES else 0) or
                (if (threshold) ACCENT_THRESHOLD else 0),
        )
        argb(accent.seed)
        text(accent.name)
        if (tones) {
            byte(accent.light.color)
            byte(accent.light.container)
            byte(accent.dark.color)
            byte(accent.dark.container)
        }
        if (threshold) byte(accent.threshold.code)
    }
}

private fun ByteWriter.pins(pins: Map<Role, RolePin>) {
    byte(pins.size)
    pins.entries.sortedBy { (role, _) -> role.code }.forEach { (role, pin) ->
        byte(role.code)
        byte((if (pin.light != null) PIN_LIGHT else 0) or (if (pin.dark != null) PIN_DARK else 0))
        pin.light?.let { color -> argb(color) }
        pin.dark?.let { color -> argb(color) }
    }
}

/**
 * Writes the target options section. The theme name is bounded only by the 28-bit varint and
 * relies on validation upstream.
 */
private fun ByteWriter.targetOptions(options: TargetOptions) {
    byte(options.flags)
    if (options.flags has OPTION_MOTION_SCHEME) byte(options.motionScheme.code)
    if (options.flags has OPTION_THEME_NAME) text(options.themeName)
    if (options.flags has OPTION_CUSTOM_TONES) {
        byte(options.customTones.size)
        options.customTones.entries.sortedBy { (slot, _) -> slot.code }.forEach { (slot, tone) ->
            byte(slot.code)
            byte(tone.light ?: NO_TONE)
            byte(tone.dark ?: NO_TONE)
        }
    }
}

/**
 * Reads a share code up to its checksum. Every read answers null once the bytes run out, and every
 * section reader answers null for anything the writer would not have written, which the caller
 * turns into [DecodeResult.Corrupt].
 */
private class ByteReader(
    private val bytes: ByteArray,
    private val end: Int,
) {
    private var position = 1

    val exhausted: Boolean
        get() = position == end

    fun byte(): Int? {
        if (position >= end) return null
        val value = bytes[position].toInt() and BYTE_MASK
        position += 1
        return value
    }

    fun argb(): Argb? {
        val red = byte() ?: return null
        val green = byte() ?: return null
        val blue = byte() ?: return null
        return Argb((red shl 16) or (green shl 8) or blue)
    }

    fun varint(): Int? {
        var value = 0
        for (index in 0 until MAX_VARINT_BYTES) {
            val next = byte() ?: return null
            value = value or ((next and 0x7F) shl (7 * index))
            if (!(next has 0x80)) {
                // A zero last byte after the first spells a smaller number the long way round.
                return if (index > 0 && next == 0) null else value
            }
        }
        return null
    }

    fun text(maxBytes: Int = Int.MAX_VALUE): String? {
        val length = varint() ?: return null
        if (length > maxBytes || length > end - position) return null
        val slice = bytes.copyOfRange(position, position + length)
        position += length
        // Malformed UTF-8 decodes to replacement characters, which never encode back to the same
        // bytes, so this one comparison refuses it without anything having to throw.
        val text = slice.decodeToString()
        return text.takeIf { text.encodeToByteArray().contentEquals(slice) }
    }

    fun tone(): Int? = byte()?.takeIf { tone -> tone <= MAX_TONE }
}

private fun ByteReader.document(): DecodeResult.Ok? {
    val seed = argb() ?: return null
    val scheme = byte() ?: return null
    val style = Style.entries.withCodeOrNull(scheme and STYLE_MASK) ?: return null
    val spec = SpecVersion.entries.withCodeOrNull((scheme shr SPEC_SHIFT) and SPEC_MASK) ?: return null
    val platform = SchemePlatform.entries.withCodeOrNull((scheme shr PLATFORM_SHIFT) and PLATFORM_MASK) ?: return null
    val target = byte() ?: return null
    if (target has TARGET_RESERVED) return null
    val library = Library.entries.withCodeOrNull(target and LIBRARY_MASK) ?: return null
    val contrast = (byte() ?: return null).toByte().toInt()
    if (contrast !in -100..100) return null
    val sections = byte() ?: return null
    if (sections has SECTION_RESERVED) return null

    val keyColors = if (sections has SECTION_KEY_COLORS) keyColors() ?: return null else KeyColors()
    val cmfTertiarySeed = if (sections has SECTION_CMF_SEED) argb() ?: return null else null
    val accents = if (sections has SECTION_ACCENTS) accents() ?: return null else emptyList()
    val pins = if (sections has SECTION_PINS) pins() ?: return null else emptyMap()
    val projectName = if (sections has SECTION_PROJECT_NAME) projectName() ?: return null else null
    val options = if (sections has SECTION_TARGET_OPTIONS) targetOptions() ?: return null else DEFAULT_OPTIONS
    if (!exhausted) return null

    val document =
        ThemeDocument(
            seed = seed,
            keyColors = keyColors,
            style = style,
            cmfTertiarySeed = cmfTertiarySeed,
            contrast = ContrastLevel(contrast).snapped(),
            spec = spec,
            platform = platform,
            amoled = scheme has AMOLED_FLAG,
            accents = accents,
            pins = pins,
            library = library,
            expressive = target has EXPRESSIVE_FLAG,
            motionScheme = options.motionScheme,
            themeName = options.themeName,
            customTones = options.customTones,
        )
    return DecodeResult.Ok(document, projectName)
}

private fun ByteReader.keyColors(): KeyColors? {
    val mask = byte() ?: return null
    if (mask == 0 || mask has KEY_COLORS_RESERVED) return null
    var keyColors = KeyColors()
    for (slot in KEY_COLORS_IN_CODE_ORDER) {
        if (mask has (1 shl slot.code)) keyColors = keyColors.with(slot, argb() ?: return null)
    }
    return keyColors
}

private fun ByteReader.accents(): List<Accent>? {
    val count = varint() ?: return null
    if (count == 0 || count > MAX_ACCENTS) return null
    val accents = mutableListOf<Accent>()
    repeat(count) { accents += accent() ?: return null }
    return accents
}

private fun ByteReader.accent(): Accent? {
    val flags = byte() ?: return null
    if (flags has ACCENT_RESERVED) return null
    val seed = argb() ?: return null
    val name = text(maxBytes = MAX_ACCENT_NAME_BYTES) ?: return null
    val light = if (flags has ACCENT_TONES) familyTones() ?: return null else DEFAULT_ACCENT.light
    val dark = if (flags has ACCENT_TONES) familyTones() ?: return null else DEFAULT_ACCENT.dark
    if (flags has ACCENT_TONES && light == DEFAULT_ACCENT.light && dark == DEFAULT_ACCENT.dark) return null
    val threshold = if (flags has ACCENT_THRESHOLD) threshold() ?: return null else DEFAULT_ACCENT.threshold
    return Accent(
        name = name,
        seed = seed,
        harmonize = flags has ACCENT_HARMONIZE,
        light = light,
        dark = dark,
        threshold = threshold,
    )
}

private fun ByteReader.familyTones(): FamilyTones? {
    val color = tone() ?: return null
    val container = tone() ?: return null
    return FamilyTones(color = color, container = container)
}

private fun ByteReader.threshold(): OnColorThreshold? {
    val threshold = OnColorThreshold.entries.withCodeOrNull(byte() ?: return null) ?: return null
    return if (threshold == DEFAULT_ACCENT.threshold) null else threshold
}

private fun ByteReader.pins(): Map<Role, RolePin>? {
    val count = byte() ?: return null
    if (count == 0) return null
    val pins = linkedMapOf<Role, RolePin>()
    var previous = -1
    repeat(count) {
        val role = Role.entries.withCodeOrNull(byte() ?: return null) ?: return null
        if (role.code <= previous) return null
        previous = role.code
        val modes = byte() ?: return null
        if (modes == 0 || modes has PIN_RESERVED) return null
        val light = if (modes has PIN_LIGHT) argb() ?: return null else null
        val dark = if (modes has PIN_DARK) argb() ?: return null else null
        pins[role] = RolePin(light = light, dark = dark)
    }
    return pins
}

private fun ByteReader.projectName(): String? =
    text(maxBytes = MAX_PROJECT_NAME_BYTES)?.takeIf { name -> name.isNotEmpty() }

private fun ByteReader.targetOptions(): TargetOptions? {
    val flags = byte() ?: return null
    if (flags == 0 || flags has OPTION_RESERVED) return null
    val motionScheme =
        if (flags has OPTION_MOTION_SCHEME) motionScheme() ?: return null else DEFAULT_OPTIONS.motionScheme
    val themeName = if (flags has OPTION_THEME_NAME) themeName() ?: return null else DEFAULT_OPTIONS.themeName
    val customTones = if (flags has OPTION_CUSTOM_TONES) customTones() ?: return null else emptyMap()
    return TargetOptions(motionScheme, themeName, customTones)
}

private fun ByteReader.motionScheme(): MotionSchemeChoice? {
    val motionScheme = MotionSchemeChoice.entries.withCodeOrNull(byte() ?: return null) ?: return null
    return if (motionScheme == DEFAULT_OPTIONS.motionScheme) null else motionScheme
}

private fun ByteReader.themeName(): String? = text()?.takeIf { name -> name != DEFAULT_OPTIONS.themeName }

private fun ByteReader.customTones(): Map<CustomSlot, CustomTone>? {
    val count = byte() ?: return null
    if (count == 0) return null
    val tones = linkedMapOf<CustomSlot, CustomTone>()
    var previous = -1
    repeat(count) {
        // A retired slot code has no entry, so it lands here and the code reads as corrupt.
        val slot = CustomSlot.entries.withCodeOrNull(byte() ?: return null) ?: return null
        if (slot.code <= previous) return null
        previous = slot.code
        val light = byte() ?: return null
        val dark = byte() ?: return null
        if (!light.isToneOrNone() || !dark.isToneOrNone()) return null
        // The writer drops a slot with neither tone moved.
        if (light == NO_TONE && dark == NO_TONE) return null
        tones[slot] = CustomTone(light = light.toneOrNull(), dark = dark.toneOrNull())
    }
    return tones
}

private fun Int.isToneOrNone(): Boolean = this <= MAX_TONE || this == NO_TONE

private fun Int.toneOrNull(): Int? = if (this == NO_TONE) null else this

/**
 * This string cut to at most [maxBytes] UTF-8 bytes, never in the middle of a character.
 */
private fun String.truncateUtf8(maxBytes: Int): String {
    val bytes = encodeToByteArray()
    if (bytes.size <= maxBytes) return this
    var end = maxBytes
    while (end > 0 && (bytes[end].toInt() and 0xC0) == 0x80) end -= 1
    return bytes.decodeToString(startIndex = 0, endIndex = end)
}
