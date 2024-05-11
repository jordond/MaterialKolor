package dislike;

import com.materialkolor.dislike.DislikeAnalyzer
import com.materialkolor.hct.Hct
import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.Test;
import utils.shouldBeExactly

class DislikeAnalyzerTest {

    @Test
    fun isDislikedFalse() {
        val expectedHct = hct.Hct.fromInt(0xB44C43)
        val expected = dislike.DislikeAnalyzer.isDisliked(expectedHct)

        val actualHct = Hct.fromInt(0xB44C43)
        val actual = DislikeAnalyzer.isDisliked(actualHct)

        expected shouldBeEqual actual
        expected shouldBeEqual false
    }

    @Test
    fun isDislikedTrue() {
        val expectedHct = hct.Hct.fromInt(0x9D9101)
        val expected = dislike.DislikeAnalyzer.isDisliked(expectedHct)

        val actualHct = Hct.fromInt(0x9D9101)
        val actual = DislikeAnalyzer.isDisliked(actualHct)

        expected shouldBeEqual actual
        expected shouldBeEqual true
    }

    @Test
    fun fixIfDisliked() {
        val expectedHct = hct.Hct.fromInt(0xB44C43)
        val expected = dislike.DislikeAnalyzer.fixIfDisliked(expectedHct)

        val actualHct = Hct.fromInt(0xB44C43)
        val actual = DislikeAnalyzer.fixIfDisliked(actualHct)

        expected shouldBeExactly actual
    }

    @Test
    fun fixIfDislikedFixed() {
        val expectedHct = hct.Hct.fromInt(0x9D9101)
        val expected = dislike.DislikeAnalyzer.fixIfDisliked(expectedHct)

        val actualHct = Hct.fromInt(0x9D9101)
        val actual = DislikeAnalyzer.fixIfDisliked(actualHct)

        expected shouldBeExactly actual
    }
}