package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.control.stateName
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.picker_plane
import com.materialkolor.builder.kit.generated.resources.picker_plane_span
import com.materialkolor.builder.kit.generated.resources.picker_plane_value
import com.materialkolor.builder.kit.generated.resources.picker_tone_stop
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.hct.Hct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Every chroma, across, and every tone, up, at the picker's current hue, with a puck on the color.
 *
 * The plane spans the widest chroma the hue reaches, so the sRGB shape fills it at every hue, and a
 * corner label says how far it goes. The sRGB edge is a line and what lies past it is hatched. A
 * dashed line marks the tone.
 *
 * Dragging or tapping anywhere sets chroma and tone together, reporting [EditPhase.Dragging] as it
 * moves and [EditPhase.Released] once when it lets go. The picture, the puck and the line are read
 * while drawing, so a drag composes nothing. Up and Down move the tone, Left and Right the chroma
 * (mirrored right to left), one a press and ten with Shift, and Home turns the color grey. It reads
 * out as a slider named "Chroma and tone" with a value like "Chroma 58, tone 56", folded into the
 * name on the web the way [GamutTrack] folds its value.
 */
@Composable
internal fun GamutPlane(
    picker: PickerState,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pictures = remember { PlanePictures() }
    LaunchedEffect(picker, pictures) { pictures.follow { picker.hue } }
    val chroma = picker.shownOf(HctChannel.Chroma)
    val tone = picker.shownOf(HctChannel.Tone)
    val valueText = stringResource(Res.string.picker_plane_value, chroma, tone)
    val name =
        stateName(stringResource(Res.string.picker_plane), ControlState.Value(valueText), role = FoldedRole.Slider)
    val asText = LocalFoldsStateIntoName.current
    val interactions = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(tokens.radius.medium)
    Box(
        modifier
            .controlRing(interactions, shape)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val move = planeKey(event.key, event.isShiftPressed, isRtl) ?: return@onKeyEvent false
                move(picker)
                true
            }.focusable(interactionSource = interactions)
            .semantics {
                roleLessName(name, asText)
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = tone.toFloat(),
                    range = HctChannel.Tone.range.start
                        .toFloat()..HctChannel.Tone.range.endInclusive
                        .toFloat(),
                )
                setProgress { target -> picker.set(HctChannel.Tone, target.toDouble(), EditPhase.Discrete) }
            }.pointerInput(picker, pictures, isRtl) { dragPlane(picker, pictures, isRtl) }
            .drawWithCache {
                val drawing = planeDrawing(pictures.shown, tokens, isRtl)
                onDrawBehind { drawPlane(drawing, picker, pictures) }
            },
    ) {
        PlaneLabels(picker, pictures)
    }
}

/**
 * What a key does to the plane, or null for a key it leaves alone.
 */
private fun planeKey(
    key: Key,
    shift: Boolean,
    isRtl: Boolean,
): ((PickerState) -> Unit)? {
    val stride = if (shift) BigStep else 1.0
    val forward = if (isRtl) -stride else stride
    val chroma = HctChannel.Chroma
    val tone = HctChannel.Tone
    return when (key) {
        Key.DirectionUp -> { picker -> picker.set(tone, picker.tone + stride, EditPhase.Discrete) }
        Key.DirectionDown -> { picker -> picker.set(tone, picker.tone - stride, EditPhase.Discrete) }
        Key.DirectionRight -> { picker -> picker.set(chroma, picker.valueOf(chroma) + forward, EditPhase.Discrete) }
        Key.DirectionLeft -> { picker -> picker.set(chroma, picker.valueOf(chroma) - forward, EditPhase.Discrete) }
        Key.MoveHome -> { picker -> picker.set(chroma, 0.0, EditPhase.Discrete) }
        else -> null
    }
}

/**
 * Follows one pointer from press to release, the way a track does. The press moves the puck to it,
 * every move after that follows, and letting go reports [EditPhase.Released] once when anything
 * moved. A drag cut off before the pointer lifts ends without [EditPhase.Released].
 */
private suspend fun PointerInputScope.dragPlane(
    picker: PickerState,
    pictures: PlanePictures,
    isRtl: Boolean,
) {
    fun moveTo(
        position: Offset,
        phase: EditPhase,
    ): Boolean {
        val across = (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
        val up = 1f - (position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
        val chroma = (if (isRtl) 1f - across else across) * pictures.span(picker.hue)
        return picker.setChromaAndTone(chroma, HctChannel.Tone.valueAt(up), phase)
    }
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        picker.startDrag()
        var moved = false
        val lifted = try {
            moved = moveTo(down.position, EditPhase.Dragging)
            drag(down.id) { change ->
                change.consume()
                if (moveTo(change.position, EditPhase.Dragging)) moved = true
            }
        } catch (cancelled: CancellationException) {
            picker.endDrag(released = false)
            throw cancelled
        }
        picker.endDrag(released = moved && lifted)
    }
}

/**
 * The small labels at tone 100, tone 0 and the chroma the plane reaches. The span label reads the
 * picture shown, so it changes only with the hue.
 */
@Composable
private fun BoxScope.PlaneLabels(
    picker: PickerState,
    pictures: PlanePictures,
) {
    val span by remember(picker, pictures) { derivedStateOf { pictures.span(picker.hue).roundToInt() } }
    PlaneLabel(stringResource(Res.string.picker_tone_stop, ToneTop), Alignment.TopStart)
    PlaneLabel(stringResource(Res.string.picker_tone_stop, 0), Alignment.BottomStart)
    PlaneLabel(stringResource(Res.string.picker_plane_span, span), Alignment.BottomEnd)
}

@Composable
private fun BoxScope.PlaneLabel(
    text: String,
    alignment: Alignment,
) {
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    BuilderText(
        text = text.uppercase(),
        modifier = Modifier
            .align(alignment)
            .padding(spacing.medium)
            .clearAndSetSemantics {}
            .background(tokens.panel, RoundedCornerShape(tokens.radius.small))
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall / 2),
        style = BuilderTextStyle.Value,
        emphasis = Emphasis.Secondary,
        maxLines = 1,
    )
}

/**
 * The plane's sampled picture at one whole hue.
 *
 * @property[span] The chroma the plane reaches, the widest chroma of the hue with some room.
 * @property[edges] The most chroma sRGB shows at each whole tone, 0 to 100.
 * @property[image] [SamplesAcross] by [SamplesUp] colors, tone 100 at the top, clamped to the edge.
 */
internal class PlanePicture(
    val hue: Int,
    val span: Double,
    val edges: DoubleArray,
    val image: ImageBitmap,
)

/**
 * Samples the plane at [hue]. Takes a few milliseconds, so it runs away from the main thread.
 */
internal fun planePicture(hue: Int): PlanePicture {
    val edges = DoubleArray(ToneTop + 1) { tone -> GamutLimit.maxChroma(hue.toDouble(), tone.toDouble()) }
    val span = edges.max() * SpanRoom
    val image = ImageBitmap(SamplesAcross, SamplesUp)
    val canvas = Canvas(image)
    val paint = Paint()
    for (row in 0 until SamplesUp) {
        val tone = ToneTop * (1.0 - (row + 0.5) / SamplesUp)
        val edge = GamutLimit.maxChroma(hue.toDouble(), tone)
        for (column in 0 until SamplesAcross) {
            val chroma = min(span * (column + 0.5) / SamplesAcross, edge)
            paint.color = Color(Hct.from(hue.toDouble(), chroma, tone).toInt())
            canvas.drawRect(column.toFloat(), row.toFloat(), column + 1f, row + 1f, paint)
        }
    }
    return PlanePicture(hue, span, edges, image)
}

/**
 * The widest chroma the plane spans at [hue], the same as its picture's once that is ready.
 */
private fun planeSpan(hue: Double): Double {
    val whole = hue.roundToInt().mod(HueSlots).toDouble()
    return (0..ToneTop).maxOf { tone -> GamutLimit.maxChroma(whole, tone.toDouble()) } * SpanRoom
}

/**
 * The pictures of the last [KeptHues] whole hues, the most recently shown last. Only the main
 * thread reads and writes it.
 */
private object KeptPictures {
    private val pictures = LinkedHashMap<Int, PlanePicture>()

    fun take(hue: Int): PlanePicture? = pictures.remove(hue)?.also { picture -> pictures[hue] = picture }

    fun keep(picture: PlanePicture) {
        pictures[picture.hue] = picture
        while (pictures.size > KeptHues) pictures.remove(pictures.keys.first())
    }
}

/**
 * The picture a plane shows, which trails the hue. The last one stays up until the next is ready.
 */
@Stable
internal class PlanePictures {
    var shown: PlanePicture? by mutableStateOf(null)
        private set

    /**
     * The chroma the plane spans, from the picture shown, or at [hue] until there is one.
     */
    fun span(hue: Double): Double = shown?.span ?: planeSpan(hue)

    /**
     * Shows the picture of each whole hue [hue] passes through, building the ones not kept away from
     * the main thread. A hue that moves on before its picture is ready never gets one.
     */
    suspend fun follow(hue: () -> Double) {
        snapshotFlow { hue().roundToInt().mod(HueSlots) }.collectLatest { whole ->
            shown = KeptPictures.take(whole)
                ?: withContext(Dispatchers.Default) { planePicture(whole) }.also(KeptPictures::keep)
        }
    }
}

/**
 * What the plane draws that holds still while the puck moves, built once per size and picture.
 *
 * @property[past] The stretch past the sRGB edge, from the edge to the far side.
 * @property[edge] The sRGB edge itself, from tone 100 down to tone 0.
 */
private class PlaneDrawing(
    val tokens: BuilderTokens,
    val isRtl: Boolean,
    val picture: PlanePicture?,
    val outline: Path,
    val past: Path?,
    val edge: Path?,
    val hatchGap: Float,
    val line: Stroke,
    val guide: Stroke,
    val puckRadius: Float,
    val rim: Stroke,
    val outerRim: Stroke,
)

private fun CacheDrawScope.planeDrawing(
    picture: PlanePicture?,
    tokens: BuilderTokens,
    isRtl: Boolean,
): PlaneDrawing {
    val radius = tokens.radius.medium.toPx()
    val stroke = tokens.spacing.extraSmall.toPx() / 2
    var past: Path? = null
    var edge: Path? = null
    if (picture != null) {
        fun xOf(chroma: Double): Float {
            val along = (chroma / picture.span).toFloat().coerceIn(0f, 1f)
            return size.width * (if (isRtl) 1f - along else along)
        }
        val farX = if (isRtl) 0f else size.width
        edge = Path()
        past = Path().apply { moveTo(farX, 0f) }
        for (tone in ToneTop downTo 0) {
            val point = Offset(xOf(picture.edges[tone]), size.height * (1f - tone / ToneTop.toFloat()))
            if (tone == ToneTop) edge.moveTo(point.x, point.y) else edge.lineTo(point.x, point.y)
            past.lineTo(point.x, point.y)
        }
        past.lineTo(farX, size.height)
        past.close()
    }
    return PlaneDrawing(
        tokens = tokens,
        isRtl = isRtl,
        picture = picture,
        outline = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius))) },
        past = past,
        edge = edge,
        hatchGap = tokens.spacing.small.toPx(),
        line = Stroke(stroke),
        guide = Stroke(stroke / 2, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 3, stroke * 3))),
        puckRadius = tokens.spacing.large.toPx() - stroke,
        rim = Stroke(stroke * 1.5f),
        outerRim = Stroke(stroke),
    )
}

/**
 * Draws the picture, veils and hatches what lies past the edge, then the tone line and the puck,
 * which are read here so moving them only redraws.
 */
private fun DrawScope.drawPlane(
    drawing: PlaneDrawing,
    picker: PickerState,
    pictures: PlanePictures,
) {
    val tokens = drawing.tokens
    val lines = tokens.textMuted.copy(alpha = HatchAlpha)
    clipPath(drawing.outline) {
        drawRect(tokens.panelRaised)
        val picture = drawing.picture
        if (picture != null) {
            scale(if (drawing.isRtl) -1f else 1f, 1f) {
                drawImage(
                    image = picture.image,
                    srcSize = IntSize(picture.image.width, picture.image.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Low,
                )
            }
        }
        drawing.past?.let { past ->
            drawPath(past, tokens.panelRaised)
            clipPath(past) {
                var x = -size.height
                while (x < size.width) {
                    drawLine(lines, Offset(x, size.height), Offset(x + size.height, 0f), drawing.line.width / 2)
                    x += drawing.hatchGap
                }
            }
        }
        drawing.edge?.let { edge -> drawPath(edge, lines, style = drawing.line) }
    }

    val span = pictures.span(picker.hue)
    val along = (picker.valueOf(HctChannel.Chroma) / span).toFloat().coerceIn(0f, 1f)
    val y = size.height * (1f - HctChannel.Tone.fractionOf(picker.tone))
    val center = Offset(size.width * (if (drawing.isRtl) 1f - along else along), y)
    drawLine(
        tokens.panel,
        Offset(0f, y),
        Offset(size.width, y),
        drawing.guide.width,
        pathEffect = drawing.guide.pathEffect,
    )
    val radius = drawing.puckRadius
    drawCircle(tokens.textStrong, radius = radius + drawing.rim.width, center = center, style = drawing.outerRim)
    drawCircle(Color(picker.color.value), radius = radius, center = center)
    drawCircle(tokens.panel, radius = radius - drawing.rim.width / 2, center = center, style = drawing.rim)
}

/**
 * The top tone. The edge is drawn through every whole tone from here down to 0.
 */
private const val ToneTop: Int = 100

/**
 * How many samples the picture takes across, along chroma, and up, along tone.
 */
private const val SamplesAcross: Int = 64
private const val SamplesUp: Int = 44

/**
 * How many whole hues keep their picture.
 */
private const val KeptHues: Int = 48

/**
 * How far past the widest chroma of a hue the plane runs, so the sRGB shape never touches its side.
 */
private const val SpanRoom: Double = 1.06

private const val HueSlots: Int = 360

/**
 * How far Shift moves.
 */
private const val BigStep: Double = 10.0

/**
 * How strongly the hatching and the edge line show over the veil.
 */
private const val HatchAlpha: Float = 0.38f
