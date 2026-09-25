package com.materialkolor.builder.preview.fluent

import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.audit.ColorRef

/**
 * The parts the Fluent gallery draws, each with whether it paints Fluent's accent fill.
 *
 * Fluent takes only its accent from the theme. A part that paints the accent fill, a checked box, a
 * switch that is on or the current segment's line, declares the on-accent ink and then the accent
 * fill through [fluentAccentRoles], the pair the contrast audit rates. Every other part paints
 * Fluent's fixed neutrals and system colors and declares none through [fluentNeutralRoles], so
 * Inspect still finds it. A disabled part paints fixed greys whatever it is and declares [Disabled].
 *
 * @property[accentFill] Whether the part paints the accent fill.
 * @property[refs] The colors the part declares, named the way the contrast audit names them.
 */
internal enum class FluentGalleryComponent(
    val accentFill: Boolean,
) {
    Gallery(accentFill = false),
    Card(accentFill = false),
    Button(accentFill = false),
    AccentButton(accentFill = true),
    SubtleButton(accentFill = false),
    ToggleButton(accentFill = false),
    CheckedToggleButton(accentFill = true),

    /**
     * The field, its text and its header. The line under a focused field takes the accent.
     */
    TextBox(accentFill = false),

    /**
     * The filled part of the rail and the thumb's dot.
     */
    Slider(accentFill = true),
    CheckBox(accentFill = false),
    CheckedCheckBox(accentFill = true),
    RadioButton(accentFill = false),
    SelectedRadioButton(accentFill = true),
    Switch(accentFill = false),
    OnSwitch(accentFill = true),
    SegmentedControl(accentFill = false),
    Segment(accentFill = false),

    /**
     * The chosen segment, whose line under the label takes the accent.
     */
    SelectedSegment(accentFill = true),
    SampleCard(accentFill = false),
    Expander(accentFill = false),
    Dialog(accentFill = false),
    SelectorItem(accentFill = false),

    /**
     * The current item of a selector bar, whose line under the label takes the accent.
     */
    SelectedSelectorItem(accentFill = true),
    TabRow(accentFill = false),
    Tab(accentFill = false),
    NavigationItem(accentFill = false),

    /**
     * The current item of a navigation list, whose pill beside the label takes the accent.
     */
    SelectedNavigationItem(accentFill = true),

    /**
     * Every severity paints a fixed system color.
     */
    InfoBar(accentFill = false),
    Progress(accentFill = true),

    /**
     * A status badge, painted with a fixed system color.
     */
    Badge(accentFill = false),

    /**
     * A count on the accent fill.
     */
    AccentBadge(accentFill = true),

    /**
     * Any disabled part, in Fluent's fixed disabled greys.
     */
    Disabled(accentFill = false),
    ;

    val refs: List<ColorRef> get() = if (accentFill) FluentAccentRefs else emptyList()
}

/**
 * Declare the colors [component] paints, for Inspect.
 */
internal fun Modifier.previewRoles(component: FluentGalleryComponent): Modifier =
    if (component.accentFill) fluentAccentRoles() else fluentNeutralRoles()

/**
 * Declare the colors of [component] while [enabled], and of the disabled look otherwise.
 */
internal fun Modifier.previewRoles(
    enabled: Boolean,
    component: FluentGalleryComponent,
): Modifier = previewRoles(if (enabled) component else FluentGalleryComponent.Disabled)
