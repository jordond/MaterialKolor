package com.materialkolor.builder.kit.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where Medium starts.
 */
public val MediumBreakpoint: Dp = 600.dp

/**
 * Where the poster earns a docked panel instead of a rail.
 */
public val DockedPosterBreakpoint: Dp = 840.dp

/**
 * Where Expanded starts.
 */
public val ExpandedBreakpoint: Dp = 1200.dp

/**
 * Where the canvas stops growing with the window.
 */
public val WideBreakpoint: Dp = 1600.dp

/**
 * How wide the canvas content is allowed to get past [WideBreakpoint].
 */
public val CanvasContentCap: Dp = 1400.dp

/**
 * Below this height a window held with a coarse pointer is a phone on its side, and gets the sheet.
 */
public val ShortHeightBreakpoint: Dp = 480.dp // b-209a

/**
 * The three widths the builder lays itself out for.
 *
 * These come from the container the builder is drawn in, not from the window and not from
 * `material3-window-size-class`, so the non Material3 skins stay free of Material3 and browser
 * zoom changes the class the way it should.
 */
public enum class WindowClass {
    /**
     * Phones. The poster lives in a bottom sheet.
     */
    Compact,

    /**
     * Tablets and small windows. The poster is a rail or a narrow docked panel.
     */
    Medium,

    /**
     * Desktop. The poster is a full docked panel.
     */
    Expanded,

    ;

    public companion object {
        /**
         * The class a container of [widthDp] belongs to.
         */
        public fun of(widthDp: Dp): WindowClass =
            when {
                widthDp < MediumBreakpoint -> Compact
                widthDp < ExpandedBreakpoint -> Medium
                else -> Expanded
            }
    }
}

/**
 * How the poster panel is shown, which is the one piece of geometry that changes at four widths
 * rather than three. A phone on its side is Medium by width but still gets the sheet (D38).
 */
public enum class PosterMode {
    /**
     * Inside the bottom sheet, with detents.
     */
    Sheet,

    /**
     * A 72 dp seed strip that opens over the canvas.
     */
    Rail72,

    /**
     * Docked at 320 dp, collapsible to the rail.
     */
    Docked320,

    /**
     * Docked at 400 dp, collapsible to the rail.
     */
    Docked400,

    ;

    public companion object {
        /**
         * The poster treatment for a container of [widthDp] by [heightDp] in [windowClass].
         *
         * A window shorter than [ShortHeightBreakpoint] with a [coarsePointer] is a phone on its side,
         * and takes the sheet whatever its width, since a rail or a docked panel would eat half of it.
         * A short window with a mouse keeps its width's treatment, where a scrolling poster works.
         */
        public fun of(
            windowClass: WindowClass,
            widthDp: Dp,
            heightDp: Dp,
            coarsePointer: Boolean,
        ): PosterMode =
            if (heightDp < ShortHeightBreakpoint && coarsePointer) {
                Sheet // b-209a
            } else {
                when (windowClass) {
                    WindowClass.Compact -> Sheet
                    WindowClass.Medium -> if (widthDp >= DockedPosterBreakpoint) Docked320 else Rail72
                    WindowClass.Expanded -> Docked400
                }
            }
    }
}

/**
 * Everything the shell and the controls need to know about the space they are in.
 *
 * The size and the pointer are the whole state. Everything else is read off them, so a `copy` to a
 * new width cannot leave a class or a poster mode behind that belongs to the old one.
 *
 * @property[widthDp] The container width.
 * @property[heightDp] The container height, which the poster mode and its peek read in short landscape.
 * @property[coarsePointer] True for touch and pen, false for a mouse or a trackpad.
 */
@Immutable
public data class LayoutInfo(
    public val widthDp: Dp,
    public val heightDp: Dp,
    public val coarsePointer: Boolean = false,
) {
    /**
     * Which of the three layouts applies.
     */
    public val windowClass: WindowClass
        get() = WindowClass.of(widthDp)

    /**
     * How the poster panel is shown.
     */
    public val posterMode: PosterMode
        get() = PosterMode.of(windowClass, widthDp, heightDp, coarsePointer) // b-209a

    /**
     * The smallest a tappable thing is allowed to be.
     *
     * A phone always gets 44 dp. Anywhere else a coarse pointer buys the same 44 dp, and a mouse
     * lets the chrome tighten up.
     */
    public val minTouchTarget: Dp
        get() = when (windowClass) {
            WindowClass.Compact -> 44.dp
            WindowClass.Medium -> if (coarsePointer) 44.dp else 32.dp
            WindowClass.Expanded -> if (coarsePointer) 44.dp else 24.dp
        }

    /**
     * The smallest a primary action is allowed to be. Only Compact asks for more than the rest.
     */
    public val primaryTouchTarget: Dp
        get() = if (windowClass == WindowClass.Compact) 48.dp else minTouchTarget

    /**
     * How wide the canvas content may grow.
     *
     * Past [WideBreakpoint] it stops at [CanvasContentCap] so a preview never stretches edge to
     * edge on a big monitor. Below that it takes what it is given.
     */
    public val canvasMaxWidth: Dp
        get() = if (widthDp >= WideBreakpoint) CanvasContentCap else Dp.Infinity

    public companion object {
        /**
         * The layout for a container of [widthDp] by [heightDp].
         */
        public fun of(
            widthDp: Dp,
            heightDp: Dp,
            coarsePointer: Boolean = false,
        ): LayoutInfo =
            LayoutInfo(
                widthDp = widthDp,
                heightDp = heightDp,
                coarsePointer = coarsePointer,
            )
    }
}

/**
 * The space the surrounding tree is laid out in.
 *
 * A plain composition local, not a static one, so dragging a window edge only recomposes the parts
 * that actually read the size.
 */
public val LocalLayout: ProvidableCompositionLocal<LayoutInfo> = compositionLocalOf {
    error("No LayoutInfo provided")
}

/**
 * Measures [content] and provides [LocalLayout] from the measured size.
 *
 * Put this once, around the workspace. Everything below reads the container, which is what makes
 * the split preview and the embedded canvas respond to their own box rather than the window.
 */
@Composable
public fun ProvideBuilderLayout(
    coarsePointer: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier) {
        val layout = LayoutInfo.of(maxWidth, maxHeight, coarsePointer)
        CompositionLocalProvider(LocalLayout provides layout, content = content)
    }
}
