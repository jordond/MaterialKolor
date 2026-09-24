package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_expand
import com.materialkolor.builder.generated.resources.poster_projects
import com.materialkolor.builder.generated.resources.poster_rail_seed
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * The poster shrunk to its 72 dp strip, the seed swatch, Shuffle, Projects and the button that
 * opens the poster again (F-66). The workspace remembers the choice in the browser's preferences.
 *
 * @param[focus] Where the projects drawer hands focus back once it closes (AR-09).
 */
@Composable
internal fun PosterRail(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    focus: PosterFocus? = null, // b-221f
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SeedSwatch(description = stringResource(Res.string.poster_rail_seed, context.document.seed.toHex()))
        ShuffleIconButton(context, dispatcher)
        PosterIconButton(
            icon = IconId.Folder,
            description = stringResource(Res.string.poster_projects),
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Projects)) },
            buttonModifier = triggerFocus(focus?.projects), // b-221f
        )
        PosterIconButton(
            icon = IconId.Expand,
            description = stringResource(Res.string.poster_expand),
            onClick = { dispatcher.dispatch(WorkspaceAction.SetPosterCollapsed(collapsed = false)) },
        )
    }
}

/**
 * The seed on the rail and in the sheet's seed row. The poster is already the seed, so the swatch
 * is the page ringed in ink, as big as the buttons beside it.
 *
 * @param[description] What it reads out, or null where the seed's hex already shows beside it.
 * @param[modifier] Applied to the swatch.
 */
@Composable
internal fun SeedSwatch(
    description: String?,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    Box(
        modifier = modifier
            .size(LocalLayout.current.primaryTouchTarget)
            .border(tokens.outlineWidth, tokens.borderStrong, RoundedCornerShape(tokens.radius.small))
            .then(if (description == null) Modifier else Modifier.semantics { contentDescription = description }),
    )
}
