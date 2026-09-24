package com.materialkolor.builder.kit.skin.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.resolve.CustomSlotColors
import com.materialkolor.builder.kit.headless.OverlayHost
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.Skin

/** The skin a Custom pane wears, which has no Expressive flavour. */
private val PaneSkin = Skin(Library.Custom, expressive = false)

/**
 * The Custom skin for a preview pane, drawn from the pane's own slots instead of the chrome's.
 *
 * A preview pane sits inside the builder's chrome, and a kit control reads the skin, identity,
 * tokens and motion of the nearest theme above it. Without this that is the chrome, so a kit
 * control in a Custom pane would paint in the chrome's colours. Inside this the controls wear
 * [slots] exactly as the document resolved them, and the overlays they open render into the
 * host the chrome already has, or into one of their own where there is none.
 *
 * This is for preview panes and never for the chrome. The chrome takes its Custom skin from
 * `BuilderTheme`, whose slots leave pins and AMOLED out so the builder stays readable.
 *
 * @param[slots] The pane's Custom slots in both modes.
 * @param[isDark] Which mode of [slots] to draw.
 * @param[reducedMotion] Whether to provide the reduced motion set instead of the builder tweens.
 * @param[content] The kit controls of the pane.
 */
@Composable
public fun CustomPaneTheme(
    slots: CustomSlotColors,
    isDark: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSkin provides PaneSkin,
        LocalReducedMotion provides reducedMotion,
    ) {
        OverlayHost {
            CustomSkinTheme(slots, isDark, reducedMotion, content)
        }
    }
}
