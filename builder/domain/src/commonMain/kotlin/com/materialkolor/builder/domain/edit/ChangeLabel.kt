package com.materialkolor.builder.domain.edit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the undo and redo buttons say about a change.
 *
 * The domain does not hold any text. [kind] names the string the UI looks up and [detail] is what
 * gets dropped into it, so a style change reads as "Undo style change to Vibrant" in whatever
 * language the UI is showing.
 *
 * @property[kind] Which sort of change this was.
 * @property[detail] The value the change landed on, when there is one worth naming.
 */
@Serializable
public data class ChangeLabel(
    @SerialName("kind")
    public val kind: ChangeKind,
    @SerialName("detail")
    public val detail: String? = null,
) {
    /**
     * The key of the string the UI shows for this label.
     */
    public val textKey: String
        get() = kind.textKey
}

/**
 * The sorts of change the history can name.
 *
 * @property[textKey] The key of the string the UI shows for this sort of change. A saved history
 *   holds the entry name, not this key, so the key can move without breaking anything.
 */
@Serializable
public enum class ChangeKind(
    public val textKey: String,
) {
    @SerialName("Seed")
    Seed(textKey = "change_seed"),

    @SerialName("Preset")
    Preset(textKey = "change_preset"),

    @SerialName("KeyColor")
    KeyColor(textKey = "change_key_color"),

    @SerialName("ResetKeyColors")
    ResetKeyColors(textKey = "change_reset_key_colors"),

    @SerialName("Style")
    Style(textKey = "change_style"),

    @SerialName("CmfSeed")
    CmfSeed(textKey = "change_cmf_seed"),

    @SerialName("Contrast")
    Contrast(textKey = "change_contrast"),

    @SerialName("Spec")
    Spec(textKey = "change_spec"),

    @SerialName("Platform")
    Platform(textKey = "change_platform"),

    @SerialName("Amoled")
    Amoled(textKey = "change_amoled"),

    @SerialName("AddAccent")
    AddAccent(textKey = "change_add_accent"),

    @SerialName("UpdateAccent")
    UpdateAccent(textKey = "change_update_accent"),

    @SerialName("RemoveAccent")
    RemoveAccent(textKey = "change_remove_accent"),

    @SerialName("Pin")
    Pin(textKey = "change_pin"),

    @SerialName("ClearPins")
    ClearPins(textKey = "change_clear_pins"),

    @SerialName("Library")
    Library(textKey = "change_library"),

    @SerialName("MotionScheme")
    MotionScheme(textKey = "change_motion_scheme"),

    @SerialName("ThemeName")
    ThemeName(textKey = "change_theme_name"),

    @SerialName("CustomTone")
    CustomTone(textKey = "change_custom_tone"),

    @SerialName("Replace")
    Replace(textKey = "change_replace"),
}
