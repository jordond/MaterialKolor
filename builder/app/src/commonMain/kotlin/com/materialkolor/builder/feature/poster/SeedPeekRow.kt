package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher

/**
 * The seed as the sheet leads with it, the swatch, the hex and the seed's name, with Shuffle at the
 * end. It is what a phone sees of the poster at rest, even on its side (D38).
 *
 * The hex here only reads. Editing it is the hero's job further down the sheet.
 */
@Composable
internal fun SeedPeekRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val seed = context.document.seed
    val spacing = LocalBuilderTokens.current.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeedSwatch(description = null)
        Column(Modifier.weight(1f)) {
            BuilderText(text = seed.toHex(), style = BuilderTextStyle.Title, maxLines = 1)
            BuilderText(
                text = remember(seed) { ColorNames.nameOf(seed) },
                style = BuilderTextStyle.Label,
                emphasis = Emphasis.Secondary,
                maxLines = 1,
            )
        }
        ShuffleIconButton(context, dispatcher, emphasis = Emphasis.Primary)
    }
}
