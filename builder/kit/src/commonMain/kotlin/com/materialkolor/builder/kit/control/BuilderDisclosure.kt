package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessDisclosure
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentDisclosure
import com.materialkolor.builder.kit.skin.headless.CustomInputStyles
import com.materialkolor.builder.kit.skin.headless.UnstyledInputStyles
import com.materialkolor.builder.kit.skin.material.MaterialDisclosure

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
    content: @Composable () -> Unit,
) {
    when (LocalSkin.current.library) {
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
