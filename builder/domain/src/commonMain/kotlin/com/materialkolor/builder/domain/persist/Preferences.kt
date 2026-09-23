package com.materialkolor.builder.domain.persist

import com.materialkolor.builder.domain.model.Library
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What this browser remembers across every project, stored under [StorageKeys.PREFS].
 *
 * None of it belongs to a theme. It stays in the browser that set it and never travels in a link.
 *
 * @property[appearance] Whether the builder's own chrome is light, dark or follows the system.
 * @property[motion] Whether the builder's own animations follow the system or are forced.
 * @property[hueLock] Whether shuffling keeps the seed's hue.
 * @property[styleLock] Whether shuffling keeps the palette style.
 * @property[seedLock] Whether shuffling keeps the seed.
 * @property[dismissedHints] The hints someone has closed, by id, so they stay closed.
 * @property[firstExportDone] Whether this browser has exported a theme before.
 * @property[posterCollapsed] Whether the poster is collapsed.
 * @property[lastProjectId] The project that was open last, or null when there is none.
 * @property[persistRequested] Whether the builder has already asked the browser for persistent
 * storage, so it only ever asks once.
 * @property[exportPrefs] The export options last used for each target. A target that has never
 * been exported starts from [ExportPrefs] as it comes, see [exportPrefsFor].
 */
@Serializable
public data class Preferences(
    @SerialName("appearance")
    public val appearance: Appearance = Appearance.System,
    @SerialName("motion")
    public val motion: MotionOverride = MotionOverride.System,
    @SerialName("hueLock")
    public val hueLock: Boolean = false,
    @SerialName("styleLock")
    public val styleLock: Boolean = true,
    @SerialName("seedLock")
    public val seedLock: Boolean = false,
    @SerialName("dismissedHints")
    public val dismissedHints: Set<String> = emptySet(),
    @SerialName("firstExportDone")
    public val firstExportDone: Boolean = false,
    @SerialName("posterCollapsed")
    public val posterCollapsed: Boolean = false,
    @SerialName("lastProjectId")
    public val lastProjectId: String? = null,
    @SerialName("persistRequested")
    public val persistRequested: Boolean = false,
    @SerialName("exportPrefs")
    public val exportPrefs: Map<ExportTarget, ExportPrefs> = emptyMap(),
) {
    /**
     * The export options for [target], or the defaults when it has never been exported.
     */
    public fun exportPrefsFor(target: ExportTarget): ExportPrefs = exportPrefs[target] ?: ExportPrefs()

    /**
     * These preferences with [prefs] remembered for [target], leaving the other targets alone.
     */
    public fun withExportPrefs(
        target: ExportTarget,
        prefs: ExportPrefs,
    ): Preferences = copy(exportPrefs = exportPrefs + (target to prefs))

    public companion object {
        /** Reads and writes [Preferences]. */
        public val Codec: RecordCodec<Preferences> = RecordCodec(serializer(), PreferencesMigrations)
    }
}

/**
 * Whether the builder's own chrome is light or dark. The theme being built is previewed separately.
 */
@Serializable
public enum class Appearance {
    /** Follow the system. */
    @SerialName("System")
    System,

    /** Always light. */
    @SerialName("Light")
    Light,

    /** Always dark. */
    @SerialName("Dark")
    Dark,
}

/**
 * Whether the builder's own animations run, whatever the system says.
 */
@Serializable
public enum class MotionOverride {
    /** Follow the system's reduced motion setting. */
    @SerialName("System")
    System,

    /** Keep motion to a minimum. */
    @SerialName("Reduce")
    Reduce,

    /** Animate fully. */
    @SerialName("Full")
    Full,
}

/**
 * What an export is written for, the library together with whether it is expressive.
 *
 * Export options are remembered per target, so someone who exports Fluent with one package name
 * and Material 3 with another gets each back where they left it.
 */
@Serializable
public enum class ExportTarget {
    /** A Compose Material 3 theme. */
    @SerialName("Material3")
    Material3,

    /** A Compose Material 3 theme with the expressive shapes, type and motion. */
    @SerialName("Material3Expressive")
    Material3Expressive,

    /** The plain color holder. */
    @SerialName("Unstyled")
    Unstyled,

    /** A Fluent color set. */
    @SerialName("Fluent")
    Fluent,

    /** A hand shaped set of slots. */
    @SerialName("Custom")
    Custom,
    ;

    public companion object {
        /**
         * The target a theme with [library] exports to. Only Material 3 has an expressive flavor,
         * every other library ignores [expressive].
         */
        public fun of(
            library: Library,
            expressive: Boolean,
        ): ExportTarget =
            when (library) {
                Library.Material3 -> if (expressive) Material3Expressive else Material3
                Library.Unstyled -> Unstyled
                Library.Fluent -> Fluent
                Library.Custom -> Custom
            }
    }
}

/**
 * The options one export target was last exported with.
 *
 * @property[packageName] The package the generated files declare.
 * @property[multiplatform] Whether the build snippets are for a multiplatform project rather than
 * an Android one.
 * @property[versionCatalog] Whether the dependency goes through a version catalog.
 * @property[mode] Whether the theme is generated at runtime or written out as fixed colors.
 * @property[animate] Whether the generated theme animates between color changes.
 * @property[animationDurationMs] How long that animation runs, in milliseconds.
 * @property[frozenVariants] Which contrast levels a frozen export writes out.
 * @property[androidDynamicColor] Whether the generated theme uses the wallpaper colors on Android
 * 12 and up. Off unless asked for, since it hides the generated scheme on those devices.
 */
@Serializable
public data class ExportPrefs(
    @SerialName("packageName")
    public val packageName: String = DEFAULT_PACKAGE_NAME,
    @SerialName("multiplatform")
    public val multiplatform: Boolean = true,
    @SerialName("versionCatalog")
    public val versionCatalog: Boolean = true,
    @SerialName("mode")
    public val mode: ExportMode = ExportMode.Dynamic,
    @SerialName("animate")
    public val animate: Boolean = false,
    @SerialName("animationDurationMs")
    public val animationDurationMs: Int = DEFAULT_ANIMATION_DURATION_MS,
    @SerialName("frozenVariants")
    public val frozenVariants: FrozenVariants = FrozenVariants.StandardOnly,
    @SerialName("androidDynamicColor")
    public val androidDynamicColor: Boolean = false,
) {
    public companion object {
        /** The package an export uses until someone types their own. */
        public const val DEFAULT_PACKAGE_NAME: String = "com.example.theme"

        /** How long color changes animate for until someone picks another duration. */
        public const val DEFAULT_ANIMATION_DURATION_MS: Int = 300
    }
}

/**
 * How the exported theme gets its colors.
 */
@Serializable
public enum class ExportMode {
    /** MaterialKolor generates the scheme from the seed at runtime. */
    @SerialName("Dynamic")
    Dynamic,

    /** Every color is written out as hex, with no MaterialKolor dependency. */
    @SerialName("Frozen")
    Frozen,
}

/**
 * Which contrast levels a frozen export writes out.
 */
@Serializable
public enum class FrozenVariants {
    /** Standard contrast only. */
    @SerialName("StandardOnly")
    StandardOnly,

    /** Standard, medium and high contrast. */
    @SerialName("AllContrasts")
    AllContrasts,
}
