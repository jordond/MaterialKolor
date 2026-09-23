package com.materialkolor.builder.kit.skin.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.headless.DisclosureChevron
import com.materialkolor.builder.kit.headless.SliderKeyPress
import com.materialkolor.builder.kit.headless.SliderRules
import com.materialkolor.builder.kit.headless.checkboxStateDescription
import com.materialkolor.builder.kit.headless.disclosureEnter
import com.materialkolor.builder.kit.headless.disclosureExit
import com.materialkolor.builder.kit.headless.disclosureStateDescription
import com.materialkolor.builder.kit.headless.sliderKeys
import com.materialkolor.builder.kit.headless.sliderSemantics
import com.materialkolor.builder.kit.headless.switchStateDescription
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.FieldStyle
import com.materialkolor.builder.kit.skin.headless.heroFieldStyle
import com.materialkolor.builder.kit.skin.headless.inputAlpha
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A Material3 switch with its label. The row is the target and carries the switch role, so the
 * switch itself takes no clicks.
 */
@Composable
internal fun MaterialSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { stateDescription = switchStateDescription(checked, enabled) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(
            text = label,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = tokens.spacing.medium)
                .alpha(inputAlpha(enabled)),
            style = BuilderTextStyle.Label,
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** A Material3 checkbox with its label, the row being the target. */
@Composable
internal fun MaterialCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .semantics { stateDescription = checkboxStateDescription(checked, enabled) },
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        BuilderText(
            text = label,
            modifier = Modifier.alpha(inputAlpha(enabled)),
            style = BuilderTextStyle.Label,
        )
    }
}

/**
 * The Material3 slider, with the builder's keys and snapping laid over it. Material draws no named
 * stops, so they only show through the snap.
 */
@Composable
internal fun MaterialSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    rules: SliderRules,
    label: String,
    stateDescription: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val press = remember { SliderKeyPress() }
    Slider(
        value = value,
        onValueChange = { raw -> onValueChange(rules.snap(raw)) },
        modifier = modifier
            .sliderKeys(value, rules, enabled, isRtl, press, onValueChange, onValueChangeFinished)
            .sliderSemantics(label, stateDescription, value, rules, enabled, onValueChange, onValueChangeFinished),
        enabled = enabled,
        valueRange = rules.range,
        onValueChangeFinished = onValueChangeFinished,
    )
}

/** The Material3 outlined text field over a builder field draft. */
@Composable
internal fun MaterialField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    message: String?,
    isError: Boolean,
    textStyle: TextStyle,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.semantics { if (isError && message != null) error(message) },
        enabled = enabled,
        textStyle = textStyle,
        label = { Text(label) },
        supportingText = message?.let { text -> { Text(text) } },
        isError = isError,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
    )
}

/**
 * The poster's seed headline in Material3, headless text with the underline of a filled field. It
 * reads the surrounding tokens, so on the poster it wears the seed.
 */
@Composable
@ReadOnlyComposable
internal fun materialHeroFieldStyle(): FieldStyle =
    heroFieldStyle(
        tokens = LocalBuilderTokens.current,
        underline = 2.dp,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    )

/**
 * A Material3 primary tab row that scrolls sideways (spec section 7), with roving focus, which
 * Material leaves out.
 *
 * Only the selected tab can take focus from Tab, so Tab enters and leaves the row in one step. The
 * arrow keys select the next or previous tab, wrapping at the ends, and move focus with it. The row
 * scrolls the selected tab into view.
 */
@Composable
internal fun <T> MaterialTabs(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier,
) {
    val selectedIndex = tabs.indexOf(selected)
    val requesters = remember(tabs.size) { List(tabs.size) { FocusRequester() } }
    val roving = remember { mutableIntStateOf(selectedIndex) }
    SideEffect { roving.intValue = selectedIndex }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    fun moveTo(index: Int) {
        val next = index.mod(tabs.size)
        roving.intValue = next
        requesters[next].requestFocus()
        onSelect(tabs[next])
    }

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier
            .focusGroup()
            .onPreviewKeyEvent { event ->
                val forward = if (isRtl) -1 else 1
                val target = when (event.key) {
                    Key.DirectionRight -> roving.intValue + forward
                    Key.DirectionLeft -> roving.intValue - forward
                    Key.MoveHome -> 0
                    Key.MoveEnd -> tabs.lastIndex
                    else -> return@onPreviewKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown) moveTo(target)
                true
            },
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .focusRequester(requesters[index])
                    .focusProperties { canFocus = index == roving.intValue },
                text = { Text(label(tab), maxLines = 1) },
            )
        }
    }
}

/**
 * A disclosure row as a Material3 list item, since Material has no disclosure of its own. The item
 * is a button that speaks its state, with expand and collapse actions while it is enabled.
 */
@Composable
internal fun MaterialDisclosure(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier,
    summary: String?,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val motion = LocalBuilderMotion.current
    Column(modifier) {
        ListItem(
            headlineContent = { Text(title) },
            modifier = Modifier
                .heightIn(min = LocalLayout.current.primaryTouchTarget)
                .clickable(enabled = enabled, role = Role.Button) { onExpandedChange(!expanded) }
                .semantics {
                    stateDescription = disclosureStateDescription(expanded, enabled)
                    if (!enabled) return@semantics
                    if (expanded) {
                        collapse {
                            onExpandedChange(false)
                            true
                        }
                    } else {
                        expand {
                            onExpandedChange(true)
                            true
                        }
                    }
                }.alpha(inputAlpha(enabled)),
            supportingContent = summary?.let { text -> { Text(text) } },
            trailingContent = { DisclosureChevron(expanded) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = disclosureEnter(motion),
            exit = disclosureExit(motion),
        ) {
            content()
        }
    }
}
