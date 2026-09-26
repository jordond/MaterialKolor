package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.Selawik_Regular
import com.materialkolor.builder.kit.generated.resources.Selawik_Semibold
import io.github.composefluent.FluentTheme
import io.github.composefluent.Typography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

/**
 * The two Selawik weights Fluent's type scale sets, Regular and SemiBold.
 */
private val SelawikFaces: List<FontResource> = listOf(Res.font.Selawik_Regular, Res.font.Selawik_Semibold)

/**
 * Selawik, Microsoft's open stand in for Segoe UI, in the weights Fluent's type scale uses.
 *
 * Only Fluent asks for it, so the site fetches it the first time Fluent shows and never before. The
 * builder's own type stays Bricolage and JetBrains Mono in Fluent as well. Selawik sets the text
 * Fluent's components draw themselves. The subset is renamed inside, since Selawik reserves its
 * name, see `builder/tools/fonts/subset.sh`.
 */
@Composable
internal fun fluentFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.Selawik_Regular, FontWeight.Normal),
        Font(Res.font.Selawik_Semibold, FontWeight.SemiBold),
    )

/**
 * Fluent's own type scale set in Selawik.
 */
@Composable
internal fun rememberFluentTypography(): Typography {
    val scale = FluentTheme.typography
    val face = fluentFontFamily()
    return remember(face, scale) { scale.inFace(face) }
}

/**
 * This scale with every style set in [face], sizes, weights and line heights kept.
 */
internal fun Typography.inFace(face: FontFamily): Typography {
    fun TextStyle.inFace(): TextStyle = copy(fontFamily = face)
    return Typography(
        caption = caption.inFace(),
        body = body.inFace(),
        bodyStrong = bodyStrong.inFace(),
        bodyLarge = bodyLarge.inFace(),
        subtitle = subtitle.inFace(),
        title = title.inFace(),
        titleLarge = titleLarge.inFace(),
        display = display.inFace(),
    )
}

private val faceFetch = Mutex()
private var faceFetched = false

/**
 * Fetches Selawik ahead of a switch to Fluent, so the new frame comes in set in its own face.
 *
 * Hand it to `SkinTransition.reveal` as `awaitBeforeReveal` when a switch lands on Fluent. The
 * reveal waits up to 300 ms for it and goes on without it after that. On the web it fetches both
 * weights into the browser's resource cache, where the Fluent skin then finds them. On the desktop
 * the files are already local and it returns straight away. It fetches once per session. A fetch
 * that fails is left for the next switch to try again, and the skin draws in the fallback face
 * until then.
 */
public suspend fun preloadFluentFace() {
    faceFetch.withLock {
        if (faceFetched) return
        try {
            val environment = getSystemResourceEnvironment()
            for (face in SelawikFaces) getFontResourceBytes(environment, face)
            faceFetched = true
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // The next switch to Fluent tries again.
        }
    }
}
