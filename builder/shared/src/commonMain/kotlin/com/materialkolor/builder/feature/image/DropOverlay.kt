package com.materialkolor.builder.feature.image

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.image_drop
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * What the window shows while files are dragged over it, a veil with a framed line saying a drop
 * pulls colors from the image.
 *
 * It only shows. The drop itself arrives through the platform, so nothing here takes a pointer
 * event or the focus.
 */
@Composable
internal fun DropOverlay(modifier: Modifier = Modifier) {
    val tokens = LocalBuilderTokens.current
    val frame = RoundedCornerShape(tokens.radius.large)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.scrim)
            .padding(tokens.spacing.section)
            .border(tokens.highlightWidth, tokens.accent, frame),
        contentAlignment = Alignment.Center,
    ) {
        BuilderText(
            text = stringResource(Res.string.image_drop),
            style = BuilderTextStyle.Title,
            modifier = Modifier
                .background(tokens.panelRaised, RoundedCornerShape(tokens.radius.medium))
                .padding(tokens.spacing.large),
        )
    }
}
