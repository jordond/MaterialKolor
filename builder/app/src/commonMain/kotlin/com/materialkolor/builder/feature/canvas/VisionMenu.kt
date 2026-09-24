package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.canvas_vision_achromatopsia
import com.materialkolor.builder.generated.resources.canvas_vision_button
import com.materialkolor.builder.generated.resources.canvas_vision_deuteranopia
import com.materialkolor.builder.generated.resources.canvas_vision_none
import com.materialkolor.builder.generated.resources.canvas_vision_protanopia
import com.materialkolor.builder.generated.resources.canvas_vision_tritanopia
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.canvas.VisionMatrices
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Tags the name of the active simulation beside the button, which assistive tech skips. */
internal const val VISION_LABEL_TAG: String = "canvas-vision-label"

/**
 * The dock's Vision menu, None and the four simulations (F-25).
 *
 * The button names the simulation in use, and while one is on its name shows beside the button too,
 * so nobody mistakes the filtered canvas for the theme.
 *
 * @param[vision] The simulation in use.
 * @param[onPick] Called with the simulation someone picks.
 * @param[modifier] Applied to the button and its label.
 */
@Composable
internal fun VisionMenu(
    vision: VisionSimulation,
    onPick: (VisionSimulation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val names = VisionSimulation.entries.associateWith { option -> stringResource(option.title) }
    val items = VisionSimulation.entries.map { option ->
        BuilderMenuItem(
            label = names.getValue(option),
            onClick = { onPick(option) },
            icon = if (option == vision) IconId.Check else null,
        )
    }
    val active = vision != VisionSimulation.None
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderMenu(expanded = open, onDismissRequest = { open = false }, items = items) {
            BuilderIconButton(
                onClick = { open = true },
                icon = IconId.Vision,
                contentDescription = stringResource(Res.string.canvas_vision_button, names.getValue(vision)),
                emphasis = if (active) Emphasis.Primary else Emphasis.Subtle,
            )
        }
        if (active) {
            // The button already reads the name out.
            BuilderText(
                text = names.getValue(vision),
                modifier = Modifier.clearAndSetSemantics { testTag = VISION_LABEL_TAG },
                style = BuilderTextStyle.Label,
                maxLines = 1,
            )
        }
    }
}

/** The color matrix the canvas is drawn through for this simulation, a fresh one each call. */
internal fun VisionSimulation.matrix(): ColorMatrix? =
    when (this) {
        VisionSimulation.None -> null
        VisionSimulation.Protanopia -> VisionMatrices.colorMatrix(VisionMatrices.Protanopia)
        VisionSimulation.Deuteranopia -> VisionMatrices.colorMatrix(VisionMatrices.Deuteranopia)
        VisionSimulation.Tritanopia -> VisionMatrices.colorMatrix(VisionMatrices.Tritanopia)
        VisionSimulation.Achromatopsia -> VisionMatrices.colorMatrix(VisionMatrices.Achromatopsia)
    }

/** What the simulation is called in the menu and on the dock. */
private val VisionSimulation.title: StringResource
    get() = when (this) {
        VisionSimulation.None -> Res.string.canvas_vision_none
        VisionSimulation.Protanopia -> Res.string.canvas_vision_protanopia
        VisionSimulation.Deuteranopia -> Res.string.canvas_vision_deuteranopia
        VisionSimulation.Tritanopia -> Res.string.canvas_vision_tritanopia
        VisionSimulation.Achromatopsia -> Res.string.canvas_vision_achromatopsia
    }
