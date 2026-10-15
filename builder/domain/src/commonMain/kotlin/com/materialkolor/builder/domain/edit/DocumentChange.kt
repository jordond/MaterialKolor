package com.materialkolor.builder.domain.edit

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument

/**
 * One edit to a theme, written down as data.
 *
 * Every control in the builder produces one of these instead of touching the document itself, so
 * the same edit can be applied, labeled for the undo button and folded into the edit before it
 * without anyone knowing which control it came from.
 *
 * A change only ever touches the fields it names. Picking a style leaves the spec where it was,
 * picking a spec leaves the style alone, and a library pick moves them only when it names them.
 */
@Immutable
public sealed interface DocumentChange {
    /**
     * Which edits this one folds into when they land close together.
     *
     * Two changes with the same key edit the same thing, so typing a hex or nudging a slider with
     * the arrow keys ends up as one step in the history instead of one per keystroke.
     */
    public val coalesceKey: String

    /**
     * What the undo and redo buttons say about this change.
     */
    public val label: ChangeLabel

    /**
     * Whether this change may fold into the one before it at all.
     *
     * A few edits are big enough that undoing them should always be its own step. A style, a
     * contrast level, a library and a preset each repaint the whole theme, a whole new document is a different theme
     * altogether, and adding, removing or clearing things is a structural step nobody expects to
     * vanish into its neighbor.
     */
    public val merges: Boolean
        get() = when (this) {
            is SetSeed -> source !is SeedSource.Preset
            is SetStyle -> false
            is SetLibrary -> false
            is Replace -> false
            is AddAccent -> false
            is RemoveAccent -> false
            is ResetKeyColors -> false
            is ClearPins -> false
            is SetKeyColor -> true
            is SetCmfSeed -> true
            is SetContrast -> false
            is SetSpec -> true
            is SetPlatform -> true
            is SetAmoled -> true
            is UpdateAccent -> true
            is SetPin -> true
            is SetMotionScheme -> true
            is SetThemeName -> true
            is SetCustomTone -> true
        }

    /**
     * The document with this change made to it. [document] itself is left as it was.
     */
    public fun apply(document: ThemeDocument): ThemeDocument

    /**
     * Sets the seed, along with where it came from.
     *
     * @property[argb] The new seed.
     * @property[source] How the seed was chosen.
     */
    @Immutable
    public data class SetSeed(
        public val argb: Argb,
        public val source: SeedSource,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "seed"

        override val label: ChangeLabel
            get() = when (source) {
                is SeedSource.Preset -> ChangeLabel(ChangeKind.Preset, detail = source.id)
                is SeedSource.Image -> ChangeLabel(ChangeKind.Seed, detail = argb.toHex())
                SeedSource.Typed -> ChangeLabel(ChangeKind.Seed, detail = argb.toHex())
                SeedSource.Picked -> ChangeLabel(ChangeKind.Seed, detail = argb.toHex())
                SeedSource.Eyedropper -> ChangeLabel(ChangeKind.Seed, detail = argb.toHex())
                SeedSource.Shuffled -> ChangeLabel(ChangeKind.Seed, detail = argb.toHex())
            }

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(seed = argb, seedSource = source)
    }

    /**
     * Sets one palette by hand, or hands it back to the seed.
     *
     * @property[slot] The palette being set.
     * @property[argb] The color it is built from, or null to derive it from the seed again.
     */
    @Immutable
    public data class SetKeyColor(
        public val slot: KeyColor,
        public val argb: Argb?,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "keyColor.${slot.name}"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.KeyColor, detail = slot.name)

        override fun apply(document: ThemeDocument): ThemeDocument =
            document.copy(keyColors = document.keyColors.with(slot, argb))
    }

    /**
     * Hands every palette back to the seed.
     */
    @Immutable
    public data object ResetKeyColors : DocumentChange {
        override val coalesceKey: String
            get() = "keyColors.reset"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.ResetKeyColors)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(keyColors = KeyColors())
    }

    /**
     * Picks the palette style. The requested spec stays where it was.
     *
     * @property[style] The style to generate with.
     */
    @Immutable
    public data class SetStyle(
        public val style: Style,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "style"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.Style, detail = style.name)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(style = style)
    }

    /**
     * Sets the second seed the [Style.Cmf] style reads, or drops it.
     *
     * @property[argb] The tertiary seed, or null to let the style choose.
     */
    @Immutable
    public data class SetCmfSeed(
        public val argb: Argb?,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "cmfSeed"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.CmfSeed, detail = argb?.toHex())

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(cmfTertiarySeed = argb)
    }

    /**
     * Sets how much contrast the scheme is generated with.
     *
     * Every control that sends this picks one of the four named levels, and links and saved
     * projects move a level in between onto the nearest one as they are read, so a document only
     * ever holds a named level.
     *
     * @property[level] The new contrast, one of [ContrastLevel.Stops].
     */
    @Immutable
    public data class SetContrast(
        public val level: ContrastLevel,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "contrast"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.Contrast)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(contrast = level)
    }

    /**
     * Picks the Material spec. The style stays where it was.
     *
     * @property[spec] The spec to generate against.
     */
    @Immutable
    public data class SetSpec(
        public val spec: SpecVersion,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "spec"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.Spec, detail = spec.name)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(spec = spec)
    }

    /**
     * Picks the device the scheme is tuned for.
     *
     * @property[platform] The device to tune for.
     */
    @Immutable
    public data class SetPlatform(
        public val platform: SchemePlatform,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "platform"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.Platform, detail = platform.name)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(platform = platform)
    }

    /**
     * Turns true black dark surfaces on or off.
     *
     * @property[amoled] Whether dark mode drops its surfaces to true black.
     */
    @Immutable
    public data class SetAmoled(
        public val amoled: Boolean,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "amoled"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.Amoled)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(amoled = amoled)
    }

    /**
     * Adds an accent after the ones already there.
     *
     * @property[accent] The accent to add.
     */
    @Immutable
    public data class AddAccent(
        public val accent: Accent,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "accent.add"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.AddAccent, detail = accent.name)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(accents = document.accents + accent)
    }

    /**
     * Swaps the accent at [index] for [accent].
     *
     * An index the document does not have leaves it as it was, so an edit that arrives after its
     * accent was removed does nothing instead of throwing.
     *
     * @property[index] Where the accent sits in the document's list.
     * @property[accent] What it becomes.
     */
    @Immutable
    public data class UpdateAccent(
        public val index: Int,
        public val accent: Accent,
    ) : DocumentChange {
        init {
            require(index >= 0) { "An accent index is never negative, got $index" }
        }

        override val coalesceKey: String
            get() = "accent.$index"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.UpdateAccent, detail = accent.name)

        override fun apply(document: ThemeDocument): ThemeDocument {
            if (index !in document.accents.indices) return document
            val accents = document.accents.toMutableList()
            accents[index] = accent
            return document.copy(accents = accents)
        }
    }

    /**
     * Removes the accent at [index].
     *
     * An index the document does not have leaves it as it was.
     *
     * @property[index] Where the accent sits in the document's list.
     */
    @Immutable
    public data class RemoveAccent(
        public val index: Int,
    ) : DocumentChange {
        init {
            require(index >= 0) { "An accent index is never negative, got $index" }
        }

        override val coalesceKey: String
            get() = "accent.remove"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.RemoveAccent)

        override fun apply(document: ThemeDocument): ThemeDocument {
            if (index !in document.accents.indices) return document
            val accents = document.accents.toMutableList()
            accents.removeAt(index)
            return document.copy(accents = accents)
        }
    }

    /**
     * Pins one mode of a role to a color, or lets that mode go back to being derived.
     *
     * The other mode is left as it was. Once neither mode holds a color the pin goes away entirely.
     *
     * @property[role] The role being pinned.
     * @property[mode] Which half of the pin changes.
     * @property[argb] The color that mode takes, or null to derive it again.
     */
    @Immutable
    public data class SetPin(
        public val role: Role,
        public val mode: PinMode,
        public val argb: Argb?,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "pin.${role.name}.${mode.name}"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.Pin, detail = role.name)

        override fun apply(document: ThemeDocument): ThemeDocument {
            val current = document.pins[role]
            val light = when (mode) {
                PinMode.Light -> argb
                PinMode.Dark -> current?.light
            }
            val dark = when (mode) {
                PinMode.Light -> current?.dark
                PinMode.Dark -> argb
            }
            val pins = if (light == null && dark == null) {
                document.pins - role
            } else {
                document.pins + (role to RolePin(light = light, dark = dark))
            }
            return document.copy(pins = pins)
        }
    }

    /**
     * Lets every pinned role go back to being derived.
     */
    @Immutable
    public data object ClearPins : DocumentChange {
        override val coalesceKey: String
            get() = "pins.clear"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.ClearPins)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(pins = emptyMap())
    }

    /**
     * Picks the module the export is written against, and whether it carries the expressive
     * shapes and type. It moves the style and the spec only when it names them, so a move onto or
     * off M3 Expressive can carry its style in the same undo step.
     *
     * @property[library] The module to export for.
     * @property[expressive] Whether the export carries the expressive shapes and type.
     * @property[style] The style the pick lands on, or null to keep the document's.
     * @property[spec] The spec the pick lands on, or null to keep the document's.
     */
    @Immutable
    public data class SetLibrary(
        public val library: Library,
        public val expressive: Boolean,
        public val style: Style? = null,
        public val spec: SpecVersion? = null,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "library"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.Library, detail = library.name)

        override fun apply(document: ThemeDocument): ThemeDocument =
            document.copy(
                library = library,
                expressive = expressive,
                style = style ?: document.style,
                spec = spec ?: document.spec,
            )
    }

    /**
     * Picks the motion scheme the export carries.
     *
     * @property[motionScheme] The motion scheme to export.
     */
    @Immutable
    public data class SetMotionScheme(
        public val motionScheme: MotionSchemeChoice,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "motionScheme"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.MotionScheme, detail = motionScheme.name)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(motionScheme = motionScheme)
    }

    /**
     * Renames the exported theme. Whether the name is usable is for validation to say.
     *
     * @property[name] The new name.
     */
    @Immutable
    public data class SetThemeName(
        public val name: String,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "themeName"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.ThemeName, detail = name)

        override fun apply(document: ThemeDocument): ThemeDocument = document.copy(themeName = name)
    }

    /**
     * Moves the tones of one custom slot, or puts them back where the slot keeps them.
     *
     * @property[slot] The slot being tuned.
     * @property[tone] The tones it takes, or null to drop the override.
     */
    @Immutable
    public data class SetCustomTone(
        public val slot: CustomSlot,
        public val tone: CustomTone?,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "customTone.${slot.name}"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.CustomTone, detail = slot.name)

        override fun apply(document: ThemeDocument): ThemeDocument {
            val customTones = if (tone == null) {
                document.customTones - slot
            } else {
                document.customTones + (slot to tone)
            }
            return document.copy(customTones = customTones)
        }
    }

    /**
     * Swaps the whole document for another one, an import, a reset or an opened link.
     *
     * @property[document] The document that takes over.
     */
    @Immutable
    public data class Replace(
        public val document: ThemeDocument,
    ) : DocumentChange {
        override val coalesceKey: String
            get() = "replace"

        override val label: ChangeLabel
            get() = ChangeLabel(ChangeKind.Replace)

        override fun apply(document: ThemeDocument): ThemeDocument = this.document
    }
}
