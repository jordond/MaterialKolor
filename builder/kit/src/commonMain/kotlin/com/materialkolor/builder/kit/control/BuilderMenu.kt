package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.headless.HeadlessMenu
import com.materialkolor.builder.kit.headless.overlayLibrary
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.fluent.fluentOverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.headless.unstyledOverlayStyle
import com.materialkolor.builder.kit.skin.material.MaterialMenu
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * One command in a [BuilderMenu].
 *
 * @property[label] What the row says.
 * @property[onClick] What choosing the row does. The menu closes first.
 * @property[icon] A glyph before the label, or null for none.
 * @property[emphasis] [Emphasis.Danger] marks a row that destroys something.
 * @property[enabled] False to show the row without letting anyone choose it.
 * @property[selected] Whether the row is the current one of a set, such as the chosen appearance,
 * or null for a plain command. A row that knows carries a check while it is the current one and
 * reads out as selected or not.
 */
public class BuilderMenuItem(
    public val label: String,
    public val onClick: () -> Unit,
    public val icon: IconId? = null,
    public val emphasis: Emphasis = Emphasis.Primary,
    public val enabled: Boolean = true,
    public val selected: Boolean? = null,
)

/**
 * A list of commands that opens under [anchor].
 *
 * Esc and a click outside close it, focus starts on the first row, and each row reads as a button,
 * or as an option that is selected or not when its item says so. Material3 draws its own
 * `DropdownMenu`, the other skins the headless dropdown.
 *
 * @param[expanded] Whether the menu is open.
 * @param[onDismissRequest] Called when the menu asks to close, including after a row is chosen.
 * @param[items] The commands, top to bottom.
 * @param[modifier] Applied to the box holding [anchor].
 * @param[anchor] What the menu opens from, usually an icon button that sets [expanded].
 */
@Composable
public fun BuilderMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<BuilderMenuItem>,
    modifier: Modifier = Modifier,
    anchor: @Composable () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    // b-221b
    when (overlayLibrary()) {
        Library.Material3 -> MaterialMenu(expanded, onDismissRequest, items, modifier, anchor)
        Library.Unstyled -> HeadlessMenu(
            expanded,
            onDismissRequest,
            items,
            unstyledOverlayStyle(tokens),
            modifier,
            anchor,
        )
        // fluent-placeholder
        Library.Fluent -> HeadlessMenu(expanded, onDismissRequest, items, fluentOverlayStyle(tokens), modifier, anchor)
        Library.Custom -> HeadlessMenu(expanded, onDismissRequest, items, customOverlayStyle(tokens), modifier, anchor)
    }
}
