package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.DisclosureStyle
import com.materialkolor.builder.kit.headless.HeadlessDisclosure
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentDisclosure
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialDisclosure
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A row that opens the content under it, such as the poster's fine-tune rows.
 *
 * The row is a button with expand and collapse actions and reads out "Expanded" or "Collapsed".
 *
 * @param[expanded] Whether the content shows.
 * @param[onExpandedChange] Called with the new state when someone opens or closes it.
 * @param[title] What the row opens, shown on it and read out as its name.
 * @param[modifier] Applied to the row and its content together.
 * @param[summary] A line under the title, for what is inside.
 * @param[enabled] Whether it takes input.
 * @param[flush] Sit flush in the column around it, the way the poster's fine tune rows do. The title
 * starts at the column's edge under a hairline, the row draws no box open or closed, and what opens
 * starts a gap below it. Fluent keeps its expander, since its card is how Fluent says this.
 * @param[content] What opens.
 */
@Composable
public fun BuilderDisclosure(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    flush: Boolean = false, // b-510
    content: @Composable () -> Unit,
) {
    val library = LocalSkin.current.library
    // b-510
    if (flush && library != Library.Fluent) {
        HeadlessDisclosure(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            title = title,
            style = flushDisclosureStyle(),
            modifier = modifier,
            summary = summary,
            enabled = enabled,
            content = content,
        )
        return
    }
    when (library) {
        Library.Material3 -> {
            MaterialDisclosure(expanded, onExpandedChange, title, modifier, summary, enabled, content)
        }
        Library.Unstyled -> {
            HeadlessDisclosure(
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                title = title,
                style = UnstyledInputStyles.disclosure,
                modifier = modifier,
                summary = summary,
                enabled = enabled,
                content = content,
            )
        }
        Library.Fluent -> {
            FluentDisclosure(expanded, onExpandedChange, title, modifier, summary, enabled, content)
        }
        Library.Custom -> {
            HeadlessDisclosure(
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                title = title,
                style = CustomInputStyles.disclosure,
                modifier = modifier,
                summary = summary,
                enabled = enabled,
                content = content,
            )
        }
    }
}

// b-510

/** A disclosure with no box of its own, a hairline over it and its title at the column's edge. */
@Composable
private fun flushDisclosureStyle(): DisclosureStyle {
    val tokens = LocalBuilderTokens.current
    return DisclosureStyle(
        container = Color.Transparent,
        shape = RoundedCornerShape(tokens.radius.small),
        outline = Color.Transparent,
        outlineWidth = 0.dp,
        headerPadding = PaddingValues(vertical = tokens.spacing.medium),
        contentPadding = PaddingValues(bottom = tokens.spacing.medium),
        focus = tokens.focus,
        rule = tokens.border,
        ruleWidth = tokens.outlineWidth,
        title = BuilderTextStyle.Title,
        summary = BuilderTextStyle.Label,
    )
}
