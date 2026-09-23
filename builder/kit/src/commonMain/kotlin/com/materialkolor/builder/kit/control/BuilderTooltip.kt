package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessTooltip
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.fluentOverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.headless.unstyledOverlayStyle
import com.materialkolor.builder.kit.skin.material.MaterialTooltip
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A short label over [content], shown on hover and on keyboard focus alike, so nothing it says is
 * only there for a mouse (AR-03).
 *
 * A tooltip repeats what an icon means. It never holds the only copy of something, so an icon
 * button under it still needs its own content description. Material3 draws its plain tooltip, the
 * other skins the headless one.
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
        Library.Material3 -> MaterialTooltip(text, modifier, content)
        Library.Unstyled -> HeadlessTooltip(text, unstyledOverlayStyle(tokens), modifier, content)
        // fluent-placeholder
        Library.Fluent -> HeadlessTooltip(text, fluentOverlayStyle(tokens), modifier, content)
        Library.Custom -> HeadlessTooltip(text, customOverlayStyle(tokens), modifier, content)
    }
}
