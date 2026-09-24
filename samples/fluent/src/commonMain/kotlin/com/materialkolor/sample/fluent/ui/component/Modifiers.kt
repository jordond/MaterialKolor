package com.materialkolor.sample.fluent.ui.component

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

/**
 * Gives a Fluent button one accessible node with a role, a label and a click.
 *
 * Fluent puts the click on a row inside the button that the `modifier` parameter never reaches, and leaves out the
 * role. So the node the modifier does reach takes over. It speaks for everything inside it and offers the same
 * click, and pointer input still lands on Fluent's own row underneath.
 *
 * @param[tag] The test tag.
 * @param[label] What the button says, read as its text or, for an icon button, as its content description.
 * @param[enabled] Whether the button can be pressed.
 * @param[iconOnly] Whether the button shows only an icon.
 * @param[action] What pressing the button does.
 */
internal fun Modifier.buttonSemantics(
    tag: String,
    label: String,
    enabled: Boolean,
    iconOnly: Boolean,
    action: () -> Unit,
): Modifier =
    clearAndSetSemantics {
        testTag = tag
        role = Role.Button
        if (iconOnly) contentDescription = label else text = AnnotatedString(label)
        onClick {
            if (enabled) action()
            enabled
        }
        if (!enabled) disabled()
    }

/**
 * Draws Fluent's focus stroke while [visible] is true.
 *
 * Fluent has the focus stroke colors but draws no focus visual of its own, so keyboard focus would be invisible
 * without this.
 *
 * @param[visible] Whether the component holds keyboard focus.
 * @param[color] The stroke color, usually `FluentTheme.colors.stroke.focus.outer`.
 * @param[shape] The outline of the component.
 */
internal fun Modifier.focusStroke(
    visible: Boolean,
    color: Color,
    shape: Shape,
): Modifier = if (visible) border(width = 2.dp, color = color, shape = shape) else this
