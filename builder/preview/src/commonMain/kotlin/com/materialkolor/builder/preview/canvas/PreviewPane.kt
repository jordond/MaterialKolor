package com.materialkolor.builder.preview.canvas

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.text.TextStyle
import com.composeunstyled.theme.ThemeComposableV2
import com.composeunstyled.theme.ThemeToken
import com.composeunstyled.theme.buildThemeV2
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.RoleEntry
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.unstyled.MaterialKolorTokens
import androidx.compose.material3.LocalContentColor as MaterialContentColor
import androidx.compose.material3.LocalTextStyle as MaterialTextStyle
import com.composeunstyled.LocalContentColor as UnstyledContentColor
import com.composeunstyled.LocalTextStyle as UnstyledTextStyle
import com.composeunstyled.theme.ColorScheme as UnstyledColorScheme

/**
 * One copy of the preview, dressed in a fresh theme of the library [LocalSkin] names.
 *
 * Every color comes from `spec.result`, the role tables for Material 3 and Unstyled so AMOLED and
 * the document's pins show, and the Custom slots for Custom. Nothing is generated here. Before the
 * library theme goes on, the pane sets content color, text style, selection colors and indication
 * for every library, so nothing the builder's own chrome provides reaches the preview, and whatever
 * the pane provides ends at its edge. Material's themes get the library's own typography and
 * shapes too, since left out they would take the chrome's.
 *
 * @param[spec] The result, mode and filter to draw.
 * @param[modifier] Applied to the pane, outside its filter and background.
 * @param[content] The screen.
 */
@Composable
public fun PreviewPane(
    spec: PaneSpec,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LocalCompositionProbe.current?.invoke("PreviewPane/${spec.label}")
    val skin = LocalSkin.current
    val roles = spec.result.roles.mode(spec.isDark)
    // Only a Custom pane reads the Custom slots, so no other pane works them out.
    val identity = remember(skin.library, spec.result, spec.isDark) {
        if (skin.library == Library.Custom) PreviewIdentity.of(spec) else null
    }
    val ink = remember(roles, identity) {
        if (identity == null) {
            PaneInk(roles.color(Role.Background), roles.color(Role.OnBackground), roles.color(Role.Primary))
        } else {
            PaneInk(identity[CustomSlot.Surface], identity[CustomSlot.OnSurface], identity[CustomSlot.Primary])
        }
    }
    Box(
        modifier = modifier
            .paneFilter(spec.filter)
            .background(ink.background),
        propagateMinConstraints = true,
    ) {
        PaneLocals(ink) {
            when (skin.library) {
                // b-306
                Library.Material3 -> MaterialPane(roles, skin.expressive, spec.result.document.motionScheme, content)
                Library.Unstyled -> UnstyledPane(roles, spec.isDark, ink, content)
                Library.Fluent -> content() // stub, the Fluent theme lands with B-405
                Library.Custom -> CompositionLocalProvider(
                    LocalPreviewIdentity provides checkNotNull(identity),
                    content = content,
                )
            }
        }
    }
}

/**
 * Every slot of the Custom target in the pane's mode, for Custom screens to draw with.
 *
 * The kit has its own for the builder's chrome. This one carries the document's slots, pins, AMOLED
 * and custom tones included.
 */
@Immutable
internal class PreviewIdentity(
    colors: Map<CustomSlot, Color>,
) {
    private val colors: Map<CustomSlot, Color> = colors.toMap()

    /** The color [slot] resolved to. */
    operator fun get(slot: CustomSlot): Color = colors.getValue(slot)

    override fun equals(other: Any?): Boolean = this === other || (other is PreviewIdentity && colors == other.colors)

    override fun hashCode(): Int = colors.hashCode()

    override fun toString(): String = "PreviewIdentity($colors)"

    companion object {
        /** The Custom slots of [spec]'s result in its mode. */
        fun of(spec: PaneSpec): PreviewIdentity =
            PreviewIdentity(
                spec.result.customSlots
                    .mode(spec.isDark)
                    .mapValues { (_, argb) -> argb.toColor() },
            )
    }
}

/** The Custom slots of the surrounding pane, which only a Custom pane provides. */
internal val LocalPreviewIdentity: ProvidableCompositionLocal<PreviewIdentity> = staticCompositionLocalOf {
    error("No PreviewIdentity provided, only a Custom pane has one")
}

/**
 * The indication every pane hands its content, which draws nothing.
 *
 * Compose Unstyled falls back to an indication `clickable` refuses, and the chrome may provide
 * anything at all, so the pane always provides one it knows `clickable` takes. Material's theme
 * replaces it with its ripple.
 */
internal object PreviewIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = -1
}

/** The three colors every pane sets up before its library theme, whatever the library. */
@Immutable
private class PaneInk(
    val background: Color,
    val content: Color,
    val accent: Color,
)

@Composable
private fun PaneLocals(
    ink: PaneInk,
    content: @Composable () -> Unit,
) {
    val selection =
        remember(ink) { TextSelectionColors(handleColor = ink.accent, backgroundColor = ink.accent.copy(alpha = 0.4f)) }
    CompositionLocalProvider(
        LocalIndication provides PreviewIndication,
        LocalTextSelectionColors provides selection,
        MaterialContentColor provides ink.content,
        MaterialTextStyle provides TextStyle.Default,
        UnstyledContentColor provides ink.content,
        UnstyledTextStyle provides TextStyle.Default,
        content = content,
    )
}

/**
 * A Material theme, expressive or not, whose scheme is all 48 roles from the role table.
 *
 * Typography and shapes are the library's defaults, the ones an app gets from a bare theme. Both
 * themes read them from the theme around them when they are not passed, and around the preview
 * that is the chrome with its brand face.
 */
@Composable
private fun MaterialPane(
    roles: Map<Role, RoleEntry>,
    expressive: Boolean,
    motion: MotionSchemeChoice, // b-306
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(roles) { roles.toColorScheme() }
    val typography = remember { Typography() }
    val shapes = remember { Shapes() }
    if (expressive) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            // b-306
            // The document's motion scheme, so the preview moves the way the export will.
            motionScheme = if (motion == MotionSchemeChoice.Standard) {
                MotionScheme.standard()
            } else {
                MotionScheme.expressive()
            },
            shapes = shapes,
            typography = typography,
            content = content,
        )
    } else {
        MaterialTheme(colorScheme = colorScheme, shapes = shapes, typography = typography, content = content)
    }
}

/**
 * An Unstyled theme whose MaterialKolor tokens are the role table.
 *
 * Only the tokens with a role behind them are written, so a screen that reaches for one of the
 * extra Android tokens fails loudly rather than drawing a color the document never had.
 */
@Composable
private fun UnstyledPane(
    roles: Map<Role, RoleEntry>,
    isDark: Boolean,
    ink: PaneInk,
    content: @Composable () -> Unit,
) {
    val theme: ThemeComposableV2 = remember(roles, ink) {
        val values = roles.toThemeValues()
        buildThemeV2 {
            properties[MaterialKolorTokens.colors] = values
            defaultIndication = PreviewIndication
            defaultContentColor = ink.content
        }
    }
    theme(colorScheme = if (isDark) UnstyledColorScheme.Dark else UnstyledColorScheme.Light, content = content)
}

/** Draw the pane through a layer whose paint carries [filter], or leave it be when there is none. */
private fun Modifier.paneFilter(filter: ColorMatrix?): Modifier {
    if (filter == null) return this
    return drawWithCache {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(filter) }
        onDrawWithContent {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(size.toRect(), paint)
                drawContent()
                canvas.restore()
            }
        }
    }
}

private fun Map<Role, RoleEntry>.color(role: Role): Color = getValue(role).argb.toColor()

private fun Map<Role, RoleEntry>.toColorScheme(): ColorScheme =
    ColorScheme(
        primary = color(Role.Primary),
        onPrimary = color(Role.OnPrimary),
        primaryContainer = color(Role.PrimaryContainer),
        onPrimaryContainer = color(Role.OnPrimaryContainer),
        inversePrimary = color(Role.InversePrimary),
        secondary = color(Role.Secondary),
        onSecondary = color(Role.OnSecondary),
        secondaryContainer = color(Role.SecondaryContainer),
        onSecondaryContainer = color(Role.OnSecondaryContainer),
        tertiary = color(Role.Tertiary),
        onTertiary = color(Role.OnTertiary),
        tertiaryContainer = color(Role.TertiaryContainer),
        onTertiaryContainer = color(Role.OnTertiaryContainer),
        background = color(Role.Background),
        onBackground = color(Role.OnBackground),
        surface = color(Role.Surface),
        onSurface = color(Role.OnSurface),
        surfaceVariant = color(Role.SurfaceVariant),
        onSurfaceVariant = color(Role.OnSurfaceVariant),
        surfaceTint = color(Role.SurfaceTint),
        inverseSurface = color(Role.InverseSurface),
        inverseOnSurface = color(Role.InverseOnSurface),
        error = color(Role.Error),
        onError = color(Role.OnError),
        errorContainer = color(Role.ErrorContainer),
        onErrorContainer = color(Role.OnErrorContainer),
        outline = color(Role.Outline),
        outlineVariant = color(Role.OutlineVariant),
        scrim = color(Role.Scrim),
        surfaceBright = color(Role.SurfaceBright),
        surfaceContainer = color(Role.SurfaceContainer),
        surfaceContainerHigh = color(Role.SurfaceContainerHigh),
        surfaceContainerHighest = color(Role.SurfaceContainerHighest),
        surfaceContainerLow = color(Role.SurfaceContainerLow),
        surfaceContainerLowest = color(Role.SurfaceContainerLowest),
        surfaceDim = color(Role.SurfaceDim),
        primaryFixed = color(Role.PrimaryFixed),
        primaryFixedDim = color(Role.PrimaryFixedDim),
        onPrimaryFixed = color(Role.OnPrimaryFixed),
        onPrimaryFixedVariant = color(Role.OnPrimaryFixedVariant),
        secondaryFixed = color(Role.SecondaryFixed),
        secondaryFixedDim = color(Role.SecondaryFixedDim),
        onSecondaryFixed = color(Role.OnSecondaryFixed),
        onSecondaryFixedVariant = color(Role.OnSecondaryFixedVariant),
        tertiaryFixed = color(Role.TertiaryFixed),
        tertiaryFixedDim = color(Role.TertiaryFixedDim),
        onTertiaryFixed = color(Role.OnTertiaryFixed),
        onTertiaryFixedVariant = color(Role.OnTertiaryFixedVariant),
    )

/** Every role as the MaterialKolor token of the same name. */
private fun Map<Role, RoleEntry>.toThemeValues(): Map<ThemeToken<Color>, Color> =
    Role.entries.associate { role -> role.token to color(role) }

private val Role.token: ThemeToken<Color>
    get() = when (this) {
        Role.Primary -> MaterialKolorTokens.primary
        Role.OnPrimary -> MaterialKolorTokens.onPrimary
        Role.PrimaryContainer -> MaterialKolorTokens.primaryContainer
        Role.OnPrimaryContainer -> MaterialKolorTokens.onPrimaryContainer
        Role.InversePrimary -> MaterialKolorTokens.inversePrimary
        Role.Secondary -> MaterialKolorTokens.secondary
        Role.OnSecondary -> MaterialKolorTokens.onSecondary
        Role.SecondaryContainer -> MaterialKolorTokens.secondaryContainer
        Role.OnSecondaryContainer -> MaterialKolorTokens.onSecondaryContainer
        Role.Tertiary -> MaterialKolorTokens.tertiary
        Role.OnTertiary -> MaterialKolorTokens.onTertiary
        Role.TertiaryContainer -> MaterialKolorTokens.tertiaryContainer
        Role.OnTertiaryContainer -> MaterialKolorTokens.onTertiaryContainer
        Role.Background -> MaterialKolorTokens.background
        Role.OnBackground -> MaterialKolorTokens.onBackground
        Role.Surface -> MaterialKolorTokens.surface
        Role.OnSurface -> MaterialKolorTokens.onSurface
        Role.SurfaceVariant -> MaterialKolorTokens.surfaceVariant
        Role.OnSurfaceVariant -> MaterialKolorTokens.onSurfaceVariant
        Role.SurfaceTint -> MaterialKolorTokens.surfaceTint
        Role.InverseSurface -> MaterialKolorTokens.inverseSurface
        Role.InverseOnSurface -> MaterialKolorTokens.inverseOnSurface
        Role.Error -> MaterialKolorTokens.error
        Role.OnError -> MaterialKolorTokens.onError
        Role.ErrorContainer -> MaterialKolorTokens.errorContainer
        Role.OnErrorContainer -> MaterialKolorTokens.onErrorContainer
        Role.Outline -> MaterialKolorTokens.outline
        Role.OutlineVariant -> MaterialKolorTokens.outlineVariant
        Role.Scrim -> MaterialKolorTokens.scrim
        Role.SurfaceBright -> MaterialKolorTokens.surfaceBright
        Role.SurfaceDim -> MaterialKolorTokens.surfaceDim
        Role.SurfaceContainer -> MaterialKolorTokens.surfaceContainer
        Role.SurfaceContainerHigh -> MaterialKolorTokens.surfaceContainerHigh
        Role.SurfaceContainerHighest -> MaterialKolorTokens.surfaceContainerHighest
        Role.SurfaceContainerLow -> MaterialKolorTokens.surfaceContainerLow
        Role.SurfaceContainerLowest -> MaterialKolorTokens.surfaceContainerLowest
        Role.PrimaryFixed -> MaterialKolorTokens.primaryFixed
        Role.PrimaryFixedDim -> MaterialKolorTokens.primaryFixedDim
        Role.OnPrimaryFixed -> MaterialKolorTokens.onPrimaryFixed
        Role.OnPrimaryFixedVariant -> MaterialKolorTokens.onPrimaryFixedVariant
        Role.SecondaryFixed -> MaterialKolorTokens.secondaryFixed
        Role.SecondaryFixedDim -> MaterialKolorTokens.secondaryFixedDim
        Role.OnSecondaryFixed -> MaterialKolorTokens.onSecondaryFixed
        Role.OnSecondaryFixedVariant -> MaterialKolorTokens.onSecondaryFixedVariant
        Role.TertiaryFixed -> MaterialKolorTokens.tertiaryFixed
        Role.TertiaryFixedDim -> MaterialKolorTokens.tertiaryFixedDim
        Role.OnTertiaryFixed -> MaterialKolorTokens.onTertiaryFixed
        Role.OnTertiaryFixedVariant -> MaterialKolorTokens.onTertiaryFixedVariant
    }
