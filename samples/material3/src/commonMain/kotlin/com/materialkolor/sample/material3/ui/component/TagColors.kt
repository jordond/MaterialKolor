package com.materialkolor.sample.material3.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.materialkolor.sample.shared.model.TaskTag

@Immutable
internal data class TagColors(
    val container: Color,
    val content: Color,
)

internal val TaskTag.colors: TagColors
    @Composable
    @ReadOnlyComposable
    get() {
        val scheme = MaterialTheme.colorScheme
        return when (this) {
            TaskTag.Personal -> TagColors(scheme.primaryContainer, scheme.onPrimaryContainer)
            TaskTag.Work -> TagColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
            TaskTag.Errand -> TagColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        }
    }
