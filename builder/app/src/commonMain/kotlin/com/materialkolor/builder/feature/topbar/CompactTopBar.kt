package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.shell.TopBarRegion
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The top bar on a phone, the project's swatch and name on the start edge and [actions] on the end
 * edge, Share, Export and the overflow menu. The library chips sit in a row of their own under it,
 * and the command palette, undo and redo live in the overflow.
 *
 * The name takes what the actions leave and ends in an ellipsis when that is not enough.
 *
 * @param[projectName] The open project's name.
 * @param[seed] The project's seed, which the swatch before the name shows.
 * @param[modifier] Applied to the bar.
 * @param[actions] The bar's buttons, laid in a row after the name.
 */
@Composable
internal fun CompactTopBar(
    projectName: String,
    seed: Argb,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    TopBarRegion(modifier) {
        // The name already says whose swatch it is, so the swatch is only drawn.
        Box(
            Modifier
                .size(tokens.iconSize + tokens.spacing.extraSmall)
                .background(seed.toColor(), CircleShape)
                .border(tokens.outlineWidth, tokens.borderStrong, CircleShape),
        )
        BuilderText(
            text = projectName,
            modifier = Modifier.weight(1f),
            style = BuilderTextStyle.Title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}
