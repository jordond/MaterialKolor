package com.materialkolor.builder.preview.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.AccentFamilies
import com.materialkolor.builder.kit.skin.custom.BuilderIdentity
import com.materialkolor.builder.kit.skin.custom.LocalBuilderIdentity
import com.materialkolor.builder.preview.inspect.previewRoles
import com.materialkolor.builder.preview.split.PaneSpec

// The colors the cafe paints itself, where each comes from and the names Inspect gives them.
// The kit controls declare theirs through CustomComponent in CustomGallery.kt.

/** Declare the Custom slots an element paints, for the role usage check and Inspect. */
internal fun Modifier.previewRoles(vararg slots: CustomSlot): Modifier =
    previewRoles(*slots.map { slot -> ColorRef.OfSlot(slot) }.toTypedArray())

/** Declare the fill and the ink of [pair]. */
internal fun Modifier.previewRoles(pair: CafePair): Modifier = previewRoles(pair.fill.ref, pair.ink.ref)

/**
 * A color the cafe paints and the name Inspect gives it.
 *
 * @property[ref] A Custom slot, or an accent of the document.
 */
@Immutable
internal class CafeInk(
    val color: Color,
    val ref: ColorRef,
)

/** A fill and the ink that reads on it. */
@Immutable
internal class CafePair(
    val fill: CafeInk,
    val ink: CafeInk,
)

/** One scheme family of the Custom slots, which an accent falls back to. */
internal enum class SlotFamily(
    val color: CustomSlot,
    val onColor: CustomSlot,
    val container: CustomSlot,
    val onContainer: CustomSlot,
) {
    Primary(CustomSlot.Primary, CustomSlot.OnPrimary, CustomSlot.PrimaryContainer, CustomSlot.OnPrimaryContainer),
    Secondary(
        CustomSlot.Secondary,
        CustomSlot.OnSecondary,
        CustomSlot.SecondaryContainer,
        CustomSlot.OnSecondaryContainer,
    ),
    Tertiary(CustomSlot.Tertiary, CustomSlot.OnTertiary, CustomSlot.TertiaryContainer, CustomSlot.OnTertiaryContainer),
    Error(CustomSlot.Error, CustomSlot.OnError, CustomSlot.ErrorContainer, CustomSlot.OnErrorContainer),
}

/**
 * The eight accents the cafe has a place for, in the order of the sample's `AppThemeSeeds`, so the
 * document's accent at index i is the entry at ordinal i (D25).
 *
 * Love marks favourites, Cold and Warm tag how a drink is served and the five drinks colour their
 * categories. Past the document's accents each one falls back to [fallback].
 */
internal enum class CafeAccent(
    val fallback: SlotFamily,
) {
    Love(SlotFamily.Primary),
    Cold(SlotFamily.Secondary),
    Warm(SlotFamily.Tertiary),
    Coffee(SlotFamily.Primary),
    Matcha(SlotFamily.Primary),
    Iced(SlotFamily.Primary),
    Tea(SlotFamily.Primary),
    Chocolate(SlotFamily.Primary),
}

/**
 * The cafe's colors in one pane, the pane's Custom slots as the document resolved them and
 * whichever accents the document has.
 */
@Immutable
internal class CafeColors(
    private val identity: BuilderIdentity,
    private val accents: AccentFamilies,
    private val isDark: Boolean,
) {
    /** The color [slot] resolved to. */
    fun slot(slot: CustomSlot): CafeInk = CafeInk(identity[slot], ColorRef.OfSlot(slot))

    /** The fill [fill] with the ink [ink] on it. */
    fun pair(
        fill: CustomSlot,
        ink: CustomSlot,
    ): CafePair = CafePair(slot(fill), slot(ink))

    /** The container of [accent] and the ink on it, for something the size of a tag or larger. */
    fun container(accent: CafeAccent): CafePair =
        accentPair(accent, AccentPart.Container, AccentPart.OnContainer)
            ?: pair(accent.fallback.container, accent.fallback.onContainer)

    /** The color of [accent] and the ink on it, for a small mark. */
    fun mark(accent: CafeAccent): CafePair =
        accentPair(accent, AccentPart.Color, AccentPart.OnColor)
            ?: pair(accent.fallback.color, accent.fallback.onColor)

    private fun accentPair(
        accent: CafeAccent,
        fill: AccentPart,
        ink: AccentPart,
    ): CafePair? =
        if (accent.ordinal < accents.families.size) {
            CafePair(accentInk(AccentSlot(accent.ordinal, fill)), accentInk(AccentSlot(accent.ordinal, ink)))
        } else {
            null
        }

    private fun accentInk(slot: AccentSlot): CafeInk = CafeInk(accents[slot, isDark].toColor(), ColorRef.OfAccent(slot))

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is CafeColors && identity == other.identity && accents == other.accents && isDark == other.isDark)

    override fun hashCode(): Int = 31 * (31 * identity.hashCode() + accents.hashCode()) + isDark.hashCode()
}

/** The cafe's colors for [spec], read inside the pane's Custom theme. */
@Composable
internal fun rememberCafeColors(spec: PaneSpec): CafeColors {
    val identity = LocalBuilderIdentity.current
    val accents = spec.result.accents
    return remember(identity, accents, spec.isDark) { CafeColors(identity, accents, spec.isDark) }
}
