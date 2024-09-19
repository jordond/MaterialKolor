package quantize

import com.materialkolor.quantize.QuantizerCelebi
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class QuantizerCelebiTest {

    private val red = 0xFFFF0000.toInt()
    private val green = 0xFF00FF00.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val random = 0xFF426088.toInt()
    private val maxColors = 256

    @Test
    fun testSingleColor() {
        val result = QuantizerCelebi.quantize(intArrayOf(red), maxColors)
        val colors = result.keys.toList()

        colors.size shouldBe 1
        colors[0] shouldBe red
    }

    @Test
    fun testMultipleColors() {
        val result = QuantizerCelebi.quantize(intArrayOf(red, green, blue), maxColors)
        val colors = result.keys.toSet()

        colors.size shouldBe 3
        colors shouldBe setOf(red, green, blue)
    }

    @Test
    fun testRandomColor() {
        val result = QuantizerCelebi.quantize(intArrayOf(random), maxColors)
        val colors = result.keys.toList()

        colors.size shouldBe 1
        colors[0] shouldBe random
    }

    @Test
    fun testWhiteColor() {
        val result = QuantizerCelebi.quantize(intArrayOf(white), maxColors)
        val colors = result.keys.toList()

        colors.size shouldBe 1
        colors[0] shouldBe white
    }

    @Test
    fun testStability() {
        val imagePixels = intArrayOf(
            0xff050505.toInt(), 0xff000000.toInt(), 0xff000000.toInt(), 0xff000000.toInt(), 0xff000000.toInt(),
            0xff090909.toInt(), 0xff060404.toInt(), 0xff030102.toInt(), 0xff080607.toInt(), 0xff070506.toInt(),
            0xff010001.toInt(), 0xff070506.toInt(), 0xff364341.toInt(), 0xff223529.toInt(), 0xff14251c.toInt(),
            0xff11221a.toInt(), 0xff1f3020.toInt(), 0xff34443a.toInt(), 0xff64817e.toInt(), 0xff638777.toInt(),
            0xff486d58.toInt(), 0xff2f5536.toInt(), 0xff467258.toInt(), 0xff7fb7b9.toInt(), 0xff6d8473.toInt(),
            0xff859488.toInt(), 0xff7a947e.toInt(), 0xff5f815d.toInt(), 0xff3a5d46.toInt(), 0xff497469.toInt(),
            0xff737a73.toInt(), 0xff656453.toInt(), 0xff445938.toInt(), 0xff657c4b.toInt(), 0xff65715b.toInt(),
            0xff6a816e.toInt(), 0xff667366.toInt(), 0xff5b5547.toInt(), 0xff3b391e.toInt(), 0xff705e3d.toInt(),
            0xff7f6c5e.toInt(), 0xff6d7c6c.toInt(), 0xffa99c9c.toInt(), 0xff8b7671.toInt(), 0xff6a3229.toInt(),
            0xff80514b.toInt(), 0xff857970.toInt(), 0xff4f5a4c.toInt(), 0xff897273.toInt(), 0xff745451.toInt(),
            0xff512823.toInt(), 0xff78585a.toInt(), 0xff535552.toInt(), 0xff40493f.toInt(), 0xff151616.toInt(),
            0xff0a0c0c.toInt(), 0xff050808.toInt(), 0xff010303.toInt(), 0xff000100.toInt(), 0xff010200.toInt(),
            0xff191816.toInt(), 0xff181818.toInt(), 0xff0c0c0c.toInt(), 0xff040404.toInt(), 0xff0c0c0c.toInt(),
            0xff151514.toInt(), 0xffb1c3b9.toInt(), 0xffbfbfbf.toInt(), 0xffbababa.toInt(), 0xffb7b7b7.toInt(),
            0xffb3b3b3.toInt(), 0xffadadad.toInt(), 0xff535756.toInt(), 0xff575656.toInt(), 0xff555555.toInt(),
            0xff555555.toInt(), 0xff545454.toInt(), 0xff474646.toInt(), 0xff000000.toInt(), 0xff000000.toInt(),
            0xff0b0b0b.toInt(), 0xff0b0b0b.toInt(), 0xff000000.toInt(), 0xff000000.toInt()
        )

        val count = 16

        val result1 = QuantizerCelebi.quantize(imagePixels, count).toList()
        val result2 = QuantizerCelebi.quantize(imagePixels, count).toList()

        result1 shouldBe result2
    }
}