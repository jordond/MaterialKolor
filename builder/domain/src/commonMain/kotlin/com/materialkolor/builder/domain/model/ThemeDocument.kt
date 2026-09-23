package com.materialkolor.builder.domain.model

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
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
 * @property[spec] The Material spec the scheme is generated against.
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
@Serializable
public data class ThemeDocument(
    public val seed: Argb,
    public val seedSource: SeedSource = SeedSource.Typed,
    public val keyColors: KeyColors = KeyColors(),
    public val style: Style = Style.TonalSpot,
    public val cmfTertiarySeed: Argb? = null,
    public val contrast: ContrastLevel = ContrastLevel.Standard,
    public val spec: SpecVersion = SpecVersion.Spec2021,
    public val platform: SchemePlatform = SchemePlatform.Phone,
    public val amoled: Boolean = false,
    public val accents: List<Accent> = emptyList(),
    public val pins: Map<Role, RolePin> = emptyMap(),
    public val library: Library = Library.Material3,
    public val expressive: Boolean = false,
    public val motionScheme: MotionSchemeChoice = MotionSchemeChoice.Expressive,
    public val themeName: String = "AppTheme",
    public val customTones: Map<CustomSlot, CustomTone> = emptyMap(),
) {
    public companion object {
        /**
         * The theme the builder opens on, and what Reset goes back to.
         */
        public val Default: ThemeDocument = ThemeDocument(seed = DEFAULT_SEED)
    }
}
