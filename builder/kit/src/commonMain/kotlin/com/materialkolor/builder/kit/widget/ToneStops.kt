package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.picker_tone_stop
import com.materialkolor.builder.kit.generated.resources.picker_tones
import com.materialkolor.builder.kit.generated.resources.picker_tones_chroma
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.hct.Hct
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * A row of tones from 0 to 100 by 10 at the picker's hue and the chroma it was asked for, not the
 * chroma the current tone reaches, so pressing tone 90 and then tone 40 comes back to the same
 * color. Pressing a stop sets the tone as one discrete edit.
 *
 * A caret over the row marks the exact tone and the nearest stop has a ring. The stops' colors, the
 * caret and the ring are read while drawing, so a drag elsewhere in the picker only redraws them.
 * The row reads as a group named "Tones", each stop as "Tone 40".
 */
@Composable
internal fun ToneStops(
    picker: PickerState,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val title = stringResource(Res.string.picker_tones)
    val asText = LocalFoldsStateIntoName.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderText(
                text = title.uppercase(),
                modifier = Modifier.clearAndSetSemantics {},
                style = BuilderTextStyle.Value,
                emphasis = Emphasis.Secondary,
                maxLines = 1,
            )
            AskedChroma(picker)
        }
        ToneCaret(picker)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(tokens.radius.medium))
                .semantics {
                    isTraversalGroup = true
                    roleLessName(title, asText)
                },
        ) {
            for (tone in StopTones) ToneStop(picker, tone, Modifier.weight(1f))
        }
    }
}

/**
 * "At chroma 58", the chroma the stops are drawn at, composed again only when the number changes.
 */
@Composable
private fun AskedChroma(picker: PickerState) {
    val chroma by remember(picker) { derivedStateOf { picker.chroma.roundToInt() } }
    BuilderText(
        text = stringResource(Res.string.picker_tones_chroma, chroma),
        style = BuilderTextStyle.Value,
        emphasis = Emphasis.Secondary,
        maxLines = 1,
    )
}

/**
 * A small caret pointing down at the exact tone, over the middle of the stop that tone falls in.
 */
@Composable
private fun ToneCaret(picker: PickerState) {
    val tokens = LocalBuilderTokens.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val path = remember { Path() }
    Canvas(Modifier.fillMaxWidth().height(tokens.spacing.small)) {
        val stop = size.width / StopTones.size
        val along = (picker.tone / StopGap + 0.5).toFloat() * stop
        val x = if (isRtl) size.width - along else along
        val half = size.height * CaretHalfWidth
        path.reset()
        path.moveTo(x - half, 0f)
        path.lineTo(x + half, 0f)
        path.lineTo(x, size.height)
        path.close()
        drawPath(path, tokens.textStrong)
    }
}

@Composable
private fun ToneStop(
    picker: PickerState,
    tone: Int,
    modifier: Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val interactions = remember { MutableInteractionSource() }
    val name = stringResource(Res.string.picker_tone_stop, tone)
    val ring = tokens.spacing.extraSmall / 2
    Box(
        modifier
            .height(LocalLayout.current.minTouchTarget)
            .controlRing(interactions, RoundedCornerShape(tokens.radius.small), offset = -ring * 2)
            .clickable(interactions, indication = null, role = Role.Button) {
                picker.set(HctChannel.Tone, tone.toDouble(), EditPhase.Discrete)
            }.semantics { contentDescription = name }
            .drawBehind {
                val hue = picker.hue
                val chroma = picker.chroma
                drawRect(Color(Hct.from(hue, chroma, tone.toDouble()).toInt()))
                if (nearest(picker.tone) == tone) {
                    // The ring takes the ink of the far end of the row, so it reads on the stop.
                    val inkTone = if (tone >= MidTone) 0.0 else StopTones.last().toDouble()
                    val ink = Color(Hct.from(hue, chroma, inkTone).toInt())
                    val width = ring.toPx()
                    drawRect(
                        color = ink,
                        topLeft = Offset(width / 2, width / 2),
                        size = size.copy(width = size.width - width, height = size.height - width),
                        style = Stroke(width),
                    )
                }
            },
    )
}

/**
 * The stop nearest [tone], ties going up.
 */
private fun nearest(tone: Double): Int = ((tone / StopGap).roundToInt() * StopGap).roundToInt()

private const val StopGap: Double = 10.0

/**
 * Tone 0 to 100 by [StopGap].
 */
private val StopTones: List<Int> = (0..100 step StopGap.toInt()).toList()

/**
 * From this tone up a stop is light, so its ring is inked dark.
 */
private const val MidTone: Int = 50

/**
 * Half the caret's width, as a share of its height.
 */
private const val CaretHalfWidth: Float = 0.75f
