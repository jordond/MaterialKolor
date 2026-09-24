package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.PreviewResult
import com.materialkolor.builder.preview.split.SplitState

/** Tags the layout Inspect wraps, the whole preview. */
internal const val PREVIEW_TAG = "preview"

/** Two roles for a test element to declare, one drawn on the other. */
internal val PrimaryPair: Array<ColorRef> = arrayOf(ColorRef.OfRole(Role.Primary), ColorRef.OfRole(Role.OnPrimary))

/** Anything drawn on the Inspect card. */
internal val OnCard: SemanticsMatcher = hasAnyAncestor(hasTestTag(INSPECT_CARD_TAG))

/** Inspect on over [content] in a 600 by 400 dp preview, in the Material chrome. */
@Composable
internal fun Inspecting(
    shown: PreviewMode,
    split: SplitState,
    onLeave: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val actions = remember {
        InspectActions(
            pinEnabled = true,
            onPin = { _, _, _ -> },
            onShowOnRamp = { _, _ -> },
            onJumpToKeyColor = { _ -> },
            onLeave = onLeave,
        )
    }
    Chrome {
        ProvideBuilderLayout(modifier = Modifier.size(600.dp, 400.dp)) {
            InspectOverlay(
                on = true,
                result = PreviewResult,
                shown = shown,
                split = split,
                actions = actions,
                modifier = Modifier.fillMaxSize().testTag(PREVIEW_TAG),
                content = content,
            )
        }
    }
}
