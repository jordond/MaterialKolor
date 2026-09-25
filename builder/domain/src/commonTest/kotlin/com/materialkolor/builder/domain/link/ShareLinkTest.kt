package com.materialkolor.builder.domain.link

import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.validate.MAX_ACCENTS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShareLinkTest {
    @Test
    fun shareLink_aDocumentThatFits_carriesItAndTheProjectName() {
        val document = ThemeDocument(seed = SEED, accents = accents(MAX_ACCENTS))

        val link = shareLink(document, "Harbour")

        assertEquals(SHARE_URL_PREFIX + ShareCodec.encode(document, "Harbour"), link)
        assertEquals(DecodeResult.Ok(document.copy(seedSource = SeedSource.Typed), "Harbour"), decode(link))
    }

    @Test
    fun shareLink_fluentWithTooManyAccents_linksToWhatFluentSeesUnderTheSameName() {
        val document = ThemeDocument(seed = SEED, library = Library.Fluent, accents = accents(MAX_ACCENTS + 1))

        val link = shareLink(document, "Harbour")

        val expected = document.forTarget(ExportTarget.Fluent).copy(seedSource = SeedSource.Typed)
        assertEquals(emptyList(), expected.accents)
        assertEquals(DecodeResult.Ok(expected, "Harbour"), decode(link))
    }

    @Test
    fun shareLink_tooManyAccentsForItsTargetToo_isNull() {
        val document = ThemeDocument(seed = SEED, library = Library.Material3, accents = accents(MAX_ACCENTS + 1))

        assertNull(shareLink(document, "Harbour"))
    }

    @Test
    fun shareLink_anotherOrigin_opensThere() {
        val document = ThemeDocument(seed = SEED)

        val link = shareLink(document, "Harbour", origin = "https://staging.materialkolor.com")

        assertEquals("https://staging.materialkolor.com/t/" + ShareCodec.encode(document, "Harbour"), link)
    }

    private fun decode(link: String?): DecodeResult {
        val url = requireNotNull(link) { "Expected a link" }
        return ShareCodec.decode(url.removePrefix(SHARE_URL_PREFIX))
    }
}

private val SEED = Argb(0xFF6750A4.toInt())

private fun accents(count: Int): List<Accent> =
    List(count) { index -> Accent(name = "accent$index", seed = Argb(0xFF102030.toInt() + index)) }
