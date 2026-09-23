package com.materialkolor.builder.kit.icon

import androidx.compose.ui.graphics.vector.ImageVector

// fluent-placeholder

/**
 * The Fluent set.
 *
 * Fluent is not a kit dependency yet, so this draws the Lucide glyphs until B-403 brings the
 * Fluent System Icons in.
 */
public object FluentIcons : BuilderIcons {
    override fun get(id: IconId): ImageVector = LucideIcons[id]
}
