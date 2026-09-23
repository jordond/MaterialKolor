package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * How wide an app screen is laid out for this device, one width inside each window class so the
 * screen re-lays itself out rather than scaling.
 */
public val DeviceWidth.screenWidth: Dp
    get() = when (this) {
        DeviceWidth.Phone -> 412.dp
        DeviceWidth.Tablet -> 840.dp
        DeviceWidth.Desktop -> 1280.dp
    }

/**
 * An app screen laid out at the dock's device [width], with an optional bezel around it.
 *
 * The bezel is off by default. When it is on, a phone and a tablet get a rounded hardware edge and a
 * desktop a thin window edge, all drawn from the skin's neutral tokens so the frame never competes
 * with the theme inside it.
 *
 * @param[width] The device the screen is laid out for.
 * @param[enabled] Whether the bezel shows.
 * @param[modifier] Applied to the frame, or to the screen when there is no bezel.
 * @param[content] The app screen.
 */
@Composable
public fun DeviceFrame(
    width: DeviceWidth,
    enabled: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier.width(width.screenWidth)) { content() }
        return
    }

    val tokens = LocalBuilderTokens.current
    val bezel = width.bezel(tokens)
    val screenRadius = width.screenRadius(tokens)
    val screenShape = RoundedCornerShape(screenRadius)
    val frameShape = RoundedCornerShape(screenRadius + bezel)
    Box(
        modifier = modifier
            .clip(frameShape)
            .background(tokens.panelRaised)
            .border(WidgetOutlineWidth, tokens.borderStrong, frameShape)
            .padding(bezel),
    ) {
        Box(
            modifier = Modifier
                .width(width.screenWidth)
                .clip(screenShape)
                .border(WidgetOutlineWidth, tokens.border, screenShape),
        ) { content() }
    }
}

/** How thick the bezel is around the screen. */
private fun DeviceWidth.bezel(tokens: BuilderTokens): Dp =
    when (this) {
        DeviceWidth.Phone -> tokens.spacing.medium
        DeviceWidth.Tablet -> tokens.spacing.large
        DeviceWidth.Desktop -> tokens.spacing.small
    }

/** The corner radius of the screen inside the bezel. */
private fun DeviceWidth.screenRadius(tokens: BuilderTokens): Dp =
    when (this) {
        DeviceWidth.Phone -> tokens.radius.large
        DeviceWidth.Tablet -> tokens.radius.medium
        DeviceWidth.Desktop -> tokens.radius.small
    }
