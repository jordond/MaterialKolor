package com.materialkolor.builder.feature.about

import com.materialkolor.builder.domain.link.DecodeResult
import com.materialkolor.builder.domain.link.SHARE_URL_PREFIX
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.link.shareLink
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

// b-314
class ReportLinkTest {
    @Test
    fun reportUrl_anyDetails_opensANewIssueWithTheTitleLeftEmpty() {
        val url = reportUrl(ISSUES_URL, details(browser = "Chrome"))

        url shouldStartWith "https://github.com/jordond/materialkolor/issues/new?title=&body="
    }

    @Test
    fun reportUrl_reservedCharacters_arriveEncodedAndDecodeBackToTheDetails() {
        val details = details(browser = "A&B=C#D+E/F?G H%")

        val body = reportUrl(ISSUES_URL, details).substringAfter("&body=")

        body shouldContain "A%26B%3DC%23D%2BE%2FF%3FG%20H%25"
        body shouldNotContain "&"
        body shouldNotContain "#"
        body shouldNotContain " "
        percentDecode(body) shouldBe details.text()
    }

    @Test
    fun reportUrl_nonAsciiText_goesOutAsUtf8Bytes() {
        val details = details(browser = "Café 日本 🦊")

        val body = reportUrl(ISSUES_URL, details).substringAfter("&body=")

        body shouldContain "Caf%C3%A9%20%E6%97%A5%E6%9C%AC%20%F0%9F%A6%8A"
        percentDecode(body) shouldBe details.text()
    }

    @Test
    fun reportUrl_unreservedCharacters_stayAsTheyAre() {
        percentEncode("AZaz09-._~") shouldBe "AZaz09-._~"
    }

    @Test
    fun text_anUnlinkableTheme_saysItIsTooBigToLink() {
        val details = details(themeLink = null)

        details.text() shouldContain "Theme: too big to link"
        reportUrl(ISSUES_URL, details) shouldContain "Theme%3A%20too%20big%20to%20link"
    }

    @Test
    fun text_everyDetail_oneLineEach() {
        val link = shareLink(ThemeDocument.Default, projectName = "")

        val lines = details(browser = "Firefox", themeLink = link).text().lines()

        lines shouldBe listOf("Builder: 2.0.0", "MaterialKolor: 6.0.0", "Browser: Firefox", "Theme: $link")
    }

    @Test
    fun shareLink_withoutAProjectName_carriesNoName() {
        val link = requireNotNull(shareLink(ThemeDocument.Default, projectName = "")) { "The default fits a link" }

        val decoded = ShareCodec.decode(link.removePrefix(SHARE_URL_PREFIX))

        decoded.shouldBeInstanceOf<DecodeResult.Ok>().projectName.orEmpty() shouldBe ""
    }

    private fun details(
        browser: String = "Firefox",
        themeLink: String? = "https://materialkolor.com/t/abc",
    ): ReportDetails = ReportDetails("2.0.0", "6.0.0", browser, themeLink)

    /**
     * Undoes [percentEncode], to check nothing was lost on the way.
     */
    private fun percentDecode(text: String): String {
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index < text.length) {
            if (text[index] == '%') {
                bytes += text.substring(index + 1, index + 3).toInt(radix = 16).toByte()
                index += 3
            } else {
                bytes += text[index].code.toByte()
                index++
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
