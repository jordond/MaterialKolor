package com.materialkolor.builder.kit.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentCard
import com.materialkolor.builder.kit.skin.headless.CardStyle
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.material.MaterialCard

/**
 * A framed group of content on a panel, such as one export target.
 *
 * With [onClick] the whole card is one button whose name is everything written inside it. Without
 * it the card only groups its content for a screen reader.
 *
 * @param[modifier] Applied to the card.
 * @param[onClick] Called when the card is pressed, or null for a card that only holds content.
 * @param[enabled] Whether a pressable card can be pressed.
 * @param[content] What the card holds, laid out in a column.
 */
@Composable
public fun BuilderCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialCard(modifier, onClick, enabled, content)
        Library.Unstyled -> HeadlessCard(UnstyledActionStyles.card, modifier, onClick, enabled, content)
        Library.Fluent -> FluentCard(modifier, onClick, enabled, content)
        Library.Custom -> HeadlessCard(CustomActionStyles.card, modifier, onClick, enabled, content)
    }
}

/**
 * A card drawn from [style].
 */
@Composable
internal fun HeadlessCard(
    style: CardStyle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressable = if (onClick == null) {
        Modifier.semantics { isTraversalGroup = true }
    } else {
        Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).controlPress(interactionSource)
            .alpha(enabledAlpha(enabled))
            .controlRing(interactionSource, style.shape)
    }
    Box(
        modifier = modifier
            .then(pressable)
            .actionSurface(style.colors, style.shape, style.borderWidth),
        propagateMinConstraints = true,
    ) {
        Column(
            modifier = Modifier.padding(style.padding),
            verticalArrangement = Arrangement.spacedBy(style.gap),
            content = content,
        )
        if (onClick != null) CardDisabledNote(enabled)
    }
}

/**
 * On the web, the disabled word as the last text of a pressable card while it is disabled.
 *
 * A card takes its name from the text inside it, and a content description would replace that
 * name, so the note joins it as text instead. It draws nothing and sits outside the card's own
 * column, so nothing on screen moves.
 *
 * The note stays in place, empty, while the card is enabled. The web mirror only writes a card's
 * text when it has some, so a card with no text of its own would keep reading disabled if the note
 * left.
 */
@Composable
internal fun CardDisabledNote(enabled: Boolean) {
    if (!LocalFoldsStateIntoName.current) return
    val note = if (enabled) "" else stateWords().disabledAfterName
    Box(Modifier.semantics { text = AnnotatedString(note) })
}
