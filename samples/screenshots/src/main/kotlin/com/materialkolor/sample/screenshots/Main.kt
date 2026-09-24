package com.materialkolor.sample.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import com.materialkolor.sample.customtheme.ui.SampleApp
import com.materialkolor.sample.fluent.ui.FluentSampleApp
import com.materialkolor.sample.material3.ui.Material3SampleApp
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.state.SampleStore
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.unstyled.ui.UnstyledSampleApp
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Run it with `./gradlew screenshots`.
 */
fun main(args: Array<String>) {
    val outputDir = File(args[0]).apply { mkdirs() }
    val readmeDir = File(args[1]).apply { mkdirs() }

    Sample.entries.forEach { sample ->
        AppSection.entries.forEach { section ->
            listOf(ThemeMode.Light, ThemeMode.Dark).forEach { mode ->
                val state = SampleState.Initial.copy(section = section, mode = mode)
                val image = render { sample.content(SampleStore(state)) }
                val name = "${sample.slug}-${section.name.lowercase()}-${mode.name.lowercase()}.png"
                image.write(outputDir.resolve(name))

                if (section == README_SECTION && mode == README_MODE) {
                    image.write(readmeDir.resolve("${sample.slug}.png"))
                }
            }
        }
    }
}

private fun BufferedImage.write(file: File) {
    ImageIO.write(this, "png", file)
    println(file)
}

private enum class Sample(
    val slug: String,
    val content: @Composable (SampleStore) -> Unit,
) {
    CustomTheme(slug = "custom-theme", content = { store -> SampleApp(store) }),
    Fluent(slug = "fluent", content = { store -> FluentSampleApp(store) }),
    Material3(slug = "material3", content = { store -> Material3SampleApp(store) }),
    Unstyled(slug = "unstyled", content = { store -> UnstyledSampleApp(store) }),
}

private fun render(content: @Composable () -> Unit): BufferedImage {
    val page = ImageComposeScene(
        width = WIDTH_DP.px,
        height = MAX_HEIGHT_DP.px,
        density = Density(SCALE),
        content = content,
    ).use { scene ->
        repeat(SETTLE_FRAMES) { frame -> scene.render(frame * FRAME_NANOS) }
        scene.render(SETTLE_FRAMES * FRAME_NANOS).toComposeImageBitmap().toAwtImage()
    }

    val height = (page.lastContentRow() + PADDING_DP.px).coerceIn(HEIGHT_DP.px, page.height)
    return page.getSubimage(0, 0, page.width, height)
}

// The pages are rendered far taller than any of them get, so the bottom row is always bare background.
private fun BufferedImage.lastContentRow(): Int {
    val background = getRGB(0, height - 1)
    val row = IntArray(width)
    return (height - 1 downTo 0).firstOrNull { y ->
        getRGB(0, y, width, 1, row, 0, width)
        row.any { pixel -> pixel != background }
    } ?: 0
}

private val Float.px: Int
    get() = (this * SCALE).toInt()

// The one shot per sample that samples/README.md shows.
private val README_SECTION = AppSection.Tasks
private val README_MODE = ThemeMode.Light

// The width matches each sample's window, and the height is its minimum so short pages still look like the window.
private const val WIDTH_DP = 960f
private const val HEIGHT_DP = 760f
private const val MAX_HEIGHT_DP = 4000f
private const val PADDING_DP = 24f
private const val SCALE = 2f

// Long enough for every enter animation to finish before the capture.
private const val SETTLE_FRAMES = 120
private const val FRAME_NANOS = 16_666_667L
