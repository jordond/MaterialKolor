package com.materialkolor.builder.kit.skin.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.CustomSlotColors
import com.materialkolor.builder.kit.icon.LucideIcons
import com.materialkolor.builder.kit.skin.ProvideSkinLocals
import com.materialkolor.builder.kit.skin.StatusColors
import com.materialkolor.builder.kit.skin.builderCodePalette
import com.materialkolor.builder.kit.skin.builderMotion
import com.materialkolor.builder.kit.skin.headless.ScrimAlpha
import com.materialkolor.builder.kit.token.BuilderTokens

/**
 * The builder's own identity, every slot of the Custom target in one mode (D5).
 *
 * The Custom skin is the builder wearing the same recipe it exports, so its widgets reach for the
 * pressed, raised, sunken and shadow slots here where the neutral tokens have no name for them.
 */
@Immutable
public class BuilderIdentity internal constructor(
    colors: Map<CustomSlot, Color>,
) {
    private val colors: Map<CustomSlot, Color> = colors.toMap()

    /**
     * The colour [slot] resolved to.
     */
    public operator fun get(slot: CustomSlot): Color = colors.getValue(slot)

    override fun equals(other: Any?): Boolean = this === other || (other is BuilderIdentity && colors == other.colors)

    override fun hashCode(): Int = colors.hashCode()

    override fun toString(): String = "BuilderIdentity($colors)"
}

/**
 * The identity of the surrounding Custom skin.
 *
 * Only the Custom skin provides it, so only Custom widgets may read it.
 */
public val LocalBuilderIdentity: ProvidableCompositionLocal<BuilderIdentity> = staticCompositionLocalOf {
    error("No BuilderIdentity provided, only the Custom skin has one")
}

/**
 * The Custom skin, the builder's own identity drawn from the Custom target's slots.
 *
 * `BuilderTheme` hands it the chrome's slots, which come from the chrome schemes with no pins, no
 * AMOLED and no custom tones, so the builder stays readable whatever the document does to its own
 * slots. `CustomPaneTheme` hands it a pane's slots exactly as the document resolved them.
 *
 * @param[slots] The chrome's or a pane's Custom slots in both modes.
 * @param[isDark] Which mode of [slots] to draw.
 * @param[reducedMotion] Whether to provide the reduced motion set instead of the builder tweens.
 * @param[content] The builder.
 */
@Composable
internal fun CustomSkinTheme(
    slots: CustomSlotColors,
    isDark: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val identity = remember(slots, isDark) {
        BuilderIdentity(slots.mode(isDark).mapValues { (_, argb) -> argb.toColor() })
    }
    val tokens = remember(identity, isDark) { identity.builderTokens(StatusColors.of(isDark)) }
    val motion = remember(reducedMotion) { builderMotion(reducedMotion) }
    CompositionLocalProvider(LocalBuilderIdentity provides identity) {
        ProvideSkinLocals(tokens, motion, LucideIcons, content)
    }
}

private fun BuilderIdentity.builderTokens(status: StatusColors): BuilderTokens =
    BuilderTokens(
        canvas = this[CustomSlot.SurfaceSunken],
        panel = this[CustomSlot.Surface],
        panelRaised = this[CustomSlot.SurfaceRaised],
        border = this[CustomSlot.BorderSoft],
        borderStrong = this[CustomSlot.BorderStrong],
        textStrong = this[CustomSlot.TextStrong],
        textMuted = this[CustomSlot.TextMuted],
        accent = this[CustomSlot.Primary],
        onAccent = this[CustomSlot.OnPrimary],
        focus = this[CustomSlot.FocusRing],
        codeBackground = this[CustomSlot.SurfaceSunken],
        codePalette = builderCodePalette(
            plain = this[CustomSlot.TextStrong],
            muted = this[CustomSlot.TextMuted],
            primary = this[CustomSlot.Primary],
            secondary = this[CustomSlot.Secondary],
            tertiary = this[CustomSlot.Tertiary],
            status = status,
        ),
        success = status.success,
        warning = status.warning,
        danger = this[CustomSlot.Error],
        scrim = this[CustomSlot.SurfaceSunken].copy(alpha = ScrimAlpha),
        iconSize = 18.dp,
    )
