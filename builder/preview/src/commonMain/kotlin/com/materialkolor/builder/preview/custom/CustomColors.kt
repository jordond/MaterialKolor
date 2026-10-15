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

// The colors the Custom sample app paints itself, where each comes from and the names Inspect
// gives them. The kit controls declare theirs through CustomComponent in CustomGallery.kt.

/**
 * Declare the Custom slots an element paints, for the role usage check and Inspect.
 */
internal fun Modifier.previewRoles(vararg slots: CustomSlot): Modifier =
    previewRoles(*slots.map { slot -> ColorRef.OfSlot(slot) }.toTypedArray())

/**
 * Declare the fill and the ink of [pair].
 */
internal fun Modifier.previewRoles(pair: CustomPair): Modifier = previewRoles(pair.fill.ref, pair.ink.ref)

/**
 * A color the app paints and the name Inspect gives it.
 *
 * @property[ref] A Custom slot, or an accent of the document.
 */
@Immutable
internal class CustomInk(
    val color: Color,
    val ref: ColorRef,
)

/**
 * A fill and the ink that reads on it.
 */
@Immutable
internal class CustomPair(
    val fill: CustomInk,
    val ink: CustomInk,
)

/**
 * The app's colors in one pane, the pane's Custom slots as the document resolved them and
 * whichever accents the document has.
 */
@Immutable
internal class CustomColors(
    private val identity: BuilderIdentity,
    private val accents: AccentFamilies,
    private val isDark: Boolean,
) {
    /**
     * The color [slot] resolved to.
     */
    fun slot(slot: CustomSlot): CustomInk = CustomInk(identity[slot], ColorRef.OfSlot(slot))

    /**
     * The fill [fill] with the ink [ink] on it.
     */
    fun pair(
        fill: CustomSlot,
        ink: CustomSlot,
    ): CustomPair = CustomPair(slot(fill), slot(ink))

    /**
     * The container of the document's accent at [index] and the ink on it, or null when the
     * document has fewer accents.
     */
    fun accentContainer(index: Int): CustomPair? =
        if (index in accents.families.indices) {
            CustomPair(
                accentInk(AccentSlot(index, AccentPart.Container)),
                accentInk(AccentSlot(index, AccentPart.OnContainer)),
            )
        } else {
            null
        }

    private fun accentInk(slot: AccentSlot): CustomInk =
        CustomInk(accents[slot, isDark].toColor(), ColorRef.OfAccent(slot))

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is CustomColors && identity == other.identity && accents == other.accents && isDark == other.isDark)

    override fun hashCode(): Int = 31 * (31 * identity.hashCode() + accents.hashCode()) + isDark.hashCode()
}

/**
 * The app's colors for [spec], read inside the pane's Custom theme.
 */
@Composable
internal fun rememberCustomColors(spec: PaneSpec): CustomColors {
    val identity = LocalBuilderIdentity.current
    val accents = spec.result.accents
    return remember(identity, accents, spec.isDark) { CustomColors(identity, accents, spec.isDark) }
}
