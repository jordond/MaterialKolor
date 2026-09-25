package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.headless.rovingTarget
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.FocusRingWidth
import com.materialkolor.builder.kit.skin.headless.controlRing
import io.github.composefluent.FluentTheme
import io.github.composefluent.LocalContentColor
import io.github.composefluent.component.SelectorBar
import io.github.composefluent.component.SelectorBarDefaults
import io.github.composefluent.component.SelectorBarItem
import io.github.composefluent.component.SelectorBarItemColor
import io.github.composefluent.scheme.VisualStateScheme

/**
 * Fluent's selector bar as the tab row, each tab one of Fluent's own `SelectorBarItem`s, which takes
 * its press on the node its modifier lands on, so the tab role, the folded name and the test tag sit
 * on the node that is pressed.
 *
 * Fluent's bar neither roves nor scrolls, so both are laid over it. Only the selected tab can take
 * focus from Tab, the arrow keys select the next or previous tab in reading order, wrapping at the
 * ends, and Home and End jump. The row scrolls sideways when the tabs do not fit, and its ends sit
 * inside the scroll with room for the focus ring's reach, so the first and last tab ring on every
 * side, as the others do (S5 row 9). The chosen tab wears Fluent's indicator, held still under
 * reduced motion.
 */
@Composable
internal fun <T> FluentTabs(
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
    val target = LocalLayout.current.primaryTouchTarget
    val shape = FluentTheme.shapes.control

    fun moveTo(index: Int) {
        roving.intValue = index
        requesters[index].requestFocus()
        onSelect(tabs[index])
    }

    SelectorBar(
        modifier = modifier
            .focusGroup()
            .onPreviewKeyEvent { event ->
                val next = rovingTarget(event.key, roving.intValue, tabs.size, isRtl, upDown = false, homeEnd = true)
                    ?: return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) moveTo(next)
                true
            }.horizontalScroll(rememberScrollState())
            .padding(horizontal = TabRingReach),
    ) {
        tabs.forEachIndexed { index, tab ->
            key(tab) {
                val interactions = remember { MutableInteractionSource() }
                val isSelected = index == selectedIndex
                SelectorBarItem(
                    selected = isSelected,
                    onSelectedChange = { onSelect(tab) },
                    text = {
                        BuilderText(
                            text = label(tab),
                            style = BuilderTextStyle.Label,
                            color = LocalContentColor.current,
                            maxLines = 1,
                        )
                    },
                    modifier = Modifier
                        .focusRequester(requesters[index])
                        .focusProperties { canFocus = index == roving.intValue }
                        .semantics { role = Role.Tab }
                        .foldState(label(tab), ControlState.Selected(isSelected), role = FoldedRole.Tab)
                        .heightIn(min = target)
                        .controlRing(interactions, shape),
                    colors = fluentTabColors(isSelected),
                    indicator = { _ -> FluentIndicator(visible = isSelected, enabled = true) },
                    interactionSource = interactions,
                )
            }
        }
    }
}

/**
 * Fluent's selector bar item colours, in the poster's ink on the poster.
 */
@Composable
internal fun fluentTabColors(selected: Boolean): VisualStateScheme<SelectorBarItemColor> {
    val fluent = if (selected) SelectorBarDefaults.selectedItemColors() else SelectorBarDefaults.defaultItemColors()
    return LocalFluentPosterInk.current?.tabs(fluent) ?: fluent
}

/**
 * How far a tab's focus ring reaches past the tab, which the ends of the row keep free.
 */
private val TabRingReach: Dp = FocusRingOffset + FocusRingWidth
