package com.materialkolor.builder.domain.model

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A theme, as it is saved and as it is shared.
 *
 * This is the whole input to the engine and nothing else. It holds what someone chose, never what
 * was computed from it, so the same document always produces the same scheme and a link is small
 * enough to fit in a URL.
 *
 * What is not here is just as deliberate. The package name and the export mode belong to whoever
 * is exporting, not to the theme, so they stay in the browser's preferences and never travel with
 * a shared link.
 *
 * @property[seed] The color the scheme is generated from.
 * @property[seedSource] How the seed was chosen, which is what the seed panel shows.
 * @property[keyColors] Palettes someone set by hand instead of deriving from the seed.
 * @property[style] The palette style the scheme is generated with.
 * @property[cmfTertiarySeed] The second seed the [Style.Cmf] style reads for its tertiary palette.
 * @property[contrast] How much contrast the scheme is generated with.
 * @property[spec] The Material spec the scheme is generated against. A new theme asks for 2026, the
 * newest, so every style starts on the newest spec it has.
 * @property[platform] The device the scheme is tuned for.
 * @property[amoled] Whether dark mode drops its surfaces to true black.
 * @property[accents] Extra color families the theme owns.
 * @property[pins] Roles nailed to a color instead of being derived.
 * @property[library] The module the exported theme is written against.
 * @property[expressive] Whether the export carries the expressive shapes and type.
 * @property[motionScheme] The motion scheme the export carries.
 * @property[themeName] What the exported theme is called.
 * @property[customTones] Tones moved on the slots of the custom target.
 */
@Immutable
@Serializable
public data class ThemeDocument(
    @SerialName("seed")
    public val seed: Argb,
    @SerialName("seedSource")
    public val seedSource: SeedSource = SeedSource.Typed,
    @SerialName("keyColors")
    public val keyColors: KeyColors = KeyColors(),
    @SerialName("style")
    public val style: Style = Style.TonalSpot,
    @SerialName("cmfTertiarySeed")
    public val cmfTertiarySeed: Argb? = null,
    @SerialName("contrast")
    public val contrast: ContrastLevel = ContrastLevel.Standard,
    @SerialName("spec")
    public val spec: SpecVersion = SpecVersion.Spec2026,
    @SerialName("platform")
    public val platform: SchemePlatform = SchemePlatform.Phone,
    @SerialName("amoled")
    public val amoled: Boolean = false,
    @SerialName("accents")
    public val accents: List<Accent> = emptyList(),
    @SerialName("pins")
    public val pins: Map<Role, RolePin> = emptyMap(),
    @SerialName("library")
    public val library: Library = Library.Material3,
    @SerialName("expressive")
    public val expressive: Boolean = false,
    @SerialName("motionScheme")
    public val motionScheme: MotionSchemeChoice = MotionSchemeChoice.Expressive,
    @SerialName("themeName")
    public val themeName: String = "AppTheme",
    @SerialName("customTones")
    public val customTones: Map<CustomSlot, CustomTone> = emptyMap(),
) {
    public companion object {
        /**
         * The theme the builder opens on, and what Reset goes back to.
         */
        public val Default: ThemeDocument = ThemeDocument(seed = DEFAULT_SEED)
    }
}
