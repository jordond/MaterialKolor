package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.kit.headless.HeadlessTooltip
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.material.MaterialTooltip
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A short label over [content], shown on hover and on keyboard focus alike, so nothing it says is
 * only there for a mouse. Focus a click leaves behind shows no label, the way it shows no
 * focus ring, and a press on [content] closes the label until the pointer moves off.
 *
 * A tooltip repeats what an icon means. It never holds the only copy of something, so an icon
 * button under it still needs its own content description. Material3 draws its plain tooltip and
 * Custom the headless one.
 *
 * @param[text] The label.
 * @param[modifier] Applied to the box around [content].
 * @param[content] The anchor, usually an icon button.
 */
@Composable
public fun BuilderTooltip(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    when (LocalSkin.current.library) {
        SkinLibrary.Material3 -> MaterialTooltip(text, modifier, content)
        SkinLibrary.Custom -> HeadlessTooltip(text, customOverlayStyle(tokens), modifier, content)
    }
}
