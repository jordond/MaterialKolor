package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentBadge
import com.materialkolor.builder.kit.skin.headless.BadgeStyle
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.material.MaterialBadge

/** What a badge reports, which picks its colour in every skin. */
public enum class BadgeStatus {
    /** A plain fact, such as a count or a library name. */
    Neutral,

    /** Something worth a glance, such as a new export target. */
    Info,

    /** Something that passed, such as a contrast pair above its target. */
    Success,

    /** Something that only just passed or was downgraded. */
    Warning,

    /** Something that failed. */
    Danger,
}

/**
 * A short read only label that reports a status, such as AA beside a contrast pair.
 *
 * The colour always comes with [label] and an optional [icon], so the status never rests on colour
 * alone (AR-03). The label and the icon read out as one.
 *
 * @param[label] The status in words. A blank label draws nothing, since the colour must never stand
 * alone.
 * @param[modifier] Applied to the badge.
 * @param[status] What the badge reports.
 * @param[icon] A glyph before the label.
 */
@Composable
public fun BuilderBadge(
    label: String,
    modifier: Modifier = Modifier,
    status: BadgeStatus = BadgeStatus.Neutral,
    icon: IconId? = null,
) {
    // Strings from resources read as blank for the first frame on the web while they load, so a
    // blank label draws nothing rather than failing the frame.
    if (label.isBlank()) return
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialBadge(label, modifier, status, icon)
        Library.Unstyled -> HeadlessBadge(label, UnstyledActionStyles.badge, modifier, status, icon)
        Library.Fluent -> FluentBadge(label, modifier, status, icon)
        Library.Custom -> HeadlessBadge(label, CustomActionStyles.badge, modifier, status, icon)
    }
}

/** A badge drawn from [style]. */
@Composable
internal fun HeadlessBadge(
    label: String,
    style: BadgeStyle,
    modifier: Modifier = Modifier,
    status: BadgeStatus = BadgeStatus.Neutral,
    icon: IconId? = null,
) {
    val colors = style.colors(status)
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .actionSurface(colors, style.shape, style.borderWidth)
            .heightIn(min = style.height)
            .padding(horizontal = style.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(style.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) BuilderIcon(icon, contentDescription = null, tint = colors.content)
        BuilderText(label, style = BuilderTextStyle.Value, color = colors.content, maxLines = 1)
    }
}
