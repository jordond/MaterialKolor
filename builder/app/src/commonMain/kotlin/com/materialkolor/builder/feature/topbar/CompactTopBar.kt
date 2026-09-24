package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.compact_mark
import com.materialkolor.builder.generated.resources.compact_mark_name
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.shell.TopBarRegion
import org.jetbrains.compose.resources.stringResource

// b-406

/**
 * The top bar on a phone, the mark and the project's name on the start edge and [actions] on the
 * end edge, Share, Export and the overflow menu. The library chips sit in a row of their own under
 * it, and the command palette, undo and redo live in the overflow.
 *
 * The name takes what the actions leave and ends in an ellipsis when that is not enough.
 *
 * @param[projectName] The open project's name.
 * @param[modifier] Applied to the bar.
 * @param[actions] The bar's buttons, laid in a row after the name.
 */
@Composable
internal fun CompactTopBar(
    projectName: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    val markName = stringResource(Res.string.compact_mark_name)
    TopBarRegion(modifier) {
        BuilderText(
            text = stringResource(Res.string.compact_mark),
            modifier = Modifier.clearAndSetSemantics { contentDescription = markName },
            style = BuilderTextStyle.Wordmark,
            maxLines = 1,
        )
        BuilderText(
            text = projectName,
            modifier = Modifier.weight(1f),
            style = BuilderTextStyle.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}
