package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCheckbox
import com.materialkolor.builder.kit.control.BuilderChoiceChips
import com.materialkolor.builder.kit.control.BuilderFilterChip
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderListRow
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSelect
import com.materialkolor.builder.kit.control.BuilderSlider
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderToggleButton
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import com.materialkolor.builder.preview.inspect.previewRoles
import kotlin.math.roundToInt

// The slots each kit control paints, then the samples of the Actions, Inputs and Selection cards.
// GalleryEntry.kt keeps the entry, the card frame and the samples of the other three groups.

/**
 * The kit controls of the Custom gallery, each with the Custom slots the Custom skin paints it in.
 *
 * The slots follow the Custom style sets of the kit, where every token is one slot. A disabled
 * control fades the slots it paints enabled, so both copies name the same ones.
 *
 * @property[refs] The slots, named the way the contrast audit names them.
 */
internal enum class CustomComponent(
    vararg slots: CustomSlot,
) {
    PrimaryAction(CustomSlot.Primary, CustomSlot.OnPrimary),
    SecondaryAction(CustomSlot.SurfaceRaised, CustomSlot.TextStrong),
    SubtleAction(CustomSlot.TextMuted),

    /** A danger action, outlined and labelled in the error ink with no fill. */
    DangerAction(CustomSlot.Error),

    /** Raised and outlined while off, filled with the accent while on. Chips and toggles alike. */
    Selectable(
        CustomSlot.SurfaceRaised,
        CustomSlot.TextStrong,
        CustomSlot.BorderStrong,
        CustomSlot.Primary,
        CustomSlot.OnPrimary,
    ),
    TextField(
        CustomSlot.SurfaceRaised,
        CustomSlot.TextStrong,
        CustomSlot.BorderStrong,
        CustomSlot.TextMuted,
        CustomSlot.Primary,
        CustomSlot.Error,
    ),
    Slider(CustomSlot.Primary, CustomSlot.SurfaceRaised, CustomSlot.OnPrimary),
    Checkbox(CustomSlot.BorderStrong, CustomSlot.Primary, CustomSlot.OnPrimary, CustomSlot.TextStrong),
    Switch(
        CustomSlot.SurfaceRaised,
        CustomSlot.BorderStrong,
        CustomSlot.Primary,
        CustomSlot.OnPrimary,
        CustomSlot.TextStrong,
    ),
    Segmented(CustomSlot.BorderStrong, CustomSlot.TextStrong, CustomSlot.Primary, CustomSlot.OnPrimary),
    SelectField(CustomSlot.SurfaceRaised, CustomSlot.BorderSoft, CustomSlot.TextMuted, CustomSlot.TextStrong),

    /** The raised panel a menu, a select's list, a dialog or a sheet opens on. */
    Overlay(CustomSlot.SurfaceRaised, CustomSlot.TextStrong, CustomSlot.TextMuted),
    Card(CustomSlot.Surface, CustomSlot.TextStrong, CustomSlot.BorderSoft),
    ListRow(CustomSlot.TextStrong, CustomSlot.TextMuted, CustomSlot.SurfaceRaised, CustomSlot.BorderStrong),
    Divider(CustomSlot.BorderSoft),
    Disclosure(CustomSlot.SurfaceRaised, CustomSlot.TextStrong, CustomSlot.TextMuted),
    Tabs(CustomSlot.SurfaceRaised, CustomSlot.TextStrong, CustomSlot.Primary, CustomSlot.OnPrimary),
    NeutralBadge(CustomSlot.SurfaceRaised, CustomSlot.TextStrong, CustomSlot.BorderSoft),

    /** A success or warning badge, a status ink no slot moves under text in the Surface slot. */
    StatusBadge(CustomSlot.Surface),
    DangerBadge(CustomSlot.Error, CustomSlot.Surface),
    Progress(CustomSlot.BorderSoft, CustomSlot.Primary),

    /** A tooltip or a toast, the Surface slot on the strong text colour. */
    Inverse(CustomSlot.TextStrong, CustomSlot.Surface),

    /** The sunken canvas the cards sit on. */
    Canvas(CustomSlot.SurfaceSunken, CustomSlot.TextStrong),
    ;

    val refs: List<ColorRef> = slots.map { slot -> ColorRef.OfSlot(slot) }
}

/** Declare the slots [component] paints, for the role usage check and Inspect. */
internal fun Modifier.previewRoles(component: CustomComponent): Modifier = previewRoles(*component.refs.toTypedArray())

/** The component a kit action of this emphasis is drawn as. */
internal val Emphasis.component: CustomComponent
    get() = when (this) {
        Emphasis.Primary -> CustomComponent.PrimaryAction
        Emphasis.Secondary -> CustomComponent.SecondaryAction
        Emphasis.Subtle -> CustomComponent.SubtleAction
        Emphasis.Danger -> CustomComponent.DangerAction
    }

/** Enabled first, then disabled, the order every card shows its copies in. */
internal val EnabledThenDisabled: List<Boolean> = listOf(true, false)

private const val SliderStops = 11
private val Formats = listOf("Hex", "RGB", "HSL")
private val Styles = listOf("Tonal", "Vivid", "Muted")
private val Contrasts = listOf("Standard", "Medium", "High")
private val IconActions = listOf(
    "Add" to IconId.Plus,
    "Copy" to IconId.Copy,
    "More" to IconId.More,
    "Delete" to IconId.Trash,
)
private val SortOrders = listOf(
    "Newest first" to IconId.ChevronDown,
    "By name" to IconId.Search,
    "By hue" to IconId.Eyedropper,
)

/** A button of [emphasis], enabled and disabled. */
@Composable
internal fun ActionButtons(
    emphasis: Emphasis,
    label: String,
    icon: IconId?,
) {
    EnabledPair { enabled ->
        BuilderButton(
            onClick = {},
            label = label,
            modifier = Modifier.previewRoles(emphasis.component),
            emphasis = emphasis,
            icon = icon,
            enabled = enabled,
        )
    }
}

/** An icon button of every emphasis, enabled on the first row and disabled on the second. */
@Composable
internal fun IconButtons() {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small)) {
                for ((emphasis, action) in Emphasis.entries.zip(IconActions)) {
                    BuilderIconButton(
                        onClick = {},
                        icon = action.second,
                        contentDescription = action.first,
                        modifier = Modifier.previewRoles(emphasis.component),
                        emphasis = emphasis,
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ToggleButtons(state: DemoAppState) {
    EnabledPair { enabled ->
        BuilderToggleButton(
            checked = state.isOn(PinKey),
            onCheckedChange = { on -> state.setOn(PinKey, on) },
            label = "Pin",
            modifier = Modifier.previewRoles(CustomComponent.Selectable),
            icon = IconId.Pin,
            enabled = enabled,
        )
    }
}

@Composable
internal fun TextFields(state: DemoAppState) {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            BuilderTextField(
                value = state.text,
                onCommit = { text -> state.text = text },
                label = "Project name",
                modifier = Modifier.fillMaxWidth().previewRoles(CustomComponent.TextField),
                error = { draft -> if (draft.length > NameLimit) "Keep it under $NameLimit characters" else null },
                supportingText = if (enabled) "Shown in the export" else "Locked while exporting",
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun Sliders(state: DemoAppState) {
    val stop = state.choice(ChromaKey, SliderStops, default = 6)
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            BuilderSlider(
                value = stop.toFloat(),
                onValueChange = { value ->
                    state.choose(ChromaKey, SliderStops, value.roundToInt().coerceIn(0, SliderStops - 1))
                },
                label = "Chroma",
                modifier = Modifier.fillMaxWidth().previewRoles(CustomComponent.Slider),
                valueRange = 0f..(SliderStops - 1f),
                step = 1f,
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun Checkboxes(state: DemoAppState) {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            BuilderCheckbox(
                checked = state.isChecked(DarkSchemeKey),
                onCheckedChange = { checked -> state.setChecked(DarkSchemeKey, checked) },
                label = if (enabled) "Export the dark scheme" else "Export the AMOLED scheme",
                modifier = Modifier.previewRoles(CustomComponent.Checkbox),
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun Switches(state: DemoAppState) {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            BuilderSwitch(
                checked = state.isOn(ReduceMotionKey),
                onCheckedChange = { on -> state.setOn(ReduceMotionKey, on) },
                label = if (enabled) "Reduce motion" else "Sync across devices",
                modifier = Modifier.previewRoles(CustomComponent.Switch),
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun SegmentedControls(state: DemoAppState) {
    val picked = state.choice(FormatKey, Formats.size)
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            BuilderSegmented(
                options = Formats,
                selected = Formats[picked],
                onSelect = { format -> state.choose(FormatKey, Formats.size, Formats.indexOf(format)) },
                label = "Format",
                modifier = Modifier.previewRoles(CustomComponent.Segmented),
                enabled = enabled,
            ) { format -> format }
        }
    }
}

@Composable
internal fun ChoiceChips(state: DemoAppState) {
    val picked = state.choice(StyleKey, Styles.size)
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            BuilderChoiceChips(
                options = Styles,
                selected = Styles[picked],
                onSelect = { style -> state.choose(StyleKey, Styles.size, Styles.indexOf(style)) },
                label = "Style",
                modifier = Modifier.previewRoles(CustomComponent.Selectable),
                enabled = enabled,
            ) { style -> style }
        }
    }
}

@Composable
internal fun FilterChips(state: DemoAppState) {
    Row(horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small)) {
        for ((index, label) in listOf("Kotlin", "Swift", "Dart").withIndex()) {
            val key = "$FilterKey.$index"
            BuilderFilterChip(
                selected = state.isOn(key),
                onSelectedChange = { on -> state.setOn(key, on) },
                label = label,
                modifier = Modifier.previewRoles(CustomComponent.Selectable),
                enabled = index != 2,
            )
        }
    }
}

/**
 * A select as it looks open, its field over its list, drawn in place rather than in a popup (D40).
 * The disabled select under it is the kit's own, which never opens.
 */
@Composable
internal fun InlineSelect(state: DemoAppState) {
    val tokens = LocalBuilderTokens.current
    val picked = state.choice(ContrastKey, Contrasts.size)
    GalleryColumn {
        val field = RoundedCornerShape(tokens.radius.small)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .previewRoles(CustomComponent.SelectField)
                .background(tokens.panelRaised, field)
                .border(Dp.Hairline, tokens.border, field)
                .padding(horizontal = tokens.spacing.medium, vertical = tokens.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                BuilderText("Contrast", style = BuilderTextStyle.Value, color = tokens.textMuted, maxLines = 1)
                BuilderText(Contrasts[picked], style = BuilderTextStyle.Label, maxLines = 1)
            }
            BuilderIcon(IconId.ChevronDown, contentDescription = null, tint = tokens.textStrong)
        }
        GalleryOverlayPanel(RoundedCornerShape(tokens.radius.medium), Modifier.fillMaxWidth()) {
            Contrasts.forEachIndexed { index, contrast ->
                BuilderListRow(
                    headline = contrast,
                    modifier = Modifier.previewRoles(CustomComponent.ListRow),
                    onClick = { state.choose(ContrastKey, Contrasts.size, index) },
                    selected = index == picked,
                )
            }
        }
        BuilderSelect(
            label = "Spec version",
            options = listOf("2025"),
            selected = "2025",
            onSelect = {},
            modifier = Modifier.fillMaxWidth().previewRoles(CustomComponent.SelectField),
            enabled = false,
        )
    }
}

/** A menu as it looks open, drawn in place rather than in a popup (D40). */
@Composable
internal fun InlineMenu(state: DemoAppState) {
    val tokens = LocalBuilderTokens.current
    val picked = state.choice(SortKey, SortOrders.size)
    GalleryOverlayPanel(RoundedCornerShape(tokens.radius.medium), Modifier.fillMaxWidth()) {
        SortOrders.forEachIndexed { index, (order, icon) ->
            BuilderListRow(
                headline = order,
                modifier = Modifier.previewRoles(CustomComponent.ListRow),
                icon = icon,
                onClick = { state.choose(SortKey, SortOrders.size, index) },
                selected = index == picked,
                enabled = index != SortOrders.lastIndex,
            )
        }
    }
}

/**
 * The raised, shadowed panel the Custom skin opens menus, lists, dialogs and sheets on, drawn in
 * place inside its card.
 */
@Composable
internal fun GalleryOverlayPanel(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = modifier
            .previewRoles(CustomComponent.Overlay)
            .shadow(tokens.spacing.small, shape)
            .background(tokens.panelRaised, shape)
            .padding(tokens.spacing.extraSmall),
        content = content,
    )
}

/** A card's controls stacked with the kit's small gap. */
@Composable
internal fun GalleryColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small), content = content)
}

/** A control's enabled copy and its disabled one, side by side. */
@Composable
internal fun EnabledPair(content: @Composable RowScope.(enabled: Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small)) {
        for (enabled in EnabledThenDisabled) content(enabled)
    }
}

private const val NameLimit = 24

// Keys into DemoAppState.
private const val PinKey = "gallery.custom.pin"
private const val ChromaKey = "gallery.custom.chroma"
private const val DarkSchemeKey = "gallery.custom.darkScheme"
private const val ReduceMotionKey = "gallery.custom.reduceMotion"
private const val FormatKey = "gallery.custom.format"
private const val StyleKey = "gallery.custom.style"
private const val FilterKey = "gallery.custom.filter"
private const val ContrastKey = "gallery.custom.contrast"
private const val SortKey = "gallery.custom.sort"
