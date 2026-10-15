package com.materialkolor.builder.kit.skin.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.SwitchLabel
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.stateName
import com.materialkolor.builder.kit.control.stateWords
import com.materialkolor.builder.kit.headless.DisclosureChevron
import com.materialkolor.builder.kit.headless.SliderKeyPress
import com.materialkolor.builder.kit.headless.SliderRules
import com.materialkolor.builder.kit.headless.disclosureEnter
import com.materialkolor.builder.kit.headless.disclosureExit
import com.materialkolor.builder.kit.headless.disclosureName
import com.materialkolor.builder.kit.headless.rovingTarget
import com.materialkolor.builder.kit.headless.sliderKeys
import com.materialkolor.builder.kit.headless.sliderSemantics
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.FieldStyle
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.FocusRingWidth
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.headless.heroFieldStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A Material3 switch with its label. The row is the target and carries the switch role, so the
 * switch itself takes no clicks. The row has the focus ring and Material's own focus layer, clipped
 * to the ring's corners so the ripple and the layer stay inside it.
 */
@Composable
internal fun MaterialSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    caption: String?,
) {
    val tokens = LocalBuilderTokens.current
    val interactions = remember { MutableInteractionSource() }
    val words = stateWords()
    val state = ControlState.Switched(checked)
    val shape = RoundedCornerShape(tokens.radius.small)
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .controlRing(interactions, shape)
            .clip(shape)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = ripple(),
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ).semantics { stateDescription = words.of(state) }
            .foldState(label, state, enabled, words, FoldedRole.Switch),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwitchLabel(
            label = label,
            caption = caption,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = tokens.spacing.medium)
                .alpha(enabledAlpha(enabled)),
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/**
 * A Material3 checkbox with its label, the row being the target. The row has the focus ring and
 * Material's own focus layer, clipped to the ring's corners like the switch.
 */
@Composable
internal fun MaterialCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
) {
    val tokens = LocalBuilderTokens.current
    val interactions = remember { MutableInteractionSource() }
    val words = stateWords()
    val state = ControlState.Checked(checked)
    val shape = RoundedCornerShape(tokens.radius.small)
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .controlRing(interactions, shape)
            .clip(shape)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = ripple(),
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ).semantics { stateDescription = words.of(state) }
            .foldState(label, state, enabled, words, FoldedRole.Checkbox),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        BuilderText(
            text = label,
            modifier = Modifier.alpha(enabledAlpha(enabled)),
            style = BuilderTextStyle.Label,
        )
    }
}

/**
 * The Material3 slider, with the builder's keys and snapping laid over it. Material draws no named
 * stops, so they only show through the snap. Material shows focus only by narrowing the thumb, so
 * the thumb gets the focus ring as well.
 *
 * The inactive track takes the outline rather than Material's secondary container. On the poster
 * and its inverse the containers sit close to the page, and the outline is held at 3 to 1 on it.
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
    val interactions = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.outline)
    Slider(
        value = value,
        onValueChange = { raw -> onValueChange(rules.snap(raw)) },
        modifier = modifier
            .sliderKeys(value, rules, enabled, isRtl, press, onValueChange, onValueChangeFinished)
            .sliderSemantics(
                name = stateName(label, ControlState.Value(stateDescription), enabled, role = FoldedRole.Slider),
                nameAsText = LocalFoldsStateIntoName.current,
                stateDescription = stateDescription,
                value = value,
                rules = rules,
                enabled = enabled,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
            ),
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactions,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactions,
                modifier = Modifier.controlRing(interactions, CircleShape),
                colors = colors,
                enabled = enabled,
            )
        },
        valueRange = rules.range,
    )
}

/**
 * The Material3 outlined text field over a builder field draft. An error field always has its
 * message here, which names the error on the field.
 */
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
    MaterialOutlinedField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.semantics { if (isError && message != null) error(message) },
        enabled = enabled,
        textStyle = textStyle,
        supportingText = message,
        isError = isError,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

/**
 * The poster's seed headline in Material3, headless text with the underline of a filled field. It
 * reads the surrounding tokens, so on the poster it is drawn in the seed.
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
 * How tall the primary tab indicator is. The row is handed this height, so the focus ring's clearance
 * above the indicator cannot drift from what Material draws.
 */
private val TabIndicatorHeight: Dp = 3.dp

/**
 * How far inside the tab its focus ring sits. The row clips at the tab's edge, so the ring goes
 * inside, a ring's offset clear of the indicator along the bottom.
 */
private val TabRingInset: Dp = TabIndicatorHeight + FocusRingOffset + FocusRingWidth

/**
 * A Material3 primary tab row that scrolls sideways, with roving focus, which Material leaves out.
 *
 * Only the selected tab can take focus from Tab, so Tab enters and leaves the row in one step. The
 * arrow keys select the next or previous tab, wrapping at the ends, and move focus with it. The row
 * scrolls the selected tab into view. The focused tab shows the focus ring inside its own edge.
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
    val sources = remember(tabs.size) { List(tabs.size) { MutableInteractionSource() } }
    val roving = remember { mutableIntStateOf(selectedIndex) }
    SideEffect { roving.intValue = selectedIndex }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    fun moveTo(index: Int) {
        roving.intValue = index
        requesters[index].requestFocus()
        onSelect(tabs[index])
    }

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier
            .focusGroup()
            .onPreviewKeyEvent { event ->
                val target = rovingTarget(event.key, roving.intValue, tabs.size, isRtl, upDown = false, homeEnd = true)
                    ?: return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) moveTo(target)
                true
            },
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = true),
                width = Dp.Unspecified,
                height = TabIndicatorHeight,
            )
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .controlRing(sources[index], RectangleShape, offset = -TabRingInset)
                    .focusRequester(requesters[index])
                    .focusProperties { canFocus = index == roving.intValue }
                    .foldState(label(tab), ControlState.Selected(index == selectedIndex), role = FoldedRole.Tab),
                text = { Text(label(tab), maxLines = 1) },
                interactionSource = sources[index],
            )
        }
    }
}

/**
 * A disclosure row as a Material3 list item, since Material has no disclosure of its own. The item
 * is a button that speaks its state, with expand and collapse actions while it is enabled. It draws
 * the focus ring square, like a Material list row.
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
    val state = ControlState.Expanded(expanded)
    val words = stateWords()
    val spoken = words.of(state)
    val interactions = remember { MutableInteractionSource() }
    Column(modifier) {
        ListItem(
            headlineContent = { Text(title) },
            modifier = Modifier
                .heightIn(min = LocalLayout.current.primaryTouchTarget)
                .controlRing(interactions, RectangleShape)
                .clickable(
                    interactionSource = interactions,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                ) { onExpandedChange(!expanded) }
                .semantics {
                    stateDescription = spoken
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
                }.foldState(disclosureName(title, summary), state, enabled, words)
                .alpha(enabledAlpha(enabled)),
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
