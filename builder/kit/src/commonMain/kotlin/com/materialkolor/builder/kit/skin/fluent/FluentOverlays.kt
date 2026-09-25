package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.token.BuilderTokens

// fluent-placeholder

/**
 * The overlays drawn the way Windows draws its flyouts, over the headless layer.
 *
 * Flyouts sit on the raised grey with a hairline and a soft shadow, and tooltips and toasts stay on
 * the same grey rather than inverting. B-403 swaps these for the real Fluent components.
 */
internal fun fluentOverlayStyle(tokens: BuilderTokens): OverlayStyle {
    val hairline = BorderStroke(tokens.outlineWidth, tokens.border)
    return OverlayStyle(
        surface = tokens.panelRaised,
        content = tokens.textStrong,
        muted = tokens.textMuted,
        border = hairline,
        popoverShape = RoundedCornerShape(tokens.radius.small),
        dialogShape = RoundedCornerShape(tokens.radius.small),
        panelRadius = tokens.radius.small,
        drawerRadius = tokens.radius.small, // b-511
        divider = tokens.border,
        shadow = 8.dp,
        scrim = tokens.scrim,
        itemShape = RoundedCornerShape(FluentItemRadius),
        highlight = tokens.textStrong.copy(alpha = 0.06f),
        selected = tokens.textStrong.copy(alpha = 0.09f),
        focus = tokens.focus,
        field = tokens.panel,
        fieldBorder = BorderStroke(tokens.outlineWidth, tokens.border),
        tooltip = tokens.panelRaised,
        tooltipContent = tokens.textStrong,
        tooltipBorder = hairline,
        toast = tokens.panelRaised,
        toastContent = tokens.textStrong,
        toastBorder = hairline,
        thumb = tokens.textMuted,
    )
}

/**
 * Windows rounds list items and buttons tighter than its flyouts.
 */
private val FluentItemRadius = 4.dp
