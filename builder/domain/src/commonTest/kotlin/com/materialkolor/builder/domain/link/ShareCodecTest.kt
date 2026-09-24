package com.materialkolor.builder.domain.link

import com.materialkolor.builder.domain.DocumentArb
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.validate.MAX_ACCENTS
import com.materialkolor.builder.domain.validate.MAX_ACCENT_NAME_BYTES
import com.materialkolor.builder.domain.validate.MAX_PROJECT_NAME_BYTES
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ShareCodecTest {
    @Test
    fun shareCodec_randomDocuments_roundTripWithATypedSeedSource() {
        val random = Random(DocumentArb.DEFAULT_ARB_SEED)
        DocumentArb().documents(count = 10_000).forEach { document ->
            val projectName = PROJECT_NAMES.random(random)
            val code = ShareCodec.encode(document, projectName)
            val expected = DecodeResult.Ok(document.copy(seedSource = SeedSource.Typed), projectName)
            assertEquals(expected, ShareCodec.decode(code), code)
        }
    }

    @Test
    fun shareCodec_randomDocuments_writeBackToTheSameCode() {
        DocumentArb(seed = 7).documents(count = 2_000).forEach { document ->
            val code = ShareCodec.encode(document, projectName = "Acme")
            val decoded = ShareCodec.decode(code) as DecodeResult.Ok
            assertEquals(code, ShareCodec.encode(decoded.document, decoded.projectName))
        }
    }

    @Test
    fun decode_contrastBetweenTheNamedLevels_opensAtTheNearestOne() {
        val cases = mapOf(30 to ContrastLevel.Medium, 20 to ContrastLevel.Standard)

        cases.forEach { (hundredths, level) ->
            val code = ShareCodec.encode(ThemeDocument.Default.copy(contrast = ContrastLevel(hundredths)))
            val opened = ThemeDocument.Default.copy(contrast = level)
            assertEquals(DecodeResult.Ok(opened, null), ShareCodec.decode(code), code)
        }
    }

    @Test
    fun encode_equalDocumentsBuiltInADifferentOrder_giveEqualCodes() {
        DocumentArb(seed = 11).documents(count = 1_000).forEach { document ->
            val reordered =
                document.copy(
                    seedSource = SeedSource.Shuffled,
                    pins = document.pins.entries
                        .reversed()
                        .associate { (role, pin) -> role to pin },
                    customTones = document.customTones.entries
                        .reversed()
                        .associate { (slot, tone) -> slot to tone },
                )
            assertEquals(ShareCodec.encode(document), ShareCodec.encode(reordered))
        }
    }

    @Test
    fun encode_seedSource_neverChangesTheCode() {
        val sources =
            listOf(
                SeedSource.Typed,
                SeedSource.Picked,
                SeedSource.Eyedropper,
                SeedSource.Shuffled,
                SeedSource.Preset(id = "res-2"),
                SeedSource.Image(name = "holiday.png", candidates = listOf(Argb(0x123456))),
            )
        val codes = sources.map { source -> ShareCodec.encode(ThemeDocument.Default.copy(seedSource = source)) }
        assertEquals(1, codes.toSet().size, codes.toString())
    }

    @Test
    fun encode_defaultDocument_isTwelveCharacters() {
        val code = ShareCodec.encode(ThemeDocument.Default)
        assertEquals(12, code.length, code)
        assertEquals(code, ShareCodec.encode(ThemeDocument.Default, projectName = ""))
    }

    @Test
    fun encode_anyDocument_putsTheSeedStyleAndLibraryAtFixedOffsets() {
        DocumentArb(seed = 13).documents(count = 500).forEach { document ->
            val bytes = checkNotNull(Base64Url.decode(ShareCodec.encode(document)))
            assertEquals(ShareCodec.VERSION, bytes.unsigned(0))
            assertEquals(document.seed.red, bytes.unsigned(1))
            assertEquals(document.seed.green, bytes.unsigned(2))
            assertEquals(document.seed.blue, bytes.unsigned(3))
            assertEquals(document.style.code, bytes.unsigned(4) and 0x0F)
            assertEquals(document.library.code, bytes.unsigned(5) and 0x03)
        }
    }

    @Test
    fun encode_everyCodedEnum_fitsTheBitsItIsGiven() {
        assertTrue(Style.entries.all { style -> style.code in 0..0x0F })
        assertTrue(SpecVersion.entries.all { spec -> spec.code in 0..0x03 })
        assertTrue(SchemePlatform.entries.all { platform -> platform.code in 0..0x01 })
        assertTrue(Library.entries.all { library -> library.code in 0..0x03 })
        assertTrue(KeyColor.entries.all { slot -> slot.code in 0..5 })
        assertTrue(Role.entries.all { role -> role.code in 0..0xFF })
        assertTrue(CustomSlot.entries.all { slot -> slot.code in 0..0xFE })
    }

    @Test
    fun encode_eachSection_addsOnlyItsOwnBytes() {
        val base = ThemeDocument.Default
        val seed = Argb(0x336699)
        val sizes =
            mapOf(
                "key color" to base.copy(keyColors = base.keyColors.with(KeyColor.Error, seed)),
                "cmf seed" to base.copy(cmfTertiarySeed = seed),
                "accent" to base.copy(accents = listOf(Accent(name = "brand", seed = seed))),
                "pin" to base.copy(pins = mapOf(Role.Primary to RolePin(light = seed))),
                "theme name" to base.copy(themeName = "Brand"),
                "custom tone" to base.copy(customTones = mapOf(CustomSlot.Shadow to CustomTone(light = 4))),
            ).mapValues { (_, document) -> checkNotNull(Base64Url.decode(ShareCodec.encode(document))).size }
        val expected =
            mapOf(
                "key color" to 9 + 1 + 3,
                "cmf seed" to 9 + 3,
                "accent" to 9 + 1 + 1 + 3 + 1 + 5,
                "pin" to 9 + 1 + 1 + 1 + 3,
                "theme name" to 9 + 1 + 1 + 5,
                "custom tone" to 9 + 1 + 1 + 3,
            )
        assertEquals(expected, sizes)
    }

    @Test
    fun encode_projectName_roundTripsInUtf8() {
        listOf("Acme", "Café Olé", "日本語のプロジェクト", "x".repeat(MAX_PROJECT_NAME_BYTES)).forEach { name ->
            val decoded = ShareCodec.decode(ShareCodec.encode(ThemeDocument.Default, name))
            assertEquals(DecodeResult.Ok(ThemeDocument.Default, name), decoded)
        }
    }

    @Test
    fun encode_projectNameOverTheLimit_isCutAtACharacterBoundary() {
        val name = "é".repeat(25)
        val decoded = ShareCodec.decode(ShareCodec.encode(ThemeDocument.Default, name)) as DecodeResult.Ok
        assertEquals("é".repeat(24), decoded.projectName)
    }

    @Test
    fun shareCodec_mostAccentsAnExportCarries_roundTrip() {
        val accents = List(MAX_ACCENTS) { index -> Accent(name = "accent$index", seed = Argb(0x102030 * (index + 1))) }
        val document = ThemeDocument.Default.copy(accents = accents)
        assertEquals(DecodeResult.Ok(document, null), ShareCodec.decode(ShareCodec.encode(document)))
    }

    @Test
    fun encode_oneAccentMoreThanAnExportCarries_throws() {
        val accents = List(MAX_ACCENTS + 1) { index -> Accent(name = "accent$index", seed = Argb(0x336699)) }
        assertFailsWith<IllegalArgumentException> { ShareCodec.encode(ThemeDocument.Default.copy(accents = accents)) }
    }

    @Test
    fun shareCodec_longestAccentName_roundTrips() {
        listOf("a".repeat(MAX_ACCENT_NAME_BYTES), "é".repeat(MAX_ACCENT_NAME_BYTES / 2)).forEach { name ->
            val document = ThemeDocument.Default.copy(accents = listOf(Accent(name = name, seed = Argb(0x336699))))
            assertEquals(DecodeResult.Ok(document, null), ShareCodec.decode(ShareCodec.encode(document)), name)
        }
    }

    @Test
    fun encode_accentNameOneByteOverTheLimit_throws() {
        listOf("a".repeat(MAX_ACCENT_NAME_BYTES + 1), "a" + "é".repeat(MAX_ACCENT_NAME_BYTES / 2)).forEach { name ->
            val document = ThemeDocument.Default.copy(accents = listOf(Accent(name = name, seed = Argb(0x336699))))
            assertFailsWith<IllegalArgumentException>(name) { ShareCodec.encode(document) }
        }
    }

    @Test
    fun encode_differentDocuments_giveDifferentCodes() {
        val codes = DocumentArb(seed = 17).documents(count = 1_000).map { document -> ShareCodec.encode(document) }
        assertEquals(codes.size, codes.toSet().size)
        assertNotEquals(ShareCodec.encode(ThemeDocument.Default), ShareCodec.encode(ThemeDocument.Default, "Acme"))
    }

    private companion object {
        val PROJECT_NAMES: List<String?> = listOf(null, "Acme", "Café Olé", "日本語のプロジェクト", "x".repeat(48))
    }
}

internal fun ByteArray.unsigned(index: Int): Int = this[index].toInt() and 0xFF
