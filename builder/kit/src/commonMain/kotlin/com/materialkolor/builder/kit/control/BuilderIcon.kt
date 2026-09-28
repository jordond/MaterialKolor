package com.materialkolor.builder.kit.control

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.icon.LocalBuilderIcons
import com.materialkolor.builder.kit.icon.glyphMotion
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The glyph the surrounding skin draws for [id].
 *
 * @param[id] Which icon.
 * @param[contentDescription] What the icon means to someone who cannot see it, or null when a
 * label beside it already says so.
 * @param[modifier] Applied to the icon.
 * @param[emphasis] Picks the ink from the skin's tokens when [tint] is left unspecified.
 * @param[tint] An explicit ink, for an icon standing on something other than a panel.
 * @param[size] The width and height, the skin's own icon size unless a caller needs another.
 */
@Composable
public fun BuilderIcon(
    id: IconId,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Primary,
    tint: Color = Color.Unspecified,
    size: Dp = LocalBuilderTokens.current.iconSize,
) {
    val ink = tint.takeOrElse { emphasis.ink(LocalBuilderTokens.current) }
    Image(
        imageVector = LocalBuilderIcons.current[id],
        contentDescription = contentDescription,
        // A progress glyph turns while motion is on.
        modifier = modifier.glyphMotion(id).size(size),
        colorFilter = ColorFilter.tint(ink),
    )
}
